package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.io.ByteArrayOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class HkdfHashAlgorithm(val hmacName: String, val hashLen: Int) {
    SHA256("HmacSHA256", 32),
    SHA512("HmacSHA512", 64)
}

data class HkdfInput(
    val ikm: String = "Master-Input-Keying-Material-2026",
    val salt: String = "CryptographicSaltValue",
    val info: String = "AZR-App-Subkey-Encryption",
    val outputKeyLengthBytes: Int = 32,
    val algorithm: HkdfHashAlgorithm = HkdfHashAlgorithm.SHA256
)

data class HkdfOutput(
    val derivedKeyHex: String,
    val pseudorandomKeyHex: String,
    val keyLengthBytes: Int,
    val algorithm: HkdfHashAlgorithm,
    val formattedReport: String,
    val summary: String
)

class HkdfKeyDerivationTool : Tool<HkdfInput, HkdfOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "hkdf_key_derivation_tool",
        name = "HKDF Key Derivation Function (RFC 5869)",
        description = "Derive cryptographically strong subkeys from master keying material using RFC 5869 HMAC-Extract and HMAC-Expand.",
        category = ToolCategory.SECURITY,
        tags = listOf("hkdf", "kdf", "cryptography", "rfc5869", "hmac", "sha256", "sha512", "subkey", "derivation", "security"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "VpnKey"
    )

    override suspend fun execute(input: HkdfInput): ToolResult<HkdfOutput> {
        val startTime = System.currentTimeMillis()
        val ikmBytes = input.ikm.toByteArray(Charsets.UTF_8)

        if (ikmBytes.isEmpty()) {
            return ToolResult.Failure("Input Keying Material (IKM) cannot be empty.")
        }

        val outLen = input.outputKeyLengthBytes.coerceIn(16, 256)
        val hashLen = input.algorithm.hashLen
        if (outLen > 255 * hashLen) {
            return ToolResult.Failure("Requested output length ($outLen) exceeds RFC 5869 maximum limit (${255 * hashLen} bytes).")
        }

        // Step 1: Extract -> PRK = HMAC-Hash(salt, IKM)
        val saltBytes = if (input.salt.isNotEmpty()) input.salt.toByteArray(Charsets.UTF_8) else ByteArray(hashLen)
        val mac = Mac.getInstance(input.algorithm.hmacName)
        mac.init(SecretKeySpec(saltBytes, input.algorithm.hmacName))
        val prk = mac.doFinal(ikmBytes)

        // Step 2: Expand -> OKM = T(1) || T(2) || ...
        val infoBytes = input.info.toByteArray(Charsets.UTF_8)
        val n = (outLen + hashLen - 1) / hashLen
        val okmStream = ByteArrayOutputStream()

        var prevT = ByteArray(0)
        for (i in 1..n) {
            mac.init(SecretKeySpec(prk, input.algorithm.hmacName))
            mac.update(prevT)
            mac.update(infoBytes)
            mac.update(i.toByte())
            prevT = mac.doFinal()
            okmStream.write(prevT)
        }

        val okm = okmStream.toByteArray().copyOf(outLen)

        val prkHex = prk.joinToString("") { "%02x".format(it) }
        val okmHex = okm.joinToString("") { "%02x".format(it) }

        val report = buildString {
            appendLine("RFC 5869 HKDF KEY DERIVATION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Algorithm:           ${input.algorithm.name} (${input.algorithm.hmacName})")
            appendLine("Hash Length:         $hashLen bytes")
            appendLine("Derived Key Length:  $outLen bytes (${outLen * 8} bits)")
            appendLine("Context Info String: \"${input.info}\"")
            appendLine()
            appendLine("PSEUDORANDOM KEY (PRK):")
            appendLine(prkHex)
            appendLine()
            appendLine("OUTPUT KEYING MATERIAL (OKM):")
            appendLine(okmHex)
        }

        val summary = "HKDF-${input.algorithm.name}: $outLen bytes (${okmHex.take(16)}...)"

        return ToolResult.Success(
            data = HkdfOutput(
                derivedKeyHex = okmHex,
                pseudorandomKeyHex = prkHex,
                keyLengthBytes = outLen,
                algorithm = input.algorithm,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
