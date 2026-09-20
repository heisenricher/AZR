package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase6MathToolsTest {

    private val sciTool = ScientificNotationTool()
    private val denomTool = DenominationCalculatorTool()
    private val primeTool = PrimeFactorizationTool()

    @Test
    fun testScientificNotation_largeNumber() = runTest {
        val result = sciTool.execute(ScientificNotationInput(
            inputNumber = "1234500000",
            significantDigits = 4
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(9, data.orderOfMagnitude)
        assertTrue(data.scientificNotation.startsWith("1.235"))
        assertTrue(data.siPrefixedValue.contains("G"))
        assertEquals("Giga (10^9)", data.siPrefixName)
    }

    @Test
    fun testScientificNotation_smallNumber() = runTest {
        val result = sciTool.execute(ScientificNotationInput(
            inputNumber = "0.000045",
            significantDigits = 3
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(-5, data.orderOfMagnitude)
        assertTrue(data.siPrefixedValue.contains("µ") || data.engineeringNotation.contains("e-06"))
    }

    @Test
    fun testScientificNotation_invalidInput() = runTest {
        val result = sciTool.execute(ScientificNotationInput(
            inputNumber = "not_a_number"
        ))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testDenominationCalculator_usdBreakdown() = runTest {
        val result = denomTool.execute(DenominationInput(
            amount = 187.35,
            currency = CurrencyPreset.USD
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(187.35, data.totalAmount, 0.001)
        assertEquals("$", data.currencySymbol)

        // 187.35 should have 1x$100, 1x$50, 1x$20, 1x$10, 1x$5, 1x$2, 0x$1, 1x$0.25, 1x$0.10
        val itemsMap = data.items.associate { it.denomination to it.count }
        assertEquals(1, itemsMap[100.0] ?: 0)
        assertEquals(1, itemsMap[50.0] ?: 0)
        assertEquals(1, itemsMap[20.0] ?: 0)
        assertEquals(1, itemsMap[10.0] ?: 0)
        assertEquals(1, itemsMap[5.0] ?: 0)
        assertEquals(1, itemsMap[2.0] ?: 0)
        assertEquals(0, itemsMap[1.0] ?: 0)
        assertEquals(1, itemsMap[0.25] ?: 0)
        assertEquals(1, itemsMap[0.10] ?: 0)
    }

    @Test
    fun testDenominationCalculator_inrBreakdown() = runTest {
        val result = denomTool.execute(DenominationInput(
            amount = 1750.0,
            currency = CurrencyPreset.INR
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("₹", data.currencySymbol)
        val itemsMap = data.items.associate { it.denomination to it.count }
        assertEquals(3, itemsMap[500.0] ?: 0) // 1500
        assertEquals(1, itemsMap[200.0] ?: 0) // 200
        assertEquals(1, itemsMap[50.0] ?: 0)  // 50 -> total 1750
    }

    @Test
    fun testPrimeFactorization_composite360() = runTest {
        val result = primeTool.execute(PrimeFactorInput(number = 360L))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertFalse(data.isPrime)
        // 360 = 2^3 * 3^2 * 5^1
        val factorsMap = data.primeFactors.associate { it.prime to it.exponent }
        assertEquals(3, factorsMap[2L] ?: 0)
        assertEquals(2, factorsMap[3L] ?: 0)
        assertEquals(1, factorsMap[5L] ?: 0)
        // 360 has 24 divisors
        assertEquals(24, data.divisorCount)
        // Euler totient phi(360) = 360 * (1 - 1/2) * (1 - 1/3) * (1 - 1/5) = 360 * 1/2 * 2/3 * 4/5 = 96
        assertEquals(96L, data.eulerTotient)
    }

    @Test
    fun testPrimeFactorization_primeNumber() = runTest {
        val result = primeTool.execute(PrimeFactorInput(number = 997L))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.isPrime)
        assertEquals(1, data.primeFactors.size)
        assertEquals(997L, data.primeFactors[0].prime)
        assertEquals(1, data.primeFactors[0].exponent)
        assertEquals(2, data.divisorCount)
        assertEquals(996L, data.eulerTotient)
    }

    @Test
    fun testPrimeFactorization_edgeCases() = runTest {
        val result = primeTool.execute(PrimeFactorInput(number = 1L))
        assertTrue(result is ToolResult.Failure)
    }
}
