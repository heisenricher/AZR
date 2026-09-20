package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class HmacAlgorithm(val jvmName: String, val label: String) {
    HMAC_SHA256("HmacSHA256", "HMAC-SHA256 (256-bit)"),
    HMAC_SHA512("HmacSHA512", "HMAC-SHA512 (512-bit)"),
    HMAC_SHA1("HmacSHA1", "HMAC-SHA1 (160-bit)"),
    HMAC_MD5("HmacMD5", "HMAC-MD5 (128-bit)")
}

enum class KeyEncoding(val label: String) {
    UTF8_TEXT("Plain Text (UTF-8)"),
    HEX_STRING("Hexadecimal Bytes")
}

data class HmacInput(
    val message: String = "",
    val secretKey: String = "",
    val algorithm: HmacAlgorithm = HmacAlgorithm.HMAC_SHA256,
    val keyEncoding: KeyEncoding = KeyEncoding.UTF8_TEXT,
    val uppercaseHex: Boolean = false
)

data class HmacOutput(
    val hexSignature: String,
    val base64Signature: String,
    val algorithm: String,
    val keyBits: Int,
    val formattedReport: String,
    val summary: String
)

class HmacGeneratorTool : Tool<HmacInput, HmacOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "hmac_generator_tool",
        name = "HMAC Message Authentication Generator",
        description = "Calculate cryptographic HMAC-SHA256, HMAC-SHA512, and HMAC-SHA1 signatures with secret keys.",
        category = ToolCategory.SECURITY,
        tags = listOf("hmac", "sha256", "sha512", "hash", "secret", "signature", "auth", "token", "mac"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Security"
    )

    override suspend fun execute(input: HmacInput): ToolResult<HmacOutput> {
        val startTime = System.currentTimeMillis()

        if (input.message.isEmpty()) {
            return ToolResult.Failure(
                message = "Message input is empty.",
                userGuidance = "Provide data to sign with HMAC."
            )
        }
        if (input.secretKey.isEmpty()) {
            return ToolResult.Failure(
                message = "Secret key is empty.",
                userGuidance = "Provide a secret key to compute the HMAC signature."
            )
        }

        return try {
            val keyBytes = when (input.keyEncoding) {
                KeyEncoding.UTF8_TEXT -> input.secretKey.toByteArray(StandardCharsets.UTF_8)
                KeyEncoding.HEX_STRING -> {
                    val cleanHex = input.secretKey.replace(Regex("[^0-9a-fA-F]"), "")
                    if (cleanHex.length % 2 != 0) {
                        return ToolResult.Failure("Hex key must have an even number of characters.")
                    }
                    hexToBytes(cleanHex)
                }
            }

            val mac = Mac.getInstance(input.algorithm.jvmName)
            val secretKeySpec = SecretKeySpec(keyBytes, input.algorithm.jvmName)
            mac.init(secretKeySpec)

            val rawMessageBytes = input.message.toByteArray(StandardCharsets.UTF_8)
            val hmacBytes = mac.doFinal(rawMessageBytes)

            val rawHex = bytesToHex(hmacBytes)
            val hexResult = if (input.uppercaseHex) rawHex.uppercase(Locale.US) else rawHex.lowercase(Locale.US)
            val b64Result = Base64.getEncoder().encodeToString(hmacBytes)

            val report = buildString {
                appendLine("HMAC SIGNATURE REPORT")
                appendLine("--------------------------------")
                appendLine("Algorithm:         ${input.algorithm.label}")
                appendLine("Key Length:        ${keyBytes.size * 8} bits (${keyBytes.size} bytes)")
                appendLine("Key Encoding:      ${input.keyEncoding.label}")
                appendLine("Message Length:    ${rawMessageBytes.size} bytes")
                appendLine("--------------------------------")
                appendLine("Hex Signature:")
                appendLine(hexResult)
                appendLine()
                appendLine("Base64 Signature:")
                appendLine(b64Result)
            }

            val summary = "${input.algorithm.jvmName}: $hexResult"

            ToolResult.Success(
                data = HmacOutput(
                    hexSignature = hexResult,
                    base64Signature = b64Result,
                    algorithm = input.algorithm.jvmName,
                    keyBits = keyBytes.size * 8,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("HMAC calculation failed: ${e.message}", cause = e)
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = HEX_ARRAY[v ushr 4]
            hexChars[i * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    companion object {
        private val HEX_ARRAY = "0123456789abcdef".toCharArray()
    }
}
