package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LoanEmiCalculatorTest {

    private val tool = LoanEmiCalculatorTool()

    @Test
    fun testStandardEmiCalculation() = runBlocking {
        // $100,000 at 12% annual rate for 1 year (12 months)
        val res = tool.execute(
            LoanEmiInput(
                principal = 100000.0,
                annualRatePercent = 12.0,
                tenureValue = 1,
                tenureUnit = TenureUnit.YEARS,
                monthlyPrepayment = 0.0
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data

        // Expected monthly EMI ~ 8884.88
        assertTrue("EMI should be ~8884.88", abs(data.monthlyEmi - 8884.88) < 1.0)
        assertEquals(12, data.tenureMonthsOriginal)
        assertEquals(12, data.tenureMonthsActual)
        assertEquals(0, data.monthsSaved)
        assertEquals(0.0, data.interestSaved, 0.01)

        // Verify schedule rows count
        assertEquals(12, data.schedule.size)
        // Verify final closing balance is 0.0
        assertEquals(0.0, data.schedule.last().closingBalance, 0.01)
        // Verify schedule CSV output
        assertTrue(data.scheduleCsv.contains("Month,Opening Balance,Total Payment"))
        assertTrue(data.scheduleCsv.contains("12,"))
    }

    @Test
    fun testPrepaymentTenureAndInterestSavings() = runBlocking {
        // $200,000 at 9% annual for 5 years (60 months) with $1,000/mo extra prepayment
        val res = tool.execute(
            LoanEmiInput(
                principal = 200000.0,
                annualRatePercent = 9.0,
                tenureValue = 5,
                tenureUnit = TenureUnit.YEARS,
                monthlyPrepayment = 1000.0
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data

        assertEquals(60, data.tenureMonthsOriginal)
        assertTrue("Actual tenure should be reduced with prepayment", data.tenureMonthsActual < 60)
        assertTrue("Months saved should be > 0", data.monthsSaved > 0)
        assertTrue("Interest saved should be > 0", data.interestSaved > 0.0)
        assertTrue(data.schedule.last().closingBalance < 0.05)
    }

    @Test
    fun testZeroInterestLoan() = runBlocking {
        // $12,000 at 0% for 12 months
        val res = tool.execute(
            LoanEmiInput(
                principal = 12000.0,
                annualRatePercent = 0.0,
                tenureValue = 12,
                tenureUnit = TenureUnit.MONTHS
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(1000.0, data.monthlyEmi, 0.01)
        assertEquals(0.0, data.totalInterestRegular, 0.01)
    }

    @Test
    fun testInvalidLoanParameters() = runBlocking {
        val badPrincipal = tool.execute(LoanEmiInput(principal = -500.0))
        assertTrue(badPrincipal is ToolResult.Failure)

        val badTenure = tool.execute(LoanEmiInput(tenureValue = 0))
        assertTrue(badTenure is ToolResult.Failure)
    }
}
