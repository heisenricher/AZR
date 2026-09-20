package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PercentageCalculatorTest {

    private lateinit var tool: PercentageCalculatorTool

    @Before
    fun setUp() {
        tool = PercentageCalculatorTool()
    }

    @Test
    fun testPercentOf() = runBlocking {
        // 15% of 80 = 12
        val res = tool.execute(PercentageInput(PercentageCalculationType.PERCENT_OF, 15.0, 80.0))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("12", data.resultFormatted)
    }

    @Test
    fun testIsWhatPercent() = runBlocking {
        // 20 is 40% of 50
        val res = tool.execute(PercentageInput(PercentageCalculationType.IS_WHAT_PERCENT, 20.0, 50.0))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("40", data.resultFormatted)
    }

    @Test
    fun testDivisionByZeroDefense() = runBlocking {
        val res = tool.execute(PercentageInput(PercentageCalculationType.IS_WHAT_PERCENT, 20.0, 0.0))
        assertTrue(res is ToolResult.Failure)
        val failure = res as ToolResult.Failure
        assertTrue(failure.message.contains("zero"))
    }

    @Test
    fun testPercentChange() = runBlocking {
        // From 50 to 75 is +50%
        val res = tool.execute(PercentageInput(PercentageCalculationType.PERCENT_CHANGE, 50.0, 75.0))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("50", data.resultFormatted)
    }
}
