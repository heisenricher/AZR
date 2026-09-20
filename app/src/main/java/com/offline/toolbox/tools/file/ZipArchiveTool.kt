package com.offline.toolbox.tools.file

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class ZipOperation(val label: String) {
    CREATE_ARCHIVE("Create ZIP from Files/Text"),
    INSPECT_ARCHIVE("Inspect ZIP Manifest & Compression"),
    SAFE_EXTRACT_CHECK("Validate & Extract (Zip Slip Defense)")
}

data class ZipEntryItem(
    val name: String,
    val uncompressedSize: Long,
    val compressedSize: Long,
    val isDirectory: Boolean,
    val crc32: Long
)

data class ZipArchiveInput(
    val operation: ZipOperation = ZipOperation.CREATE_ARCHIVE,
    // Map of filename -> text content for creating an archive
    val filesToArchive: Map<String, String> = emptyMap(),
    // Base64-encoded ZIP byte array for inspection or extraction
    val zipBase64: String = "",
    // Target base path for extraction simulation and Zip Slip defense
    val baseExtractDirectory: String = "/safe_target_dir"
)

data class ZipArchiveOutput(
    val generatedZipBase64: String?,
    val entries: List<ZipEntryItem>,
    val totalUncompressedBytes: Long,
    val totalCompressedBytes: Long,
    val compressionRatioPercent: Double,
    val isZipSlipSafe: Boolean,
    val formattedReport: String,
    val summary: String
)

