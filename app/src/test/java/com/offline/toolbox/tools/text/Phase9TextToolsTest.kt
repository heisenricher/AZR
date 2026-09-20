package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9TextToolsTest {

    private val readabilityTool = ReadabilityScoreTool()
    private val justifierTool = TextJustifierTool()

    @Test
    fun testReadability_computesStandardMetrics() = runTest {
        val sample = """
            The cat sat on the mat. The sun was hot and bright. A dog ran down the path.
            Children played with a ball in the garden. They were very happy and had much fun together.
        """.trimIndent()

        val res = readabilityTool.execute(ReadabilityInput(text = sample))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.wordCount > 20)
        assertTrue(data.sentenceCount >= 4)
        assertTrue(data.fleschReadingEase > 70.0) // Simple text has high reading ease
        assertTrue(data.fleschKincaidGradeLevel < 7.0) // Easy elementary grade level
        assertTrue(data.syllableCount > 20)
    }

    @Test
    fun testReadability_emptyTextFails() = runTest {
        val res = readabilityTool.execute(ReadabilityInput(text = "   "))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testTextJustifier_exactColumnWidth() = runTest {
        val text = "The quick brown fox jumps over the lazy dog and runs across the wide open meadow into the sunset."
        val colWidth = 35

        val res = justifierTool.execute(JustifierInput(
            text = text,
            lineWidth = colWidth
        ))

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        val lines = data.justifiedText.lines()
        assertTrue(lines.size >= 2)

        // All lines except the last one must be EXACTLY colWidth characters
        for (i in 0 until lines.size - 1) {
            assertEquals("Line $i should match column width $colWidth", colWidth, lines[i].length)
        }
        // Last line should not exceed colWidth
        assertTrue(lines.last().length <= colWidth)
    }

    @Test
    fun testTextJustifier_emptyTextFails() = runTest {
        val res = justifierTool.execute(JustifierInput(
            text = "   ",
            lineWidth = 40
        ))
        assertTrue(res is ToolResult.Failure)
    }
}
