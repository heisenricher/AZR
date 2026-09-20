package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.HtmlColorNameInput
import com.offline.toolbox.tools.color.HtmlColorNameTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase6MediaColorToolsTest {

    private val noteTool = FrequencyToNoteTool()
    private val dpiTool = DpiDensityCalculatorTool()
    private val colorTool = HtmlColorNameTool()

    @Test
    fun testFrequencyToNote_standardA4() = runTest {
        val result = noteTool.execute(FrequencyNoteInput(frequencyHz = 440.0, referenceA4Hz = 440.0))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("A", data.noteName)
        assertEquals(4, data.octave)
        assertEquals(69, data.midiNoteNumber)
        assertEquals(0.0, data.centsDeviation, 0.1)
    }

    @Test
    fun testFrequencyToNote_middleC() = runTest {
        // C4 is ~261.63 Hz, MIDI 60
        val result = noteTool.execute(FrequencyNoteInput(frequencyHz = 261.63, referenceA4Hz = 440.0))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("C", data.noteName)
        assertEquals(4, data.octave)
        assertEquals(60, data.midiNoteNumber)
    }

    @Test
    fun testFrequencyToNote_invalidFrequencyFails() = runTest {
        val result = noteTool.execute(FrequencyNoteInput(frequencyHz = -10.0))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testDpiDensityCalculator_phoneScreen() = runTest {
        val result = dpiTool.execute(DpiDensityInput(
            widthPixels = 1080,
            heightPixels = 1920,
            diagonalInches = 5.0,
            sampleDpToConvert = 16.0
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        // sqrt(1080^2 + 1920^2) / 5.0 = 2202.9 / 5.0 = 440.58 PPI -> xxhdpi (3.0x scale)
        assertEquals(440.58, data.calculatedPpi, 1.0)
        assertEquals("xxhdpi (~480 dpi)", data.densityBucket)
        assertEquals(3.0, data.densityScaleFactor, 0.01)
        assertEquals(48.0, data.convertedPixelsForSampleDp, 0.01)
    }

    @Test
    fun testDpiDensityCalculator_invalidDimensions() = runTest {
        val result = dpiTool.execute(DpiDensityInput(
            widthPixels = 0,
            heightPixels = 100,
            diagonalInches = 5.0
        ))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testHtmlColorName_exactMatchRed() = runTest {
        val result = colorTool.execute(HtmlColorNameInput(queryOrHex = "#FF0000"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("red", data.closestNamedColor)
        assertEquals("#FF0000", data.closestHex)
        assertTrue(data.exactMatch)
        assertEquals(0.0, data.euclideanDistance, 0.001)
    }

    @Test
    fun testHtmlColorName_queryByName() = runTest {
        val result = colorTool.execute(HtmlColorNameInput(queryOrHex = "cornflowerblue"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("cornflowerblue", data.closestNamedColor)
        assertEquals("#6495ED", data.closestHex)
        assertTrue(data.exactMatch)
    }

    @Test
    fun testHtmlColorName_nearestMatch() = runTest {
        // Slightly off from black: #000003
        val result = colorTool.execute(HtmlColorNameInput(queryOrHex = "#000003"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("black", data.closestNamedColor)
        assertTrue(data.euclideanDistance < 5.0)
    }
}
