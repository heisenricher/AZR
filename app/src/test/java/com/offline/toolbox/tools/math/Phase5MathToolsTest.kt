package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5MathToolsTest {

    private val fuelTool = FuelCostCalculatorTool()
    private val gpaTool = GpaCalculatorTool()

    @Test
    fun testFuelCost_singlePassengerMetric() = runTest {
        val result = fuelTool.execute(
            FuelCostInput(
                distance = 100.0,
                efficiencyValue = 8.0, // 8 L / 100km
                fuelUnit = FuelUnit.METRIC_L_PER_100KM,
                fuelPricePerUnit = 1.50, // $1.50 / L
                passengers = 1
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(8.0, data.totalFuelConsumed, 0.001)
        assertEquals(12.0, data.totalTripCost, 0.001)
        assertEquals(12.0, data.costPerPassenger, 0.001)
        assertEquals(0.12, data.costPerDistanceUnit, 0.001)
    }

    @Test
    fun testFuelCost_carpoolSplit() = runTest {
        val result = fuelTool.execute(
            FuelCostInput(
                distance = 200.0,
                efficiencyValue = 10.0, // 20 L required
                fuelUnit = FuelUnit.METRIC_L_PER_100KM,
                fuelPricePerUnit = 2.0, // $40 total
                passengers = 4
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(20.0, data.totalFuelConsumed, 0.001)
        assertEquals(40.0, data.totalTripCost, 0.001)
        assertEquals(10.0, data.costPerPassenger, 0.001)
    }

    @Test
    fun testGpa_standard4Scale() = runTest {
        // Course 1: A (4.0) x 3 credits = 12.0
        // Course 2: B (3.0) x 3 credits = 9.0
        // Total points = 21.0, total credits = 6 -> GPA = 3.5
        val courses = listOf(
            CourseEntry("Math", 3.0, "A"),
            CourseEntry("Physics", 3.0, "B")
        )
        val result = gpaTool.execute(GpaInput(courses, GpaScale.SCALE_4_0))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3.5, data.semesterGpa, 0.01)
        assertEquals(3.5, data.cumulativeGpa, 0.01)
        assertEquals(6.0, data.semesterCredits, 0.01)
    }

    @Test
    fun testGpa_withPriorCumulative() = runTest {
        val courses = listOf(
            CourseEntry("Algorithms", 4.0, "A") // 4.0 * 4 = 16 pts
        )
        val result = gpaTool.execute(
            GpaInput(
                courses = courses,
                scale = GpaScale.SCALE_4_0,
                priorCumulativeGpa = 3.0,
                priorCredits = 12.0 // 36 pts
                // Total = 52 pts / 16 cr = 3.25
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(4.0, data.semesterGpa, 0.01)
        assertEquals(3.25, data.cumulativeGpa, 0.01)
        assertEquals(16.0, data.totalCreditsOverall, 0.01)
    }
}
