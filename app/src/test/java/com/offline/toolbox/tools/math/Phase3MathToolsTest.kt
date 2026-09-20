package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3MathToolsTest {

    // --- CompoundInterestTool ---
    @Test
    fun testCompoundInterest_annualGrowth() = runBlocking {
        val tool = CompoundInterestTool()
        // $1,000 at 10% for 2 years compounded annually -> 1000 * 1.10 * 1.10 = $1,210.00
        val input = CompoundInterestInput(
            principal = 1000.0,
            annualRatePercent = 10.0,
            years = 2.0,
            frequency = CompoundFrequency.ANNUALLY,
            monthlyContribution = 0.0
        )
        val result = tool.execute(input)
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1210.0, data.finalBalance, 0.01)
        assertEquals(1000.0, data.totalInvested, 0.01)
        assertEquals(210.0, data.totalInterestEarned, 0.01)
        assertEquals(2, data.yearlyBreakdown.size)
    }

    @Test
    fun testCompoundInterest_invalidParametersFails() = runBlocking {
        val tool = CompoundInterestTool()
        val result = tool.execute(CompoundInterestInput(-500.0, 5.0, 1.0))
        assertTrue(result is ToolResult.Failure)
    }

    // --- DiscountCalculatorTool ---
    @Test
    fun testDiscountCalculator_singleAndStacked() = runBlocking {
        val tool = DiscountCalculatorTool()
        // $100 with 20% off -> $80
        val singleRes = tool.execute(DiscountInput(originalPrice = 100.0, primaryDiscountPercent = 20.0))
        assertTrue(singleRes is ToolResult.Success)
        val singleData = (singleRes as ToolResult.Success).data
        assertEquals(80.0, singleData.finalPrice, 0.01)
        assertEquals(20.0, singleData.totalSavings, 0.01)

        // $100 with 20% off, plus 10% extra coupon, plus 10% sales tax
        // 100 -> 80 -> 72 -> + 7.20 tax = 79.20
        val stackedRes = tool.execute(
            DiscountInput(
                originalPrice = 100.0,
                primaryDiscountPercent = 20.0,
                additionalDiscountPercent = 10.0,
                salesTaxPercent = 10.0
            )
        )
        assertTrue(stackedRes is ToolResult.Success)
        val stackedData = (stackedRes as ToolResult.Success).data
        assertEquals(79.20, stackedData.finalPrice, 0.01)
        assertEquals(28.0, stackedData.totalSavings, 0.01)
    }
}
