package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ColorConverterTest {

    private lateinit var tool: ColorConverterTool

    @Before
    fun setUp() {
        tool = ColorConverterTool()
    }

    @Test
    fun testPureWhite() = runBlocking {
        val res = tool.execute("#FFFFFF")
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("#FFFFFF", data.hex6)
        assertEquals("rgb(255, 255, 255)", data.rgbString)
        assertEquals(1.0, data.relativeLuminance, 0.01)
        assertEquals(1.0, data.contrastWhite, 0.01)
        assertEquals(21.0, data.contrastBlack, 0.01)
    }

    @Test
    fun testPureBlack() = runBlocking {
        val res = tool.execute("#000000")
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("#000000", data.hex6)
        assertEquals(0.0, data.relativeLuminance, 0.01)
        assertEquals(21.0, data.contrastWhite, 0.01)
        assertEquals(1.0, data.contrastBlack, 0.01)
    }

    @Test
    fun testThreeCharHex() = runBlocking {
        val res = tool.execute("F00")
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("#FF0000", data.hex6)
        assertEquals(255, data.red)
        assertEquals(0, data.green)
        assertEquals(0, data.blue)
    }

    @Test
    fun testInvalidColorCode() = runBlocking {
        val res = tool.execute("ZZZZZZ")
        assertTrue(res is ToolResult.Failure)
        val failure = res as ToolResult.Failure
        assertTrue(failure.userGuidance != null)
    }
}
