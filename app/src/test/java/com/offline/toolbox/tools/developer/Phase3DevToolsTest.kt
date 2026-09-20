package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3DevToolsTest {

    // --- SqlFormatterTool ---
    @Test
    fun testSqlFormatter_beautifiesQuery() = runBlocking {
        val tool = SqlFormatterTool()
        val raw = "select id, name, email from users where active = 1 and age > 18 order by name desc"
        val result = tool.execute(SqlFormatterInput(raw))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        val formatted = data.formattedSql
        assertTrue(formatted.contains("SELECT"))
        assertTrue(formatted.contains("\nFROM"))
        assertTrue(formatted.contains("\nWHERE"))
        assertTrue(formatted.contains("\nORDER BY"))
    }

    @Test
    fun testSqlFormatter_emptyFails() = runBlocking {
        val tool = SqlFormatterTool()
        val result = tool.execute(SqlFormatterInput(""))
        assertTrue(result is ToolResult.Failure)
    }

    // --- XmlFormatterTool ---
    @Test
    fun testXmlFormatter_beautifyAndMinify() = runBlocking {
        val tool = XmlFormatterTool()
        val xml = "<root><item id=\"1\"><name>Test</name></item></root>"

        val beautifyRes = tool.execute(XmlFormatterInput(xml, indentSpaces = 2, minify = false))
        assertTrue(beautifyRes is ToolResult.Success)
        val beautified = (beautifyRes as ToolResult.Success).data.formattedXml
        assertTrue(beautified.contains("<item id=\"1\">"))
        assertTrue(beautified.lines().size >= 3)

        val minifyRes = tool.execute(XmlFormatterInput(xml, minify = true))
        assertTrue(minifyRes is ToolResult.Success)
        val minified = (minifyRes as ToolResult.Success).data.formattedXml
        assertTrue(minified.contains("<root>"))
    }

    @Test
    fun testXmlFormatter_invalidXmlFails() = runBlocking {
        val tool = XmlFormatterTool()
        val result = tool.execute(XmlFormatterInput("<unclosed><tag></other>"))
        assertTrue(result is ToolResult.Failure)
    }

    // --- MarkdownPreviewTool ---
    @Test
    fun testMarkdownPreview_outlineAndMetrics() = runBlocking {
        val tool = MarkdownPreviewTool()
        val md = """
            # Main Title
            Introduction text with [a link](https://example.com).
            
            ## Subtitle
            Some **bold text** and `inline code`.
            
            ```kotlin
            val x = 42
            ```
        """.trimIndent()

        val result = tool.execute(md)
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(2, data.stats.headingCount)
        assertEquals(1, data.stats.linkCount)
        assertEquals(1, data.stats.codeBlockCount)
        assertTrue(data.headingsOutline[0].contains("H1: Main Title"))
        assertTrue(data.headingsOutline[1].contains("H2: Subtitle"))
    }

    @Test
    fun testMarkdownPreview_empty() = runBlocking {
        val tool = MarkdownPreviewTool()
        val result = tool.execute("")
        assertTrue(result is ToolResult.Success)
        assertEquals("", (result as ToolResult.Success).data.plainText)
    }
}
