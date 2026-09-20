package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.pow

data class RefinanceInput(
    val currentPrincipalBalance: Double = 300000.0,
    val currentAnnualRatePct: Double = 6.5,
    val remainingTermMonths: Int = 300,
    val newAnnualRatePct: Double = 5.0,
    val newTermMonths: Int = 300,
    val closingCosts: Double = 4500.0
)

data class RefinanceOutput(
    val currentMonthlyEmi: Double,
    val newMonthlyEmi: Double,
    val monthlySavings: Double,
    val breakevenMonths: Double,
    val currentTotalInterest: Double,
    val newTotalInterest: Double,
    val netLifetimeSavings: Double,
    val recommendation: String,
    val formattedReport: String,
    val summary: String
)

class LoanRefinanceComparatorTool : Tool<RefinanceInput, RefinanceOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "loan_refinance_comparator_tool",
        name = "Loan Refinance & Breakeven Comparator",
        description = "Evaluate mortgage/loan refinancing offers, monthly payment reduction, closing cost breakeven horizons, and lifetime interest savings.",
        category = ToolCategory.MATH,
        tags = listOf("refinance", "loan", "mortgage", "emi", "interest", "breakeven", "finance", "savings"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "AttachMoney"
    )

    override suspend fun execute(input: RefinanceInput): ToolResult<RefinanceOutput> {
        val startTime = System.currentTimeMillis()
        val p = input.currentPrincipalBalance
        val r1 = input.currentAnnualRatePct / 100.0 / 12.0
        val n1 = input.remainingTermMonths
        val r2 = input.newAnnualRatePct / 100.0 / 12.0
        val n2 = input.newTermMonths
        val fees = input.closingCosts

        if (p <= 0.0 || n1 <= 0 || n2 <= 0) {
            return ToolResult.Failure("Principal balance and loan terms must be greater than zero.")
        }
        if (input.currentAnnualRatePct <= 0.0 || input.newAnnualRatePct <= 0.0) {
            return ToolResult.Failure("Interest rates must be positive.")
        }

        fun calcEmi(principal: Double, ratePerMonth: Double, months: Int): Double {
            val factor = (1.0 + ratePerMonth).pow(months.toDouble())
            return principal * (ratePerMonth * factor) / (factor - 1.0)
        }

        val emi1 = calcEmi(p, r1, n1)
        val emi2 = calcEmi(p, r2, n2)

        val monthlySavings = emi1 - emi2
        val breakevenMonths = if (monthlySavings > 0) fees / monthlySavings else Double.POSITIVE_INFINITY

        val totalPaid1 = emi1 * n1
        val totalPaid2 = emi2 * n2
        val interestPaid1 = totalPaid1 - p
        val interestPaid2 = totalPaid2 - p

        val netSavings = (interestPaid1 - interestPaid2) - fees

        val rec = when {
            netSavings > 0 && breakevenMonths <= 36 -> "STRONGLY RECOMMENDED: Fast breakeven (${String.format(Locale.US, "%.1f", breakevenMonths)} mo) with substantial savings."
            netSavings > 0 -> "FAVORABLE: Long-term positive savings ($${String.format(Locale.US, "%,.2f", netSavings)})."
            else -> "NOT RECOMMENDED: Refinancing would increase overall costs by $${String.format(Locale.US, "%,.2f", -netSavings)}."
        }

        val report = buildString {
            appendLine("LOAN REFINANCING & BREAKEVEN AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Current Loan Balance:    $${String.format(Locale.US, "%,.2f", p)}")
            appendLine("Current Terms:           ${input.currentAnnualRatePct}% APR ($n1 months remaining)")
            appendLine("New Refinance Terms:     ${input.newAnnualRatePct}% APR ($n2 months new term)")
            appendLine("Refinance Closing Costs: $${String.format(Locale.US, "%,.2f", fees)}")
            appendLine()
            appendLine("MONTHLY PAYMENT COMPARISON:")
            appendLine("• Current Monthly EMI:   $${String.format(Locale.US, "%,.2f", emi1)} / month")
            appendLine("• New Refinance EMI:     $${String.format(Locale.US, "%,.2f", emi2)} / month")
            appendLine("• Monthly Payment Shift: ${if (monthlySavings >= 0) "Savings of $" else "Increase of $"}${String.format(Locale.US, "%,.2f", kotlin.math.abs(monthlySavings))} / month")
            appendLine()
            appendLine("FINANCIAL RECOVERY & BREAKEVEN:")
            appendLine("• Breakeven Horizon:     ${if (breakevenMonths.isInfinite()) "Never (Monthly cost increased)" else "${String.format(Locale.US, "%.1f", breakevenMonths)} months (${String.format(Locale.US, "%.1f", breakevenMonths / 12.0)} years)"}")
            appendLine("• Lifetime Interest (A): $${String.format(Locale.US, "%,.2f", interestPaid1)}")
            appendLine("• Lifetime Interest (B): $${String.format(Locale.US, "%,.2f", interestPaid2)}")
            appendLine("• Net Lifetime Savings:  $${String.format(Locale.US, "%,.2f", netSavings)} (After all fees)")
            appendLine()
            appendLine("VERDICT: $rec")
        }

        val summary = "Save $${String.format(Locale.US, "%.0f", monthlySavings)}/mo | Breakeven: ${if (breakevenMonths.isInfinite()) "N/A" else "${String.format(Locale.US, "%.0f", breakevenMonths)} mo"}"

        return ToolResult.Success(
            data = RefinanceOutput(
                currentMonthlyEmi = emi1,
                newMonthlyEmi = emi2,
                monthlySavings = monthlySavings,
                breakevenMonths = breakevenMonths,
                currentTotalInterest = interestPaid1,
                newTotalInterest = interestPaid2,
                netLifetimeSavings = netSavings,
                recommendation = rec,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
