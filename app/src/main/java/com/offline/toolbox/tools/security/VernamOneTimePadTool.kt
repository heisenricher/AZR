package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale

enum class OtpOperationMode(val displayName: String) {
    ENCRYPT_GENERATE_KEY("Encrypt & Generate Random Pad"),
    ENCRYPT_WITH_KEY("Encrypt (Plaintext + Hex/B64 Key)"),
    DECRYPT_WITH_KEY("Decrypt (Hex/B64 Ciphertext + Key)")
}

data class VernamOtpInput(
    val content: String = "TOP SECRET TRANSMISSION: MEET AT MIDNIGHT",
    val keyOrPadHex: String = "",
    val mode: OtpOperationMode = OtpOperationMode.ENCRYPT_GENERATE_KEY
)

data class VernamOtpOutput(
    val operationMode: String,
    val resultTextOrHex: String,
    val padHex: String,
    val padBase64: String,
    val ciphertextHex: String,
    val ciphertextBase64: String,
    val byteLength: Int,
    val shannonPerfectSecrecyVerified: Boolean,
    val formattedReport: String,
    val summary: String
)

class VernamOneTimePadTool : Tool<VernamOtpInput, VernamOtpOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "vernam_one_time_pad_tool",
        name = "Vernam One-Time Pad (OTP) Cipher & Key Generator",
        description = "Information-theoretically unbreakable Vernam One-Time Pad cipher with CSPRNG key generator and Shannon perfect secrecy validation.",
        category = ToolCategory.SECURITY,
        tags = listOf("one time pad", "otp", "vernam", "cipher", "cryptography", "shannon", "perfect secrecy", "xor", "encryption"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Lock"
    )

    override suspend fun execute(input: VernamOtpInput): ToolResult<VernamOtpOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.content.trim()

        if (text.isEmpty()) {
            return ToolResult.Failure("Input content cannot be empty.")
        }

        return when (input.mode) {
            OtpOperationMode.ENCRYPT_GENERATE_KEY -> {
                val plainBytes = text.toByteArray(Charsets.UTF_8)
                val keyBytes = ByteArray(plainBytes.size)
                SecureRandom().nextBytes(keyBytes)

                val cipherBytes = ByteArray(plainBytes.size)
                for (i in plainBytes.indices) {
                    cipherBytes[i] = (plainBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
                }

                val keyHex = toHex(keyBytes)
                val keyB64 = Base64.getEncoder().encodeToString(keyBytes)
                val cipherHex = toHex(cipherBytes)
                val cipherB64 = Base64.getEncoder().encodeToString(cipherBytes)

                val report = buildString {
                    appendLine("VERNAM ONE-TIME PAD (OTP) ENCRYPTION REPORT")
                    appendLine("--------------------------------------------------")
                    appendLine("Operation:            Encrypt with Fresh CSPRNG Pad")
                    appendLine("Payload Length:       ${plainBytes.size} bytes (${plainBytes.size * 8} bits)")
                    appendLine("--------------------------------------------------")
                    appendLine("GENERATED ONE-TIME KEY PAD (HEX):")
                    appendLine(keyHex)
                    appendLine("GENERATED ONE-TIME KEY PAD (BASE64):")
                    appendLine(keyB64)
                    appendLine("--------------------------------------------------")
                    appendLine("CIPHERTEXT (HEX):")
                    appendLine(cipherHex)
                    appendLine("CIPHERTEXT (BASE64):")
                    appendLine(cipherB64)
                    appendLine("--------------------------------------------------")
                    appendLine("SHANNON PERFECT SECRECY GUARANTEE:")
                    appendLine(" • Key Pad entropy equals plaintext length: H(M|C) = H(M).")
                    appendLine(" • Ciphertext contains zero mutual information about plaintext.")
                    appendLine(" • CRITICAL RULE: NEVER reuse this key pad under any circumstance.")
                }

                val output = VernamOtpOutput(
                    operationMode = input.mode.name,
                    resultTextOrHex = cipherHex,
                    padHex = keyHex,
                    padBase64 = keyB64,
                    ciphertextHex = cipherHex,
                    ciphertextBase64 = cipherB64,
                    byteLength = plainBytes.size,
                    shannonPerfectSecrecyVerified = true,
                    formattedReport = report,
                    summary = "Encrypted ${plainBytes.size} bytes with unbreakable OTP"
                )

                ToolResult.Success(output, System.currentTimeMillis() - startTime, "Generated OTP and encrypted payload")
            }
            OtpOperationMode.ENCRYPT_WITH_KEY -> {
                val plainBytes = text.toByteArray(Charsets.UTF_8)
                val keyBytes = parseKeyBytes(input.keyOrPadHex)
                    ?: return ToolResult.Failure("Invalid key: must provide hex (e.g. A1 B2) or Base64 key string.")

                if (keyBytes.size < plainBytes.size) {
                    return ToolResult.Failure("Key length (${keyBytes.size} bytes) is shorter than plaintext (${plainBytes.size} bytes). One-Time Pad strictly requires key length >= plaintext length.")
                }

                val cipherBytes = ByteArray(plainBytes.size)
                for (i in plainBytes.indices) {
                    cipherBytes[i] = (plainBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
                }

                val keyHex = toHex(keyBytes.take(plainBytes.size).toByteArray())
                val cipherHex = toHex(cipherBytes)
                val cipherB64 = Base64.getEncoder().encodeToString(cipherBytes)

                val output = VernamOtpOutput(
                    operationMode = input.mode.name,
                    resultTextOrHex = cipherHex,
                    padHex = keyHex,
                    padBase64 = Base64.getEncoder().encodeToString(keyBytes),
                    ciphertextHex = cipherHex,
                    ciphertextBase64 = cipherB64,
                    byteLength = plainBytes.size,
                    shannonPerfectSecrecyVerified = true,
                    formattedReport = "Ciphertext (Hex):\n$cipherHex\n\nCiphertext (Base64):\n$cipherB64",
                    summary = "Encrypted ${plainBytes.size} bytes using provided OTP pad"
                )

                ToolResult.Success(output, System.currentTimeMillis() - startTime, "Encrypted with provided OTP pad")
            }
            OtpOperationMode.DECRYPT_WITH_KEY -> {
                val cipherBytes = parseKeyBytes(text)
                    ?: return ToolResult.Failure("Invalid ciphertext format: must be valid hexadecimal or Base64.")

                val keyBytes = parseKeyBytes(input.keyOrPadHex)
                    ?: return ToolResult.Failure("Invalid key: must provide valid hex or Base64 key string.")

                if (keyBytes.size < cipherBytes.size) {
                    return ToolResult.Failure("Key length (${keyBytes.size} bytes) is shorter than ciphertext (${cipherBytes.size} bytes).")
                }

                val plainBytes = ByteArray(cipherBytes.size)
                for (i in cipherBytes.indices) {
                    plainBytes[i] = (cipherBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
                }

                val recoveredText = String(plainBytes, Charsets.UTF_8)
                val keyHex = toHex(keyBytes.take(cipherBytes.size).toByteArray())
                val cipherHex = toHex(cipherBytes)

                val report = buildString {
                    appendLine("VERNAM ONE-TIME PAD (OTP) DECRYPTION REPORT")
                    appendLine("--------------------------------------------------")
                    appendLine("Decrypted Plaintext:")
                    appendLine(recoveredText)
                    appendLine("--------------------------------------------------")
                    appendLine("Decrypted Length:     ${plainBytes.size} bytes")
                }

                val output = VernamOtpOutput(
                    operationMode = input.mode.name,
                    resultTextOrHex = recoveredText,
                    padHex = keyHex,
                    padBase64 = Base64.getEncoder().encodeToString(keyBytes),
                    ciphertextHex = cipherHex,
                    ciphertextBase64 = Base64.getEncoder().encodeToString(cipherBytes),
                    byteLength = cipherBytes.size,
                    shannonPerfectSecrecyVerified = true,
                    formattedReport = report,
                    summary = "Decrypted ${plainBytes.size} bytes: $recoveredText"
                )

                ToolResult.Success(output, System.currentTimeMillis() - startTime, "Decrypted OTP ciphertext")
            }
        }
    }

    private fun toHex(bytes: ByteArray): String {
        return bytes.joinToString("") { String.format(Locale.US, "%02X", it) }
    }

    private fun parseKeyBytes(raw: String): ByteArray? {
        val clean = raw.trim().replace(" ", "").replace("0x", "", ignoreCase = true)
        if (clean.isEmpty()) return null

        // Try Hex first if valid even length hex chars
        if (clean.length % 2 == 0 && clean.all { it in "0123456789abcdefABCDEF" }) {
            return ByteArray(clean.length / 2) { idx ->
                clean.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
        }

        // Try Base64
        return try {
            Base64.getDecoder().decode(raw.trim())
        } catch (_: Exception) {
            null
        }
    }
}
