package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

data class JwtClaimSummary(
    val algorithm: String?,
    val tokenType: String?,
    val subject: String?,
    val issuer: String?,
    val audience: String?,
    val issuedAtFormatted: String?,
    val expirationFormatted: String?,
    val isExpired: Boolean?
)

data class JwtDecoderOutput(
    val formattedHeaderJson: String,
    val formattedPayloadJson: String,
    val signatureHex: String,
    val claims: JwtClaimSummary,
    val summary: String
)

class JwtDecoderTool : Tool<String, JwtDecoderOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "jwt_decoder",
        name = "JWT Decoder & Inspector",
        description = "Decode and inspect JSON Web Tokens (JWT) claims, headers, signatures, and expiration offline.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("jwt", "token", "decode", "json web token", "claims", "auth", "bearer", "inspect"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.JSON,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Key"
    )

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")

    override suspend fun execute(input: String): ToolResult<JwtDecoderOutput> {
        val startTime = System.currentTimeMillis()
        val rawToken = input.trim().removePrefix("Bearer ").trim()

        if (rawToken.isEmpty()) {
            return ToolResult.Failure(
                message = "Input token is empty.",
                userGuidance = "Paste a valid JWT string in header.payload.signature format."
            )
        }

        val parts = rawToken.split(".")
        if (parts.size < 2 || parts.size > 3) {
            return ToolResult.Failure(
                message = "Invalid JWT structure: found ${parts.size} segments (expected 3 separated by dots).",
                userGuidance = "A standard JWT consists of three Base64URL-encoded parts separated by dots: header.payload.signature."
            )
        }

        return try {
            val headerJson = decodeBase64Url(parts[0])
            val payloadJson = decodeBase64Url(parts[1])
            val signature = if (parts.size == 3) parts[2] else "(None)"

            val headerObj = JSONObject(headerJson)
            val payloadObj = JSONObject(payloadJson)

            val algo = if (headerObj.has("alg")) headerObj.optString("alg") else null
            val typ = if (headerObj.has("typ")) headerObj.optString("typ") else null
            val sub = if (payloadObj.has("sub")) payloadObj.optString("sub") else null
            val iss = if (payloadObj.has("iss")) payloadObj.optString("iss") else null
            val aud = if (payloadObj.has("aud")) payloadObj.optString("aud") else null

            val iatLong = if (payloadObj.has("iat")) payloadObj.optLong("iat") else null
            val expLong = if (payloadObj.has("exp")) payloadObj.optLong("exp") else null

            val iatFormatted = iatLong?.let { formatEpoch(it) }
            val expFormatted = expLong?.let { formatEpoch(it) }
            val isExpired = expLong?.let { it * 1000 < System.currentTimeMillis() }

            val statusText = when (isExpired) {
                true -> "EXPIRED"
                false -> "ACTIVE"
                null -> "No expiration set"
            }

            val summary = "JWT: $statusText • Alg: ${algo ?: "Unknown"}"

            ToolResult.Success(
                data = JwtDecoderOutput(
                    formattedHeaderJson = headerObj.toString(2),
                    formattedPayloadJson = payloadObj.toString(2),
                    signatureHex = signature,
                    claims = JwtClaimSummary(
                        algorithm = algo,
                        tokenType = typ,
                        subject = sub,
                        issuer = iss,
                        audience = aud,
                        issuedAtFormatted = iatFormatted,
                        expirationFormatted = expFormatted,
                        isExpired = isExpired
                    ),
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Failed to decode JWT: ${e.message}",
                userGuidance = "Ensure the token is properly Base64URL encoded and contains valid JSON in header and payload.",
                cause = e
            )
        }
    }

    private fun decodeBase64Url(base64Url: String): String {
        var base64 = base64Url.replace('-', '+').replace('_', '/')
        while (base64.length % 4 != 0) {
            base64 += "="
        }
        val bytes = Base64.getDecoder().decode(base64)
        return String(bytes, Charsets.UTF_8)
    }

    private fun formatEpoch(epochSec: Long): String {
        return Instant.ofEpochSecond(epochSec)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }
}
