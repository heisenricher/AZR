package com.offline.toolbox.tools.file

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileToolsTest {

    private val zipTool = ZipArchiveTool()
    private val checksumTool = FileChecksumTool()

    @Test
    fun testZipArchiveCreationAndInspection() = runBlocking {
        val files = mapOf(
            "hello.txt" to "Hello Offline World!",
            "data/config.json" to "{\"mode\": \"offline\", \"secure\": true}"
        )

        val createRes = zipTool.execute(
            ZipArchiveInput(
                operation = ZipOperation.CREATE_ARCHIVE,
                filesToArchive = files
            )
        )

        assertTrue(createRes is ToolResult.Success)
        val createData = (createRes as ToolResult.Success).data
        assertNotNull(createData.generatedZipBase64)
        assertEquals(2, createData.entries.size)
        assertTrue(createData.totalUncompressedBytes > 0)
        assertTrue(createData.isZipSlipSafe)

        // Now inspect the created ZIP
        val inspectRes = zipTool.execute(
            ZipArchiveInput(
                operation = ZipOperation.INSPECT_ARCHIVE,
                zipBase64 = createData.generatedZipBase64!!
            )
        )

        assertTrue(inspectRes is ToolResult.Success)
        val inspectData = (inspectRes as ToolResult.Success).data
        assertEquals(2, inspectData.entries.size)
        assertTrue(inspectData.isZipSlipSafe)
        assertTrue(inspectData.entries.any { it.name == "hello.txt" })
    }

    @Test
    fun testZipSlipPathTraversalExploitDefense() = runBlocking {
        // Craft a malicious ZIP payload with path traversal entries (Zip Slip vulnerability)
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val evilEntry = ZipEntry("../../etc/passwd")
            zos.putNextEntry(evilEntry)
            zos.write("root:x:0:0:root:/root:/bin/bash".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            val safeEntry = ZipEntry("normal_file.txt")
            zos.putNextEntry(safeEntry)
            zos.write("Safe content".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        val maliciousZipBase64 = Base64.getEncoder().encodeToString(baos.toByteArray())

        val res = zipTool.execute(
            ZipArchiveInput(
                operation = ZipOperation.SAFE_EXTRACT_CHECK,
                zipBase64 = maliciousZipBase64,
                baseExtractDirectory = "/app/user_files"
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // Verify OWASP Zip Slip defense successfully blocked the traversal
        assertFalse("Zip Slip vulnerability must be detected!", data.isZipSlipSafe)
        assertTrue(data.formattedReport.contains("MALICIOUS ENTRY DETECTED"))
        assertTrue(data.formattedReport.contains("attempts to escape"))
    }

    @Test
    fun testFileChecksumCalculationsAndVerification() = runBlocking {
        val testData = "The quick brown fox jumps over the lazy dog"
        // Known SHA-256 for this exact sentence:
        val expectedSha256 = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592"
        val expectedMd5 = "9e107d9d372bb6826bd81d3542a419d6"

        val res = checksumTool.execute(
            FileChecksumInput(
                content = testData,
                isBase64 = false,
                algorithm = ChecksumAlgorithm.ALL,
                expectedChecksum = expectedSha256
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(testData.length.toLong(), data.bytesProcessed)
        assertEquals(expectedSha256, data.hashes["SHA-256"])
        assertEquals(expectedMd5, data.hashes["MD5"])
        assertNotNull(data.hashes["SHA-1"])
        assertNotNull(data.hashes["SHA-512"])
        assertNotNull(data.hashes["CRC32"])
        assertEquals(true, data.verified)
    }

    @Test
    fun testFileChecksumVerificationMismatch() = runBlocking {
        val res = checksumTool.execute(
            FileChecksumInput(
                content = "Test content",
                isBase64 = false,
                algorithm = ChecksumAlgorithm.SHA256,
                expectedChecksum = "0000000000000000000000000000000000000000000000000000000000000000"
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(false, data.verified)
        assertTrue(data.formattedReport.contains("MISMATCH"))
    }
}
