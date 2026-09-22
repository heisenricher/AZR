package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

data class ResistorCircuitInput(
    val topology: String = "SERIES", // SERIES, PARALLEL, VOLTAGE_DIVIDER
    val resistorValues: String = "100, 220, 470", // Ohms, supports k/M suffixes e.g. 10k, 2.2k
    val supplyVoltageVolts: Double = 12.0,
    val loadResistanceOhms: Double? = 1000.0 // For voltage divider load testing
)

data class ResistorBranchMetric(
    val index: Int,
    val nominalOhms: Double,
    val voltageDropVolts: Double,
    val currentAmperes: Double,
    val powerWatts: Double,
    val percentageOfTotalPower: Double
)

data class ResistorCircuitOutput(
    val topology: String,
    val count: Int,
    val equivalentResistanceOhms: Double,
    val totalCurrentAmperes: Double,
    val totalPowerWatts: Double,
    val branches: List<ResistorBranchMetric>,
    val dividerVoutNoLoad: Double?,
    val dividerVoutWithLoad: Double?,
    val nearestE12StandardOhms: Double,
    val nearestE24StandardOhms: Double,
    val formattedReport: String,
    val summary: String
)

class ResistorEquivalentCircuitTool : Tool<ResistorCircuitInput, ResistorCircuitOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "resistor_equivalent_circuit_tool",
        name = "Resistor Network (Series/Parallel) & Voltage Divider Solver",
        description = "Calculate equivalent resistance (Req), branch currents, voltage drops, and power dissipation for Series, Parallel, and Voltage Divider resistor circuits with E12/E24 standard component matching.",
        category = ToolCategory.MATH,
        tags = listOf("resistor", "electronics", "circuit", "series", "parallel", "voltage divider", "ohms", "e12", "e24", "power"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Cpu"
    )

    private val e12Base = listOf(1.0, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2)
    private val e24Base = listOf(
        1.0, 1.1, 1.2, 1.3, 1.5, 1.6, 1.8, 2.0, 2.2, 2.4, 2.7, 3.0,
        3.3, 3.6, 3.9, 4.3, 4.7, 5.1, 5.6, 6.2, 6.8, 7.5, 8.2, 9.1
    )

    private fun findNearestStandard(value: Double, baseList: List<Double>): Double {
        if (value <= 0.0) return 1.0
        val exponent = kotlin.math.floor(log10(value)).toInt()
        val decade = 10.0.pow(exponent)
        val normalized = value / decade

        var bestDiff = Double.MAX_VALUE
        var bestVal = baseList.first() * decade

        for (base in baseList) {
            val candidate = base * decade
            val diff = kotlin.math.abs(candidate - value)
            if (diff < bestDiff) {
                bestDiff = diff
                bestVal = candidate
            }
            // Also check next decade boundary
            val candidateNext = (base * 10.0) * decade
            val diffNext = kotlin.math.abs(candidateNext - value)
            if (diffNext < bestDiff) {
                bestDiff = diffNext
                bestVal = candidateNext
            }
        }
        return (bestVal * 100.0).roundToInt() / 100.0
    }

    private fun parseResistorList(text: String): List<Double> {
        val tokens = text.split(",", ";", " ", "\n", "\t")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val values = mutableListOf<Double>()
        for (tok in tokens) {
            val clean = tok.uppercase(Locale.US).replace("Ω", "").replace("OHM", "").replace("OHMS", "").trim()
            val num = when {
                clean.endsWith("K") -> clean.removeSuffix("K").toDoubleOrNull()?.let { it * 1000.0 }
                clean.endsWith("M") -> clean.removeSuffix("M").toDoubleOrNull()?.let { it * 1000000.0 }
                clean.endsWith("R") -> clean.removeSuffix("R").toDoubleOrNull()
                else -> clean.toDoubleOrNull()
            }
            if (num != null && num > 0.0) {
                values.add(num)
            }
        }
        return values
    }

    override suspend fun execute(input: ResistorCircuitInput): ToolResult<ResistorCircuitOutput> {
        val startTime = System.currentTimeMillis()
        val resistors = parseResistorList(input.resistorValues)

        if (resistors.isEmpty()) {
            return ToolResult.Failure("Please enter at least one valid positive resistor value in Ohms (e.g. '100, 220, 4.7k').")
        }

        val top = input.topology.trim().uppercase(Locale.US)
        val vSupply = input.supplyVoltageVolts.coerceAtLeast(0.0)

        val req: Double
        val branches = mutableListOf<ResistorBranchMetric>()
        var vOutNoLoad: Double? = null
        var vOutWithLoad: Double? = null

        when {
            top.contains("PARALLEL") -> {
                var invSum = 0.0
                for (r in resistors) {
                    invSum += (1.0 / r)
                }
                req = if (invSum > 0.0) 1.0 / invSum else 0.0
                val totalPower = if (req > 0.0) (vSupply * vSupply) / req else 0.0

                for ((idx, r) in resistors.withIndex()) {
                    val vDrop = vSupply
                    val iBranch = vDrop / r
                    val pBranch = vDrop * iBranch
                    val pct = if (totalPower > 0.0) (pBranch / totalPower) * 100.0 else 0.0
                    branches.add(ResistorBranchMetric(idx + 1, r, vDrop, iBranch, pBranch, pct))
                }
            }
            top.contains("DIVIDER") -> {
                if (resistors.size < 2) {
                    return ToolResult.Failure("Voltage divider requires at least 2 resistors (R1 = Top, R2 = Bottom).")
                }
                val r1 = resistors[0]
                val r2 = resistors[1]
                vOutNoLoad = vSupply * (r2 / (r1 + r2))

                val rLoad = input.loadResistanceOhms
                if (rLoad != null && rLoad > 0.0) {
                    val r2Parallel = (r2 * rLoad) / (r2 + rLoad)
                    req = r1 + r2Parallel
                    vOutWithLoad = vSupply * (r2Parallel / (r1 + r2Parallel))
                } else {
                    req = r1 + r2
                }

                val totalCurrent = if (req > 0.0) vSupply / req else 0.0
                val totalPower = vSupply * totalCurrent

                // Branch 1: R1
                val v1 = totalCurrent * r1
                val p1 = v1 * totalCurrent
                val pct1 = if (totalPower > 0.0) (p1 / totalPower) * 100.0 else 0.0
                branches.add(ResistorBranchMetric(1, r1, v1, totalCurrent, p1, pct1))

                // Branch 2: R2
                val v2 = vSupply - v1
                val i2 = v2 / r2
                val p2 = v2 * i2
                val pct2 = if (totalPower > 0.0) (p2 / totalPower) * 100.0 else 0.0
                branches.add(ResistorBranchMetric(2, r2, v2, i2, p2, pct2))

                if (rLoad != null && rLoad > 0.0) {
                    val iLoad = v2 / rLoad
                    val pLoad = v2 * iLoad
                    val pctLoad = if (totalPower > 0.0) (pLoad / totalPower) * 100.0 else 0.0
                    branches.add(ResistorBranchMetric(3, rLoad, v2, iLoad, pLoad, pctLoad))
                }
            }
            else -> {
                // SERIES default
                req = resistors.sum()
                val totalCurrent = if (req > 0.0) vSupply / req else 0.0
                val totalPower = vSupply * totalCurrent

                for ((idx, r) in resistors.withIndex()) {
                    val vDrop = totalCurrent * r
                    val pBranch = totalCurrent * totalCurrent * r
                    val pct = if (totalPower > 0.0) (pBranch / totalPower) * 100.0 else 0.0
                    branches.add(ResistorBranchMetric(idx + 1, r, vDrop, totalCurrent, pBranch, pct))
                }
            }
        }

        val totalCurrent = if (req > 0.0) vSupply / req else 0.0
        val totalPower = vSupply * totalCurrent

        val e12Standard = findNearestStandard(req, e12Base)
        val e24Standard = findNearestStandard(req, e24Base)

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== RESISTOR CIRCUIT & VOLTAGE DIVIDER SOLVER ===")
            appendLine("Topology:            ${if (top.contains("PARALLEL")) "PARALLEL" else if (top.contains("DIVIDER")) "VOLTAGE DIVIDER" else "SERIES"}")
            appendLine("Supply Voltage:      ${String.format(Locale.US, "%.2f", vSupply)} V")
            appendLine("Equivalent Req:      ${String.format(Locale.US, "%,.3f", req)} Ω")
            appendLine("Total Current:       ${String.format(Locale.US, "%.6f", totalCurrent)} A (${String.format(Locale.US, "%.3f", totalCurrent * 1000.0)} mA)")
            appendLine("Total Power Dissip:  ${String.format(Locale.US, "%.4f", totalPower)} W (${String.format(Locale.US, "%.2f", totalPower * 1000.0)} mW)")
            appendLine("----------------------------------------")
            appendLine("STANDARD EIA COMPONENT MATCH:")
            appendLine("  Nearest E12 (10%): ${String.format(Locale.US, "%,.2f", e12Standard)} Ω")
            appendLine("  Nearest E24 (5%):  ${String.format(Locale.US, "%,.2f", e24Standard)} Ω")

            if (vOutNoLoad != null) {
                appendLine("----------------------------------------")
                appendLine("VOLTAGE DIVIDER METRICS:")
                appendLine("  Vout (Open / No Load): ${String.format(Locale.US, "%.3f", vOutNoLoad)} V")
                if (vOutWithLoad != null) {
                    val droop = vOutNoLoad - vOutWithLoad
                    appendLine("  Vout (With Load ${String.format(Locale.US, "%,.0f", input.loadResistanceOhms ?: 0.0)} Ω): ${String.format(Locale.US, "%.3f", vOutWithLoad)} V")
                    appendLine("  Voltage Droop / Sag:   ${String.format(Locale.US, "%.3f", droop)} V (${String.format(Locale.US, "%.1f", (droop / vOutNoLoad) * 100.0)}%)")
                }
            }

            appendLine("----------------------------------------")
            appendLine("BRANCH BREAKDOWN:")
            appendLine("%-4s | %-10s | %-9s | %-10s | %-8s | %s".format(Locale.US, "#", "Nominal", "Drop", "Current", "Power", "Share"))
            branches.forEach { b ->
                appendLine(
                    "%-4d | %-10s | %-7.2f V | %-8.3f mA | %-6.2f mW | %.1f%%".format(
                        Locale.US,
                        b.index,
                        "${String.format(Locale.US, "%,.1f", b.nominalOhms)} Ω",
                        b.voltageDropVolts,
                        b.currentAmperes * 1000.0,
                        b.powerWatts * 1000.0,
                        b.percentageOfTotalPower
                    )
                )
            }
        }

        val summaryText = "Req = ${String.format(Locale.US, "%,.2f", req)} Ω | I = ${String.format(Locale.US, "%.2f", totalCurrent * 1000.0)} mA | P = ${String.format(Locale.US, "%.2f", totalPower * 1000.0)} mW"

        return ToolResult.Success(
            data = ResistorCircuitOutput(
                topology = top,
                count = resistors.size,
                equivalentResistanceOhms = req,
                totalCurrentAmperes = totalCurrent,
                totalPowerWatts = totalPower,
                branches = branches,
                dividerVoutNoLoad = vOutNoLoad,
                dividerVoutWithLoad = vOutWithLoad,
                nearestE12StandardOhms = e12Standard,
                nearestE24StandardOhms = e24Standard,
                formattedReport = report,
                summary = summaryText
            ),
            executionTimeMs = elapsed,
            summary = summaryText
        )
    }
}
