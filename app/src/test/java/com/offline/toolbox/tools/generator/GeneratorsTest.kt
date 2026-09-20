package com.offline.toolbox.tools.generator

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorsTest {

    // --- RandomNumberTool ---
    @Test
    fun testRandomNumber_boundsAndCount() = runBlocking {
        val tool = RandomNumberTool()
        val result = tool.execute(RandomNumberConfig(min = 10, max = 50, count = 10, sortResults = true))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(10, data.numbers.size)
        assertTrue(data.numbers.all { it in 10..50 })
        // Check sorted
        for (i in 0 until data.numbers.size - 1) {
            assertTrue(data.numbers[i] <= data.numbers[i + 1])
        }
    }

    @Test
    fun testRandomNumber_uniqueNumbers() = runBlocking {
        val tool = RandomNumberTool()
        val result = tool.execute(RandomNumberConfig(min = 1, max = 20, count = 15, uniqueOnly = true))
        assertTrue(result is ToolResult.Success)
        val numbers = (result as ToolResult.Success).data.numbers
        assertEquals(15, numbers.distinct().size)
    }

    @Test
    fun testRandomNumber_uniqueImpossibleFails() = runBlocking {
        val tool = RandomNumberTool()
        // Range size is 3 (1..3), asking for 5 unique numbers
        val result = tool.execute(RandomNumberConfig(min = 1, max = 3, count = 5, uniqueOnly = true))
        assertTrue(result is ToolResult.Failure)
    }

    // --- RandomChoiceTool ---
    @Test
    fun testRandomChoice_customList() = runBlocking {
        val tool = RandomChoiceTool()
        val options = listOf("Pizza", "Burger", "Pasta")
        val result = tool.execute(RandomChoiceInput(mode = ChoiceMode.CUSTOM_LIST, itemsText = "Pizza, Burger, Pasta"))
        assertTrue(result is ToolResult.Success)
        val chosen = (result as ToolResult.Success).data.chosenResult
        assertTrue(options.contains(chosen))
    }

    @Test
    fun testRandomChoice_coinFlip() = runBlocking {
        val tool = RandomChoiceTool()
        val result = tool.execute(RandomChoiceInput(mode = ChoiceMode.COIN_FLIP))
        assertTrue(result is ToolResult.Success)
        val chosen = (result as ToolResult.Success).data.chosenResult
        assertTrue(chosen.equals("HEADS", ignoreCase = true) || chosen.equals("TAILS", ignoreCase = true))
    }

    @Test
    fun testRandomChoice_diceRoll() = runBlocking {
        val tool = RandomChoiceTool()
        val result = tool.execute(RandomChoiceInput(mode = ChoiceMode.DICE_ROLL, diceSides = 20, diceCount = 2))
        assertTrue(result is ToolResult.Success)
        val total = (result as ToolResult.Success).data.chosenResult.substringBefore(" ").toLongOrNull()
        assertTrue(total != null && total in 2..40)
    }

    @Test
    fun testRandomChoice_emptyListFails() = runBlocking {
        val tool = RandomChoiceTool()
        val result = tool.execute(RandomChoiceInput(mode = ChoiceMode.CUSTOM_LIST, itemsText = "   "))
        assertTrue(result is ToolResult.Failure)
    }
}
