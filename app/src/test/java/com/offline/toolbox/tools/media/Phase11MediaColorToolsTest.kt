package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorDeltaE2000Tool
import com.offline.toolbox.tools.color.DeltaEInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase11MediaColorToolsTest {

    private val reverbTool = ReverbRt60AcousticTool()
    private val deltaETool = ColorDeltaE2000Tool()

    @Test
    fun testReverbRt60_recordingStudioPreset() = runTest {
        val input = ReverbRt60Input(
            lengthMeters = 8.0,
            widthMeters = 6.0,
            heightMeters = 3.0,
            roomTypePreset = "RECORDING_STUDIO"
        )
        val res = reverbTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(144.0, data.roomVolumeM3, 1e-4)
        assertEquals(180.0, data.totalSurfaceAreaM2, 1e-4)
        // For studio with alpha 0.45, Eyring RT60 should be tightly controlled (~0.2 - 0.5s)
        assertTrue(data.eyringRt60Seconds in 0.2..0.5)
        assertTrue(data.acousticVerdict.contains("Studio") || data.acousticVerdict.contains("Dry") || data.acousticVerdict.contains("Controlled"))
    }

    @Test
    fun testReverbRt60_invalidDimensions() = runTest {
        val res = reverbTool.execute(ReverbRt60Input(lengthMeters = -5.0))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testColorDeltaE2000_identicalColorsZeroDifference() = runTest {
        val res = deltaETool.execute(DeltaEInput("#3498db", "#3498db"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(0.0, data.deltaE2000, 1e-4)
        assertEquals(0.0, data.deltaE76, 1e-4)
        assertTrue(data.perceptualAssessment.contains("Imperceptible"))
    }

    @Test
    fun testColorDeltaE2000_perceptibleDifference() = runTest {
        val res = deltaETool.execute(DeltaEInput("#3498db", "#2980b9"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.deltaE2000 > 1.0)
        assertTrue(data.deltaE2000 < 15.0)
    }

    @Test
    fun testColorDeltaE2000_invalidHexFails() = runTest {
        val res = deltaETool.execute(DeltaEInput("NotAColor", "#FFFFFF"))
        assertTrue(res is ToolResult.Failure)
    }
}
