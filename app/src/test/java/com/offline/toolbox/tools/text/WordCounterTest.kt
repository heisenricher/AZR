package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WordCounterTest {

    private lateinit var tool: WordCounterTool

    @Before
    fun setUp() {
        tool = WordCounterTool()
    }

    @Test
    fun testEmptyInput() = runBlocking {
        val res = tool.execute("")
        assertTrue(res is ToolResult.Success)
        val stats = (res as ToolResult.Success).data
        assertEquals(0, stats.wordCount)
        assertEquals(0, stats.charCountWithSpaces)
    }

    @Test
    fun testStandardSentence() = runBlocking {
        val sample = "The quick brown fox jumps over the lazy dog."
        val res = tool.execute(sample)
        assertTrue(res is ToolResult.Success)
        val stats = (res as ToolResult.Success).data
        assertEquals(9, stats.wordCount)
        assertEquals(44, stats.charCountWithSpaces)
        assertEquals(36, stats.charCountWithoutSpaces)
        assertEquals(1, stats.sentenceCount)
        assertEquals(1, stats.paragraphCount)
    }

    @Test
    fun testMultiParagraph() = runBlocking {
        val sample = "Paragraph one.\n\nParagraph two.\n\nParagraph three."
        val res = tool.execute(sample)
        assertTrue(res is ToolResult.Success)
        val stats = (res as ToolResult.Success).data
        assertEquals(6, stats.wordCount)
        assertEquals(3, stats.paragraphCount)
    }
}
