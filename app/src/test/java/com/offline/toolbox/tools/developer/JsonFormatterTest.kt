package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JsonFormatterTest {

    private lateinit var tool: JsonFormatterTool

    @Before
    fun setUp() {
        tool = JsonFormatterTool()
    }

    @Test
    fun testValidObjectFormatting() = runBlocking {
        val raw = "{\"name\":\"Offline\",\"offline\":true}"
        val res = tool.execute(JsonFormatterInput(raw, JsonOperation.FORMAT_2_SPACES))
        assertTrue(res is ToolResult.Success)
        val out = (res as ToolResult.Success).data
        assertTrue(out.isValid)
        assertTrue(out.processedText.contains("\n"))
    }

    @Test
    fun testMinification() = runBlocking {
        val raw = """
            {
              "name": "Offline",
              "tools": 50
            }
        """.trimIndent()
        val res = tool.execute(JsonFormatterInput(raw, JsonOperation.MINIFY))
        assertTrue(res is ToolResult.Success)
        val out = (res as ToolResult.Success).data
        assertTrue(out.isValid)
        assertEquals("{\"name\":\"Offline\",\"tools\":50}", out.processedText)
    }

    @Test
    fun testMalformedJsonReportsDiagnostics() = runBlocking {
        val malformed = "{ \"name\": \"Offline\"" // missing closing brace
        val res = tool.execute(JsonFormatterInput(malformed, JsonOperation.FORMAT_2_SPACES))
        assertTrue(res is ToolResult.Failure)
        val failure = res as ToolResult.Failure
        assertTrue(failure.userGuidance != null)
        assertTrue(failure.userGuidance!!.contains("braces"))
    }
}
