package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

data class StatisticsDistributionInput(
    val xValue: Double = 1.96,
    val mean: Double = 0.0,
    val standardDeviation: Double = 1.0
)

data class StatisticsDistributionOutput(
    val xValue: Double,
    val mean: Double,
    val standardDeviation: Double,
    val zScore: Double,
    val pdfValue: Double,
    val cdfValue: Double,
    val twoTailedPValue: Double,
    val ci95Lower: Double,
    val ci95Upper: Double,
    val ci99Lower: Double,
    val ci99Upper: Double,
    val formattedReport: String,
    val summary: String
)

class StatisticsDistributionTool : Tool<StatisticsDistributionInput, StatisticsDistributionOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "statistics_distribution_tool",
        name = "Gaussian & Normal Distribution Calculator",
        description = "Compute Probability Density Function (PDF), Cumulative Distribution (CDF), Z-scores, and confidence intervals for normal distributions.",
        category = ToolCategory.MATH,
        tags = listOf("statistics", "normal", "gaussian", "distribution", "z-score", "pdf", "cdf", "probability", "p-value"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "ShowChart"
    )

    override suspend fun execute(input: StatisticsDistributionInput): ToolResult<StatisticsDistributionOutput> {
        val startTime = System.currentTimeMillis()
        val x = input.xValue
        val mu = input.mean
        val sigma = input.standardDeviation

        if (sigma <= 0.0) {
            return ToolResult.Failure("Standard deviation must be strictly greater than 0 (got $sigma).")
        }

        val z = (x - mu) / sigma

        // PDF: f(x) = (1 / (sigma * sqrt(2 * PI))) * exp(-0.5 * z^2)
        val pdf = (1.0 / (sigma * sqrt(2.0 * PI))) * exp(-0.5 * z * z)

        // CDF using polynomial approximation of error function (erf)
        val cdf = normalCdf(z)
        val twoTailedP = 2.0 * (1.0 - normalCdf(abs(z)))

        // 95% CI (1.95996) and 99% CI (2.57583)
        val ci95Low = mu - 1.95996 * sigma
        val ci95High = mu + 1.95996 * sigma
        val ci99Low = mu - 2.57583 * sigma
        val ci99High = mu + 2.57583 * sigma

        val report = buildString {
            appendLine("GAUSSIAN / NORMAL DISTRIBUTION ANALYSIS")
            appendLine("--------------------------------------------------")
            appendLine("Parameters:")
            appendLine("• Mean (μ):              ${String.format(Locale.US, "%.4f", mu)}")
            appendLine("• Standard Dev (σ):      ${String.format(Locale.US, "%.4f", sigma)}")
            appendLine("• Evaluation Point (x):  ${String.format(Locale.US, "%.4f", x)}")
            appendLine()
            appendLine("PROBABILITY METRICS")
            appendLine("• Standard Score (Z):    ${String.format(Locale.US, "%.4f", z)}")
            appendLine("• Density (PDF f(x)):    ${String.format(Locale.US, "%.6f", pdf)}")
            appendLine("• Cumulative (CDF P≤x):  ${String.format(Locale.US, "%.6f", cdf)} (${String.format(Locale.US, "%.2f", cdf * 100)}%)")
            appendLine("• Tail Area (P>x):       ${String.format(Locale.US, "%.6f", 1.0 - cdf)}")
            appendLine("• Two-Tailed p-value:    ${String.format(Locale.US, "%.6f", twoTailedP)}")
            appendLine()
            appendLine("CONFIDENCE INTERVALS")
            appendLine("• 95% Confidence Bounds: [${String.format(Locale.US, "%.3f", ci95Low)}, ${String.format(Locale.US, "%.3f", ci95High)}]")
            appendLine("• 99% Confidence Bounds: [${String.format(Locale.US, "%.3f", ci99Low)}, ${String.format(Locale.US, "%.3f", ci99High)}]")
        }

        val summary = "Z = ${String.format(Locale.US, "%.2f", z)} | CDF = ${String.format(Locale.US, "%.4f", cdf)}"

        return ToolResult.Success(
            data = StatisticsDistributionOutput(
                xValue = x,
                mean = mu,
                standardDeviation = sigma,
                zScore = z,
                pdfValue = pdf,
                cdfValue = cdf,
                twoTailedPValue = twoTailedP,
                ci95Lower = ci95Low,
                ci95Upper = ci95High,
                ci99Lower = ci99Low,
                ci99Upper = ci99High,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    // Abramowitz and Stegun approximation for standard normal CDF
    private fun normalCdf(z: Double): Double {
        val b1 = 0.319381530
        val b2 = -0.356563782
        val b3 = 1.781477937
        val b4 = -1.821255978
        val b5 = 1.330274429
        val p = 0.2316419
        val c = 0.3989422804014327 // 1 / sqrt(2 * PI)

        if (z >= 0.0) {
            val t = 1.0 / (1.0 + p * z)
            return 1.0 - c * exp(-z * z / 2.0) * t * (t * (t * (t * (t * b5 + b4) + b3) + b2) + b1)
        } else {
            val t = 1.0 / (1.0 - p * z)
            return c * exp(-z * z / 2.0) * t * (t * (t * (t * (t * b5 + b4) + b3) + b2) + b1)
        }
    }
}
