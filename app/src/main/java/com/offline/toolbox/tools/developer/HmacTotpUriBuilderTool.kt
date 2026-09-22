package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

enum class TotpUriOperationMode(val displayName: String) {
    BUILD_KEY_URI("Build Key URI (Parameters → otpauth://)"),
    PARSE_KEY_URI("Parse & Validate URI (otpauth:// → Parameters)")
}

data class HmacTotpUriInput(
    val mode: TotpUriOperationMode = TotpUriOperationMode.BUILD_KEY_URI,
    // Build parameters:
    val issuer: String = "AZR Offline",
    val accountName: String = "user@example.com",
    val base32Secret: String = "JBSWY3DPEHPK3PXP", // Standard RFC 3548 Base32 secret
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val periodSeconds: Int = 30,
    // Parse parameter:
    val rawUri: String = "otpauth://totp/AZR%20Offline:user%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=AZR%20Offline&algorithm=SHA1&digits=6&period=30"
)

data class HmacTotpUriOutput(
    val operationMode: String,
    val fullUri: String,
    val type: String, // TOTP or HOTP
    val issuer: String,
    val accountName: String,
    val base32Secret: String,
    val secretBitLength: Int,
    val algorithm: String,
    val digits: Int,
    val periodOrCounter: Int,
    val isValidBase32: Boolean,
    val formattedReport: String,
    val summary: String
)

class HmacTotpUriBuilderTool : Tool<HmacTotpUriInput, HmacTotpUriOutput> {
    companion object {
        private val BASE32_REGEX = Regex("^[A-Z2-7]+=*$")
    }

    override val metadata: ToolMetadata = ToolMetadata(
        id = "hmac_totp_uri_builder_tool",
        name = "TOTP Key URI (otpauth://) Builder & Inspector",
        description = "Construct, validate, and parse Google Authenticator compatible Key URIs (otpauth://totp/) with Base32 secret validation.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("totp", "otpauth", "2fa", "authenticator", "uri", "rfc6238", "qr code", "security", "base32"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Key"
    )

    override suspend fun execute(input: HmacTotpUriInput): ToolResult<HmacTotpUriOutput> {
        val startTime = System.currentTimeMillis()

        return if (input.mode == TotpUriOperationMode.BUILD_KEY_URI) {
            buildUri(input, startTime)
        } else {
            parseUri(input.rawUri.trim(), startTime)
        }
    }

    private fun buildUri(input: HmacTotpUriInput, startTime: Long): ToolResult<HmacTotpUriOutput> {
        val issuer = input.issuer.trim()
        val account = input.accountName.trim()
        val cleanSecret = input.base32Secret.trim().replace(" ", "").uppercase(Locale.US)

        if (cleanSecret.isEmpty()) {
            return ToolResult.Failure("Base32 secret key cannot be empty.")
        }

        val isValidB32 = BASE32_REGEX.matches(cleanSecret)
        if (!isValidB32) {
            return ToolResult.Failure("Invalid Base32 secret key. Only characters A-Z and 2-7 are permitted in RFC 4648 Base32.")
        }

        val label = if (issuer.isNotEmpty()) {
            URLEncoder.encode(issuer, "UTF-8") + ":" + URLEncoder.encode(account, "UTF-8")
        } else {
            URLEncoder.encode(account, "UTF-8")
        }

        val queryParams = mutableListOf<String>()
        queryParams.add("secret=$cleanSecret")
        if (issuer.isNotEmpty()) {
            queryParams.add("issuer=${URLEncoder.encode(issuer, "UTF-8")}")
        }
        val alg = input.algorithm.uppercase(Locale.US)
        if (alg != "SHA1") {
            queryParams.add("algorithm=$alg")
        }
        val digits = input.digits.coerceIn(6, 8)
        if (digits != 6) {
            queryParams.add("digits=$digits")
        }
        val period = input.periodSeconds.coerceIn(10, 300)
        if (period != 30) {
            queryParams.add("period=$period")
        }

        val uri = "otpauth://totp/$label?" + queryParams.joinToString("&")
        val secretBits = cleanSecret.filter { it != '=' }.length * 5

        val report = buildString {
            appendLine("OTPAUTH 2FA KEY URI GENERATION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Full URI (QR Payload):")
            appendLine(uri)
            appendLine("--------------------------------------------------")
            appendLine("PARAMETERS:")
            appendLine(" • Type:             TOTP (Time-based)")
            appendLine(" • Issuer:           $issuer")
            appendLine(" • Account:          $account")
            appendLine(" • Base32 Secret:    $cleanSecret ($secretBits bits)")
            appendLine(" • Algorithm:        $alg")
            appendLine(" • Digits:           $digits digits")
            appendLine(" • Period:           $period seconds")
            appendLine(" • Compatibility:    Google Authenticator, Aegis, 1Password, Bitwarden")
        }

        val output = HmacTotpUriOutput(
            operationMode = input.mode.name,
            fullUri = uri,
            type = "TOTP",
            issuer = issuer,
            accountName = account,
            base32Secret = cleanSecret,
            secretBitLength = secretBits,
            algorithm = alg,
            digits = digits,
            periodOrCounter = period,
            isValidBase32 = true,
            formattedReport = report,
            summary = "URI Built: $uri"
        )

        return ToolResult.Success(output, System.currentTimeMillis() - startTime, "Constructed TOTP Key URI")
    }