class ZipArchiveTool : Tool<ZipArchiveInput, ZipArchiveOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "zip_archive_tool",
        name = "ZIP Archiver & Security Inspector",
        description = "Create ZIP archives, inspect compression manifests, and extract with strict Zip Slip path traversal defenses.",
        category = ToolCategory.FILE,
        tags = listOf("zip", "archive", "compress", "extract", "unzip", "security", "zip slip", "files"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FolderZip"
    )

    override suspend fun execute(input: ZipArchiveInput): ToolResult<ZipArchiveOutput> {
        val startTime = System.currentTimeMillis()

        return when (input.operation) {
            ZipOperation.CREATE_ARCHIVE -> {
                if (input.filesToArchive.isEmpty()) {
                    return ToolResult.Failure(
                        message = "No files provided to archive.",
                        userGuidance = "Provide at least one filename and content pair to build a ZIP."
                    )
                }

                val baos = ByteArrayOutputStream()
                ZipOutputStream(baos).use { zos ->
                    for ((filename, content) in input.filesToArchive) {
                        val bytes = content.toByteArray(StandardCharsets.UTF_8)
                        val entry = ZipEntry(filename)
                        entry.size = bytes.size.toLong()
                        zos.putNextEntry(entry)
                        zos.write(bytes)
                        zos.closeEntry()
                    }
                }

                val zipBytes = baos.toByteArray()
                val zipBase64 = Base64.getEncoder().encodeToString(zipBytes)

                // Inspect generated archive
                val items = mutableListOf<ZipEntryItem>()
                var totalUncompressed = 0L
                val buffer = ByteArray(2048)
                ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        var streamBytes = 0L
                        var r: Int
                        while (zis.read(buffer).also { r = it } != -1) {
                            streamBytes += r
                        }
                        val entryUncompressed = if (entry.size >= 0) entry.size else streamBytes
                        items.add(
                            ZipEntryItem(
                                name = entry.name,
                                uncompressedSize = entryUncompressed,
                                compressedSize = entry.compressedSize.coerceAtLeast(0),
                                isDirectory = entry.isDirectory,
                                crc32 = entry.crc
                            )
                        )
                        totalUncompressed += entryUncompressed
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                val ratio = if (totalUncompressed > 0) {
                    ((totalUncompressed - zipBytes.size).toDouble() / totalUncompressed) * 100.0
                } else 0.0

                val report = buildString {
                    appendLine("STATUS: Successfully Created ZIP Archive")
                    appendLine("Entries:               ${items.size}")
                    appendLine("Total Raw Size:        $totalUncompressed bytes")
                    appendLine("Compressed ZIP Size:   ${zipBytes.size} bytes")
                    appendLine("Space Saved:           ${"%.1f".format(Locale.US, ratio)}%")
                    appendLine("--------------------------------")
                    appendLine("Archive Manifest:")
                    items.forEach { appendLine("• ${it.name}") }
                }

                val summary = "Created ZIP (${items.size} files, ${zipBytes.size} bytes)"

                ToolResult.Success(
                    data = ZipArchiveOutput(
                        generatedZipBase64 = zipBase64,
                        entries = items,
                        totalUncompressedBytes = totalUncompressed,
                        totalCompressedBytes = zipBytes.size.toLong(),
                        compressionRatioPercent = ratio,
                        isZipSlipSafe = true,
                        formattedReport = report,
                        summary = summary
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            }

            ZipOperation.INSPECT_ARCHIVE, ZipOperation.SAFE_EXTRACT_CHECK -> {
                if (input.zipBase64.isBlank()) {
                    return ToolResult.Failure(
                        message = "No ZIP data provided.",
                        userGuidance = "Provide a Base64-encoded ZIP archive to inspect or validate."
                    )
                }

                val zipBytes = try {
                    Base64.getDecoder().decode(input.zipBase64.trim())
                } catch (e: Exception) {
                    return ToolResult.Failure("Invalid Base64 ZIP payload: ${e.message}", cause = e)
                }

                val items = mutableListOf<ZipEntryItem>()
                var totalUncompressed = 0L
                var isSafe = true
                val securityIssues = mutableListOf<String>()

                val baseDir = File(input.baseExtractDirectory)
                val canonicalBase = baseDir.canonicalPath

                try {
                    val buffer = ByteArray(2048)
                    ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val entryName = entry.name
                            val destFile = File(baseDir, entryName)
                            val canonicalDest = destFile.canonicalPath

                            // OWASP Zip Slip defense verification
                            if (!canonicalDest.startsWith(canonicalBase + File.separator) && canonicalDest != canonicalBase) {
                                isSafe = false
                                securityIssues.add("MALICIOUS ENTRY DETECTED: '$entryName' attempts to escape to '$canonicalDest'")
                            }

                            var streamBytes = 0L
                            var r: Int
                            while (zis.read(buffer).also { r = it } != -1) {
                                streamBytes += r
                            }
                            val entryUncompressed = if (entry.size >= 0) entry.size else streamBytes

                            items.add(
                                ZipEntryItem(
                                    name = entryName,
                                    uncompressedSize = entryUncompressed,
                                    compressedSize = entry.compressedSize.coerceAtLeast(0),
                                    isDirectory = entry.isDirectory,
                                    crc32 = entry.crc
                                )
                            )
                            totalUncompressed += entryUncompressed
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                } catch (e: Exception) {
                    return ToolResult.Failure("Malformed ZIP archive: ${e.message}", cause = e)
                }

                val report = buildString {
                    appendLine("ARCHIVE INSPECTION REPORT:")
                    appendLine("Total Entries:         ${items.size}")
                    appendLine("Compressed Size:       ${zipBytes.size} bytes")
                    appendLine("Uncompressed Size:     $totalUncompressed bytes")
                    appendLine("Zip Slip Status:       ${if (isSafe) "SECURE (No path traversal detected)" else "VULNERABILITY DETECTED"}")
                    if (securityIssues.isNotEmpty()) {
                        appendLine()
                        appendLine("SECURITY WARNINGS:")
                        securityIssues.forEach { appendLine("⚠️ $it") }
                    }
                    appendLine("--------------------------------")
                    appendLine("Archive Manifest:")
                    items.forEach { appendLine("• ${it.name} (${it.uncompressedSize} bytes)") }
                }

                val summary = if (isSafe) "Verified ${items.size} entries (Secure)" else "ALERT: Insecure entries detected!"

                ToolResult.Success(
                    data = ZipArchiveOutput(
                        generatedZipBase64 = null,
                        entries = items,
                        totalUncompressedBytes = totalUncompressed,
                        totalCompressedBytes = zipBytes.size.toLong(),
                        compressionRatioPercent = 0.0,
                        isZipSlipSafe = isSafe,
                        formattedReport = report,
                        summary = summary
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            }
        }
    }
}
