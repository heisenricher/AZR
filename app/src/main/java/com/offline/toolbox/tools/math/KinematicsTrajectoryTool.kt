package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class TrajectoryWaypoint(
    val timeSeconds: Double,
    val xMeters: Double,
    val yMeters: Double,
    val speedMetersPerSec: Double
)

data class KinematicsInput(
    val initialVelocityMps: Double = 50.0,
    val launchAngleDegrees: Double = 45.0,
    val initialHeightMeters: Double = 0.0,
    val gravityMps2: Double = 9.80665
)

data class KinematicsOutput(
    val totalFlightTimeSeconds: Double,
    val maxApogeeMeters: Double,
    val horizontalRangeMeters: Double,
    val timeToApogeeSeconds: Double,
    val impactVelocityMps: Double,
    val impactAngleDegrees: Double,
    val waypoints: List<TrajectoryWaypoint>,
    val formattedReport: String,
    val summary: String
)

class KinematicsTrajectoryTool : Tool<KinematicsInput, KinematicsOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "kinematics_trajectory_tool",
        name = "2D Kinematics & Projectile Trajectory Calculator",
        description = "Calculate ballistic trajectory, apogee, flight time, range, impact velocity, and time-stepped waypoints for 2D projectile motion.",
        category = ToolCategory.MATH,
        tags = listOf("kinematics", "physics", "trajectory", "projectile", "ballistics", "apogee", "velocity", "range", "motion"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Crosshair"
    )

    override suspend fun execute(input: KinematicsInput): ToolResult<KinematicsOutput> {
        val startTime = System.currentTimeMillis()

        if (input.initialVelocityMps <= 0.0) return ToolResult.Failure("Initial velocity must be strictly positive.")
        if (input.gravityMps2 <= 0.0) return ToolResult.Failure("Gravity acceleration must be strictly positive.")
        if (input.initialHeightMeters < 0.0) return ToolResult.Failure("Initial elevation height cannot be negative.")
        if (input.launchAngleDegrees < -90.0 || input.launchAngleDegrees > 90.0) {
            return ToolResult.Failure("Launch angle must be between -90° and +90°.")
        }

        val angleRad = Math.toRadians(input.launchAngleDegrees)
        val v0x = input.initialVelocityMps * cos(angleRad)
        val v0y = input.initialVelocityMps * sin(angleRad)
        val g = input.gravityMps2
        val h0 = input.initialHeightMeters

        // Apogee
        val timeToApex = if (v0y > 0.0) v0y / g else 0.0
        val maxApogee = if (v0y > 0.0) h0 + (v0y * v0y) / (2.0 * g) else h0

        // Total flight time: 0 = h0 + v0y * T - 0.5 * g * T^2
        // 0.5 * g * T^2 - v0y * T - h0 = 0
        val discriminant = (v0y * v0y) + (2.0 * g * h0)
        if (discriminant < 0.0) {
            return ToolResult.Failure("Math error: complex flight time discriminant.")
        }
        val totalFlightTime = (v0y + sqrt(discriminant)) / g
        val horizontalRange = v0x * totalFlightTime

        // Impact velocity
        val vfx = v0x
        val vfy = v0y - g * totalFlightTime
        val impactVelocity = hypot(vfx, vfy)
        val impactAngleDegrees = Math.toDegrees(atan2(-vfy, vfx))

        // Waypoints
        val waypoints = mutableListOf<TrajectoryWaypoint>()
        val steps = 10
        val dt = totalFlightTime / steps
        for (i in 0..steps) {
            val t = (i * dt).coerceAtMost(totalFlightTime)
            val x = v0x * t
            val y = (h0 + v0y * t - 0.5 * g * t * t).coerceAtLeast(0.0)
            val vx = v0x
            val vy = v0y - g * t
            waypoints.add(TrajectoryWaypoint(t, x, y, hypot(vx, vy)))
        }

        val report = buildString {
            appendLine("2D PROJECTILE TRAJECTORY KINEMATICS REPORT")
            appendLine("--------------------------------------------------")
            appendLine(String.format(Locale.US, "Initial Velocity (v0):  %.2f m/s", input.initialVelocityMps))
            appendLine(String.format(Locale.US, "Launch Angle (θ):       %.2f°", input.launchAngleDegrees))
            appendLine(String.format(Locale.US, "Initial Elevation (h0): %.2f m", h0))
            appendLine(String.format(Locale.US, "Gravity (g):            %.5f m/s²", g))
            appendLine("--------------------------------------------------")
            appendLine(String.format(Locale.US, "Maximum Altitude:       %.3f m (Apex at t = %.2f s)", maxApogee, timeToApex))
            appendLine(String.format(Locale.US, "Horizontal Range:       %.3f m", horizontalRange))
            appendLine(String.format(Locale.US, "Total Flight Duration:  %.3f seconds", totalFlightTime))
            appendLine(String.format(Locale.US, "Impact Speed:           %.3f m/s (%.2f km/h)", impactVelocity, impactVelocity * 3.6))
            appendLine(String.format(Locale.US, "Impact Angle:           %.2f° below horizontal", impactAngleDegrees))
            appendLine("--------------------------------------------------")
            appendLine("TRAJECTORY WAYPOINTS:")
            appendLine(String.format(Locale.US, "%-8s | %-12s | %-12s | %-12s", "Time (s)", "X (m)", "Y (m)", "Speed (m/s)"))
            appendLine("---------+--------------+--------------+-------------")
            waypoints.forEach { wp ->
                appendLine(
                    String.format(
                        Locale.US,
                        "%-8.2f | %-12.2f | %-12.2f | %-12.2f",
                        wp.timeSeconds,
                        wp.xMeters,
                        wp.yMeters,
                        wp.speedMetersPerSec
                    )
                )
            }
        }

        return ToolResult.Success(
            data = KinematicsOutput(
                totalFlightTimeSeconds = totalFlightTime,
                maxApogeeMeters = maxApogee,
                horizontalRangeMeters = horizontalRange,
                timeToApogeeSeconds = timeToApex,
                impactVelocityMps = impactVelocity,
                impactAngleDegrees = impactAngleDegrees,
                waypoints = waypoints,
                formattedReport = report,
                summary = String.format(Locale.US, "Range: %.1fm | Apogee: %.1fm | Flight: %.1fs", horizontalRange, maxApogee, totalFlightTime)
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = String.format(Locale.US, "Range: %.1fm, Apogee: %.1fm", horizontalRange, maxApogee)
        )
    }
}
