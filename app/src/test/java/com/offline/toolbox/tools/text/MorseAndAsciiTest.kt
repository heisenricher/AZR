package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseAndAsciiTest {

    private val morseTool = MorseCodeTool()
    private val asciiTool = AsciiArtBannerTool()

    @Test
    fun testMorseCodeTextToMorse() = runBlocking {
        val res = morseTool.execute(
            MorseCodeInput(
                text = "SOS",
                direction = MorseDirection.TEXT_TO_MORSE
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("... --- ...", data.convertedText)
        assertEquals(6, data.dotCount)
        assertEquals(3, data.dashCount)
        assertTrue(data.totalUnits > 0)
    }

    @Test
    fun testMorseCodeMorseToText() = runBlocking {
        val res = morseTool.execute(
            MorseCodeInput(
                text = "... --- ...",
                direction = MorseDirection.MORSE_TO_TEXT
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("SOS", data.convertedText)
    }

    @Test
    fun testMorseCodeAutoDetect() = runBlocking {
        val morseInput = "... . -.-. .-. . - / -.- . -.--"
        val res = morseTool.execute(
            MorseCodeInput(
                text = morseInput,
                direction = MorseDirection.AUTO_DETECT
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(MorseDirection.MORSE_TO_TEXT, data.detectedDirection)
        assertEquals("SECRET KEY", data.convertedText)
    }

    @Test
    fun testAsciiArtBannerBlockFont() = runBlocking {
        val res = asciiTool.execute(
            AsciiArtBannerInput(
                text = "DEV",
                style = AsciiBannerStyle.BLOCK_FONT
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(5, data.lineCount)
        assertTrue(data.banner.contains("█"))
    }

    @Test
    fun testAsciiArtBannerBoxBorders() = runBlocking {
        val singleBox = asciiTool.execute(
            AsciiArtBannerInput(
                text = "OFFLINE TOOLBOX",
                style = AsciiBannerStyle.BOX_BORDER_SINGLE
            )
        )

        assertTrue(singleBox is ToolResult.Success)
        val singleData = (singleBox as ToolResult.Success).data
        assertTrue(singleData.banner.startsWith("┌"))
        assertTrue(singleData.banner.contains("OFFLINE TOOLBOX"))
        assertTrue(singleData.banner.endsWith("┘"))

        val doubleBox = asciiTool.execute(
            AsciiArtBannerInput(
                text = "SECURE CARD",
                style = AsciiBannerStyle.BOX_BORDER_DOUBLE
            )
        )

        assertTrue(doubleBox is ToolResult.Success)
        val doubleData = (doubleBox as ToolResult.Success).data
        assertTrue(doubleData.banner.startsWith("╔"))
        assertTrue(doubleData.banner.endsWith("╝"))
    }
}
