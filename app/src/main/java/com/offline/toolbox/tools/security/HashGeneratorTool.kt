package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.MessageDigest

enum class HashAlgorithm(val algorithmName: String, val bitLength: Int) {
    MD5("MD5", 128),
    SHA_1("SHA-1", 160),
    SHA_224("SHA-224", 224),
    SHA_256("SHA-256", 256),
    SHA_384("SHA-384", 384),
    SHA_512("SHA-512", 512)
}

data class HashInput(
    val text: String,
    val algorithm: HashAlgorithm = HashAlgorithm.SHA_256,
    val uppercase: Boolean = false,
    val salt: String = ""
)

data class HashOutput(
    val hash: String,
    val algorithm: String,
    val bitLength: Int,
    val byteCount: Int
)

class HashGeneratorTool : Tool<HashInput, HashOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "hash_generator",
        name = "Hash Generator",
        description = "Calculate cryptographic hashes (MD5, SHA-1, SHA-256, SHA-512) with optional salt.",
        category = ToolCategory.SECURITY,
        tags = listOf("hash", "sha256", "md5", "sha512", "sha1", "crypto", "digest", "checksum"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Fingerprint"
    )

    override suspend fun execute(input: HashInput): ToolResult<HashOutput> {
        val startTime = System.currentTimeMillis()

        return try {
            val digest = MessageDigest.getInstance(input.algorithm.algorithmName)
            if (input.salt.isNotEmpty()) {
                digest.update(input.salt.toByteArray(Charsets.UTF_8))
            }
            val hashBytes = digest.digest(input.text.toByteArray(Charsets.UTF_8))

            val format = if (input.uppercase) "%02X" else "%02x"
            val hashString = hashBytes.joinToString("") { format.format(it) }

            ToolResult.Success(
                data = HashOutput(
                    hash = hashString,
                    algorithm = input.algorithm.algorithmName,
                    bitLength = input.algorithm.bitLength,
                    byteCount = hashBytes.size
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "${input.algorithm.algorithmName}: ${hashString.take(12)}..."
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Failed to compute ${input.algorithm.algorithmName} hash: ${e.message}",
                cause = e
            )
        }
    }
}
