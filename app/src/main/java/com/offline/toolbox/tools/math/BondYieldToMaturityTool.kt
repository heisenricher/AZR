package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

data class BondYtmInput(
    val faceValue: Double = 1000.0,
    val marketPrice: Double = 950.0,
    val couponRatePercent: Double = 5.0,
    val yearsToMaturity: Double = 10.0,
    val paymentFrequencyPerYear: Int = 2 // 1: Annual, 2: Semiannual, 4: Quarterly
)

data class BondYtmOutput(
    val exactYtmPercent: Double,
    val approximateYtmPercent: Double,
    val currentYieldPercent: Double,
    val macaulayDurationYears: Double,
    val modifiedDurationYears: Double,
    val convexity: Double,
    val totalCashFlows: Double,
    val periodCoupon: Double,
    val totalPeriods: Int,
    val formattedReport: String,
    val summary: String
)

class BondYieldToMaturityTool : Tool<BondYtmInput, BondYtmOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "bond_yield_to_maturity_tool",
        name = "Bond Yield to Maturity (YTM) & Duration Solver",
        description = "Compute exact bond Yield to Maturity using Newton-Raphson iteration, Macaulay/Modified duration, and convexity.",
        category = ToolCategory.MATH,
        tags = listOf("bond", "ytm", "yield to maturity", "finance", "fixed income", "interest", "duration", "convexity", "investment"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "TrendingUp"
    )

    override suspend fun execute(input: BondYtmInput): ToolResult<BondYtmOutput> {
        val startTime = System.currentTimeMillis()

        if (input.faceValue <= 0.0) return ToolResult.Failure("Face value must be strictly positive.")
        if (input.marketPrice <= 0.0) return ToolResult.Failure("Market price must be strictly positive.")
        if (input.couponRatePercent < 0.0) return ToolResult.Failure("Coupon rate cannot be negative.")
        if (input.yearsToMaturity <= 0.0) return ToolResult.Failure("Years to maturity must be greater than zero.")
        if (input.paymentFrequencyPerYear !in listOf(1, 2, 4, 12)) {
            return ToolResult.Failure("Payment frequency must be 1 (Annual), 2 (Semiannual), 4 (Quarterly), or 12 (Monthly).")
        }

        val m = input.paymentFrequencyPerYear.toDouble()
        val n = (input.yearsToMaturity * m).toInt()
        val c = (input.couponRatePercent / 100.0)
        val periodCoupon = (input.faceValue * c) / m
        val totalCashFlows = (periodCoupon * n) + input.faceValue
        val currentYieldPercent = ((input.faceValue * c) / input.marketPrice) * 100.0

        // Approximate YTM formula
        val approxPeriodYtm = (periodCoupon + (input.faceValue - input.marketPrice) / n) / ((input.faceValue + input.marketPrice) / 2.0)
        val approxAnnualYtm = approxPeriodYtm * m * 100.0

        // Newton-Raphson solver for exact annual YTM (y)
        var y = (approxAnnualYtm / 100.0).coerceAtLeast(0.001)
        val maxIterations = 100
        val tolerance = 1e-8

        for (iter in 1..maxIterations) {
            val r = y / m // period discount rate
            var price = 0.0
            var dPrice = 0.0 // dPrice / dy

            for (k in 1..n) {
                val factor = (1.0 + r).pow(k)
                price += periodCoupon / factor
                dPrice -= (k * periodCoupon) / ((1.0 + r).pow(k + 1) * m)
            }
            val parFactor = (1.0 + r).pow(n)
            price += input.faceValue / parFactor
            dPrice -= (n * input.faceValue) / ((1.0 + r).pow(n + 1) * m)

            val diff = price - input.marketPrice
            if (abs(diff) < tolerance) {
                break
            }
            if (abs(dPrice) < 1e-12) break
            y -= (diff / dPrice)
            if (y <= -0.99) y = 0.001
        }

        val exactYtmPercent = y * 100.0
        val finalR = y / m

        // Macaulay Duration, Modified Duration & Convexity
        var weightedTimeSum = 0.0
        var convexitySum = 0.0

        for (k in 1..n) {
            val tYears = k / m
            val pvCoupon = periodCoupon / (1.0 + finalR).pow(k)
            weightedTimeSum += tYears * pvCoupon
            convexitySum += (k * (k + 1) * periodCoupon) / (m * m * (1.0 + finalR).pow(k + 2))
        }
        val pvPar = input.faceValue / (1.0 + finalR).pow(n)
        weightedTimeSum += input.yearsToMaturity * pvPar
        convexitySum += (n * (n + 1) * input.faceValue) / (m * m * (1.0 + finalR).pow(n + 2))

        val macaulayDuration = weightedTimeSum / input.marketPrice
        val modifiedDuration = macaulayDuration / (1.0 + finalR)
        val convexity = convexitySum / input.marketPrice

        val report = buildString {
            appendLine("BOND YIELD TO MATURITY (YTM) ANALYSIS")
            appendLine("--------------------------------------------------")
            appendLine(String.format(Locale.US, "Face Value (Par):    $%.2f", input.faceValue))
            appendLine(String.format(Locale.US, "Market Price:        $%.2f (%s)", input.marketPrice, if (input.marketPrice < input.faceValue) "Discount" else if (input.marketPrice > input.faceValue) "Premium" else "Par"))
            appendLine(String.format(Locale.US, "Annual Coupon Rate:  %.2f%% ($%.2f/yr)", input.couponRatePercent, input.faceValue * c))
            appendLine(String.format(Locale.US, "Payment Frequency:   %d times/year ($%.2f/period)", input.paymentFrequencyPerYear, periodCoupon))
            appendLine(String.format(Locale.US, "Years to Maturity:   %.2f years (%d coupon periods)", input.yearsToMaturity, n))
            appendLine("--------------------------------------------------")
            appendLine(String.format(Locale.US, "Exact YTM (Annual):  %.4f%%", exactYtmPercent))
            appendLine(String.format(Locale.US, "Approximate YTM:     %.4f%%", approxAnnualYtm))
            appendLine(String.format(Locale.US, "Current Yield:       %.4f%%", currentYieldPercent))
            appendLine("--------------------------------------------------")
            appendLine("RISK & SENSITIVITY METRICS:")
            appendLine(String.format(Locale.US, "Macaulay Duration:   %.4f years", macaulayDuration))
            appendLine(String.format(Locale.US, "Modified Duration:   %.4f years (ΔP ≈ -ModDur × Δy)", modifiedDuration))
            appendLine(String.format(Locale.US, "Convexity:           %.4f", convexity))
            appendLine(String.format(Locale.US, "Total Cash Inflow:   $%.2f (Interest: $%.2f)", totalCashFlows, totalCashFlows - input.faceValue))
        }

        return ToolResult.Success(
            data = BondYtmOutput(
                exactYtmPercent = exactYtmPercent,
                approximateYtmPercent = approxAnnualYtm,
                currentYieldPercent = currentYieldPercent,
                macaulayDurationYears = macaulayDuration,
                modifiedDurationYears = modifiedDuration,
                convexity = convexity,
                totalCashFlows = totalCashFlows,
                periodCoupon = periodCoupon,
                totalPeriods = n,
                formattedReport = report,
                summary = String.format(Locale.US, "YTM: %.2f%% (ModDur: %.2f yrs)", exactYtmPercent, modifiedDuration)
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = String.format(Locale.US, "Calculated Bond YTM: %.2f%%", exactYtmPercent)
        )
    }
}
