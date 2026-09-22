package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class Phase11MathToolsTest {

    private val monteCarloTool = MonteCarloPiSimulatorTool()
    private val ytmTool = BondYieldToMaturityTool()
    private val kinematicsTool = KinematicsTrajectoryTool()

    @Test
    fun testMonteCarloPi_accurateConvergence() = runTest {
        val res = monteCarloTool.execute(MonteCarloPiInput(totalSamples = 100000, randomSeed = 42L))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(100000, data.totalSamples)
        assertTrue("Estimated Pi should be close to 3.14159", abs(data.estimatedPi - Math.PI) < 0.05)
        assertTrue(data.relativeErrorPercent < 2.0)
        assertEquals(10, data.convergenceHistory.size)
    }

    @Test
    fun testMonteCarloPi_invalidSamples() = runTest {
        val res = monteCarloTool.execute(MonteCarloPiInput(totalSamples = 10))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testBondYieldToMaturity_discountBondSolver() = runTest {
        val input = BondYtmInput(
            faceValue = 1000.0,
            marketPrice = 950.0,
            couponRatePercent = 5.0,
            yearsToMaturity = 10.0,
            paymentFrequencyPerYear = 2
        )
        val res = ytmTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // For a discount bond, YTM > Coupon Rate (5.0%)
        assertTrue(data.exactYtmPercent > 5.0)
        assertTrue(data.exactYtmPercent < 6.5)
        assertTrue(data.macaulayDurationYears > 6.0 && data.macaulayDurationYears < 10.0)
        assertTrue(data.convexity > 0.0)
    }

    @Test
    fun testBondYieldToMaturity_invalidPrice() = runTest {
        val res = ytmTool.execute(BondYtmInput(marketPrice = -100.0))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testKinematicsTrajectory_45DegreeLaunch() = runTest {
        val input = KinematicsInput(
            initialVelocityMps = 50.0,
            launchAngleDegrees = 45.0,
            initialHeightMeters = 0.0,
            gravityMps2 = 9.80665
        )
        val res = kinematicsTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // Range = v0^2 / g = 2500 / 9.80665 ≈ 254.93 m
        assertEquals(254.93, data.horizontalRangeMeters, 1.0)
        // Apogee = v0y^2 / 2g ≈ 63.73 m
        assertEquals(63.73, data.maxApogeeMeters, 1.0)
        // Total flight time ≈ 7.21 s
        assertEquals(7.21, data.totalFlightTimeSeconds, 0.5)
        assertEquals(11, data.waypoints.size)
    }

    @Test
    fun testKinematicsTrajectory_negativeVelocityFails() = runTest {
        val res = kinematicsTool.execute(KinematicsInput(initialVelocityMps = -10.0))
        assertTrue(res is ToolResult.Failure)
    }
}
