package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.pow

enum class CompoundFrequency(val periodsPerYear: Int, val label: String) {
    ANNUALLY(1, "Annually (1x/yr)"),
    SEMI_ANNUALLY(2, "Semi-Annually (2x/yr)"),
    QUARTERLY(4, "Quarterly (4x/yr)"),
    MONTHLY(12, "Monthly (12x/yr)"),
    DAILY(365, "Daily (365x/yr)")
}

data class CompoundInterestInput(
    val principal: Double,
    val annualRatePercent: Double,
    val years: Double,
    val frequency: CompoundFrequency = CompoundFrequency.ANNUALLY,
    val monthlyContribution: Double = 0.0
)

data class YearlyGrowth(
    val year: Int,
    val balance: Double,
    val totalInvested: Double,
    val totalInterest: Double
)

data class CompoundInterestOutput(
    val finalBalance: Double,
    val totalInvested: Double,
    val totalInterestEarned: Double,
    val formattedSummary: String,
    val yearlyBreakdown: List<YearlyGrowth>,
    val summary: String
)

class CompoundInterestTool : Tool<CompoundInterestInput, CompoundInterestOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "compound_interest",
        name = "Compound Interest Calculator",
        description = "Calculate investment growth, compound interest, contributions, and year-by-year projections.",
        category = ToolCategory.MATH,
        tags = listOf("compound", "interest", "finance", "investment", "growth", "savings", "apy", "money"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "TrendingUp"
    )

    override suspend fun execute(input: CompoundInterestInput): ToolResult<CompoundInterestOutput> {
        val startTime = System.currentTimeMillis()

        if (input.principal < 0.0 || input.annualRatePercent < 0.0 || input.years <= 0.0) {
            return ToolResult.Failure(
                message = "Invalid financial parameters.",
                userGuidance = "Principal and interest rate must be non-negative, and years must be greater than zero."
            )
        }

        val r = input.annualRatePercent / 100.0
        val n = input.frequency.periodsPerYear.toDouble()
        val totalYears = input.years.coerceIn(0.1, 100.0)

        val yearlyList = mutableListOf<YearlyGrowth>()
        val wholeYears = totalYears.toInt().coerceAtLeast(1)

        for (y in 1..wholeYears) {
            val t = y.toDouble()
            // Compound on initial principal: P * (1 + r/n)^(nt)
            val pGrowth = input.principal * (1.0 + r / n).pow(n * t)
            // Future value of monthly contributions: PMT * [((1 + r/12)^(12t) - 1) / (r/12)]
            val pmt = input.monthlyContribution
            val pmtGrowth = if (r > 0.0 && pmt > 0.0) {
                val rMonthly = r / 12.0
                pmt * (((1.0 + rMonthly).pow(12.0 * t) - 1.0) / rMonthly)
            } else {
                pmt * 12.0 * t
            }

            val balance = pGrowth + pmtGrowth
            val invested = input.principal + (pmt * 12.0 * t)
            val interest = (balance - invested).coerceAtLeast(0.0)

            yearlyList.add(YearlyGrowth(y, balance, invested, interest))
        }

        // Final result at input.years
        val finalP = input.principal * (1.0 + r / n).pow(n * totalYears)
        val finalPmt = if (r > 0.0 && input.monthlyContribution > 0.0) {
            val rMonthly = r / 12.0
            input.monthlyContribution * (((1.0 + rMonthly).pow(12.0 * totalYears) - 1.0) / rMonthly)
        } else {
            input.monthlyContribution * 12.0 * totalYears
        }

        val finalBalance = finalP + finalPmt
        val totalInvested = input.principal + (input.monthlyContribution * 12.0 * totalYears)
        val totalInterest = (finalBalance - totalInvested).coerceAtLeast(0.0)

        val formatted = buildString {
            appendLine("Final Balance:         \$${"%,.2f".format(Locale.US, finalBalance)}")
            appendLine("Total Contributions:   \$${"%,.2f".format(Locale.US, totalInvested)}")
            appendLine("Total Interest Earned: \$${"%,.2f".format(Locale.US, totalInterest)}")
            appendLine("Compounding Frequency: ${input.frequency.label}")
            appendLine("Investment Horizon:    $totalYears years")
            if (yearlyList.isNotEmpty()) {
                appendLine()
                appendLine("Year-by-Year Milestone Breakdown:")
                appendLine("Yr | Total Balance   | Total Invested  | Interest")
                appendLine("---|-----------------|-----------------|---------")
                yearlyList.forEach { yg ->
                    appendLine(
                        "%2d | \$%14s | \$%14s | \$%s".format(
                            Locale.US,
                            yg.year,
                            "%,.2f".format(Locale.US, yg.balance),
                            "%,.2f".format(Locale.US, yg.totalInvested),
                            "%,.2f".format(Locale.US, yg.totalInterest)
                        )
                    )
                }
            }
        }

        val summary = "Final: \$${"%,.2f".format(Locale.US, finalBalance)} (Earned \$${"%,.2f".format(Locale.US, totalInterest)} interest)"

        return ToolResult.Success(
            data = CompoundInterestOutput(
                finalBalance = finalBalance,
                totalInvested = totalInvested,
                totalInterestEarned = totalInterest,
                formattedSummary = formatted,
                yearlyBreakdown = yearlyList,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
