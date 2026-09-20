package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10

data class ScientificNotationInput(
    val inputNumber: String = "1234500000",
    val significantDigits: Int = 4
)

data class ScientificNotationOutput(
    val standardDecimal: String,
    val scientificNotation: String,
    val engineeringNotation: String,
    val siPrefixedValue: String,
    val siPrefixName: String,
    val orderOfMagnitude: Int,
    val formattedReport: String,
    val summary: String
)

class ScientificNotationTool : Tool<ScientificNotationInput, ScientificNotationOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "scientific_notation_tool",
        name = "Scientific & Engineering Notation",
        description = "Convert numbers across Standard Decimal, Scientific, Engineering, and SI Metric prefixes (Giga, Mega, Micro, Nano).",
        category = ToolCategory.MATH,
        tags = listOf("scientific", "notation", "engineering", "exponent", "si", "prefix", "magnitude", "decimal"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Calculate"
    )

    private val siPrefixes = mapOf(
        24 to Pair("Y", "Yotta (10^24)"),
        21 to Pair("Z", "Zetta (10^21)"),
        18 to Pair("E", "Exa (10^18)"),
        15 to Pair("P", "Peta (10^15)"),
        12 to Pair("T", "Tera (10^12)"),
        9 to Pair("G", "Giga (10^9)"),
        6 to Pair("M", "Mega (10^6)"),
        3 to Pair("k", "kilo (10^3)"),
        0 to Pair("", "Unit (10^0)"),
        -3 to Pair("m", "milli (10^-3)"),
        -6 to Pair("µ", "micro (10^-6)"),
        -9 to Pair("n", "nano (10^-9)"),
        -12 to Pair("p", "pico (10^-12)"),
        -15 to Pair("f", "femto (10^-15)"),
        -18 to Pair("a", "atto (10^-18)"),
        -21 to Pair("z", "zepto (10^-21)"),
        -24 to Pair("y", "yocto (10^-24)")
    )

    override suspend fun execute(input: ScientificNotationInput): ToolResult<ScientificNotationOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.inputNumber.trim().replace(",", "")

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input number cannot be empty.")
        }

        val bd: BigDecimal = try {
            BigDecimal(raw)
        } catch (_: Exception) {
            return ToolResult.Failure("Invalid numeric format: '$raw'")
        }

        if (bd.compareTo(BigDecimal.ZERO) == 0) {
            val summary = "0.0 (Magnitude 0)"
            return ToolResult.Success(
                data = ScientificNotationOutput(
                    standardDecimal = "0",
                    scientificNotation = "0 × 10^0",
                    engineeringNotation = "0 × 10^0",
                    siPrefixedValue = "0",
                    siPrefixName = "Zero",
                    orderOfMagnitude = 0,
                    formattedReport = "Value is exactly Zero.",
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        }

        val doubleVal = bd.toDouble()
        val sign = if (bd < BigDecimal.ZERO) "-" else ""
        val absBd = bd.abs()

        // Order of magnitude
        val exponent = floor(log10(absBd.toDouble())).toInt()

        // Scientific Notation: m * 10^exponent where 1 <= m < 10
        val sciMantissa = if (exponent >= 0) {
            absBd.divide(BigDecimal.TEN.pow(exponent.coerceIn(0, 50)), MathContext(input.significantDigits, RoundingMode.HALF_UP))
        } else {
            absBd.multiply(BigDecimal.TEN.pow((-exponent).coerceIn(0, 50)), MathContext(input.significantDigits, RoundingMode.HALF_UP))
        }
        val scientificStr = "$sign$sciMantissa × 10^$exponent"

        // Engineering Notation: exponent multiple of 3
        val engExp = if (exponent >= 0) {
            (exponent / 3) * 3
        } else {
            ((exponent - 2) / 3) * 3
        }
        val engMantissa = (if (engExp >= 0) {
            absBd.divide(BigDecimal.TEN.pow(engExp.coerceIn(0, 50)), MathContext(input.significantDigits + 2, RoundingMode.HALF_UP))
        } else {
            absBd.multiply(BigDecimal.TEN.pow((-engExp).coerceIn(0, 50)), MathContext(input.significantDigits + 2, RoundingMode.HALF_UP))
        }).setScale(input.significantDigits - 1, RoundingMode.HALF_UP).stripTrailingZeros()
        val engineeringStr = "$sign$engMantissa × 10^$engExp"

        // SI Prefix
        val siEntry = siPrefixes[engExp]
        val (siSymbol, siName) = siEntry ?: Pair("", "No standard SI prefix for 10^$engExp")
        val siValueStr = if (siSymbol.isNotEmpty()) "$sign$engMantissa $siSymbol" else "$sign$engMantissa × 10^$engExp"

        val report = buildString {
            appendLine("SCIENTIFIC & ENGINEERING NOTATION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Input Number:         $raw")
            appendLine("Order of Magnitude:   10^$exponent")
            appendLine("Standard Decimal:     ${bd.toPlainString()}")
            appendLine("Scientific Notation:  $scientificStr")
            appendLine("Engineering Notation: $engineeringStr")
            appendLine("SI Metric Form:       $siValueStr ($siName)")
        }

        val summary = "$scientificStr ($siValueStr)"

        return ToolResult.Success(
            data = ScientificNotationOutput(
                standardDecimal = bd.toPlainString(),
                scientificNotation = scientificStr,
                engineeringNotation = engineeringStr,
                siPrefixedValue = siValueStr,
                siPrefixName = siName,
                orderOfMagnitude = exponent,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
