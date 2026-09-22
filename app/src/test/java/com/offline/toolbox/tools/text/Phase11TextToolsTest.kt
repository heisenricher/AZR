package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase11TextToolsTest {

    private val rleTool = RunLengthEncodingTool()
    private val levenshteinTool = LevenshteinMatrixVisualizerTool()

    @Test
    fun testRle_encodeAndDecodeRoundtrip() = runTest {
        val original = "WWWWWWWWWWWWBWWWWWWWWWWWWBBBWWWWWWWWWWWWWWWWWWWWWWWWB"
        val encRes = rleTool.execute(RleInput(operation = "ENCODE", text = original))
        assertTrue(encRes is ToolResult.Success)
        val encData = (encRes as ToolResult.Success).data
        assertEquals("12W1B12W3B24W1B", encData.resultText)
        assertTrue(encData.spaceSavingsPercent > 50.0)

        val decRes = rleTool.execute(RleInput(operation = "DECODE", text = encData.resultText))
        assertTrue(decRes is ToolResult.Success)
        val decData = (decRes as ToolResult.Success).data
        assertEquals(original, decData.resultText)
    }

    @Test
    fun testRle_decodeMalformed() = runTest {
        val res = rleTool.execute(RleInput(operation = "DECODE", text = "invalid_stream"))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testLevenshteinMatrix_kittenSittingDistance3() = runTest {
        val res = levenshteinTool.execute(LevenshteinMatrixInput("kitten", "sitting"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(3, data.distance)
        assertEquals(6, data.sourceLength)
        assertEquals(7, data.targetLength)
        assertEquals(data.alignedSource.length, data.alignedTarget.length)
        assertTrue(data.matrixGridText.contains("kitten") || data.matrixGridText.contains("k"))
        assertTrue(data.similarityScorePercent > 50.0)
    }

    @Test
    fun testLevenshteinMatrix_identicalStringsDistance0() = runTest {
        val res = levenshteinTool.execute(LevenshteinMatrixInput("offline", "offline"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(0, data.distance)
        assertEquals(100.0, data.similarityScorePercent, 1e-4)
    }
}
