package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9MathToolsTest {

    private val refinanceTool = LoanRefinanceComparatorTool()
    private val trigTool = TrigonometricFunctionsTool()
    private val constantsTool = ScientificConstantsTool()

    @Test
    fun testLoanRefinance_beneficialRefinanceCalculations() = runTest {
        val res = refinanceTool.execute(RefinanceInput(
            currentPrincipalBalance = 300000.0,
            currentAnnualRatePct = 6.5,
            remainingTermMonths = 300,
            newAnnualRatePct = 5.0,
            newTermMonths = 300,
            closingCosts = 3000.0
        ))

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.monthlySavings > 0)
        assertTrue(data.netLifetimeSavings > 0)
        assertTrue(data.breakevenMonths in 1.0..36.0)
        assertTrue(data.recommendation.contains("RECOMMENDED") || data.recommendation.contains("FAVORABLE"))
    }

    @Test
    fun testLoanRefinance_higherRateIsNotBeneficial() = runTest {
        val res = refinanceTool.execute(RefinanceInput(
            currentPrincipalBalance = 200000.0,
            currentAnnualRatePct = 5.0,
            remainingTermMonths = 240,
            newAnnualRatePct = 7.0,
            newTermMonths = 240,
            closingCosts = 2000.0
        ))

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.monthlySavings < 0)
    }

    @Test
    fun testTrigonometry_exactValuesAndUnits() = runTest {
        // 30 degrees: sin = 0.5, cos = 0.866025
        val res30 = trigTool.execute(TrigInput(angleValue = 30.0, unit = AngleUnit.DEGREES))
        assertTrue(res30 is ToolResult.Success)
        val data30 = (res30 as ToolResult.Success).data
        assertEquals(0.5, data30.sinVal, 0.0001)
        assertEquals(0.8660, data30.cosVal, 0.001)
        assertEquals(0.5773, data30.tanVal ?: 0.0, 0.001)

        // 90 degrees: tan is undefined
        val res90 = trigTool.execute(TrigInput(angleValue = 90.0, unit = AngleUnit.DEGREES))
        assertTrue(res90 is ToolResult.Success)
        val data90 = (res90 as ToolResult.Success).data
        assertEquals(1.0, data90.sinVal, 0.0001)
        assertNull(data90.tanVal) // undefined at 90 deg

        // Hyperbolic at 0: sinh(0) = 0, cosh(0) = 1, tanh(0) = 0
        val res0 = trigTool.execute(TrigInput(angleValue = 0.0, unit = AngleUnit.RADIANS))
        assertTrue(res0 is ToolResult.Success)
        val data0 = (res0 as ToolResult.Success).data
        assertEquals(0.0, data0.sinhVal, 0.0001)
        assertEquals(1.0, data0.coshVal, 0.0001)
        assertEquals(0.0, data0.tanhVal, 0.0001)
    }

    @Test
    fun testScientificConstants_lookupAndFiltering() = runTest {
        val resAll = constantsTool.execute(ConstantsInput(category = ConstantCategory.ALL))
        assertTrue(resAll is ToolResult.Success)
        val dataAll = (resAll as ToolResult.Success).data
        assertTrue(dataAll.constants.size >= 20)

        // Speed of light check
        val c = dataAll.constants.firstOrNull { it.symbol == "c" }
        assertNotNull(c)
        assertTrue(c?.valueStr?.contains("299,792,458") == true)

        // Search query filter
        val resSearch = constantsTool.execute(ConstantsInput(query = "Planck", category = ConstantCategory.ALL))
        assertTrue(resSearch is ToolResult.Success)
        val dataSearch = (resSearch as ToolResult.Success).data
        assertTrue(dataSearch.constants.any { it.name.contains("Planck") })

        // Category filter
        val resAstro = constantsTool.execute(ConstantsInput(category = ConstantCategory.ASTRONOMICAL))
        assertTrue(resAstro is ToolResult.Success)
        val dataAstro = (resAstro as ToolResult.Success).data
        assertTrue(dataAstro.constants.all { it.category == ConstantCategory.ASTRONOMICAL })
    }
}
