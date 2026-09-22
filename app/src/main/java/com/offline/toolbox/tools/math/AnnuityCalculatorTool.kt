package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.pow

data class AnnuityInput(
    val solveFor: String = "FV", // FV, PV, PMT_FROM_FV, PMT_FROM_PV
    val annuityType: String = "ORDINARY", // ORDINARY (end of period) or DUE (beginning of period)
    val periodicPayment: Double = 500.0,
    val presentValue: Double = 0.0,
    val futureValue: Double = 0.0,
    val annualRatePercent: Double = 7.0,
    val compoundingFrequency: Int = 12, // 1=Annually, 2=Semi-Annually, 4=Quarterly, 12=Monthly, 26=Biweekly
    val years: Double = 5.0
)

data class AnnuityScheduleRow(
    val period: Int,
    val startBalance: Double,
    val payment: Double,
    val interestEarned: Double,
    val endBalance: Double
)

data class AnnuityOutput(
    val solveFor: String,
    val annuityType: String,
    val calculatedValue: Double,
    val periodicPayment: Double,
    val presentValue: Double,
    val futureValue: Double,
    val totalPayments: Double,
    val totalInterest: Double,
    val totalPeriods: Int,
    val periodicRatePercent: Double,
    val sampleSchedule: List<AnnuityScheduleRow>,
    val formattedReport: String,
    val summary: String
)

