package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.nio.ByteBuffer
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

data class TotpInput(
    val base32Secret: String = "JBSWY3DPEHPK3PXP",
    val timeStepSeconds: Int = 30,
    val digits: Int = 6,
    val customEpochSeconds: Long? = null
)

data class TotpOutput(
    val currentCode: String,
    val nextCode: String,
    val remainingSeconds: Int,
    val currentStepNumber: Long,
    val algorithm: String,
    val digits: Int,
    val formattedReport: String,
    val summary: String
)

class TotpGeneratorTool : Tool<TotpInput, TotpOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "totp_generator_tool",
        name = "Offline TOTP / 2FA Authenticator Code Generator",
        description = "RFC 6238 Time-based One-Time Password (TOTP) authenticator using Base32 keys and on-device HMAC-SHA1 computation.",
        category = ToolCategory.SECURITY,
        tags = listOf("totp", "hotp", "2fa", "mfa", "authenticator", "otp", "rfc6238", "security", "base32"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Security"
    )

    override suspend fun execute(input: TotpInput): ToolResult<TotpOutput> {
        val startTime = System.currentTimeMillis()
        val rawSecret = input.base32Secret.replace(" ", "").uppercase(Locale.ROOT)

        if (rawSecret.isEmpty()) {
            return ToolResult.Failure("Base32 secret key cannot be empty.")
        }

        val keyBytes = decodeBase32(rawSecret)
            ?: return ToolResult.Failure("Invalid Base32 secret string. Only characters A-Z and 2-7 are permitted.")

        val step = input.timeStepSeconds.coerceIn(15, 120)
        val digitCount = input.digits.coerceIn(6, 8)
        val epochSeconds = input.customEpochSeconds ?: (System.currentTimeMillis() / 1000L)

        val timeStep = epochSeconds / step
        val remaining = (step - (epochSeconds % step)).toInt()

        val currentCode = generateOtp(keyBytes, timeStep, digitCount)
        val nextCode = generateOtp(keyBytes, timeStep + 1, digitCount)

        val report = buildString {
            appendLine("OFFLINE 2FA AUTHENTICATOR (RFC 6238)")
            appendLine("--------------------------------------------------")
            appendLine("Current Code:     $currentCode (valid for $remaining seconds)")
            appendLine("Next Code:        $nextCode")
            appendLine("Time Step:        $step seconds (Step #$timeStep)")
            appendLine("Code Length:      $digitCount digits")
            appendLine("Algorithm:        HMAC-SHA1")
            appendLine("Secret (Masked):  ${rawSecret.take(4)}••••••••${rawSecret.takeLast(4)}")
        }

        val summary = "2FA Code: $currentCode ($remaining s left)"

        return ToolResult.Success(
            data = TotpOutput(
                currentCode = currentCode,
                nextCode = nextCode,
                remainingSeconds = remaining,
                currentStepNumber = timeStep,
                algorithm = "HMAC-SHA1",
                digits = digitCount,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun generateOtp(key: ByteArray, step: Long, digits: Int): String {
        val data = ByteBuffer.allocate(8).putLong(step).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "RAW"))
        val hash = mac.doFinal(data)

        val offset = (hash[hash.size - 1].toInt() and 0x0F)
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % (10.0.pow(digits.toDouble()).toInt())
        return otp.toString().padStart(digits, '0')
    }

    private fun decodeBase32(base32: String): ByteArray? {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = base32.trim().trimEnd('=').uppercase(Locale.ROOT)
        var buffer = 0
        var bitsLeft = 0
        val output = mutableListOf<Byte>()

        for (c in clean) {
            val v = base32Chars.indexOf(c)
            if (v < 0) return null
            buffer = (buffer shl 5) or v
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return output.toByteArray()
    }
}
