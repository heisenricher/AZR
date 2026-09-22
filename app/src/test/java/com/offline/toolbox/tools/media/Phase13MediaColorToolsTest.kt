package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorShadeTintGeneratorTool
import com.offline.toolbox.tools.color.ColorShadeTintInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase13MediaColorToolsTest {

    private val lufsTool = LufsLoudnessMeterTool()
    private val colorScaleTool = ColorShadeTintGeneratorTool()

    // 1. LUFS Loudness & Streaming Normalization Tests
    @Test
    fun testLufsLoudness_spotifyTargetEvaluation() = runTest {
        val input = LufsLoudnessInput(
            integratedLufs = -11.5,
            truePeakDbtp = -0.3,
            loudnessRangeLu = 6.5,
            platformTarget = "SPOTIFY"
        )
        val res = lufsTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // Spotify targets -14.0 LUFS. Gain adjustment = -14.0 - (-11.5) = -2.5 dB
        assertEquals(-2.5, data.gainAdjustmentDb, 0.01)
        assertTrue(data.intersampleClipRisk) // Peak -0.3 > -1.0 dBTP
        assertTrue(data.dynamicRating.contains("Commercial Pop"))
        assertEquals(7, data.platformEvaluations.size)
    }

    @Test
    fun testLufsLoudness_ebuBroadcastCompliance() = runTest {
        val input = LufsLoudnessInput(
            integratedLufs = -23.0,
            truePeakDbtp = -1.5,
            loudnessRangeLu = 10.0,
            platformTarget = "EBU_R128"
        )
        val res = lufsTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(0.0, data.gainAdjustmentDb, 0.01)
        assertFalse(data.intersampleClipRisk)
        val ebu = data.platformEvaluations.first { it.platformName.contains("EBU R128") }
        assertTrue(ebu.isCompliant)
    }

    // 2. Monochromatic Color Shade & Tint Generator Tests
    @Test
    fun testColorShadeTint_generatesScaleWithWcagContrast() = runTest {
        val input = ColorShadeTintInput(
            baseHex = "#3F51B5", // Indigo base
            stepCount = 10
        )
        val res = colorScaleTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(9, data.tints.size)
        assertEquals(9, data.shades.size)
        assertEquals(19, data.fullScale.size) // 9 tints + 1 base + 9 shades

        // Tints should have higher luminance than shades
        val lightestTint = data.tints.first() // Tint +90%
        val darkestShade = data.shades.last() // Shade -90%
        assertTrue(lightestTint.relativeLuminance > darkestShade.relativeLuminance)

        // Lightest tint requires black text for contrast, darkest requires white text
        assertEquals("#000000", lightestTint.recommendedTextHex)
        assertEquals("#FFFFFF", darkestShade.recommendedTextHex)
    }

    @Test
    fun testColorShadeTint_invalidHexFails() = runTest {
        val res = colorScaleTool.execute(ColorShadeTintInput(baseHex = "invalid_hex"))
        assertTrue(res is ToolResult.Failure)
    }
}