class AnnuityCalculatorTool : Tool<AnnuityInput, AnnuityOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "annuity_calculator_tool",
        name = "Ordinary & Annuity Due Calculator",
        description = "Calculate Future Value (FV), Present Value (PV), and Periodic Payment (PMT) for ordinary annuities and annuities due with compounding schedules.",
        category = ToolCategory.MATH,
        tags = listOf("annuity", "finance", "present value", "future value", "pmt", "interest", "compounding", "retirement", "investment"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "DollarSign"
    )

    override suspend fun execute(input: AnnuityInput): ToolResult<AnnuityOutput> {
        val startTime = System.currentTimeMillis()
        val freq = if (input.compoundingFrequency <= 0) 12 else input.compoundingFrequency
        val years = if (input.years <= 0) 1.0 else input.years
        val n = (years * freq).toInt()
        if (n <= 0) {
            return ToolResult.Failure("Total compounding periods must be greater than zero.")
        }

        val annualRate = input.annualRatePercent / 100.0
        val r = annualRate / freq
        val isDue = input.annuityType.trim().uppercase(Locale.US) == "DUE"
        val dueFactor = if (isDue) (1.0 + r) else 1.0

        val solve = input.solveFor.trim().uppercase(Locale.US)
        var pmt = input.periodicPayment
        var pv = input.presentValue
        var fv = input.futureValue

        when (solve) {
            "FV" -> {
                fv = if (r == 0.0) {
                    pmt * n
                } else {
                    pmt * ((1.0 + r).pow(n) - 1.0) / r * dueFactor
                }
                pv = if (r == 0.0) {
                    pmt * n
                } else {
                    pmt * (1.0 - (1.0 + r).pow(-n)) / r * dueFactor
                }
            }
            "PV" -> {
                pv = if (r == 0.0) {
                    pmt * n
                } else {
                    pmt * (1.0 - (1.0 + r).pow(-n)) / r * dueFactor
                }
                fv = if (r == 0.0) {
                    pmt * n
                } else {
                    pmt * ((1.0 + r).pow(n) - 1.0) / r * dueFactor
                }
            }
            "PMT_FROM_FV" -> {
                pmt = if (r == 0.0) {
                    fv / n
                } else {
                    fv / (((1.0 + r).pow(n) - 1.0) / r * dueFactor)
                }
                pv = if (r == 0.0) {
                    pmt * n
                } else {
                    pmt * (1.0 - (1.0 + r).pow(-n)) / r * dueFactor
                }
            }
            "PMT_FROM_PV" -> {
                pmt = if (r == 0.0) {
                    pv / n
                } else {
                    pv / ((1.0 - (1.0 + r).pow(-n)) / r * dueFactor)
                }
                fv = if (r == 0.0) {
                    pmt * n
                } else {
                    pmt * ((1.0 + r).pow(n) - 1.0) / r * dueFactor
                }
            }
            else -> {
                return ToolResult.Failure("Unknown solve target '$solve'. Supported: FV, PV, PMT_FROM_FV, PMT_FROM_PV.")
            }
        }

        val totalPayments = pmt * n
        val totalInterest = kotlin.math.abs(fv - totalPayments)

        // Build schedule simulation for FV growth
        val schedule = mutableListOf<AnnuityScheduleRow>()
        var curBalance = 0.0
        for (i in 1..n) {
            val start = curBalance
            val p = pmt
            val interest: Double
            val end: Double
            if (isDue) {
                val sum = start + p
                interest = sum * r
                end = sum + interest
            } else {
                interest = start * r
                end = start + interest + p
            }
            curBalance = end
            if (i <= 3 || i > n - 3 || (n <= 12)) {
                schedule.add(
                    AnnuityScheduleRow(
                        period = i,
                        startBalance = start,
                        payment = p,
                        interestEarned = interest,
                        endBalance = end
                    )
                )
            }
        }

        val primaryResult = when (solve) {
            "FV" -> fv
            "PV" -> pv
            else -> pmt
        }

        val elapsed = System.currentTimeMillis() - startTime
        val freqLabel = when (freq) {
            1 -> "Annual"
            2 -> "Semi-Annual"
            4 -> "Quarterly"
            12 -> "Monthly"
            26 -> "Bi-Weekly"
            52 -> "Weekly"
            365 -> "Daily"
            else -> "$freq times/yr"
        }

        val report = buildString {
            appendLine("=== ANNUITY VALUATION REPORT ===")
            appendLine("Annuity Type: ${if (isDue) "Annuity Due (Beginning of Period)" else "Ordinary Annuity (End of Period)"}")
            appendLine("Solve For: $solve")
            appendLine("Annual Rate: ${input.annualRatePercent}% ($freqLabel compounding)")
            appendLine("Periodic Rate: ${String.format(Locale.US, "%.4f", r * 100)}% per period")
            appendLine("Duration: $years years ($n total periods)")
            appendLine("----------------------------------------")
            appendLine("Periodic Payment (PMT): $${String.format(Locale.US, "%,.2f", pmt)}")
            appendLine("Present Value (PV):     $${String.format(Locale.US, "%,.2f", pv)}")
            appendLine("Future Value (FV):      $${String.format(Locale.US, "%,.2f", fv)}")
            appendLine("Total Contributions:    $${String.format(Locale.US, "%,.2f", totalPayments)}")
            appendLine("Total Interest Earned:  $${String.format(Locale.US, "%,.2f", totalInterest)}")
            appendLine("----------------------------------------")
            appendLine("Sample Growth Schedule:")
            appendLine("%-6s | %-12s | %-10s | %-10s | %-12s".format(Locale.US, "Period", "Start", "Deposit", "Interest", "End"))
            schedule.forEach { row ->
                appendLine("%-6d | $%-11.2f | $%-9.2f | $%-9.2f | $%-11.2f".format(Locale.US, row.period, row.startBalance, row.payment, row.interestEarned, row.endBalance))
            }
        }

        return ToolResult.Success(
            data = AnnuityOutput(
                solveFor = solve,
                annuityType = if (isDue) "DUE" else "ORDINARY",
                calculatedValue = primaryResult,
                periodicPayment = pmt,
                presentValue = pv,
                futureValue = fv,
                totalPayments = totalPayments,
                totalInterest = totalInterest,
                totalPeriods = n,
                periodicRatePercent = r * 100.0,
                sampleSchedule = schedule,
                formattedReport = report,
                summary = "Computed Annuity $solve: $${String.format(Locale.US, "%,.2f", primaryResult)} (Total Deposits: $${String.format(Locale.US, "%,.2f", totalPayments)}, Interest: $${String.format(Locale.US, "%,.2f", totalInterest)})."
            ),
            executionTimeMs = elapsed,
            summary = "Computed Annuity $solve: $${String.format(Locale.US, "%.2f", primaryResult)}"
        )
    }
}
