package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

data class VectorInput(
    val ax: Double = 1.0,
    val ay: Double = 2.0,
    val az: Double = 3.0,
    val bx: Double = 4.0,
    val by: Double = 5.0,
    val bz: Double = 6.0
)

data class VectorOutput(
    val magnitudeA: Double,
    val magnitudeB: Double,
    val unitVectorA: String,
    val unitVectorB: String,
    val dotProduct: Double,
    val crossProduct: String,
    val angleDegrees: Double,
    val angleRadians: Double,
    val projectionAonB: String,
    val formattedReport: String,
    val summary: String
)

class VectorMathCalculatorTool : Tool<VectorInput, VectorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "vector_math_calculator_tool",
        name = "2D & 3D Vector Math Engine",
        description = "Compute dot product, cross product, vector magnitude, unit vectors, angle between vectors, and projections.",
        category = ToolCategory.MATH,
        tags = listOf("vector", "dot product", "cross product", "magnitude", "unit vector", "angle", "projection", "linear algebra", "physics"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Navigation"
    )

    override suspend fun execute(input: VectorInput): ToolResult<VectorOutput> {
        val startTime = System.currentTimeMillis()

        val ax = input.ax; val ay = input.ay; val az = input.az
        val bx = input.bx; val by = input.by; val bz = input.bz

        val magA = sqrt(ax * ax + ay * ay + az * az)
        val magB = sqrt(bx * bx + by * by + bz * bz)

        val unitA = if (magA > 0) {
            "(%.4f, %.4f, %.4f)".format(Locale.US, ax / magA, ay / magA, az / magA)
        } else "(0, 0, 0)"

        val unitB = if (magB > 0) {
            "(%.4f, %.4f, %.4f)".format(Locale.US, bx / magB, by / magB, bz / magB)
        } else "(0, 0, 0)"

        // Dot product: A · B = ax*bx + ay*by + az*bz
        val dot = ax * bx + ay * by + az * bz

        // Cross product: A × B = (ay*bz - az*by, az*bx - ax*bz, ax*by - ay*bx)
        val cx = ay * bz - az * by
        val cy = az * bx - ax * bz
        val cz = ax * by - ay * bx
        val crossStr = "(%.3f, %.3f, %.3f)".format(Locale.US, cx, cy, cz)

        // Angle theta = acos(dot / (magA * magB))
        val cosTheta = if (magA > 0 && magB > 0) {
            (dot / (magA * magB)).coerceIn(-1.0, 1.0)
        } else 0.0

        val rad = acos(cosTheta)
        val deg = rad * 180.0 / PI

        // Projection of A onto B: (dot / magB^2) * B
        val projStr = if (magB > 0) {
            val scalar = dot / (magB * magB)
            "(%.3f, %.3f, %.3f)".format(Locale.US, scalar * bx, scalar * by, scalar * bz)
        } else "(0, 0, 0)"

        val report = buildString {
            appendLine("3D VECTOR ARITHMETIC REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Vector A:            (%.3f, %.3f, %.3f)".format(Locale.US, ax, ay, az))
            appendLine("Vector B:            (%.3f, %.3f, %.3f)".format(Locale.US, bx, by, bz))
            appendLine()
            appendLine("MAGNITUDES & UNIT VECTORS:")
            appendLine("• |A| Magnitude:     %.4f".format(Locale.US, magA))
            appendLine("• Â Unit Vector:     $unitA")
            appendLine("• |B| Magnitude:     %.4f".format(Locale.US, magB))
            appendLine("• B̂ Unit Vector:     $unitB")
            appendLine()
            appendLine("PRODUCTS & ORIENTATION:")
            appendLine("• Dot Product (A·B): %.4f".format(Locale.US, dot))
            appendLine("• Cross Prod (A×B):  $crossStr")
            appendLine("• Angle Between (θ): %.2f° (%.4f rad)".format(Locale.US, deg, rad))
            appendLine("• Proj of A on B:    $projStr")
        }

        val summary = "A·B = %.2f | A×B = $crossStr | θ = %.1f°".format(Locale.US, dot, deg)

        return ToolResult.Success(
            data = VectorOutput(
                magnitudeA = magA,
                magnitudeB = magB,
                unitVectorA = unitA,
                unitVectorB = unitB,
                dotProduct = dot,
                crossProduct = crossStr,
                angleDegrees = deg,
                angleRadians = rad,
                projectionAonB = projStr,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
