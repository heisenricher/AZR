package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln

data class RocketInput(
    val solveFor: String = "DELTA_V", // DELTA_V, PROPELLANT_MASS, FINAL_DRY_MASS, REQUIRED_ISP
    val initialWetMassKg: Double = 549054.0, // e.g. Falcon 9 approx wet mass
    val finalDryMassKg: Double = 22200.0,
    val specificImpulseSec: Double = 311.0, // e.g. Merlin 1D sea level / vacuum average
    val targetDeltaVMs: Double = 9400.0,
    val standardGravity: Double = 9.80665
)

data class MissionDeltaVComparison(
    val missionName: String,
    val requiredDeltaVMs: Double,
    val achievable: Boolean,
    val marginMs: Double
)

data class RocketOutput(
    val solveFor: String,
    val deltaVMs: Double,
    val wetMassKg: Double,
    val dryMassKg: Double,
    val propellantMassKg: Double,
    val specificImpulseSec: Double,
    val exhaustVelocityMs: Double,
    val massRatio: Double,
    val propellantMassFractionPercent: Double,
    val missionComparisons: List<MissionDeltaVComparison>,
    val formattedReport: String,
    val summary: String
)

class TsiolkovskyRocketEquationTool : Tool<RocketInput, RocketOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "tsiolkovsky_rocket_equation_tool",
        name = "Tsiolkovsky Rocket Propulsion Equation Solver",
        description = "Solve the ideal rocket equation Δv = Isp · g₀ · ln(m₀ / mf) for delta-v, propellant mass, dry mass, or required specific impulse with orbital mission feasibility budgets.",
        category = ToolCategory.MATH,
        tags = listOf("rocket", "aerospace", "tsiolkovsky", "propulsion", "delta-v", "isp", "orbital", "astronautics", "physics"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Rocket"
    )

    private val standardMissions = listOf(
        "Low Earth Orbit (LEO) from Surface" to 9300.0,
        "Geostationary Transfer Orbit (GTO) from LEO" to 2450.0,
        "Translunar Injection (TLI) from LEO" to 3150.0,
        "Earth Escape Orbit (C3=0) from LEO" to 3220.0,
        "Trans-Mars Injection (TMI) from LEO" to 3600.0
    )

    override suspend fun execute(input: RocketInput): ToolResult<RocketOutput> {
        val startTime = System.currentTimeMillis()
        val g0 = if (input.standardGravity <= 0.0) 9.80665 else input.standardGravity
        val solve = input.solveFor.trim().uppercase(Locale.US)

        var wet = input.initialWetMassKg
        var dry = input.finalDryMassKg
        var isp = input.specificImpulseSec
        var dv = input.targetDeltaVMs

        when (solve) {
            "DELTA_V" -> {
                if (wet <= 0 || dry <= 0) {
                    return ToolResult.Failure("Initial wet mass and final dry mass must be greater than zero.")
                }
                if (wet < dry) {
                    return ToolResult.Failure("Wet mass ($wet kg) cannot be less than dry mass ($dry kg).")
                }
                if (isp <= 0) {
                    return ToolResult.Failure("Specific impulse Isp must be greater than zero.")
                }
                val ve = isp * g0
                val ratio = wet / dry
                dv = ve * ln(ratio)
            }
            "PROPELLANT_MASS" -> {
                if (dry <= 0) return ToolResult.Failure("Dry mass must be greater than zero.")
                if (isp <= 0) return ToolResult.Failure("Specific impulse Isp must be greater than zero.")
                if (dv <= 0) return ToolResult.Failure("Target Delta-V must be greater than zero.")
                val ve = isp * g0
                wet = dry * exp(dv / ve)
            }
            "FINAL_DRY_MASS" -> {
                if (wet <= 0) return ToolResult.Failure("Wet mass must be greater than zero.")
                if (isp <= 0) return ToolResult.Failure("Specific impulse Isp must be greater than zero.")
                if (dv <= 0) return ToolResult.Failure("Target Delta-V must be greater than zero.")
                val ve = isp * g0
                dry = wet * exp(-dv / ve)
            }
            "REQUIRED_ISP" -> {
                if (wet <= 0 || dry <= 0) return ToolResult.Failure("Wet mass and dry mass must be greater than zero.")
                if (wet <= dry) return ToolResult.Failure("Wet mass must be greater than dry mass.")
                if (dv <= 0) return ToolResult.Failure("Target Delta-V must be greater than zero.")
                val ratio = wet / dry
                isp = dv / (g0 * ln(ratio))
            }
            else -> {
                return ToolResult.Failure("Unknown solve target '$solve'. Supported: DELTA_V, PROPELLANT_MASS, FINAL_DRY_MASS, REQUIRED_ISP.")
            }
        }

        val propMass = (wet - dry).coerceAtLeast(0.0)
        val massRatio = if (dry > 0) wet / dry else 0.0
        val propFraction = if (wet > 0) (propMass / wet) * 100.0 else 0.0
        val ve = isp * g0

        val comparisons = standardMissions.map { (name, reqDv) ->
            val margin = dv - reqDv
            MissionDeltaVComparison(
                missionName = name,
                requiredDeltaVMs = reqDv,
                achievable = margin >= 0.0,
                marginMs = margin
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== TSIOLKOVSKY ROCKET PROPULSION ANALYSIS ===")
            appendLine("Solve Mode: $solve")
            appendLine("Formula: Δv = Isp · g₀ · ln(m₀ / mf)")
            appendLine("Standard Gravity (g₀): $g0 m/s²")
            appendLine("Specific Impulse (Isp): ${String.format(Locale.US, "%.1f", isp)} s")
            appendLine("Effective Exhaust Velocity (Ve): ${String.format(Locale.US, "%,.1f", ve)} m/s")
            appendLine("----------------------------------------")
            appendLine("Initial Total Wet Mass (m₀): ${String.format(Locale.US, "%,.1f", wet)} kg")
            appendLine("Final Burnout Dry Mass (mf): ${String.format(Locale.US, "%,.1f", dry)} kg")
            appendLine("Consumable Propellant (mp):  ${String.format(Locale.US, "%,.1f", propMass)} kg")
            appendLine("Mass Ratio (m₀ / mf):        ${String.format(Locale.US, "%.2f", massRatio)}:1")
            appendLine("Propellant Mass Fraction:    ${String.format(Locale.US, "%.2f", propFraction)}%")
            appendLine("----------------------------------------")
            appendLine("TOTAL VELOCITY CHANGE (Δv):  ${String.format(Locale.US, "%,.1f", dv)} m/s (${String.format(Locale.US, "%,.2f", dv / 1000.0)} km/s)")
            appendLine("----------------------------------------")
            appendLine("Standard Mission Feasibility:")
            comparisons.forEach { m ->
                val status = if (m.achievable) "ACHIEVABLE [+" + String.format(Locale.US, "%.0f", m.marginMs) + " m/s]" else "DEFICIT [" + String.format(Locale.US, "%.0f", m.marginMs) + " m/s]"
                appendLine("  %-42s : %s".format(Locale.US, m.missionName, status))
            }
        }

        return ToolResult.Success(
            data = RocketOutput(
                solveFor = solve,
                deltaVMs = dv,
                wetMassKg = wet,
                dryMassKg = dry,
                propellantMassKg = propMass,
                specificImpulseSec = isp,
                exhaustVelocityMs = ve,
                massRatio = massRatio,
                propellantMassFractionPercent = propFraction,
                missionComparisons = comparisons,
                formattedReport = report,
                summary = "Calculated Δv = ${String.format(Locale.US, "%,.1f", dv)} m/s (Wet: ${String.format(Locale.US, "%,.0f", wet)} kg, Dry: ${String.format(Locale.US, "%,.0f", dry)} kg, Isp: ${String.format(Locale.US, "%.0f", isp)}s)."
            ),
            executionTimeMs = elapsed,
            summary = "Delta-v: ${String.format(Locale.US, "%,.1f", dv)} m/s"
        )
    }
}
