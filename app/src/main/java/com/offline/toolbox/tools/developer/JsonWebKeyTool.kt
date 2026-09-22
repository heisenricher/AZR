package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

data class JsonWebKeyInput(
    val jwkJson: String = """
        {
          "kty": "RSA",
          "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fTr4Cqhe07dXg-9_11Qz0Y31_79GzK-j80e791e84",
          "e": "AQAB",
          "alg": "RS256",
          "kid": "2026-offline-key-01",
          "use": "sig"
        }
    """.trimIndent()
)

data class JsonWebKeyOutput(
    val keyType: String,
    val thumbprintSha256: String,
    val thumbprintSha256Hex: String,
    val canonicalJson: String,
    val keyId: String?,
    val algorithm: String?,
    val keyUse: String?,
    val curveOrBitLength: String,
    val isValid: Boolean,
    val formattedReport: String,
    val summary: String
)

class JsonWebKeyTool : Tool<JsonWebKeyInput, JsonWebKeyOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "json_web_key_tool",
        name = "JWK Inspector & RFC 7638 Thumbprint Calculator",
        description = "Inspect JSON Web Keys (RSA, EC, OKP, oct) and calculate deterministic RFC 7638 SHA-256 thumbprints.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("jwk", "jwt", "rfc7638", "thumbprint", "rsa", "ec", "elliptic curve", "key", "cryptography", "oauth"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Key"
    )

    override suspend fun execute(input: JsonWebKeyInput): ToolResult<JsonWebKeyOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.jwkJson.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("JWK JSON content cannot be empty.")
        }

        val json: JSONObject
        try {
            json = JSONObject(raw)
        } catch (e: Exception) {
            return ToolResult.Failure("Invalid JSON format: ${e.message}")
        }

        val kty = json.optString("kty", "").trim()
        if (kty.isEmpty()) {
            return ToolResult.Failure("Missing mandatory 'kty' (Key Type) parameter in JWK.")
        }

        val kid = if (json.has("kid")) json.getString("kid") else null
        val alg = if (json.has("alg")) json.getString("alg") else null
        val use = if (json.has("use")) json.getString("use") else null

        val canonicalJson: String
        val curveOrBitLength: String

        when (kty.uppercase(Locale.US)) {
            "RSA" -> {
                val e = json.optString("e", "")
                val n = json.optString("n", "")
                if (e.isEmpty() || n.isEmpty()) {
                    return ToolResult.Failure("RSA JWK must contain both 'e' (exponent) and 'n' (modulus).")
                }
                // RFC 7638 Section 3.2: Lexicographic member order: e, kty, n
                canonicalJson = "{\"e\":\"$e\",\"kty\":\"RSA\",\"n\":\"$n\"}"
                val bitEst = try {
                    val decodedModulus = Base64.getUrlDecoder().decode(n)
                    decodedModulus.size * 8
                } catch (_: Exception) {
                    (n.length * 6 / 8) * 8
                }
                curveOrBitLength = "$bitEst bits (approx)"
            }
            "EC" -> {
                val crv = json.optString("crv", "")
                val x = json.optString("x", "")
                val y = json.optString("y", "")
                if (crv.isEmpty() || x.isEmpty() || y.isEmpty()) {
                    return ToolResult.Failure("EC JWK must contain 'crv', 'x', and 'y'.")
                }
                // RFC 7638 Section 3.2: Lexicographic member order: crv, kty, x, y
                canonicalJson = "{\"crv\":\"$crv\",\"kty\":\"EC\",\"x\":\"$x\",\"y\":\"$y\"}"
                curveOrBitLength = "Curve: $crv"
            }
            "OKP" -> {
                val crv = json.optString("crv", "")
                val x = json.optString("x", "")
                if (crv.isEmpty() || x.isEmpty()) {
                    return ToolResult.Failure("OKP (Edwards curve) JWK must contain 'crv' and 'x'.")
                }
                // RFC 7638 Section 3.2: crv, kty, x
                canonicalJson = "{\"crv\":\"$crv\",\"kty\":\"OKP\",\"x\":\"$x\"}"
                curveOrBitLength = "Edwards Curve: $crv"
            }
            "OCT" -> {
                val k = json.optString("k", "")
                if (k.isEmpty()) {
                    return ToolResult.Failure("Symmetric (oct) JWK must contain 'k' (key bytes).")
                }
                // RFC 7638 Section 3.2: k, kty
                canonicalJson = "{\"k\":\"$k\",\"kty\":\"oct\"}"
                val bitEst = try {
                    Base64.getUrlDecoder().decode(k).size * 8
                } catch (_: Exception) {
                    k.length * 6
                }
                curveOrBitLength = "$bitEst bits symmetric"
            }
            else -> {
                return ToolResult.Failure("Unsupported JWK key type: '$kty'. Supported types: RSA, EC, OKP, oct.")
            }
        }

        // Calculate RFC 7638 SHA-256 Digest
        val digestBytes = MessageDigest.getInstance("SHA-256").digest(canonicalJson.toByteArray(Charsets.UTF_8))
        val thumbprintB64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(digestBytes)
        val thumbprintHex = digestBytes.joinToString("") { String.format(Locale.US, "%02x", it) }

        val report = buildString {
            appendLine("RFC 7638 JSON WEB KEY (JWK) THUMBPRINT ANALYSIS")
            appendLine("--------------------------------------------------")
            appendLine("Key Type (kty):       $kty")
            appendLine("Details:              $curveOrBitLength")
            if (kid != null) appendLine("Key ID (kid):         $kid")
            if (alg != null) appendLine("Algorithm (alg):      $alg")
            if (use != null) appendLine("Key Usage (use):      $use")
            appendLine("--------------------------------------------------")
            appendLine("RFC 7638 Canonical JSON:")
            appendLine(canonicalJson)
            appendLine("--------------------------------------------------")
            appendLine("SHA-256 Thumbprint (Base64URL):")
            appendLine(thumbprintB64Url)
            appendLine("SHA-256 Thumbprint (Hex):")
            appendLine(thumbprintHex)
        }

        val output = JsonWebKeyOutput(
            keyType = kty,
            thumbprintSha256 = thumbprintB64Url,
            thumbprintSha256Hex = thumbprintHex,
            canonicalJson = canonicalJson,
            keyId = kid,
            algorithm = alg,
            keyUse = use,
            curveOrBitLength = curveOrBitLength,
            isValid = true,
            formattedReport = report,
            summary = "JWK ($kty): Thumbprint = $thumbprintB64Url"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Computed RFC 7638 thumbprint for JWK ($kty)"
        )
    }
}
