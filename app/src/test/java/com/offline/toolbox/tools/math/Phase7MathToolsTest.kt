package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7MathToolsTest {

    private val matrixTool = MatrixCalculatorTool()
    private val statsTool = StatisticsDistributionTool()
    private val cagrTool = CompoundAnnualGrowthRateTool()

    @Test
    fun testMatrix_2x2DeterminantAndInverse() = runTest {
        // [ 4, 7 ]
        // [ 2, 6 ] -> det = 4*6 - 7*2 = 24 - 14 = 10
        val result = matrixTool.execute(MatrixInput(
            matrixA = "4, 7\n2, 6",
            scalarMultiplier = 3.0
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(10.0, data.determinant, 0.001)
        assertEquals(10.0, data.trace, 0.001) // 4 + 6
        assertTrue(data.isInvertible)
        assertTrue(data.inverseMatrix.contains("0.60")) // 6 / 10
    }

    @Test
    fun testMatrix_3x3DeterminantAndTrace() = runTest {
        // Identity matrix 3x3
        val result = matrixTool.execute(MatrixInput(
            matrixA = "1, 0, 0\n0, 1, 0\n0, 0, 1",
            scalarMultiplier = 1.0
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1.0, data.determinant, 0.001)
        assertEquals(3.0, data.trace, 0.001)
        assertTrue(data.isInvertible)
    }

    @Test
    fun testMatrix_singularMatrixNonInvertible() = runTest {
        // Col 1 == Col 2 => det = 0
        val result = matrixTool.execute(MatrixInput(
            matrixA = "2, 2\n4, 4"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(0.0, data.determinant, 0.001)
        assertFalse(data.isInvertible)
        assertTrue(data.inverseMatrix.contains("Non-invertible"))
    }

    @Test
    fun testStats_standardNormalDistribution() = runTest {
        // Standard normal (mean=0, sigma=1) at x=1.96
        val result = statsTool.execute(StatisticsDistributionInput(
            xValue = 1.96,
            mean = 0.0,
            standardDeviation = 1.0
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1.96, data.zScore, 0.001)
        assertEquals(0.975, data.cdfValue, 0.005) // CDF at 1.96 ~ 0.9750
        assertEquals(0.05, data.twoTailedPValue, 0.01) // p ~ 0.05
        assertEquals(-1.95996, data.ci95Lower, 0.01)
        assertEquals(1.95996, data.ci95Upper, 0.01)
    }

    @Test
    fun testStats_invalidStdDevFails() = runTest {
        val result = statsTool.execute(StatisticsDistributionInput(
            xValue = 5.0,
            mean = 0.0,
            standardDeviation = -1.0
        ))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testCagr_calculationAndProjections() = runTest {
        // $10,000 -> $25,000 over 5 years: CAGR = (2.5)^(0.2) - 1 = ~20.11%
        val result = cagrTool.execute(CagrInput(
            initialInvestment = 10000.0,
            finalValue = 25000.0,
            durationYears = 5.0
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(20.11, data.cagrPercentage, 0.1)
        assertEquals(150.0, data.absoluteReturnPercentage, 0.01)
        assertEquals(15000.0, data.totalProfit, 0.01)
        assertTrue(data.projectedValue10Years > 60000.0)
    }

    @Test
    fun testCagr_invalidInputFails() = runTest {
        val result = cagrTool.execute(CagrInput(
            initialInvestment = -500.0,
            finalValue = 1000.0,
            durationYears = 2.0
        ))
        assertTrue(result is ToolResult.Failure)
    }
}
