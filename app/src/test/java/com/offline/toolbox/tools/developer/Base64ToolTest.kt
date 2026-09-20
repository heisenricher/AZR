package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Base64ToolTest {

    private lateinit var tool: Base64Tool

    @Before
    fun setUp() {
        tool = Base64Tool()
    }

    @Test
    fun testStandardEncodeAndDecode() = runBlocking {
        val original = "Hello Android Offline Toolbox!"
        val encodeRes = tool.execute(Base64Input(original, Base64Mode.ENCODE_STANDARD))
        assertTrue(encodeRes is ToolResult.Success)
        val encoded = (encodeRes as ToolResult.Success).data

        val decodeRes = tool.execute(Base64Input(encoded, Base64Mode.DECODE_STANDARD))
        assertTrue(decodeRes is ToolResult.Success)
        val decoded = (decodeRes as ToolResult.Success).data

        assertEquals(original, decoded)
    }

    @Test
    fun testHexConversion() = runBlocking {
        val hex = "48656C6C6F" // "Hello"
        val res = tool.execute(Base64Input(hex, Base64Mode.HEX_TO_BASE64))
        assertTrue(res is ToolResult.Success)
        val b64 = (res as ToolResult.Success).data
        assertEquals("SGVsbG8=", b64)

        val hexBack = tool.execute(Base64Input(b64, Base64Mode.BASE64_TO_HEX))
        assertTrue(hexBack is ToolResult.Success)
        assertEquals("48656C6C6F", (hexBack as ToolResult.Success).data)
    }
}
