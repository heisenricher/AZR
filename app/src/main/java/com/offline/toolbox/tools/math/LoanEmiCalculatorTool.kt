package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.pow

enum class TenureUnit(val label: String) {
    YEARS("Years"),
    MONTHS("Months")
}

data class AmortizationRow(
    val month: Int,
    val openingBalance: Double,
    val emi: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val prepayment: Double,
    val closingBalance: Double
)

data class LoanEmiInput(
    val principal: Double = 100000.0,
    val annualRatePercent: Double = 8.5,
    val tenureValue: Int = 5,
    val tenureUnit: TenureUnit = TenureUnit.YEARS,
    val monthlyPrepayment: Double = 0.0
)

data class LoanEmiOutput(
    val monthlyEmi: Double,
    val totalPrincipal: Double,
    val totalInterestRegular: Double,
    val totalAmountRegular: Double,
    val totalInterestWithPrepayment: Double,
    val interestSaved: Double,
    val tenureMonthsOriginal: Int,
    val tenureMonthsActual: Int,
    val monthsSaved: Int,
    val schedule: List<AmortizationRow>,
    val scheduleCsv: String,
    val formattedReport: String,
    val summary: String
)

class LoanEmiCalculatorTool : Tool<LoanEmiInput, LoanEmiOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "loan_emi_calculator_tool",
        name = "Loan EMI & Amortization Calculator",
        description = "Calculate loan EMI, interest breakdowns, prepayment savings, and month-by-month amortization schedules.",
        category = ToolCategory.MATH,
        tags = listOf("emi", "loan", "mortgage", "interest", "finance", "amortization", "calculator", "prepayment"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "AccountBalance"
    )

    override suspend fun execute(input: LoanEmiInput): ToolResult<LoanEmiOutput> {
        val startTime = System.currentTimeMillis()

        if (input.principal <= 0.0) {
            return ToolResult.Failure("Principal amount must be greater than 0.")
        }
        if (input.annualRatePercent < 0.0) {
            return ToolResult.Failure("Interest rate cannot be negative.")
        }
        if (input.tenureValue <= 0) {
            return ToolResult.Failure("Loan tenure must be greater than 0.")
        }

        val totalMonthsOriginal = if (input.tenureUnit == TenureUnit.YEARS) {
            input.tenureValue * 12
        } else {
            input.tenureValue
        }

        val monthlyRate = (input.annualRatePercent / 12.0) / 100.0

        // EMI formula: E = P * r * (1+r)^n / ((1+r)^n - 1)
        val emi = if (monthlyRate > 0.0) {
            val factor = (1.0 + monthlyRate).pow(totalMonthsOriginal.toDouble())
            (input.principal * monthlyRate * factor) / (factor - 1.0)
        } else {
            input.principal / totalMonthsOriginal
        }

        // Regular schedule without prepayment
        val totalInterestRegular = (emi * totalMonthsOriginal) - input.principal
        val totalAmountRegular = emi * totalMonthsOriginal

        // Compute actual schedule with optional prepayment
        val schedule = mutableListOf<AmortizationRow>()
        var balance = input.principal
        var totalInterestPaid = 0.0
        var month = 1

        while (balance > 0.01 && month <= totalMonthsOriginal * 2) {
            val interestMonth = balance * monthlyRate
            val scheduledPrincipal = (emi - interestMonth).coerceAtMost(balance)
            val prepaymentActual = input.monthlyPrepayment.coerceAtMost(balance - scheduledPrincipal)
            val totalPrincipalThisMonth = (scheduledPrincipal + prepaymentActual).coerceAtMost(balance)
            val effectivePayment = totalPrincipalThisMonth + interestMonth
            val closing = (balance - totalPrincipalThisMonth).coerceAtLeast(0.0)

            schedule.add(
                AmortizationRow(
                    month = month,
                    openingBalance = balance,
                    emi = effectivePayment,
                    principalPaid = totalPrincipalThisMonth,
                    interestPaid = interestMonth,
                    prepayment = prepaymentActual,
                    closingBalance = closing
                )
            )

            totalInterestPaid += interestMonth
            balance = closing
            month++
        }

        val actualMonths = schedule.size
        val monthsSaved = (totalMonthsOriginal - actualMonths).coerceAtLeast(0)
        val interestSaved = (totalInterestRegular - totalInterestPaid).coerceAtLeast(0.0)

        val csv = buildString {
            appendLine("Month,Opening Balance,Total Payment,Principal Paid,Interest Paid,Prepayment,Closing Balance")
            schedule.forEach { r ->
                appendLine(
                    "${r.month}," +
                    "%.2f,".format(Locale.US, r.openingBalance) +
                    "%.2f,".format(Locale.US, r.emi) +
                    "%.2f,".format(Locale.US, r.principalPaid) +
                    "%.2f,".format(Locale.US, r.interestPaid) +
                    "%.2f,".format(Locale.US, r.prepayment) +
                    "%.2f".format(Locale.US, r.closingBalance)
                )
            }
        }

        val report = buildString {
            appendLine("LOAN EMI & AMORTIZATION REPORT")
            appendLine("--------------------------------")
            appendLine("Principal Amount:       %.2f".format(Locale.US, input.principal))
            appendLine("Annual Interest Rate:   %.2f%%".format(Locale.US, input.annualRatePercent))
            appendLine("Tenure:                 $totalMonthsOriginal months (${totalMonthsOriginal / 12}y ${totalMonthsOriginal % 12}m)")
            appendLine("Monthly Base EMI:       %.2f".format(Locale.US, emi))
            appendLine("--------------------------------")
            appendLine("Total Interest (Std):   %.2f".format(Locale.US, totalInterestRegular))
            appendLine("Total Payment (Std):    %.2f".format(Locale.US, totalAmountRegular))

            if (input.monthlyPrepayment > 0.0) {
                appendLine("--------------------------------")
                appendLine("WITH MONTHLY PREPAYMENT (%.2f/mo):".format(Locale.US, input.monthlyPrepayment))
                appendLine("Effective Tenure:       $actualMonths months (Saved $monthsSaved months!)")
                appendLine("Total Interest Paid:    %.2f".format(Locale.US, totalInterestPaid))
                appendLine("Total Interest Saved:   %.2f (%.1f%% saving)".format(
                    Locale.US,
                    interestSaved,
                    if (totalInterestRegular > 0) (interestSaved / totalInterestRegular) * 100.0 else 0.0
                ))
            }

            appendLine("--------------------------------")
            appendLine("FIRST 12 MONTHS AMORTIZATION:")
            appendLine("Mo |   Opening   |     EMI     |  Principal  |  Interest   |   Closing")
            schedule.take(12).forEach { r ->
                appendLine(
                    "%-2d | %11.2f | %11.2f | %11.2f | %11.2f | %11.2f".format(
                        Locale.US, r.month, r.openingBalance, r.emi, r.principalPaid, r.interestPaid, r.closingBalance
                    )
                )
            }
            if (schedule.size > 12) {
                appendLine("... and ${schedule.size - 12} more months (exportable to CSV)")
            }
        }

        val summary = "Monthly EMI: %.2f | Interest: %.2f".format(Locale.US, emi, totalInterestPaid)

        return ToolResult.Success(
            data = LoanEmiOutput(
                monthlyEmi = emi,
                totalPrincipal = input.principal,
                totalInterestRegular = totalInterestRegular,
                totalAmountRegular = totalAmountRegular,
                totalInterestWithPrepayment = totalInterestPaid,
                interestSaved = interestSaved,
                tenureMonthsOriginal = totalMonthsOriginal,
                tenureMonthsActual = actualMonths,
                monthsSaved = monthsSaved,
                schedule = schedule,
                scheduleCsv = csv,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
