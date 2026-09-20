package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.pow

data class CagrInput(
    val initialInvestment: Double = 10000.0,
    val finalValue: Double = 25000.0,
    val durationYears: Double = 5.0
)

data class CagrOutput(
    val initialInvestment: Double,
    val finalValue: Double,
    val durationYears: Double,
    val cagrPercentage: Double,
    val absoluteReturnPercentage: Double,
    val totalProfit: Double,
    val projectedValue10Years: Double,
    val projectedValue20Years: Double,
    val formattedReport: String,
    val summary: String
)

class CompoundAnnualGrowthRateTool : Tool<CagrInput, CagrOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "cagr_calculator_tool",
        name = "Compound Annual Growth Rate (CAGR)",
        description = "Calculate CAGR, absolute percentage returns, total capital growth, and future compounding projections over time.",
        category = ToolCategory.MATH,
        tags = listOf("cagr", "finance", "investment", "growth", "interest", "returns", "stocks", "mutual funds", "wealth"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "TrendingUp"
    )

    override suspend fun execute(input: CagrInput): ToolResult<CagrOutput> {
        val startTime = System.currentTimeMillis()
        val p0 = input.initialInvestment
        val p1 = input.finalValue
        val n = input.durationYears

        if (p0 <= 0.0) {
            return ToolResult.Failure("Initial investment must be greater than zero.")
        }
        if (p1 <= 0.0) {
            return ToolResult.Failure("Final value must be greater than zero.")
        }
        if (n <= 0.0) {
            return ToolResult.Failure("Duration in years must be greater than zero.")
        }

        // CAGR formula: (p1 / p0) ^ (1 / n) - 1
        val cagrRate = (p1 / p0).pow(1.0 / n) - 1.0
        val cagrPct = cagrRate * 100.0

        val totalProfit = p1 - p0
        val absoluteReturnPct = (totalProfit / p0) * 100.0

        // Future projections assuming the same CAGR
        val proj10 = p0 * (1.0 + cagrRate).pow(10.0)
        val proj20 = p0 * (1.0 + cagrRate).pow(20.0)

        val report = buildString {
            appendLine("COMPOUND ANNUAL GROWTH RATE (CAGR) REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Initial Capital:   ${String.format(Locale.US, "$%,.2f", p0)}")
            appendLine("Final Value:       ${String.format(Locale.US, "$%,.2f", p1)}")
            appendLine("Period:            ${String.format(Locale.US, "%.1f", n)} years")
            appendLine()
            appendLine("PERFORMANCE METRICS")
            appendLine("• CAGR (Annual):   ${String.format(Locale.US, "%.2f", cagrPct)}% per year")
            appendLine("• Absolute Return: ${String.format(Locale.US, "%.2f", absoluteReturnPct)}% total gain")
            appendLine("• Total Net Gain:  ${String.format(Locale.US, "$%,.2f", totalProfit)}")
            appendLine()
            appendLine("COMPOUNDING PROJECTIONS (at ${String.format(Locale.US, "%.2f", cagrPct)}% CAGR)")
            appendLine("• In 10 Years:     ${String.format(Locale.US, "$%,.2f", proj10)}")
            appendLine("• In 20 Years:     ${String.format(Locale.US, "$%,.2f", proj20)}")
        }

        val summary = "CAGR: ${String.format(Locale.US, "%.2f", cagrPct)}% (${String.format(Locale.US, "%.1f", n)} yrs)"

        return ToolResult.Success(
            data = CagrOutput(
                initialInvestment = p0,
                finalValue = p1,
                durationYears = n,
                cagrPercentage = cagrPct,
                absoluteReturnPercentage = absoluteReturnPct,
                totalProfit = totalProfit,
                projectedValue10Years = proj10,
                projectedValue20Years = proj20,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
