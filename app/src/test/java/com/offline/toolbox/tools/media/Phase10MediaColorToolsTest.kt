package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorHarmonyMixingTool
import com.offline.toolbox.tools.color.ColorMixingInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10MediaColorToolsTest {

    private val binauralTool = BeatsBinauralAcousticTool()
    private val colorMixTool = ColorHarmonyMixingTool()

    @Test
    fun testBinauralBeats_alphaBandDetection() = runTest {
        val res = binauralTool.execute(
            BeatsBinauralInput(leftChannelHz = 432.0, rightChannelHz = 440.0)
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(8.0, data.beatFreqHz, 0.01)
        assertEquals(436.0, data.carrierFreqHz, 0.01)
        assertEquals(BrainwaveBand.ALPHA, data.brainwaveBand)
        assertTrue(data.isTrueBinauralRange)
    }

    @Test
    fun testBinauralBeats_deltaBandDetection() = runTest {
        val res = binauralTool.execute(
            BeatsBinauralInput(leftChannelHz = 432.0, rightChannelHz = 434.0)
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(2.0, data.beatFreqHz, 0.01)
        assertEquals(BrainwaveBand.DELTA, data.brainwaveBand)
    }

    @Test
    fun testColorHarmonyMixing_additiveVsPigment() = runTest {
        // Mixing Yellow (#FFFF00) and Blue (#0000FF)
        val res = colorMixTool.execute(
            ColorMixingInput(color1Hex = "#FFFF00", color2Hex = "#0000FF", ratioColor1Percent = 50)
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // Additive RGB mix of Yellow (255, 255, 0) and Blue (0, 0, 255) is middle gray (128, 128, 128)
        assertEquals("#808080", data.additiveRgbHex)
        // Subtractive CMY mix
        assertEquals("#808080", data.subtractiveCmyHex)
        // Pigment RYB mix yields green tones (dominant green channel)
        assertTrue(data.pigmentRybHex.startsWith("#"))
        assertTrue(data.pigmentRybValues.contains("rgb("))
    }

    @Test
    fun testColorHarmonyMixing_invalidHexFails() = runTest {
        val res = colorMixTool.execute(
            ColorMixingInput(color1Hex = "not-a-color", color2Hex = "#0000FF")
        )
        assertTrue(res is ToolResult.Failure)
    }
}
