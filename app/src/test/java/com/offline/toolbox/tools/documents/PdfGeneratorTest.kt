package com.offline.toolbox.tools.documents

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class PdfGeneratorTest {

    private val tool = PdfGeneratorTool()

    @Test
    fun testGenerateValidPdfStructure() = runBlocking {
        val input = PdfGeneratorInput(
            title = "Test Document",
            author = "Offline User",
            content = "This is a clean offline PDF generation test."
        )

        val res = tool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertNotNull(data.pdfBase64)
        assertTrue(data.totalBytes > 0)

        val pdfBytes = Base64.getDecoder().decode(data.pdfBase64)
        val pdfText = String(pdfBytes, StandardCharsets.ISO_8859_1)

        // Verify standard PDF 1.4 specification compliance
        assertTrue("PDF must start with %PDF-1.4", pdfText.startsWith("%PDF-1.4"))
        assertTrue("PDF must contain catalog object", pdfText.contains("/Type /Catalog"))
        assertTrue("PDF must contain pages object", pdfText.contains("/Type /Pages"))
        assertTrue("PDF must contain cross-reference table", pdfText.contains("xref"))
        assertTrue("PDF must contain trailer dictionary", pdfText.contains("trailer"))
        assertTrue("PDF must end with %%EOF", pdfText.trim().endsWith("%%EOF"))
        assertTrue("PDF must contain the escaped title", pdfText.contains("Test Document"))
    }

    @Test
    fun testEmptyInputFailure() = runBlocking {
        val res = tool.execute(PdfGeneratorInput(title = "", content = ""))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testMultiPagePagination() = runBlocking {
        // Generate long content that spans multiple pages
        val longContent = (1..150).joinToString("\n") { "Line $it: Paragraph explaining offline security design principles." }
        val res = tool.execute(
            PdfGeneratorInput(
                title = "Long Document",
                content = longContent,
                pageFormat = PdfPageFormat.A4
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue("Should generate at least 2 pages for 150 lines", data.pageCount >= 2)
    }
}
