package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

data class KeplerOrbitalInput(
    val solveMode: String = "PERIOD_FROM_ALTITUDE", // PERIOD_FROM_ALTITUDE, ALTITUDE_FROM_PERIOD
    val primaryBody: String = "EARTH", // EARTH, SUN, MOON, MARS
    val altitudeKm: Double = 420.0,    // e.g. ISS altitude ~420 km
    val targetPeriodHours: Double = 23.9344696 // e.g. Geostationary sidereal day ~23.93 hrs
)

data class KeplerOrbitalOutput(
    val primaryBodyName: String,
    val semiMajorAxisKm: Double,
    val altitudeKm: Double,
    val orbitalPeriodSeconds: Double,
    val orbitalPeriodMinutes: Double,
    val orbitalPeriodHours: Double,
    val orbitalPeriodDays: Double,
    val orbitalVelocityKms: Double,
    val standardOrbitComparison: String,
    val formattedReport: String,
    val summary: String
)

class KeplerOrbitalPeriodTool : Tool<KeplerOrbitalInput, KeplerOrbitalOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "kepler_orbital_period_tool",
        name = "Kepler's 3rd Law & Orbital Mechanics Solver",
        description = "Calculate satellite orbital periods, circular velocity, and orbital altitudes using Kepler's Third Law T² = (4π² / GM) · a³ with reference celestial bodies.",
        category = ToolCategory.MATH,
        tags = listOf("kepler", "orbital", "astronomy", "physics", "satellites", "space", "iss", "geostationary", "gravity", "mechanics"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Globe"
    )

    private val G = 6.67430e-11 // m^3 kg^-1 s^-2

    private data class CelestialBody(val name: String, val massKg: Double, val radiusKm: Double)

    private val bodies = mapOf(
        "EARTH" to CelestialBody("Earth", 5.9722e24, 6371.0),
        "SUN" to CelestialBody("Sun", 1.9885e30, 696340.0),
        "MOON" to CelestialBody("Moon", 7.342e22, 1737.4),
        "MARS" to CelestialBody("Mars", 6.4171e23, 3389.5)
    )

    override suspend fun execute(input: KeplerOrbitalInput): ToolResult<KeplerOrbitalOutput> {
        val startTime = System.currentTimeMillis()
        val bodyKey = input.primaryBody.trim().uppercase(Locale.US)
        val body = bodies[bodyKey] ?: bodies["EARTH"]!!

        val mu = G * body.massKg // standard gravitational parameter GM in m^3/s^2
        val rMeters = body.radiusKm * 1000.0

        val mode = input.solveMode.trim().uppercase(Locale.US)
        var aMeters: Double
        var periodSec: Double
        var altKm: Double

        when (mode) {
            "PERIOD_FROM_ALTITUDE" -> {
                altKm = input.altitudeKm.coerceAtLeast(0.0)
                aMeters = (altKm + body.radiusKm) * 1000.0
                // T = 2 * PI * sqrt(a^3 / mu)
                periodSec = 2.0 * PI * sqrt(aMeters.pow(3.0) / mu)
            }
            "ALTITUDE_FROM_PERIOD" -> {
                periodSec = (input.targetPeriodHours * 3600.0).coerceAtLeast(1.0)
                // a = (mu * T^2 / (4 * PI^2))^(1/3)
                aMeters = (mu * periodSec.pow(2.0) / (4.0 * PI * PI)).pow(1.0 / 3.0)
                altKm = (aMeters - rMeters) / 1000.0
            }
            else -> return ToolResult.Failure("Unknown solve mode '$mode'. Supported: PERIOD_FROM_ALTITUDE, ALTITUDE_FROM_PERIOD.")
        }

        val periodMin = periodSec / 60.0
        val periodHrs = periodSec / 3600.0
        val periodDays = periodSec / 86400.0

        // Circular orbital velocity v = sqrt(mu / a) in m/s
        val vMs = sqrt(mu / aMeters)
        val vKms = vMs / 1000.0
        val aKm = aMeters / 1000.0

        val comparison = if (bodyKey == "EARTH") {
            when {
                altKm in 300.0..600.0 -> "Low Earth Orbit (LEO) — similar to ISS (~420 km, ~93 min)"
                altKm in 19000.0..24000.0 -> "Medium Earth Orbit (MEO) — similar to GPS Navigation (~20,200 km, ~12 hrs)"
                altKm in 35500.0..36000.0 -> "Geostationary Earth Orbit (GEO) — stationary above equator (~35,786 km, ~24 hrs)"
                altKm > 350000.0 -> "Lunar Distance Range — similar to Moon's orbit (~384,400 km, ~27.3 days)"
                else -> "Standard Orbit (Altitude: ${String.format(Locale.US, "%,.0f", altKm)} km)"
            }
        } else {
            "Orbiting ${body.name} at ${String.format(Locale.US, "%,.0f", altKm)} km"
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== KEPLER'S THIRD LAW ORBITAL REPORT ===")
            appendLine("Central Body:     ${body.name} (Mass: ${body.massKg} kg, Radius: ${body.radiusKm} km)")
            appendLine("Semi-Major Axis:  ${String.format(Locale.US, "%,.1f", aKm)} km (${String.format(Locale.US, "%,.0f", aMeters)} m)")
            appendLine("Orbital Altitude: ${String.format(Locale.US, "%,.1f", altKm)} km above surface")
            appendLine("----------------------------------------")
            appendLine("ORBITAL VELOCITY: ${String.format(Locale.US, "%.3f", vKms)} km/s (${String.format(Locale.US, "%,.1f", vMs)} m/s)")
            appendLine("ORBITAL PERIOD:")
            appendLine("  Seconds: %,.1f s".format(Locale.US, periodSec))
            appendLine("  Minutes: ${String.format(Locale.US, "%.2f", periodMin)} min")
            appendLine("  Hours:   ${String.format(Locale.US, "%.3f", periodHrs)} hrs")
            appendLine("  Days:    ${String.format(Locale.US, "%.4f", periodDays)} days")
            appendLine("----------------------------------------")
            appendLine("Classification:   $comparison")
        }

        return ToolResult.Success(
            data = KeplerOrbitalOutput(
                primaryBodyName = body.name,
                semiMajorAxisKm = aKm,
                altitudeKm = altKm,
                orbitalPeriodSeconds = periodSec,
                orbitalPeriodMinutes = periodMin,
                orbitalPeriodHours = periodHrs,
                orbitalPeriodDays = periodDays,
                orbitalVelocityKms = vKms,
                standardOrbitComparison = comparison,
                formattedReport = report,
                summary = "Period: ${String.format(Locale.US, "%.1f", periodMin)} min (${String.format(Locale.US, "%.2f", periodHrs)} hrs) at ${String.format(Locale.US, "%,.0f", altKm)} km [${String.format(Locale.US, "%.2f", vKms)} km/s]."
            ),
            executionTimeMs = elapsed,
            summary = "Period: ${String.format(Locale.US, "%.1f", periodMin)} min (${String.format(Locale.US, "%.2f", vKms)} km/s)"
        )
    }
}
