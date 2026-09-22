package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorBlendInput
import com.offline.toolbox.tools.color.ColorHexAlphaBlenderTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12MediaColorToolsTest {

    private val snrTool = SnrAudioCalculatorTool()
    private val blendTool = ColorHexAlphaBlenderTool()

    @Test
    fun testSnrAudioCalculator_snrAndEnob() = runTest {
        val input = SnrAudioInput(
            signalRmsVolts = 1.0,
            noiseRmsVolts = 0.0001, // 80 dB SNR
            bitDepth = 16,
            harmonicVolts = listOf(0.0001, 0.00005)
        )
        val res = snrTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(80.0, data.snrDb, 0.1)
        assertEquals(98.08, data.theoreticalAdcSnrDb, 0.1)
        assertTrue(data.enobBits > 12.0)
        assertTrue(data.thdPercent < 0.02)
    }

    @Test
    fun testColorHexAlphaBlender_sourceOverCompositing() = runTest {
        // Red with 50% alpha (#80FF0000) over solid Blue (#0000FF)
        val input = ColorBlendInput(
            foregroundHex = "#80FF0000",
            backgroundHex = "#0000FF",
            hexFormat = "ARGB",
            colorSpace = "SRGB"
        )
        val res = blendTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // Result should have roughly half red, half blue (purple)
        assertTrue(data.red > 100)
        assertTrue(data.blue > 100)
        assertEquals(0, data.green)
        assertTrue(data.blendedHexArgb.startsWith("#FF"))
    }

    @Test
    fun testColorHexAlphaBlender_contrastRatios() = runTest {
        val input = ColorBlendInput(
            foregroundHex = "#000000",
            backgroundHex = "#000000"
        )
        val res = blendTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(21.0, data.contrastRatioOnWhite, 0.5)
        assertEquals(1.0, data.contrastRatioOnBlack, 0.1)
    }
}