    private fun parseUri(rawUri: String, startTime: Long): ToolResult<HmacTotpUriOutput> {
        if (!rawUri.startsWith("otpauth://", ignoreCase = true)) {
            return ToolResult.Failure("Invalid Key URI scheme. Expected prefix 'otpauth://'.")
        }

        val withoutScheme = rawUri.substring("otpauth://".length)
        val slashIdx = withoutScheme.indexOf('/')
        if (slashIdx == -1) {
            return ToolResult.Failure("Invalid Key URI format: missing OTP type (totp/hotp).")
        }

        val type = withoutScheme.substring(0, slashIdx).uppercase(Locale.US)
        val remainder = withoutScheme.substring(slashIdx + 1)

        val qIdx = remainder.indexOf('?')
        val labelPart = if (qIdx != -1) remainder.substring(0, qIdx) else remainder
        val queryPart = if (qIdx != -1) remainder.substring(qIdx + 1) else ""

        val decodedLabel = URLDecoder.decode(labelPart, "UTF-8")
        val labelTokens = decodedLabel.split(":", limit = 2)
        val issuerFromLabel = if (labelTokens.size > 1) labelTokens[0] else ""
        val account = if (labelTokens.size > 1) labelTokens[1] else labelTokens[0]

        val params = mutableMapOf<String, String>()
        if (queryPart.isNotEmpty()) {
            queryPart.split("&").forEach { pair ->
                val kv = pair.split("=", limit = 2)
                if (kv.size == 2) {
                    params[kv[0].lowercase(Locale.US)] = URLDecoder.decode(kv[1], "UTF-8")
                }
            }
        }

        val secret = params["secret"]?.trim()?.replace(" ", "")?.uppercase(Locale.US) ?: ""
        if (secret.isEmpty()) {
            return ToolResult.Failure("Missing mandatory 'secret' query parameter in Key URI.")
        }

        val isValidB32 = BASE32_REGEX.matches(secret)
        val issuer = params["issuer"] ?: issuerFromLabel
        val algorithm = params["algorithm"]?.uppercase(Locale.US) ?: "SHA1"
        val digits = params["digits"]?.toIntOrNull() ?: 6
        val period = params["period"]?.toIntOrNull() ?: 30
        val secretBits = secret.filter { it != '=' }.length * 5

        val report = buildString {
            appendLine("OTPAUTH 2FA KEY URI DECODING REPORT")
            appendLine("--------------------------------------------------")
            appendLine("OTP Type:            $type")
            appendLine("Issuer:              ${if (issuer.isNotEmpty()) issuer else "(None)"}")
            appendLine("Account:             ${if (account.isNotEmpty()) account else "(None)"}")
            appendLine("Base32 Secret:       $secret")
            appendLine("Base32 Valid:        ${if (isValidB32) "YES" else "NO (Invalid RFC 4648 characters detected)"}")
            appendLine("Secret Bit Length:   $secretBits bits")
            appendLine("Digest Algorithm:    $algorithm")
            appendLine("Token Digits:        $digits")
            appendLine("Period / Step:       $period seconds")
        }

        val output = HmacTotpUriOutput(
            operationMode = "PARSE_KEY_URI",
            fullUri = rawUri,
            type = type,
            issuer = issuer,
            accountName = account,
            base32Secret = secret,
            secretBitLength = secretBits,
            algorithm = algorithm,
            digits = digits,
            periodOrCounter = period,
            isValidBase32 = isValidB32,
            formattedReport = report,
            summary = "Parsed $type ($issuer:$account) Secret: ${secret.take(4)}... ($secretBits bits)"
        )

        return ToolResult.Success(output, System.currentTimeMillis() - startTime, "Parsed TOTP Key URI")
    }
}
