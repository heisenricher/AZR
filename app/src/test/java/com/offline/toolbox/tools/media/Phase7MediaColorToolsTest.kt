package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorBlindnessInput
import com.offline.toolbox.tools.color.ColorBlindnessSimulatorTool
import com.offline.toolbox.tools.color.ColorBlindnessType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7MediaColorToolsTest {

    private val bpmTool = BpmTapperTool()
    private val colorBlindnessTool = ColorBlindnessSimulatorTool()

    @Test
    fun testBpm_directValueQuarterNotes() = runTest {
        val result = bpmTool.execute(BpmInput(directBpm = 120.0))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(120.0, data.calculatedBpm, 0.01)
        assertTrue(data.tempoMarking.contains("Allegro"))
        assertEquals(500.0, data.quarterNoteMs, 0.01)
        assertEquals(250.0, data.eighthNoteMs, 0.01)
        assertEquals(125.0, data.sixteenthNoteMs, 0.01)
    }

    @Test
    fun testBpm_calculatedFromTaps() = runTest {
        // 4 taps spaced exactly 500ms apart -> 120 BPM
        val taps = listOf(1000L, 1500L, 2000L, 2500L)
        val result = bpmTool.execute(BpmInput(tapTimestampsMs = taps))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(120.0, data.calculatedBpm, 0.5)
        assertEquals(4, data.tapCount)
    }

    @Test
    fun testBpm_outOfBoundsFails() = runTest {
        val result = bpmTool.execute(BpmInput(directBpm = -10.0))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testColorBlindness_deuteranopiaSimulation() = runTest {
        val result = colorBlindnessTool.execute(ColorBlindnessInput(
            hexColor = "#E11D48",
            deficiency = ColorBlindnessType.DEUTERANOPIA
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("#E11D48", data.originalHex)
        assertTrue(data.simulatedHex.startsWith("#"))
        assertEquals(7, data.simulatedHex.length)
        assertTrue(data.contrastDelta > 0.0)
    }

    @Test
    fun testColorBlindness_achromatopsiaGrayscale() = runTest {
        // Pure red #FF0000 -> grayscale gray = 0.2126 * 255 = ~54 -> #363636
        val result = colorBlindnessTool.execute(ColorBlindnessInput(
            hexColor = "#FF0000",
            deficiency = ColorBlindnessType.ACHROMATOPSIA
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        // In monochromacy, R == G == B
        val rgbValues = data.simulatedRgb.removePrefix("rgb(").removeSuffix(")").split(",").map { it.trim().toInt() }
        assertEquals(rgbValues[0], rgbValues[1])
        assertEquals(rgbValues[1], rgbValues[2])
    }

    @Test
    fun testColorBlindness_invalidHexFails() = runTest {
        val result = colorBlindnessTool.execute(ColorBlindnessInput(
            hexColor = "invalid_hex"
        ))
        assertTrue(result is ToolResult.Failure)
    }
}
