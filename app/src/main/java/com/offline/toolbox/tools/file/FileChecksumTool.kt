package com.offline.toolbox.tools.file

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import java.util.zip.CRC32

enum class ChecksumAlgorithm(val algorithmName: String) {
    ALL("All Algorithms"),
    SHA256("SHA-256"),
    MD5("MD5"),
    SHA1("SHA-1"),
    SHA512("SHA-512"),
    CRC32("CRC32")
}

data class FileChecksumInput(
    val content: String = "",
    val isBase64: Boolean = false,
    val algorithm: ChecksumAlgorithm = ChecksumAlgorithm.ALL,
    val expectedChecksum: String? = null
)

data class FileChecksumOutput(
    val bytesProcessed: Long,
    val hashes: Map<String, String>,
    val verified: Boolean?,
    val formattedReport: String,
    val summary: String
)

class FileChecksumTool : Tool<FileChecksumInput, FileChecksumOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "file_checksum_tool",
        name = "File & Data Checksum Calculator",
        description = "Calculate and verify MD5, SHA-1, SHA-256, SHA-512, and CRC32 checksums using streaming buffers.",
        category = ToolCategory.FILE,
        tags = listOf("checksum", "file", "hash", "sha256", "md5", "sha1", "crc32", "integrity", "verify"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Security"
    )

    override suspend fun execute(input: FileChecksumInput): ToolResult<FileChecksumOutput> {
        val startTime = System.currentTimeMillis()

        if (input.content.isEmpty()) {
            return ToolResult.Failure(
                message = "Input data is empty.",
                userGuidance = "Provide text or Base64-encoded file data to compute checksums."
            )
        }

        val rawBytes = try {
            if (input.isBase64) {
                Base64.getDecoder().decode(input.content.trim())
            } else {
                input.content.toByteArray(StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            return ToolResult.Failure("Failed to decode Base64 input: ${e.message}", cause = e)
        }

        return try {
            val stream = ByteArrayInputStream(rawBytes)
            val result = computeFromStream(stream, rawBytes.size.toLong(), input.algorithm, input.expectedChecksum)

            ToolResult.Success(
                data = result,
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = result.summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("Checksum calculation failed: ${e.message}", cause = e)
        }
    }

    fun computeFromStream(
        stream: InputStream,
        totalBytes: Long,
        algorithm: ChecksumAlgorithm,
        expectedChecksum: String? = null
    ): FileChecksumOutput {
        val digests = mutableMapOf<String, MessageDigest>()
        var crc: CRC32? = null

        val runAll = algorithm == ChecksumAlgorithm.ALL
        if (runAll || algorithm == ChecksumAlgorithm.MD5) digests["MD5"] = MessageDigest.getInstance("MD5")
        if (runAll || algorithm == ChecksumAlgorithm.SHA1) digests["SHA-1"] = MessageDigest.getInstance("SHA-1")
        if (runAll || algorithm == ChecksumAlgorithm.SHA256) digests["SHA-256"] = MessageDigest.getInstance("SHA-256")
        if (runAll || algorithm == ChecksumAlgorithm.SHA512) digests["SHA-512"] = MessageDigest.getInstance("SHA-512")
        if (runAll || algorithm == ChecksumAlgorithm.CRC32) crc = CRC32()

        val buffer = ByteArray(8192)
        var read: Int
        var totalRead = 0L

        while (stream.read(buffer).also { read = it } != -1) {
            totalRead += read
            for (md in digests.values) {
                md.update(buffer, 0, read)
            }
            crc?.update(buffer, 0, read)
        }

        val results = mutableMapOf<String, String>()
        for ((name, md) in digests) {
            results[name] = bytesToHex(md.digest())
        }
        if (crc != null) {
            results["CRC32"] = "%08X".format(Locale.US, crc.value)
        }

        val expectedTrimmed = expectedChecksum?.trim()
        val verified = if (!expectedTrimmed.isNullOrEmpty()) {
            results.values.any { it.equals(expectedTrimmed, ignoreCase = true) }
        } else null

        val report = buildString {
            appendLine("CHECKSUM INTEGRITY REPORT:")
            appendLine("Processed Size: $totalRead bytes")
            appendLine("--------------------------------")
            results.forEach { (algo, hash) ->
                appendLine("%-10s %s".format(algo + ":", hash))
            }
            if (verified != null) {
                appendLine("--------------------------------")
                if (verified) {
                    appendLine("VERIFICATION: MATCH SUCCESSFUL (Verified against '$expectedTrimmed')")
                } else {
                    appendLine("VERIFICATION: MISMATCH (Expected '$expectedTrimmed')")
                }
            }
        }

        val primaryHash = results["SHA-256"] ?: results.values.firstOrNull() ?: ""
        val summary = if (verified == true) "Checksum verified ($totalRead bytes)" else "Computed ${results.size} hashes ($totalRead bytes)"

        return FileChecksumOutput(
            bytesProcessed = totalRead,
            hashes = results,
            verified = verified,
            formattedReport = report,
            summary = summary
        )
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

    companion object {
        private val HEX_ARRAY = "0123456789abcdef".toCharArray()
    }
}
