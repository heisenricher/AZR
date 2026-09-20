package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3TextToolsTest {

    // --- SlugGeneratorTool ---
    @Test
    fun testSlugGenerator_basicAndAccents() = runBlocking {
        val tool = SlugGeneratorTool()
        val input = "Café & Crème Brûlée — 2026!"
        val result = tool.execute(SlugInput(input))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("cafe-creme-brulee-2026", data.slug)
    }

    @Test
    fun testSlugGenerator_customSeparator() = runBlocking {
        val tool = SlugGeneratorTool()
        val result = tool.execute(SlugInput("Offline Android Toolbox", separator = "_"))
        assertTrue(result is ToolResult.Success)
        assertEquals("offline_android_toolbox", (result as ToolResult.Success).data.slug)
    }

    @Test
    fun testSlugGenerator_empty() = runBlocking {
        val tool = SlugGeneratorTool()
        val result = tool.execute(SlugInput(""))
        assertTrue(result is ToolResult.Success)
        assertEquals("", (result as ToolResult.Success).data.slug)
    }

    // --- StringInspectorTool ---
    @Test
    fun testStringInspector_asciiAndUnicode() = runBlocking {
        val tool = StringInspectorTool()
        val input = "Hello World! 123"
        val result = tool.execute(input)
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(16, data.charCount)
        assertEquals(16, data.codePointCount)
        assertEquals(16, data.utf8Bytes)
        assertEquals(16, data.asciiCount)
        assertEquals(0, data.nonAsciiCount)
        assertEquals(3, data.digitCount)
        assertEquals(2, data.whitespaceCount)
        assertEquals(1, data.punctuationCount)
    }

    @Test
    fun testStringInspector_multibyteEmoji() = runBlocking {
        val tool = StringInspectorTool()
        val input = "🛠️" // Wrench emoji + variation selector
        val result = tool.execute(input)
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.utf8Bytes > data.charCount)
        assertTrue(data.nonAsciiCount > 0)
    }

    // --- Rot13CipherTool ---
    @Test
    fun testRot13Cipher_roundTrip() = runBlocking {
        val tool = Rot13CipherTool()
        val original = "The Quick Brown Fox Jumps Over The Lazy Dog!"
        val enc = tool.execute(Rot13Input(original, shift = 13))
        assertTrue(enc is ToolResult.Success)
        val encrypted = (enc as ToolResult.Success).data.transformedText
        assertEquals("Gur Dhvpx Oebja Sbk Whzcf Bire Gur Ynml Qbt!", encrypted)

        // ROT-13 twice returns original
        val dec = tool.execute(Rot13Input(encrypted, shift = 13))
        assertTrue(dec is ToolResult.Success)
        assertEquals(original, (dec as ToolResult.Success).data.transformedText)
    }

    @Test
    fun testRot13Cipher_customShiftAndDigits() = runBlocking {
        val tool = Rot13CipherTool()
        val result = tool.execute(Rot13Input("abc123", shift = 1, rotateDigits = true))
        assertTrue(result is ToolResult.Success)
        assertEquals("bcd234", (result as ToolResult.Success).data.transformedText)
    }
}
