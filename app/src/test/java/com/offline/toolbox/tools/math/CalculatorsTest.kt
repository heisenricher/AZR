package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorsTest {

    // --- TipCalculatorTool ---
    @Test
    fun testTipCalculator_basicAndSplit() = runBlocking {
        val tool = TipCalculatorTool()
        val result = tool.execute(TipInput(billAmount = 100.0, tipPercentage = 15.0, splitCount = 2))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(15.0, data.tipAmount, 0.001)
        assertEquals(115.0, data.totalBillWithTip, 0.001)
        assertEquals(7.5, data.tipPerPerson, 0.001)
        assertEquals(57.5, data.totalPerPerson, 0.001)
    }

    @Test
    fun testTipCalculator_roundUp() = runBlocking {
        val tool = TipCalculatorTool()
        val result = tool.execute(TipInput(billAmount = 82.35, tipPercentage = 18.0, roundUpTotal = true))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(98.0, data.totalBillWithTip, 0.001)
    }

    // --- BmiCalculatorTool ---
    @Test
    fun testBmiCalculator_metricNormal() = runBlocking {
        val tool = BmiCalculatorTool()
        val result = tool.execute(BmiInput(weight = 70.0, height = 175.0, unitSystem = BmiUnitSystem.METRIC))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(22.86, data.bmiScore, 0.05)
        assertTrue(data.category.contains("Normal", ignoreCase = true))
    }

    @Test
    fun testBmiCalculator_invalidZeroFails() = runBlocking {
        val tool = BmiCalculatorTool()
        val result = tool.execute(BmiInput(weight = 0.0, height = 170.0))
        assertTrue(result is ToolResult.Failure)
    }

    // --- AverageCalculatorTool ---
    @Test
    fun testAverageCalculator_statistics() = runBlocking {
        val tool = AverageCalculatorTool()
        val result = tool.execute("10, 20, 30, 40, 50")
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(5, data.count)
        assertEquals(150.0, data.sum, 0.001)
        assertEquals(30.0, data.mean, 0.001)
        assertEquals(30.0, data.median, 0.001)
        assertEquals(10.0, data.min, 0.001)
        assertEquals(50.0, data.max, 0.001)
    }

    @Test
    fun testAverageCalculator_emptyFails() = runBlocking {
        val tool = AverageCalculatorTool()
        val result = tool.execute("abc, def")
        assertTrue(result is ToolResult.Failure)
    }

    // --- FractionCalculatorTool ---
    @Test
    fun testFractionCalculator_additionAndMultiplication() = runBlocking {
        val tool = FractionCalculatorTool()
        // 1/2 + 1/4 = 3/4
        val addRes = tool.execute(FractionInput(1, 2, FractionOperator.ADD, 1, 4))
        assertTrue(addRes is ToolResult.Success)
        val addData = (addRes as ToolResult.Success).data
        assertEquals("3/4", addData.simplifiedFraction)
        assertEquals(0.75, addData.decimalValue, 0.001)

        // 3/4 * 2/3 = 1/2
        val mulRes = tool.execute(FractionInput(3, 4, FractionOperator.MULTIPLY, 2, 3))
        assertTrue(mulRes is ToolResult.Success)
        assertEquals("1/2", (mulRes as ToolResult.Success).data.simplifiedFraction)
    }

    @Test
    fun testFractionCalculator_zeroDenominatorFails() = runBlocking {
        val tool = FractionCalculatorTool()
        val res = tool.execute(FractionInput(1, 0, FractionOperator.ADD, 1, 2))
        assertTrue(res is ToolResult.Failure)
    }
}
