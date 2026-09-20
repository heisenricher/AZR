package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextToolsTest {

    // --- TextCleanerTool ---
    @Test
    fun testTextCleaner_emptyInput() = runBlocking {
        val tool = TextCleanerTool()
        val result = tool.execute(TextCleanerInput(""))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("", data.cleanedText)
        assertEquals(0, data.charactersRemoved)
    }

    @Test
    fun testTextCleaner_cleansWhitespaceAndBlanks() = runBlocking {
        val tool = TextCleanerTool()
        val input = "  Hello   world!  \n\n   Line   2   \n\n\n"
        val result = tool.execute(TextCleanerInput(input))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("Hello world!\nLine 2", data.cleanedText)
        assertTrue(data.charactersRemoved > 0)
        assertTrue(data.linesRemoved > 0)
    }

    @Test
    fun testTextCleaner_stripHtml() = runBlocking {
        val tool = TextCleanerTool()
        val input = "<p>Hello <b>World</b>!</p>"
        val result = tool.execute(
            TextCleanerInput(
                input,
                TextCleanerOptions(stripHtmlTags = true)
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("Hello World!", data.cleanedText)
    }

    // --- LineOperationsTool ---
    @Test
    fun testLineOperations_emptyInput() = runBlocking {
        val tool = LineOperationsTool()
        val result = tool.execute(LineOperationsInput(""))
        assertTrue(result is ToolResult.Success)
        assertEquals("", (result as ToolResult.Success).data)
    }

    @Test
    fun testLineOperations_sortAZ() = runBlocking {
        val tool = LineOperationsTool()
        val input = "banana\nApple\ncherry"
        val result = tool.execute(LineOperationsInput(input, LineOperation.SORT_AZ, ignoreCase = true))
        assertTrue(result is ToolResult.Success)
        assertEquals("Apple\nbanana\ncherry", (result as ToolResult.Success).data)
    }

    @Test
    fun testLineOperations_removeDuplicates() = runBlocking {
        val tool = LineOperationsTool()
        val input = "one\nTwo\none\nthree\nTWO"
        val result = tool.execute(LineOperationsInput(input, LineOperation.REMOVE_DUPLICATES, ignoreCase = true))
        assertTrue(result is ToolResult.Success)
        assertEquals("one\nTwo\nthree", (result as ToolResult.Success).data)
    }

    @Test
    fun testLineOperations_reverseAndNumbering() = runBlocking {
        val tool = LineOperationsTool()
        val input = "A\nB\nC"
        val revRes = tool.execute(LineOperationsInput(input, LineOperation.REVERSE_ORDER))
        assertTrue(revRes is ToolResult.Success)
        assertEquals("C\nB\nA", (revRes as ToolResult.Success).data)

        val numRes = tool.execute(LineOperationsInput(input, LineOperation.ADD_LINE_NUMBERS))
        assertTrue(numRes is ToolResult.Success)
        assertEquals("1. A\n2. B\n3. C", (numRes as ToolResult.Success).data)
    }

    // --- FindAndReplaceTool ---
    @Test
    fun testFindAndReplace_emptyInput() = runBlocking {
        val tool = FindAndReplaceTool()
        val result = tool.execute(FindAndReplaceInput("", "abc", "def"))
        assertTrue(result is ToolResult.Success)
        assertEquals("", (result as ToolResult.Success).data.resultText)
    }

    @Test
    fun testFindAndReplace_emptyQueryFails() = runBlocking {
        val tool = FindAndReplaceTool()
        val result = tool.execute(FindAndReplaceInput("hello world", "", "test"))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testFindAndReplace_basicAndWholeWord() = runBlocking {
        val tool = FindAndReplaceTool()
        val text = "The cat scattered the catnip next to the cat."

        val basicRes = tool.execute(FindAndReplaceInput(text, "cat", "dog"))
        assertTrue(basicRes is ToolResult.Success)
        val basicData = (basicRes as ToolResult.Success).data
        assertEquals(4, basicData.matchCount)
        assertEquals("The dog sdogtered the dognip next to the dog.", basicData.resultText)

        val wholeWordRes = tool.execute(FindAndReplaceInput(text, "cat", "dog", wholeWord = true))
        assertTrue(wholeWordRes is ToolResult.Success)
        val wholeWordData = (wholeWordRes as ToolResult.Success).data
        assertEquals(2, wholeWordData.matchCount)
        assertEquals("The dog scattered the catnip next to the dog.", wholeWordData.resultText)
    }

    @Test
    fun testFindAndReplace_regex() = runBlocking {
        val tool = FindAndReplaceTool()
        val text = "Item 123, SKU 456, Order 789"
        val result = tool.execute(FindAndReplaceInput(text, "\\d+", "#", useRegex = true))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3, data.matchCount)
        assertEquals("Item #, SKU #, Order #", data.resultText)
    }

    // --- LoremIpsumGeneratorTool ---
    @Test
    fun testLoremIpsumGenerator_words() = runBlocking {
        val tool = LoremIpsumGeneratorTool()
        val result = tool.execute(LoremIpsumInput(unit = LoremUnit.WORDS, count = 10, startWithStandardLead = true))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        val words = data.split(" ")
        assertEquals(10, words.size)
        assertTrue(data.startsWith("Lorem ipsum"))
    }

    @Test
    fun testLoremIpsumGenerator_paragraphs() = runBlocking {
        val tool = LoremIpsumGeneratorTool()
        val result = tool.execute(LoremIpsumInput(unit = LoremUnit.PARAGRAPHS, count = 2))
        assertTrue(result is ToolResult.Success)
        val paragraphs = (result as ToolResult.Success).data.split("\n\n")
        assertEquals(2, paragraphs.size)
    }

    // --- TextDiffTool ---
    @Test
    fun testTextDiff_identicalText() = runBlocking {
        val tool = TextDiffTool()
        val text = "line 1\nline 2"
        val result = tool.execute(TextDiffInput(text, text))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(0, data.additions)
        assertEquals(0, data.deletions)
        assertEquals(2, data.unchanged)
    }

    @Test
    fun testTextDiff_changesDetected() = runBlocking {
        val tool = TextDiffTool()
        val orig = "Apple\nBanana\nCherry"
        val mod = "Apple\nBlueberry\nCherry\nDate"
        val result = tool.execute(TextDiffInput(orig, mod))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(2, data.additions) // Blueberry, Date
        assertEquals(1, data.deletions) // Banana
        assertEquals(2, data.unchanged) // Apple, Cherry
    }
}
