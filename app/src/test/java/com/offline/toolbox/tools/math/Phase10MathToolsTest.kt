package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10MathToolsTest {

    private val bsTool = BlackScholesOptionPricerTool()
    private val dopplerTool = DopplerEffectCalculatorTool()
    private val romanTool = RomanNumeralsAdvancedTool()

    @Test
    fun testBlackScholes_atTheMoneyBenchmark() = runTest {
        // S = 100, K = 100, T = 1 year, r = 5% (0.05), sigma = 20% (0.20)
        // Standard Black-Scholes benchmark: Call ≈ $10.4506, Put ≈ $5.5735
        val res = bsTool.execute(
            BlackScholesInput(
                spotPrice = 100.0,
                strikePrice = 100.0,
                timeToMaturityYears = 1.0,
                riskFreeRatePercent = 5.0,
                volatilityPercent = 20.0
            )
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(10.45, data.callPrice, 0.05)
        assertEquals(5.57, data.putPrice, 0.05)
        assertEquals(0.63, data.callDelta, 0.05)
        assertEquals(-0.36, data.putDelta, 0.05)
        assertTrue(data.gamma > 0.0)
        assertTrue(data.vega > 0.0)
    }

    @Test
    fun testDopplerEffect_acousticApproachingSource() = runTest {
        val res = dopplerTool.execute(
            DopplerInput(
                domain = DopplerDomain.ACOUSTIC_SOUND,
                sourceFrequencyHz = 1000.0,
                sourceVelocityMs = 30.0,
                airTemperatureCelsius = 20.0
            )
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.observedFrequencyHz > 1000.0)
        assertTrue(data.frequencyShiftHz > 0.0)
        assertEquals("Higher Pitch (Approaching)", data.classification)
    }

    @Test
    fun testDopplerEffect_relativisticBlueshift() = runTest {
        val res = dopplerTool.execute(
            DopplerInput(
                domain = DopplerDomain.OPTICAL_RELATIVISTIC,
                sourceFrequencyHz = 5.0e14, // Green optical light
                relativeVelocityKmS = 30000.0 // 30,000 km/s (~0.1c) approaching
            )
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.observedFrequencyHz > 5.0e14)
        assertTrue(data.redshiftZ != null && data.redshiftZ!! < 0.0) // z < 0 for blueshift
        assertTrue(data.classification.contains("Blueshift"))
    }

    @Test
    fun testRomanNumeralsAdvanced_vinculumRoundtrip() = runTest {
        val number = 2542456L
        val encRes = romanTool.execute(
            RomanNumeralsInput(value = number.toString(), mode = RomanConversionMode.INTEGER_TO_ROMAN)
        )
        assertTrue(encRes is ToolResult.Success)
        val data = (encRes as ToolResult.Success).data
        val vincStr = data.vinculumRepresentation
        assertTrue(vincStr.contains("\u0305")) // Has combining overline

        val decRes = romanTool.execute(
            RomanNumeralsInput(value = vincStr, mode = RomanConversionMode.ROMAN_TO_INTEGER)
        )
        assertTrue(decRes is ToolResult.Success)
        assertEquals(number, (decRes as ToolResult.Success).data.integerVal)
    }

    @Test
    fun testRomanNumeralsAdvanced_bracketNotationDecode() = runTest {
        val bracketStr = "[M][M][D][XL]MMCDLVI" // 2,542,456
        val decRes = romanTool.execute(
            RomanNumeralsInput(value = bracketStr, mode = RomanConversionMode.ROMAN_TO_INTEGER)
        )
        assertTrue(decRes is ToolResult.Success)
        assertEquals(2542456L, (decRes as ToolResult.Success).data.integerVal)
    }
}
