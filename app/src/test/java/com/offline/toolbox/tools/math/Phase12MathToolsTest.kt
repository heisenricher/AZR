package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12MathToolsTest {

    private val annuityTool = AnnuityCalculatorTool()
    private val weatherTool = HeatIndexWindChillTool()
    private val rocketTool = TsiolkovskyRocketEquationTool()

    @Test
    fun testAnnuityCalculator_futureValue() = runTest {
        val input = AnnuityInput(
            solveFor = "FV",
            annuityType = "ORDINARY",
            periodicPayment = 500.0,
            annualRatePercent = 6.0,
            compoundingFrequency = 12,
            years = 5.0
        )
        val res = annuityTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // PMT = 500, r = 0.005, n = 60 -> FV = 500 * ((1.005)^60 - 1) / 0.005 ~= 34,885
        assertEquals(34885.0, data.futureValue, 50.0)
        assertEquals(30000.0, data.totalPayments, 0.1)
        assertTrue(data.totalInterest > 4800.0)
    }

    @Test
    fun testAnnuityCalculator_annuityDueHigherThanOrdinary() = runTest {
        val ordinaryRes = annuityTool.execute(
            AnnuityInput(solveFor = "FV", annuityType = "ORDINARY", periodicPayment = 200.0, annualRatePercent = 8.0, years = 3.0)
        )
        val dueRes = annuityTool.execute(
            AnnuityInput(solveFor = "FV", annuityType = "DUE", periodicPayment = 200.0, annualRatePercent = 8.0, years = 3.0)
        )
        assertTrue(ordinaryRes is ToolResult.Success)
        assertTrue(dueRes is ToolResult.Success)
        val ordFv = (ordinaryRes as ToolResult.Success).data.futureValue
        val dueFv = (dueRes as ToolResult.Success).data.futureValue
        assertTrue(dueFv > ordFv)
    }

    @Test
    fun testHeatIndex_calculatesRothfuszDanger() = runTest {
        val input = WeatherIndexInput(
            calculationMode = "HEAT_INDEX",
            temperature = 95.0,
            unit = "FAHRENHEIT",
            relativeHumidityPercent = 75.0
        )
        val res = weatherTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.apparentTempF > 115.0)
        assertTrue(data.dangerCategory == "Danger" || data.dangerCategory == "Extreme Danger")
    }

    @Test
    fun testWindChill_calculatesColdIndex() = runTest {
        val input = WeatherIndexInput(
            calculationMode = "WIND_CHILL",
            temperature = 15.0,
            unit = "FAHRENHEIT",
            windSpeedMph = 25.0
        )
        val res = weatherTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.apparentTempF < 0.0) // Wind chill should be below 0 F
        assertTrue(data.dangerCategory.contains("Caution") || data.dangerCategory.contains("Danger"))
    }

    @Test
    fun testRocketEquation_deltaVComputation() = runTest {
        val input = RocketInput(
            solveFor = "DELTA_V",
            initialWetMassKg = 549054.0,
            finalDryMassKg = 22200.0,
            specificImpulseSec = 311.0
        )
        val res = rocketTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // Ve = 311 * 9.80665 = 3049.86 m/s, ln(549054 / 22200) = ln(24.732) ~= 3.208 -> dV ~= 9784 m/s
        assertEquals(9784.0, data.deltaVMs, 100.0)
        assertTrue(data.massRatio > 24.0)
        assertTrue(data.missionComparisons.any { it.missionName.contains("Low Earth Orbit") && it.achievable })
    }
}
