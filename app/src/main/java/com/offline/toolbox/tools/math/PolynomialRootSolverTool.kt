package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.sign
import kotlin.math.sinh
import kotlin.math.sqrt

enum class PolynomialDegree {
    QUADRATIC,
    CUBIC
}

data class PolynomialInput(
    val degree: PolynomialDegree = PolynomialDegree.QUADRATIC,
    val a: Double = 1.0,
    val b: Double = -5.0,
    val c: Double = 6.0,
    val d: Double = 0.0 // Used for cubic: ax^3 + bx^2 + cx + d = 0
)

data class PolynomialOutput(
    val degree: PolynomialDegree,
    val roots: List<String>,
    val discriminant: Double,
    val natureOfRoots: String,
    val formattedEquation: String,
    val formattedReport: String,
    val summary: String
)

class PolynomialRootSolverTool : Tool<PolynomialInput, PolynomialOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "polynomial_root_solver_tool",
        name = "Quadratic & Cubic Polynomial Root Solver",
        description = "Calculate exact real and complex roots for quadratic and cubic polynomials with discriminant analysis and factoring.",
        category = ToolCategory.MATH,
        tags = listOf("polynomial", "quadratic", "cubic", "algebra", "roots", "discriminant", "math", "equation", "formula"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Calculate"
    )

    override suspend fun execute(input: PolynomialInput): ToolResult<PolynomialOutput> {
        val startTime = System.currentTimeMillis()
        val a = input.a

        if (abs(a) < 1e-12) {
            return ToolResult.Failure("Leading coefficient 'a' cannot be zero.")
        }

        return if (input.degree == PolynomialDegree.QUADRATIC) {
            val b = input.b
            val c = input.c
            val delta = b * b - 4.0 * a * c

            val roots = mutableListOf<String>()
            val nature: String

            when {
                abs(delta) < 1e-10 -> {
                    val r = -b / (2.0 * a)
                    roots.add(formatNumber(r))
                    nature = "One Repeated Real Root (Double Root)"
                }
                delta > 0 -> {
                    val sqrtD = sqrt(delta)
                    val r1 = (-b + sqrtD) / (2.0 * a)
                    val r2 = (-b - sqrtD) / (2.0 * a)
                    roots.add(formatNumber(r1))
                    roots.add(formatNumber(r2))
                    nature = "Two Distinct Real Roots"
                }
                else -> {
                    val realPart = -b / (2.0 * a)
                    val imagPart = sqrt(-delta) / (2.0 * a)
                    roots.add("${formatNumber(realPart)} + ${formatNumber(abs(imagPart))}i")
                    roots.add("${formatNumber(realPart)} - ${formatNumber(abs(imagPart))}i")
                    nature = "Two Complex Conjugate Roots"
                }
            }

            val eqStr = "${formatCoeff(a, "x²", isFirst = true)} ${formatCoeff(b, "x")} ${formatCoeff(c, "")} = 0"
            val report = buildString {
                appendLine("QUADRATIC POLYNOMIAL ROOT ANALYSIS")
                appendLine("--------------------------------------------------")
                appendLine("Equation:         $eqStr")
                appendLine("Discriminant (Δ): ${formatNumber(delta)}")
                appendLine("Root Nature:      $nature")
                appendLine()
                appendLine("SOLUTIONS:")
                roots.forEachIndexed { i, r -> appendLine("• x${i + 1} = $r") }
            }

            val summary = "Quadratic: ${roots.joinToString(", ")} (Δ = ${formatNumber(delta)})"

            ToolResult.Success(
                data = PolynomialOutput(
                    degree = input.degree,
                    roots = roots,
                    discriminant = delta,
                    natureOfRoots = nature,
                    formattedEquation = eqStr,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } else {
            // CUBIC: ax^3 + bx^2 + cx + d = 0
            val b = input.b
            val c = input.c
            val d = input.d

            // Depressed cubic t^3 + pt + q = 0 where x = t - b/(3a)
            val p = (3.0 * a * c - b * b) / (3.0 * a * a)
            val q = (2.0 * b * b * b - 9.0 * a * b * c + 27.0 * a * a * d) / (27.0 * a * a * a)
            val delta = (q * q / 4.0) + (p * p * p / 27.0)

            val roots = mutableListOf<String>()
            val nature: String
            val shift = -b / (3.0 * a)

            if (delta > 1e-10) {
                // One real root and two complex conjugate roots (Cardano's formula)
                val u = cbrt(-q / 2.0 + sqrt(delta))
                val v = cbrt(-q / 2.0 - sqrt(delta))
                val r1 = u + v + shift
                val realPart = -(u + v) / 2.0 + shift
                val imagPart = sqrt(3.0) / 2.0 * (u - v)

                roots.add(formatNumber(r1))
                roots.add("${formatNumber(realPart)} + ${formatNumber(abs(imagPart))}i")
                roots.add("${formatNumber(realPart)} - ${formatNumber(abs(imagPart))}i")
                nature = "One Real Root & Two Complex Conjugate Roots"
            } else if (delta < -1e-10) {
                // Three distinct real roots (Trigonometric method)
                val m = 2.0 * sqrt(-p / 3.0)
                val theta = acos((3.0 * q) / (p * m)) / 3.0
                val r1 = m * cos(theta) + shift
                val r2 = m * cos(theta - 2.0 * PI / 3.0) + shift
                val r3 = m * cos(theta - 4.0 * PI / 3.0) + shift

                roots.add(formatNumber(r1))
                roots.add(formatNumber(r2))
                roots.add(formatNumber(r3))
                nature = "Three Distinct Real Roots"
            } else {
                // Multiple real roots
                val u = cbrt(-q / 2.0)
                val r1 = 2.0 * u + shift
                val r2 = -u + shift
                roots.add(formatNumber(r1))
                roots.add(formatNumber(r2))
                nature = "Real Roots with Multiplicity"
            }

            val eqStr = "${formatCoeff(a, "x³", isFirst = true)} ${formatCoeff(b, "x²")} ${formatCoeff(c, "x")} ${formatCoeff(d, "")} = 0"
            val report = buildString {
                appendLine("CUBIC POLYNOMIAL ROOT ANALYSIS")
                appendLine("--------------------------------------------------")
                appendLine("Equation:         $eqStr")
                appendLine("Discriminant:     ${formatNumber(delta)}")
                appendLine("Root Nature:      $nature")
                appendLine()
                appendLine("SOLUTIONS:")
                roots.forEachIndexed { i, r -> appendLine("• x${i + 1} = $r") }
            }

            val summary = "Cubic: ${roots.joinToString(", ")}"

            ToolResult.Success(
                data = PolynomialOutput(
                    degree = input.degree,
                    roots = roots,
                    discriminant = delta,
                    natureOfRoots = nature,
                    formattedEquation = eqStr,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        }
    }

    private fun cbrt(v: Double): Double {
        return sign(v) * Math.pow(abs(v), 1.0 / 3.0)
    }

    private fun formatNumber(v: Double): String {
        return String.format(Locale.US, "%.4f", if (abs(v) < 1e-10) 0.0 else v)
    }

    private fun formatCoeff(coeff: Double, term: String, isFirst: Boolean = false): String {
        if (abs(coeff) < 1e-10) return ""
        val sign = if (coeff < 0) "- " else if (isFirst) "" else "+ "
        val absVal = abs(coeff)
        val numStr = if (term.isNotEmpty() && absVal == 1.0) "" else String.format(Locale.US, "%.2f", absVal).removeSuffix(".00")
        return "$sign$numStr$term"
    }
}
