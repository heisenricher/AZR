package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase8MathToolsTest {

    private val polyTool = PolynomialRootSolverTool()
    private val vectorTool = VectorMathCalculatorTool()
    private val bmrTool = BmrTdeeCalculatorTool()

    @Test
    fun testPolynomial_quadraticRealRoots() = runTest {
        // x^2 - 5x + 6 = 0 -> roots are 2 and 3
        val result = polyTool.execute(PolynomialInput(
            degree = PolynomialDegree.QUADRATIC,
            a = 1.0,
            b = -5.0,
            c = 6.0
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(2, data.roots.size)
        assertTrue(data.roots.any { it.startsWith("2.0") })
        assertTrue(data.roots.any { it.startsWith("3.0") })
        assertEquals(1.0, data.discriminant, 0.001)
    }

    @Test
    fun testPolynomial_leadingZeroFails() = runTest {
        val result = polyTool.execute(PolynomialInput(
            degree = PolynomialDegree.QUADRATIC,
            a = 0.0,
            b = 2.0,
            c = 3.0
        ))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testVector_dotAndCrossProduct() = runTest {
        // A = (1, 0, 0), B = (0, 1, 0)
        // Dot = 0, Cross = (0, 0, 1), Angle = 90 deg
        val result = vectorTool.execute(VectorInput(
            ax = 1.0, ay = 0.0, az = 0.0,
            bx = 0.0, by = 1.0, bz = 0.0
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1.0, data.magnitudeA, 0.001)
        assertEquals(1.0, data.magnitudeB, 0.001)
        assertEquals(0.0, data.dotProduct, 0.001)
        assertEquals(90.0, data.angleDegrees, 0.001)
        assertTrue(data.crossProduct.contains("1.000"))
    }

    @Test
    fun testBmr_mifflinStJeorCalculation() = runTest {
        // Male, 75kg, 178cm, 28 years old
        // BMR = 10*75 + 6.25*178 - 5*28 + 5 = 750 + 1112.5 - 140 + 5 = 1727.5 ~ 1728 kcal
        val result = bmrTool.execute(BmrInput(
            weightKg = 75.0,
            heightCm = 178.0,
            ageYears = 28,
            gender = BiologicalGender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE // 1.55x
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1728, data.bmrCalories)
        assertEquals(2678, data.tdeeCalories) // 1728 * 1.55 = 2678.4 ~ 2678
        assertEquals(2178, data.cuttingCalories) // 2678 - 500
        assertEquals(3178, data.bulkingCalories) // 2678 + 500
    }

    @Test
    fun testBmr_invalidInputFails() = runTest {
        val result = bmrTool.execute(BmrInput(
            weightKg = -50.0
        ))
        assertTrue(result is ToolResult.Failure)
    }
}
