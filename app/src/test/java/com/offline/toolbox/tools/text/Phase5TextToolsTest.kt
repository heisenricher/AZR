package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5TextToolsTest {

    private val binHexTool = TextBinaryHexTool()
    private val leetTool = LeetspeakTool()

    @Test
    fun testTextToBinaryHex_ascii() = runTest {
        val result = binHexTool.execute(TextBinaryHexInput("Hello", TextBinaryHexMode.TEXT_TO_ALL))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("Hello", data.text)
        assertEquals(5, data.byteCount)
        assertEquals("48 65 6C 6C 6F", data.hexSpaced.uppercase())
        assertEquals("48656C6C6F", data.hexContinuous.uppercase())
        assertEquals("01001000 01100101 01101100 01101100 01101111", data.binary)
        assertEquals("72 101 108 108 111", data.decimal)
    }

    @Test
    fun testBinaryToAll_roundTrip() = runTest {
        val result = binHexTool.execute(
            TextBinaryHexInput(
                input = "01001000 01101001", // "Hi"
                mode = TextBinaryHexMode.BINARY_TO_ALL
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("Hi", data.text)
        assertEquals("48 69", data.hexSpaced.uppercase())
    }

    @Test
    fun testHexToAll_roundTrip() = runTest {
        val result = binHexTool.execute(
            TextBinaryHexInput(
                input = "0x4F 0x4B", // "OK"
                mode = TextBinaryHexMode.HEX_TO_ALL
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("OK", data.text)
    }

    @Test
    fun testDecimalToAll_roundTrip() = runTest {
        val result = binHexTool.execute(
            TextBinaryHexInput(
                input = "65, 66, 67", // "ABC"
                mode = TextBinaryHexMode.DECIMAL_TO_ALL
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("ABC", data.text)
    }

    @Test
    fun testLeetspeak_basicEncode() = runTest {
        val result = leetTool.execute(
            LeetInput(
                text = "elite hacker",
                level = LeetLevel.BASIC,
                mode = LeetMode.ENCODE
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("3l173 h4ck3r", data.resultText)
        assertTrue(data.substitutedCount > 0)
    }

    @Test
    fun testLeetspeak_intermediateEncode() = runTest {
        val result = leetTool.execute(
            LeetInput(
                text = "big buzz",
                level = LeetLevel.INTERMEDIATE,
                mode = LeetMode.ENCODE
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        // 'b' -> '8', 'i' -> '1', 'g' -> '9', 'z' -> '2'
        assertEquals("819 8u22", data.resultText)
    }

    @Test
    fun testLeetspeak_decodeBasic() = runTest {
        val result = leetTool.execute(
            LeetInput(
                text = "h3ll0",
                mode = LeetMode.DECODE
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("hello", data.resultText)
    }
}
