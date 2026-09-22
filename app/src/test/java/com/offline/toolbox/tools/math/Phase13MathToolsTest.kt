package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase13MathToolsTest {

    private val ohmsTool = OhmsLawPowerTool()
    private val dewPointTool = DewPointRelativeHumidityTool()
    private val keplerTool = KeplerOrbitalPeriodTool()

    // 1. Ohm's Law & Power Tests
    @Test
    fun testOhmsLaw_solveFromVoltageAndResistance() = runTest {
        val input = OhmsLawInput(
            voltageVolts = 12.0,
            currentAmperes = null,
            resistanceOhms = 4.0,
            powerWatts = null,
            resistorBands = "Brown, Black, Red, Gold"
        )
        val res = ohmsTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(3.0, data.currentAmperes, 0.001)
        assertEquals(36.0, data.powerWatts, 0.001)
        assertEquals(1000.0, data.decodedResistorOhms ?: 0.0, 0.001)
        assertEquals(5.0, data.decodedTolerancePercent ?: 0.0, 0.001)
    }

    @Test
    fun testOhmsLaw_insufficientParametersFails() = runTest {
        val input = OhmsLawInput(
            voltageVolts = 12.0,
            currentAmperes = null,
            resistanceOhms = null,
            powerWatts = null
        )
        val res = ohmsTool.execute(input)
        assertTrue(res is ToolResult.Failure)
    }

    // 2. Dew Point & Relative Humidity Tests
    @Test
    fun testDewPoint_celsiusCalculation() = runTest {
        val input = DewPointInput(
            temperature = 25.0,
            unit = "CELSIUS",
            relativeHumidityPercent = 60.0
        )
        val res = dewPointTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // At 25°C and 60% RH, dew point is ~16.7°C
        assertEquals(16.7, data.dewPointC, 0.5)
        assertTrue(data.dewPointF > 60.0)
        assertTrue(data.absoluteHumidityGm3 > 10.0)
    }

    @Test
    fun testDewPoint_fahrenheitCalculation() = runTest {
        val input = DewPointInput(
            temperature = 77.0,
            unit = "FAHRENHEIT",
            relativeHumidityPercent = 60.0
        )
        val res = dewPointTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(25.0, data.inputTempC, 0.1)
        assertEquals(16.7, data.dewPointC, 0.5)
    }

    // 3. Kepler's Orbital Period Tests
    @Test
    fun testKeplerOrbital_issPeriodFromAltitude() = runTest {
        val input = KeplerOrbitalInput(
            solveMode = "PERIOD_FROM_ALTITUDE",
            primaryBody = "EARTH",
            altitudeKm = 420.0
        )
        val res = keplerTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // ISS completes orbit in ~92.9 minutes at ~7.66 km/s
        assertEquals(92.9, data.orbitalPeriodMinutes, 1.0)
        assertEquals(7.66, data.orbitalVelocityKms, 0.2)
        assertTrue(data.standardOrbitComparison.contains("ISS"))
    }

    @Test
    fun testKeplerOrbital_geostationaryAltitudeFromPeriod() = runTest {
        val input = KeplerOrbitalInput(
            solveMode = "ALTITUDE_FROM_PERIOD",
            primaryBody = "EARTH",
            targetPeriodHours = 23.9344696 // 1 sidereal day
        )
        val res = keplerTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // GEO altitude is ~35,786 km
        assertEquals(35786.0, data.altitudeKm, 100.0)
    }
}
