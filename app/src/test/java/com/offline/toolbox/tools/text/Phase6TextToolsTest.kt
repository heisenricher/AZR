package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase6TextToolsTest {

    private val wrapTool = TextWrapTool()
    private val zalgoTool = ZalgoTextTool()
    private val anagramTool = AnagramSolverTool()

    @Test
    fun testTextWrap_basicLimit() = runTest {
        val longSentence = "The quick brown fox jumps over the lazy dog and runs across the green meadow"
        val result = wrapTool.execute(TextWrapInput(
            text = longSentence,
            columnWidth = 25,
            reflowParagraphs = true
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.lineCount > 2)
        assertTrue(data.maxLineWidth <= 25)
    }

    @Test
    fun testTextWrap_prefixAndIndents() = runTest {
        val text = "Line one\nLine two\nLine three"
        val result = wrapTool.execute(TextWrapInput(
            text = text,
            columnWidth = 30,
            linePrefix = "> ",
            firstLineIndent = "  ",
            reflowParagraphs = false
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.wrappedText.lines().filter { it.isNotBlank() }.all { it.contains("> ") })
    }

    @Test
    fun testZalgo_corruptAndClean() = runTest {
        val original = "CLEAN TEXT NO GLITCH"

        // Corrupt
        val corruptResult = zalgoTool.execute(ZalgoInput(
            text = original,
            mode = ZalgoMode.CORRUPT_ZALGO,
            intensity = ZalgoIntensity.MEDIUM
        ))
        assertTrue(corruptResult is ToolResult.Success)
        val corrupted = (corruptResult as ToolResult.Success).data.resultText
        assertTrue(corrupted.length > original.length)

        // Clean
        val cleanResult = zalgoTool.execute(ZalgoInput(
            text = corrupted,
            mode = ZalgoMode.CLEAN_ZALGO
        ))
        assertTrue(cleanResult is ToolResult.Success)
        val cleaned = (cleanResult as ToolResult.Success).data.resultText
        assertEquals(original, cleaned)
    }

    @Test
    fun testAnagram_detectValidAnagram() = runTest {
        val result = anagramTool.execute(AnagramInput(
            primaryWord = "orchestra",
            secondaryWord = "carthorse"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue("Should be an anagram", data.isAnagram)
        assertEquals(data.letterFrequenciesPrimary, data.letterFrequenciesSecondary)
    }

    @Test
    fun testAnagram_detectPalindromes() = runTest {
        val result = anagramTool.execute(AnagramInput(
            primaryWord = "racecar",
            secondaryWord = "not a palindrome"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.isPrimaryPalindrome)
        assertFalse(data.isSecondaryPalindrome)
        assertFalse(data.isAnagram)
    }

    @Test
    fun testAnagram_emptyWordFails() = runTest {
        val result = anagramTool.execute(AnagramInput(
            primaryWord = "   "
        ))
        assertTrue(result is ToolResult.Failure)
    }
}
