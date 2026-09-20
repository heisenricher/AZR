package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextCaseConverterTest {

    private lateinit var tool: TextCaseConverterTool

    @Before
    fun setUp() {
        tool = TextCaseConverterTool()
    }

    @Test
    fun testEmptyInput() = runBlocking {
        val result = tool.execute(TextCaseInput("", CaseType.UPPERCASE))
        assertTrue(result is ToolResult.Success)
        assertEquals("", (result as ToolResult.Success).data)
    }

    @Test
    fun testUppercase() = runBlocking {
        val result = tool.execute(TextCaseInput("hello world", CaseType.UPPERCASE))
        assertTrue(result is ToolResult.Success)
        assertEquals("HELLO WORLD", (result as ToolResult.Success).data)
    }

    @Test
    fun testLowercase() = runBlocking {
        val result = tool.execute(TextCaseInput("HELLO WORLD", CaseType.LOWERCASE))
        assertTrue(result is ToolResult.Success)
        assertEquals("hello world", (result as ToolResult.Success).data)
    }

    @Test
    fun testCamelCase() = runBlocking {
        val result = tool.execute(TextCaseInput("offline toolbox app", CaseType.CAMEL_CASE))
        assertTrue(result is ToolResult.Success)
        assertEquals("offlineToolboxApp", (result as ToolResult.Success).data)
    }

    @Test
    fun testPascalCase() = runBlocking {
        val result = tool.execute(TextCaseInput("offline toolbox app", CaseType.PASCAL_CASE))
        assertTrue(result is ToolResult.Success)
        assertEquals("OfflineToolboxApp", (result as ToolResult.Success).data)
    }

    @Test
    fun testSnakeCase() = runBlocking {
        val result = tool.execute(TextCaseInput("Offline Toolbox App", CaseType.SNAKE_CASE))
        assertTrue(result is ToolResult.Success)
        assertEquals("offline_toolbox_app", (result as ToolResult.Success).data)
    }

    @Test
    fun testKebabCase() = runBlocking {
        val result = tool.execute(TextCaseInput("Offline Toolbox App", CaseType.KEBAB_CASE))
        assertTrue(result is ToolResult.Success)
        assertEquals("offline-toolbox-app", (result as ToolResult.Success).data)
    }

    @Test
    fun testConstantCase() = runBlocking {
        val result = tool.execute(TextCaseInput("Offline Toolbox App", CaseType.CONSTANT_CASE))
        assertTrue(result is ToolResult.Success)
        assertEquals("OFFLINE_TOOLBOX_APP", (result as ToolResult.Success).data)
    }
}
