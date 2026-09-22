package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

data class BlackScholesInput(
    val spotPrice: Double = 100.0,
    val strikePrice: Double = 100.0,
    val timeToMaturityYears: Double = 1.0,
    val riskFreeRatePercent: Double = 5.0,     // 5% = 0.05
    val volatilityPercent: Double = 20.0,       // 20% = 0.20
    val dividendYieldPercent: Double = 0.0     // 0%
)

data class BlackScholesOutput(
    val callPrice: Double,
    val putPrice: Double,
    val d1: Double,
    val d2: Double,
    val callDelta: Double,
    val putDelta: Double,
    val gamma: Double,
    val vega: Double,
    val callThetaAnnual: Double,
    val callThetaPerDay: Double,
    val putThetaAnnual: Double,
    val putThetaPerDay: Double,
    val callRho: Double,
    val putRho: Double,
    val formattedReport: String,
    val summary: String
)

class BlackScholesOptionPricerTool : Tool<BlackScholesInput, BlackScholesOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "black_scholes_option_pricer_tool",
        name = "Black-Scholes Option Pricing & Greeks Calculator",
        description = "Calculate theoretical European Call and Put option prices and primary Greeks (Delta, Gamma, Theta, Vega, Rho).",
        category = ToolCategory.MATH,
        tags = listOf("black scholes", "options", "finance", "greeks", "delta", "gamma", "theta", "vega", "rho", "call", "put", "volatility"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Calculator"
    )

    override suspend fun execute(input: BlackScholesInput): ToolResult<BlackScholesOutput> {
        val startTime = System.currentTimeMillis()

        val s = input.spotPrice
        val k = input.strikePrice
        val t = input.timeToMaturityYears
        val r = input.riskFreeRatePercent / 100.0
        val sigma = input.volatilityPercent / 100.0
        val q = input.dividendYieldPercent / 100.0

        if (s <= 0.0 || k <= 0.0) {
            return ToolResult.Failure("Spot price and Strike price must be strictly greater than 0.")
        }
        if (t <= 0.0) {
            return ToolResult.Failure("Time to maturity must be greater than 0 years.")
        }
        if (sigma <= 0.0) {
            return ToolResult.Failure("Implied volatility must be greater than 0%.")
        }

        val sqrtT = sqrt(t)
        val d1 = (ln(s / k) + (r - q + 0.5 * sigma * sigma) * t) / (sigma * sqrtT)
        val d2 = d1 - sigma * sqrtT

        val nd1 = normalCdf(d1)
        val nd2 = normalCdf(d2)
        val nNegD1 = normalCdf(-d1)
        val nNegD2 = normalCdf(-d2)
        val npdfD1 = normalPdf(d1)

        val expMinusQT = exp(-q * t)
        val expMinusRT = exp(-r * t)

        // Prices
        val callPrice = s * expMinusQT * nd1 - k * expMinusRT * nd2
        val putPrice = k * expMinusRT * nNegD2 - s * expMinusQT * nNegD1

        // Greeks
        val callDelta = expMinusQT * nd1
        val putDelta = expMinusQT * (nd1 - 1.0)

        val gamma = (expMinusQT * npdfD1) / (s * sigma * sqrtT)
        val vega = s * expMinusQT * sqrtT * npdfD1 // Total Vega (per 100% vol change)
        val vega1Pct = vega / 100.0

        // Theta (annualized)
        val term1 = -(s * expMinusQT * npdfD1 * sigma) / (2.0 * sqrtT)
        val callTheta = term1 - r * k * expMinusRT * nd2 + q * s * expMinusQT * nd1
        val putTheta = term1 + r * k * expMinusRT * nNegD2 - q * s * expMinusQT * nNegD1
        val callThetaDay = callTheta / 365.0
        val putThetaDay = putTheta / 365.0

        // Rho (annualized, per 100% rate change)
        val callRho = k * t * expMinusRT * nd2
        val putRho = -k * t * expMinusRT * nNegD2
        val callRho1Pct = callRho / 100.0
        val putRho1Pct = putRho / 100.0

        val report = buildString {
            appendLine("BLACK-SCHOLES OPTION VALUATION & GREEKS")
            appendLine("--------------------------------------------------")
            appendLine("Spot Price (S):        ${String.format(Locale.US, "$%.2f", s)}")
            appendLine("Strike Price (K):      ${String.format(Locale.US, "$%.2f", k)}")
            appendLine("Time to Expiry (T):    ${String.format(Locale.US, "%.3f years (%d days)", t, (t * 365).toInt())}")
            appendLine("Risk-Free Rate (r):    ${input.riskFreeRatePercent}%")
            appendLine("Volatility (σ):        ${input.volatilityPercent}%")
            if (input.dividendYieldPercent > 0.0) {
                appendLine("Dividend Yield (q):    ${input.dividendYieldPercent}%")
            }
            appendLine("--------------------------------------------------")
            appendLine("d1: ${String.format(Locale.US, "%.5f", d1)} | d2: ${String.format(Locale.US, "%.5f", d2)}")
            appendLine("--------------------------------------------------")
            appendLine("OPTION THEORETICAL VALUES:")
            appendLine(" * European Call:      ${String.format(Locale.US, "$%.4f", callPrice)}")
            appendLine(" * European Put:       ${String.format(Locale.US, "$%.4f", putPrice)}")
            appendLine("--------------------------------------------------")
            appendLine("RISK SENSITIVITIES (GREEKS):")
            appendLine(" * Delta (Δ):          Call: ${String.format(Locale.US, "%+.4f", callDelta)} | Put: ${String.format(Locale.US, "%+.4f", putDelta)}")
            appendLine(" * Gamma (Γ):          ${String.format(Locale.US, "%.4f", gamma)} (per $1 move in underlying)")
            appendLine(" * Vega (ν):           ${String.format(Locale.US, "$%.4f", vega1Pct)} per 1% change in volatility")
            appendLine(" * Theta (Θ):          Call: ${String.format(Locale.US, "$%.4f", callThetaDay)}/day | Put: ${String.format(Locale.US, "$%.4f", putThetaDay)}/day")
            appendLine(" * Rho (ρ):            Call: ${String.format(Locale.US, "$%.4f", callRho1Pct)}/1% | Put: ${String.format(Locale.US, "$%.4f", putRho1Pct)}/1%")
        }

        val output = BlackScholesOutput(
            callPrice = callPrice,
            putPrice = putPrice,
            d1 = d1,
            d2 = d2,
            callDelta = callDelta,
            putDelta = putDelta,
            gamma = gamma,
            vega = vega1Pct,
            callThetaAnnual = callTheta,
            callThetaPerDay = callThetaDay,
            putThetaAnnual = putTheta,
            putThetaPerDay = putThetaDay,
            callRho = callRho1Pct,
            putRho = putRho1Pct,
            formattedReport = report,
            summary = "Call: ${String.format(Locale.US, "$%.2f", callPrice)}, Put: ${String.format(Locale.US, "$%.2f", putPrice)}, Δ(Call): ${String.format(Locale.US, "%.2f", callDelta)}"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Computed Black-Scholes pricing and option Greeks"
        )
    }

    private fun normalPdf(x: Double): Double {
        return (1.0 / sqrt(2.0 * Math.PI)) * exp(-0.5 * x * x)
    }

    private fun normalCdf(x: Double): Double {
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911

        val sign = if (x < 0) -1.0 else 1.0
        val absX = abs(x) / sqrt(2.0)
        val t = 1.0 / (1.0 + p * absX)
        val erf = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-absX * absX)

        return 0.5 * (1.0 + sign * erf)
    }
}
