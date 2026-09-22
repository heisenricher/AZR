package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

data class MonteCarloStep(
    val sampleCount: Int,
    val pointsInside: Int,
    val estimatedPi: Double,
    val absoluteError: Double
)

data class MonteCarloPiInput(
    val totalSamples: Int = 100000,
    val randomSeed: Long = 42L
)

data class MonteCarloPiOutput(
    val totalSamples: Int,
    val pointsInside: Int,
    val pointsOutside: Int,
    val estimatedPi: Double,
    val truePi: Double,
    val absoluteError: Double,
    val relativeErrorPercent: Double,
    val standardError: Double,
    val convergenceHistory: List<MonteCarloStep>,
    val formattedReport: String,
    val summary: String
)

class MonteCarloPiSimulatorTool : Tool<MonteCarloPiInput, MonteCarloPiOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "monte_carlo_pi_simulator_tool",
        name = "Monte Carlo Pi Simulator & Error Estimator",
        description = "Simulate numerical integration of Pi using Monte Carlo quadrant sampling, tracking standard error and convergence trajectory.",
        category = ToolCategory.MATH,
        tags = listOf("monte carlo", "pi", "simulation", "probability", "numerical analysis", "stochastic", "math", "convergence"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "PieChart"
    )

    override suspend fun execute(input: MonteCarloPiInput): ToolResult<MonteCarloPiOutput> {
        val startTime = System.currentTimeMillis()
        val n = input.totalSamples

        if (n < 100 || n > 10_000_000) {
            return ToolResult.Failure("Total sample count must be between 100 and 10,000,000.")
        }

        val rng = if (input.randomSeed != 0L) Random(input.randomSeed) else Random()
        var insideCount = 0
        val history = mutableListOf<MonteCarloStep>()
        val stepInterval = (n / 10).coerceAtLeast(1)

        for (i in 1..n) {
            val x = rng.nextDouble()
            val y = rng.nextDouble()
            if (x * x + y * y <= 1.0) {
                insideCount++
            }

            if (i % stepInterval == 0 || i == n) {
                val currentEst = 4.0 * insideCount / i
                history.add(
                    MonteCarloStep(
                        sampleCount = i,
                        pointsInside = insideCount,
                        estimatedPi = currentEst,
                        absoluteError = abs(currentEst - PI)
                    )
                )
            }
        }

        val estimatedPi = 4.0 * insideCount / n
        val absError = abs(estimatedPi - PI)
        val relError = (absError / PI) * 100.0
        val p = insideCount.toDouble() / n
        val stdError = 4.0 * sqrt((p * (1.0 - p)) / n)
        val outsideCount = n - insideCount

        val report = buildString {
            appendLine("MONTE CARLO PI SIMULATION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Total Samples (N):   $n")
            appendLine("Seed:                ${if (input.randomSeed != 0L) input.randomSeed.toString() else "System Entropy"}")
            appendLine("Inside Quadrant:     $insideCount (${String.format(Locale.US, "%.2f", p * 100)}%)")
            appendLine("Outside Quadrant:    $outsideCount")
            appendLine("--------------------------------------------------")
            appendLine(String.format(Locale.US, "Estimated Pi:        %.7f", estimatedPi))
            appendLine(String.format(Locale.US, "True Pi:             %.7f", PI))
            appendLine(String.format(Locale.US, "Absolute Error:      %.7f", absError))
            appendLine(String.format(Locale.US, "Relative Error:      %.5f%%", relError))
            appendLine(String.format(Locale.US, "Standard Error (SE): %.7f (95%% CI: ±%.5f)", stdError, 1.96 * stdError))
            appendLine("--------------------------------------------------")
            appendLine("CONVERGENCE PROGRESSION:")
            appendLine(String.format(Locale.US, "%-12s | %-12s | %-12s | %-12s", "Samples", "Inside", "Est. Pi", "Error"))
            appendLine("-------------+--------------+--------------+-------------")
            history.forEach { step ->
                appendLine(
                    String.format(
                        Locale.US,
                        "%-12d | %-12d | %-12.6f | %-12.6f",
                        step.sampleCount,
                        step.pointsInside,
                        step.estimatedPi,
                        step.absoluteError
                    )
                )
            }
        }

        return ToolResult.Success(
            data = MonteCarloPiOutput(
                totalSamples = n,
                pointsInside = insideCount,
                pointsOutside = outsideCount,
                estimatedPi = estimatedPi,
                truePi = PI,
                absoluteError = absError,
                relativeErrorPercent = relError,
                standardError = stdError,
                convergenceHistory = history,
                formattedReport = report,
                summary = String.format(Locale.US, "Pi ≈ %.5f (Error: %.4f%%, N=%d)", estimatedPi, relError, n)
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = String.format(Locale.US, "Simulated Pi ≈ %.5f", estimatedPi)
        )
    }
}
