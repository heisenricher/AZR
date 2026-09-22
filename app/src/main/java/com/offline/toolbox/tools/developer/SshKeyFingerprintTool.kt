package com.offline.toolbox.tools.developer

import java.util.Base64
import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.MessageDigest
import java.util.Locale

data class SshKeyInput(
    val publicKeyString: String = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl dev@offline.toolbox"
)

data class SshKeyOutput(
    val keyType: String,
    val bitLength: Int,
    val sha256Fingerprint: String,
    val md5Fingerprint: String,
    val comment: String?,
    val formattedReport: String,
    val summary: String
)

class SshKeyFingerprintTool : Tool<SshKeyInput, SshKeyOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "ssh_key_fingerprint_tool",
        name = "SSH Public Key Inspector & Fingerprint Calculator",
        description = "Inspect OpenSSH public keys, calculate SHA-256 and MD5 fingerprints (OpenSSH format), extract key length, algorithm, and comments.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("ssh", "ssh-keygen", "fingerprint", "sha256", "md5", "public key", "ed25519", "rsa", "ecdsa", "security"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Key"
    )

    override suspend fun execute(input: SshKeyInput): ToolResult<SshKeyOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.publicKeyString.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("SSH public key input cannot be empty.")
        }

        // OpenSSH format: <algorithm> <base64-blob> [comment]
        val tokens = raw.split(Regex("\\s+"), 3)
        if (tokens.size < 2) {
            return ToolResult.Failure("Invalid SSH public key format. Expected '<algorithm> <base64_key> [comment]'.")
        }

        val claimedType = tokens[0]
        val b64Blob = tokens[1]
        val comment = if (tokens.size >= 3) tokens[2].trim() else null

        val rawBytes: ByteArray
        try {
            rawBytes = Base64.getDecoder().decode(b64Blob)
        } catch (e: Exception) {
            return ToolResult.Failure("Failed to decode Base64 key payload: ${e.message}")
        }

        if (rawBytes.size < 8) {
            return ToolResult.Failure("Key payload is too short to be a valid OpenSSH public key blob.")
        }

        // Extract key type from internal binary blob
        val internalKeyType: String
        var bitLength = 0
        try {
            var offset = 0
            val typeLen = readInt32(rawBytes, offset)
            offset += 4
            if (typeLen <= 0 || offset + typeLen > rawBytes.size) {
                return ToolResult.Failure("Corrupted OpenSSH wire format header.")
            }
            internalKeyType = String(rawBytes, offset, typeLen, Charsets.US_ASCII)
            offset += typeLen

            bitLength = when (internalKeyType) {
                "ssh-ed25519" -> 256
                "ssh-rsa" -> {
                    // next: e (exponent), then n (modulus)
                    val eLen = readInt32(rawBytes, offset)
                    offset += 4 + eLen
                    val nLen = readInt32(rawBytes, offset)
                    offset += 4
                    // If leading byte is 0x00 (sign padding in mpint), subtract 1 byte
                    var modBytes = nLen
                    if (rawBytes[offset] == 0.toByte()) {
                        modBytes -= 1
                    }
                    modBytes * 8
                }
                "ecdsa-sha2-nistp256" -> 256
                "ecdsa-sha2-nistp384" -> 384
                "ecdsa-sha2-nistp521" -> 521
                else -> 256
            }
        } catch (e: Exception) {
            return ToolResult.Failure("Failed to parse OpenSSH wire attributes: ${e.message}")
        }

        // Compute SHA-256 fingerprint: SHA256:<base64-without-padding>
        val sha256Digest = MessageDigest.getInstance("SHA-256").digest(rawBytes)
        val sha256B64 = Base64.getEncoder().withoutPadding().encodeToString(sha256Digest)
        val sha256Formatted = "SHA256:$sha256B64"

        // Compute MD5 fingerprint: MD5:xx:xx:...
        val md5Digest = MessageDigest.getInstance("MD5").digest(rawBytes)
        val md5Formatted = "MD5:" + md5Digest.joinToString(":") { String.format(Locale.US, "%02x", it) }

        val report = buildString {
            appendLine("OPENSSH PUBLIC KEY INSPECTION & FINGERPRINT")
            appendLine("--------------------------------------------------")
            appendLine("Key Algorithm:    $internalKeyType")
            appendLine("Key Bit Length:   $bitLength bits")
            appendLine("Blob Byte Length: ${rawBytes.size} bytes")
            if (!comment.isNullOrBlank()) {
                appendLine("Comment / Identity: $comment")
            }
            appendLine()
            appendLine("FINGERPRINTS (OpenSSH Standard):")
            appendLine("• SHA-256: $sha256Formatted")
            appendLine("• MD5:     $md5Formatted")
            appendLine()
            appendLine("SECURITY ASSESSMENT:")
            when {
                internalKeyType == "ssh-ed25519" -> appendLine("• Modern Ed25519 elliptic curve key: High security, compact size, immune to side-channel timing attacks.")
                internalKeyType.startsWith("ecdsa-") -> appendLine("• NIST P-curve ECDSA key: Recommended minimum 256 bits.")
                internalKeyType == "ssh-rsa" && bitLength >= 3072 -> appendLine("• Strong RSA key: $bitLength bits meets current NIST standards (>= 3072 bits).")
                internalKeyType == "ssh-rsa" && bitLength in 2048..3071 -> appendLine("• Acceptable RSA key: 2048 bits is acceptable but 3072+ bits or Ed25519 is recommended.")
                internalKeyType == "ssh-rsa" && bitLength < 2048 -> appendLine("• WEAK RSA KEY WARNING: Key length ($bitLength bits) is below 2048 bits and vulnerable to factorization.")
            }
        }

        val summary = "$internalKeyType ($bitLength-bit) | $sha256Formatted"

        return ToolResult.Success(
            data = SshKeyOutput(
                keyType = internalKeyType,
                bitLength = bitLength,
                sha256Fingerprint = sha256Formatted,
                md5Fingerprint = md5Formatted,
                comment = comment,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun readInt32(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }
}
