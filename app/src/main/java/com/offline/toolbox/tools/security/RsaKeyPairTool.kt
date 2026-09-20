package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.Base64

enum class RsaKeySize(val bits: Int, val label: String) {
    RSA_1024(1024, "1024-bit (Fast / Legacy)"),
    RSA_2048(2048, "2048-bit (Standard Recommended)"),
    RSA_4096(4096, "4096-bit (High Security)")
}

data class RsaKeyPairInput(
    val keySize: RsaKeySize = RsaKeySize.RSA_2048
)

data class RsaKeyPairOutput(
    val publicKeyPem: String,
    val privateKeyPem: String,
    val keySizeBits: Int,
    val algorithm: String,
    val formattedReport: String,
    val summary: String
)

class RsaKeyPairTool : Tool<RsaKeyPairInput, RsaKeyPairOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "rsa_key_pair_tool",
        name = "RSA Key Pair & PEM Generator",
        description = "Generate cryptographically secure RSA public and private key pairs (PKCS#8 & X.509 PEM format).",
        category = ToolCategory.SECURITY,
        tags = listOf("rsa", "key", "pem", "cryptography", "public key", "private key", "pkcs8", "x509", "security"),
        inputType = ToolDataType.NONE,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "VpnKey"
    )

    override suspend fun execute(input: RsaKeyPairInput): ToolResult<RsaKeyPairOutput> {
        val startTime = System.currentTimeMillis()

        return try {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(input.keySize.bits, SecureRandom())
            val keyPair = keyGen.generateKeyPair()

            val pubBytes = keyPair.public.encoded
            val privBytes = keyPair.private.encoded

            val pubPem = formatPem("PUBLIC KEY", pubBytes)
            val privPem = formatPem("PRIVATE KEY", privBytes)

            val report = buildString {
                appendLine("RSA ASYMMETRIC KEY PAIR GENERATED")
                appendLine("--------------------------------")
                appendLine("Algorithm:         RSA")
                appendLine("Key Size:          ${input.keySize.bits} bits")
                appendLine("Public Key Format: X.509 SubjectPublicKeyInfo (PEM)")
                appendLine("Private Key Format: PKCS#8 Encoded (PEM)")
                appendLine("--------------------------------")
                appendLine(pubPem)
                appendLine()
                appendLine(privPem)
            }

            val summary = "Generated RSA ${input.keySize.bits}-bit Key Pair"

            ToolResult.Success(
                data = RsaKeyPairOutput(
                    publicKeyPem = pubPem,
                    privateKeyPem = privPem,
                    keySizeBits = input.keySize.bits,
                    algorithm = "RSA",
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("Failed to generate RSA key pair: ${e.message}", cause = e)
        }
    }

    private fun formatPem(type: String, bytes: ByteArray): String {
        val b64 = Base64.getEncoder().encodeToString(bytes)
        val sb = StringBuilder()
        sb.append("-----BEGIN $type-----\n")
        var i = 0
        while (i < b64.length) {
            val end = (i + 64).coerceAtMost(b64.length)
            sb.append(b64.substring(i, end)).append("\n")
            i += 64
        }
        sb.append("-----END $type-----")
        return sb.toString()
    }
}
