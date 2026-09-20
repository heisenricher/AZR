package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.media.AspectRatioCalculatorTool
import com.offline.toolbox.tools.media.AspectRatioInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5ColorMediaToolsTest {

    private val paletteTool = ColorPaletteGeneratorTool()
    private val aspectTool = AspectRatioCalculatorTool()

    @Test
    fun testColorPalette_complementary() = runTest {
        val result = paletteTool.execute(
            ColorPaletteInput(
                baseHex = "#3B82F6",
                harmony = PaletteHarmony.COMPLEMENTARY
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.palette.isNotEmpty())
        assertEquals("#3B82F6", data.baseColor.hex.uppercase())
    }

    @Test
    fun testColorPalette_triadic() = runTest {
        val result = paletteTool.execute(
            ColorPaletteInput(
                baseHex = "#FF0000",
                harmony = PaletteHarmony.TRIADIC
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.palette.isNotEmpty())
    }

    @Test
    fun testAspectRatio_1080p() = runTest {
        val result = aspectTool.execute(AspectRatioInput(originalWidth = 1920, originalHeight = 1080))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("16:9", data.ratioSimplified)
        assertTrue(data.standardMatch.contains("16:9") || data.standardMatch.contains("Full HD") || data.standardMatch.contains("Widescreen"))
    }

    @Test
    fun testAspectRatio_scalingProportions() = runTest {
        val result = aspectTool.execute(AspectRatioInput(originalWidth = 1920, originalHeight = 1080, targetWidth = 1280))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertNotNull(data.scaledDimensions)
        assertEquals(1280, data.scaledDimensions?.first)
        assertEquals(720, data.scaledDimensions?.second)
    }

    @Test
    fun testAspectRatio_square() = runTest {
        val result = aspectTool.execute(AspectRatioInput(originalWidth = 500, originalHeight = 500))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("1:1", data.ratioSimplified)
    }

    @Test
    fun testAspectRatio_invalid() = runTest {
        val result = aspectTool.execute(AspectRatioInput(originalWidth = 0, originalHeight = 100))
        assertTrue(result is ToolResult.Failure)
    }
}
