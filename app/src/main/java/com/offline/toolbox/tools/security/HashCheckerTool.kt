package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.MessageDigest

data class HashCheckerInput(
    val content: String,
    val expectedHash: String,
    val algorithm: HashAlgorithm? = null
)

data class HashCheckerOutput(
    val matches: Boolean,
    val algorithmUsed: String,
    val calculatedHash: String,
    val expectedHashCleaned: String,
    val summary: String
)

class HashCheckerTool : Tool<HashCheckerInput, HashCheckerOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "hash_checker",
        name = "Hash & Checksum Verifier",
        description = "Verify integrity by comparing calculated hashes against expected checksums.",
        category = ToolCategory.SECURITY,
        tags = listOf("hash", "checksum", "verify", "integrity", "sha256", "md5", "security"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "VerifiedUser"
    )

    override suspend fun execute(input: HashCheckerInput): ToolResult<HashCheckerOutput> {
        val startTime = System.currentTimeMillis()
        val expectedClean = input.expectedHash.trim().lowercase().replace(" ", "")

        if (expectedClean.isEmpty()) {
            return ToolResult.Failure(
                message = "Expected hash is empty.",
                userGuidance = "Paste or enter the expected checksum to verify against."
            )
        }

        // Auto-detect algorithm based on hex string length if not specified
        val algo = input.algorithm ?: when (expectedClean.length) {
            32 -> HashAlgorithm.MD5
            40 -> HashAlgorithm.SHA_1
            56 -> HashAlgorithm.SHA_224
            64 -> HashAlgorithm.SHA_256
            96 -> HashAlgorithm.SHA_384
            128 -> HashAlgorithm.SHA_512
            else -> HashAlgorithm.SHA_256
        }

        val digest = MessageDigest.getInstance(algo.algorithmName)
        val hashBytes = digest.digest(input.content.toByteArray(Charsets.UTF_8))
        val calculated = hashBytes.joinToString("") { "%02x".format(it) }

        val matches = calculated.equals(expectedClean, ignoreCase = true)
        val statusText = if (matches) "VERIFIED MATCH (Checksums match)" else "MISMATCH (Content differs)"
        val summary = "$statusText [${algo.algorithmName}]"

        return ToolResult.Success(
            data = HashCheckerOutput(
                matches = matches,
                algorithmUsed = algo.algorithmName,
                calculatedHash = calculated,
                expectedHashCleaned = expectedClean,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
