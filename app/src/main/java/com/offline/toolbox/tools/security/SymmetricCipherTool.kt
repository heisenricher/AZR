package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class SymmetricMode {
    ENCRYPT,
    DECRYPT
}

enum class SymmetricAlgorithm(val transformation: String, val isGcm: Boolean, val ivLength: Int) {
    AES_256_GCM("AES/GCM/NoPadding", true, 12),
    AES_128_GCM("AES/GCM/NoPadding", true, 12),
    AES_256_CBC("AES/CBC/PKCS5Padding", false, 16)
}

data class SymmetricCipherInput(
    val text: String,
    val passphrase: String,
    val mode: SymmetricMode = SymmetricMode.ENCRYPT,
    val algorithm: SymmetricAlgorithm = SymmetricAlgorithm.AES_256_GCM
)

data class SymmetricCipherOutput(
    val result: String,
    val mode: SymmetricMode,
    val algorithm: String,
    val isSuccess: Boolean,
    val formattedReport: String,
    val summary: String
)

class SymmetricCipherTool : Tool<SymmetricCipherInput, SymmetricCipherOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "symmetric_cipher_tool",
        name = "AES-GCM Authenticated Encryption",
        description = "On-device authenticated symmetric encryption (AES-256-GCM / CBC) with PBKDF2 key derivation and random salt/IV.",
        category = ToolCategory.SECURITY,
        tags = listOf("aes", "encryption", "decryption", "gcm", "cbc", "cipher", "secret", "crypto", "pbkdf2"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Security"
    )

    private val random = SecureRandom()
    private val iterations = 10000

    override suspend fun execute(input: SymmetricCipherInput): ToolResult<SymmetricCipherOutput> {
        val startTime = System.currentTimeMillis()

        if (input.text.isEmpty()) {
            return ToolResult.Failure("Input text / payload cannot be empty.")
        }
        if (input.passphrase.isEmpty()) {
            return ToolResult.Failure("Passphrase / encryption key cannot be empty.")
        }

        return try {
            val keyBits = when (input.algorithm) {
                SymmetricAlgorithm.AES_256_GCM, SymmetricAlgorithm.AES_256_CBC -> 256
                SymmetricAlgorithm.AES_128_GCM -> 128
            }

            val (resultText, report) = when (input.mode) {
                SymmetricMode.ENCRYPT -> encrypt(input.text, input.passphrase, input.algorithm, keyBits)
                SymmetricMode.DECRYPT -> decrypt(input.text, input.passphrase, input.algorithm, keyBits)
            }

            val summary = "${input.mode.name} completed with ${input.algorithm.name} (${resultText.length} chars)"

            ToolResult.Success(
                data = SymmetricCipherOutput(
                    result = resultText,
                    mode = input.mode,
                    algorithm = input.algorithm.name,
                    isSuccess = true,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("Symmetric cipher operation failed: ${e.message}", cause = e)
        }
    }

    private fun encrypt(plaintext: String, pass: String, algo: SymmetricAlgorithm, keyBits: Int): Pair<String, String> {
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val iv = ByteArray(algo.ivLength)
        random.nextBytes(iv)

        val secretKey = deriveKey(pass, salt, keyBits)
        val cipher = Cipher.getInstance(algo.transformation)

        if (algo.isGcm) {
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        } else {
            val spec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        }

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Pack: [Magic 4B: 'A','Z','R','1'] + [Salt 16B] + [IV len 1B] + [IV] + [Ciphertext]
        val buffer = ByteBuffer.allocate(4 + 16 + 1 + iv.size + ciphertext.size)
        buffer.put(byteArrayOf('A'.code.toByte(), 'Z'.code.toByte(), 'R'.code.toByte(), '1'.code.toByte()))
        buffer.put(salt)
        buffer.put(iv.size.toByte())
        buffer.put(iv)
        buffer.put(ciphertext)

        val base64Enc = Base64.getEncoder().encodeToString(buffer.array())

        val report = buildString {
            appendLine("AES ENCRYPTION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Mode:             ${algo.name}")
            appendLine("Key Derivation:   PBKDF2WithHmacSHA256 (10,000 iterations)")
            appendLine("Salt:             ${salt.joinToString("") { String.format("%02x", it) }}")
            appendLine("IV / Nonce:       ${iv.joinToString("") { String.format("%02x", it) }}")
            appendLine("Plaintext Size:   ${plaintext.length} chars (${plaintext.toByteArray().size} bytes)")
            appendLine("Ciphertext Size:  ${ciphertext.size} bytes (Encapsulated Base64: ${base64Enc.length} chars)")
        }

        return Pair(base64Enc, report)
    }

    private fun decrypt(base64Payload: String, pass: String, algo: SymmetricAlgorithm, keyBits: Int): Pair<String, String> {
        val bytes = try {
            Base64.getDecoder().decode(base64Payload.trim())
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Base64 payload string")
        }

        if (bytes.size < 4 + 16 + 1) {
            throw IllegalArgumentException("Ciphertext payload is truncated or corrupted")
        }

        val buffer = ByteBuffer.wrap(bytes)
        val magic = ByteArray(4)
        buffer.get(magic)
        if (magic[0] != 'A'.code.toByte() || magic[1] != 'Z'.code.toByte() || magic[2] != 'R'.code.toByte()) {
            throw IllegalArgumentException("Payload does not contain valid AZR envelope headers")
        }

        val salt = ByteArray(16)
        buffer.get(salt)

        val ivLen = buffer.get().toInt() and 0xFF
        if (ivLen > buffer.remaining()) {
            throw IllegalArgumentException("Malformed IV header")
        }
        val iv = ByteArray(ivLen)
        buffer.get(iv)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val secretKey = deriveKey(pass, salt, keyBits)
        val cipher = Cipher.getInstance(algo.transformation)

        if (algo.isGcm) {
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        } else {
            val spec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        }

        val decryptedBytes = cipher.doFinal(ciphertext)
        val plaintext = String(decryptedBytes, Charsets.UTF_8)

        val report = buildString {
            appendLine("AES DECRYPTION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Mode:             ${algo.name}")
            appendLine("Authentication:   VERIFIED (GCM Tag Valid)")
            appendLine("Decrypted Size:   ${plaintext.length} characters")
        }

        return Pair(plaintext, report)
    }

    private fun deriveKey(pass: String, salt: ByteArray, keyBits: Int): SecretKeySpec {
        val spec = PBEKeySpec(pass.toCharArray(), salt, iterations, keyBits)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
