package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

data class OhmsLawInput(
    val voltageVolts: Double? = 12.0,
    val currentAmperes: Double? = null,
    val resistanceOhms: Double? = 4.0,
    val powerWatts: Double? = null,
    val resistorBands: String = "Brown, Black, Red, Gold" // Optional resistor color bands
)

data class OhmsLawOutput(
    val voltageVolts: Double,
    val currentAmperes: Double,
    val resistanceOhms: Double,
    val powerWatts: Double,
    val solvedVariables: List<String>,
    val decodedResistorOhms: Double?,
    val decodedTolerancePercent: Double?,
    val formattedReport: String,
    val summary: String
)

class OhmsLawPowerTool : Tool<OhmsLawInput, OhmsLawOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "ohms_law_power_tool",
        name = "Ohm's Law, Joule's Power & Resistor Band Solver",
        description = "Solve electrical circuits (Voltage, Current, Resistance, Power) using Ohm's Law and Joule's Law given any two known parameters, with 4/5-band resistor color decoding.",
        category = ToolCategory.MATH,
        tags = listOf("ohms law", "electronics", "voltage", "current", "resistance", "power", "watts", "resistor color code", "circuits", "physics"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Zap"
    )

    private val colorDigits = mapOf(
        "black" to 0, "brown" to 1, "red" to 2, "orange" to 3, "yellow" to 4,
        "green" to 5, "blue" to 6, "violet" to 7, "gray" to 8, "white" to 9
    )

    private val colorMultipliers = mapOf(
        "black" to 1.0, "brown" to 10.0, "red" to 100.0, "orange" to 1000.0, "yellow" to 10000.0,
        "green" to 100000.0, "blue" to 1000000.0, "gold" to 0.1, "silver" to 0.01
    )

    private val colorTolerances = mapOf(
        "brown" to 1.0, "red" to 2.0, "green" to 0.5, "blue" to 0.25,
        "violet" to 0.1, "gold" to 5.0, "silver" to 10.0
    )

    private fun decodeBands(bandStr: String): Pair<Double, Double>? {
        val bands = bandStr.split(",", " ").map { it.trim().lowercase(Locale.US) }.filter { it.isNotEmpty() }
        if (bands.size < 4) return null

        return if (bands.size == 4) {
            val d1 = colorDigits[bands[0]] ?: return null
            val d2 = colorDigits[bands[1]] ?: return null
            val mult = colorMultipliers[bands[2]] ?: return null
            val tol = colorTolerances[bands[3]] ?: 20.0
            val r = (d1 * 10 + d2) * mult
            Pair(r, tol)
        } else {
            val d1 = colorDigits[bands[0]] ?: return null
            val d2 = colorDigits[bands[1]] ?: return null
            val d3 = colorDigits[bands[2]] ?: return null
            val mult = colorMultipliers[bands[3]] ?: return null
            val tol = colorTolerances[bands[4]] ?: 20.0
            val r = (d1 * 100 + d2 * 10 + d3) * mult
            Pair(r, tol)
        }
    }

    override suspend fun execute(input: OhmsLawInput): ToolResult<OhmsLawOutput> {
        val startTime = System.currentTimeMillis()

        var v = input.voltageVolts
        var i = input.currentAmperes
        var r = input.resistanceOhms
        var p = input.powerWatts

        val knowns = listOfNotNull(
            if (v != null && v > 0) "V" else null,
            if (i != null && i > 0) "I" else null,
            if (r != null && r > 0) "R" else null,
            if (p != null && p > 0) "P" else null
        )

        if (knowns.size < 2) {
            return ToolResult.Failure("At least two positive electrical parameters (Voltage, Current, Resistance, Power) must be provided.")
        }

        val solved = mutableListOf<String>()

        when {
            v != null && i != null -> {
                r = v / i
                p = v * i
                solved.addAll(listOf("Resistance (R)", "Power (P)"))
            }
            v != null && r != null -> {
                i = v / r
                p = (v * v) / r
                solved.addAll(listOf("Current (I)", "Power (P)"))
            }
            v != null && p != null -> {
                i = p / v
                r = (v * v) / p
                solved.addAll(listOf("Current (I)", "Resistance (R)"))
            }
            i != null && r != null -> {
                v = i * r
                p = i * i * r
                solved.addAll(listOf("Voltage (V)", "Power (P)"))
            }
            i != null && p != null -> {
                v = p / i
                r = p / (i * i)
                solved.addAll(listOf("Voltage (V)", "Resistance (R)"))
            }
            r != null && p != null -> {
                v = sqrt(p * r)
                i = sqrt(p / r)
                solved.addAll(listOf("Voltage (V)", "Current (I)"))
            }
        }

        val finalV = v ?: 0.0
        val finalI = i ?: 0.0
        val finalR = r ?: 0.0
        val finalP = p ?: 0.0

        val decodedResistor = if (input.resistorBands.isNotBlank()) decodeBands(input.resistorBands) else null

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== OHM'S LAW & JOULE'S POWER SOLUTION ===")
            appendLine("Voltage (V):    ${String.format(Locale.US, "%.4f", finalV)} Volts (V)")
            appendLine("Current (I):    ${String.format(Locale.US, "%.4f", finalI)} Amperes (A) [${String.format(Locale.US, "%.2f", finalI * 1000)} mA]")
            appendLine("Resistance (R): ${String.format(Locale.US, "%.4f", finalR)} Ohms (Ω)")
            appendLine("Power (P):      ${String.format(Locale.US, "%.4f", finalP)} Watts (W) [${String.format(Locale.US, "%.2f", finalP * 1000)} mW]")
            appendLine("Derived Variables: ${solved.joinToString(", ")}")
            if (decodedResistor != null) {
                appendLine("----------------------------------------")
                appendLine("Resistor Color Bands: ${input.resistorBands}")
                appendLine("Decoded Resistance:   ${String.format(Locale.US, "%,.1f", decodedResistor.first)} Ω (±${decodedResistor.second}%)")
            }
        }

        return ToolResult.Success(
            data = OhmsLawOutput(
                voltageVolts = finalV,
                currentAmperes = finalI,
                resistanceOhms = finalR,
                powerWatts = finalP,
                solvedVariables = solved,
                decodedResistorOhms = decodedResistor?.first,
                decodedTolerancePercent = decodedResistor?.second,
                formattedReport = report,
                summary = "V=${String.format(Locale.US, "%.2f", finalV)}V, I=${String.format(Locale.US, "%.2f", finalI)}A, R=${String.format(Locale.US, "%.2f", finalR)}Ω, P=${String.format(Locale.US, "%.2f", finalP)}W."
            ),
            executionTimeMs = elapsed,
            summary = "Solved: ${String.format(Locale.US, "%.1f", finalV)}V, ${String.format(Locale.US, "%.2f", finalI)}A, ${String.format(Locale.US, "%.1f", finalR)}Ω, ${String.format(Locale.US, "%.1f", finalP)}W"
        )
    }
}
