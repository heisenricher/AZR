package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ApcaContrastInput
import com.offline.toolbox.tools.color.WcagApcaContrastTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase8MediaColorToolsTest {

    private val apcaTool = WcagApcaContrastTool()
    private val intervalTool = AudioFrequencyIntervalTool()

    @Test
    fun testApcaContrast_blackOnWhiteOptimal() = runTest {
        val result = apcaTool.execute(ApcaContrastInput(
            textColorHex = "#000000",
            backgroundColorHex = "#FFFFFF"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.wcag2AaPass)
        assertTrue(data.wcag2AaaPass)
        assertEquals(21.0, data.wcag2ContrastRatio, 0.5)
        assertTrue(data.apcaLcValue >= 100.0)
        assertTrue(data.apcaRating.contains("PREFERRED"))
    }

    @Test
    fun testApcaContrast_invalidHexFails() = runTest {
        val result = apcaTool.execute(ApcaContrastInput(
            textColorHex = "invalid_color"
        ))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testAudioInterval_perfectFifthHarmonicRatio() = runTest {
        // Base 440 Hz (A4), Perfect Fifth (7 semitones, 3:2 ratio)
        // 12-TET = 440 * 2^(7/12) = 659.255 Hz
        // Just = 440 * (3/2) = 660.000 Hz
        // Cents divergence = 1200 * log2(660 / 659.255) = ~+1.955 cents
        val result = intervalTool.execute(AudioIntervalInput(
            baseFrequencyHz = 440.0,
            interval = MusicalInterval.PERFECT_FIFTH
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(659.26, data.equalTemperamentHz, 0.05)
        assertEquals(660.00, data.justIntonationHz, 0.05)
        assertEquals(1.955, data.centsDivergence, 0.05)
        assertEquals("3:2", data.justRatio)
    }

    @Test
    fun testAudioInterval_invalidFrequencyFails() = runTest {
        val result = intervalTool.execute(AudioIntervalInput(
            baseFrequencyHz = -50.0
        ))
        assertTrue(result is ToolResult.Failure)
    }
}
