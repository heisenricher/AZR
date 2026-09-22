package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class CieLab(val l: Double, val a: Double, val b: Double) {
    override fun toString(): String = String.format(Locale.US, "L*: %.2f, a*: %.2f, b*: %.2f", l, a, b)
}

data class DeltaEInput(
    val color1Hex: String = "#3498db",
    val color2Hex: String = "#2980b9"
)

data class DeltaEOutput(
    val color1Hex: String,
    val color2Hex: String,
    val lab1: CieLab,
    val lab2: CieLab,
    val deltaE2000: Double,
    val deltaE76: Double,
    val perceptualAssessment: String,
    val formattedReport: String,
    val summary: String
)

class ColorDeltaE2000Tool : Tool<DeltaEInput, DeltaEOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "color_delta_e_2000_tool",
        name = "CIEDE2000 Color Difference (ΔE00) Calculator",
        description = "Calculate perceptual color difference using the official CIEDE2000 (ΔE00) standard and CIE76 with Just Noticeable Difference (JND) rating.",
        category = ToolCategory.COLOR,
        tags = listOf("delta e", "ciede2000", "color difference", "cielab", "perceptual", "color", "design", "jnd", "hex"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Palette"
    )

    override suspend fun execute(input: DeltaEInput): ToolResult<DeltaEOutput> {
        val startTime = System.currentTimeMillis()

        val hex1 = cleanHex(input.color1Hex)
            ?: return ToolResult.Failure("Invalid Color 1 HEX format: '${input.color1Hex}'. Expected e.g. #3498db or #FFF.")
        val hex2 = cleanHex(input.color2Hex)
            ?: return ToolResult.Failure("Invalid Color 2 HEX format: '${input.color2Hex}'. Expected e.g. #2980b9 or #000.")

        val lab1 = hexToLab(hex1)
        val lab2 = hexToLab(hex2)

        val dE76 = sqrt((lab1.l - lab2.l).pow(2) + (lab1.a - lab2.a).pow(2) + (lab1.b - lab2.b).pow(2))
        val dE00 = calculateCiede2000(lab1, lab2)

        val assessment = when {
            dE00 <= 1.0 -> "Imperceptible (<= 1.0 JND: Indistinguishable to normal human eye)"
            dE00 <= 2.0 -> "Perceptible through close observation (1.0 - 2.0: Barely noticeable difference)"
            dE00 <= 5.0 -> "Noticeable at a glance (2.0 - 5.0: Readily distinguishable colors)"
            dE00 <= 10.0 -> "Distinct difference (5.0 - 10.0: Obvious variation)"
            dE00 <= 49.0 -> "Substantially different shades (10.0 - 49.0: Dissimilar colors)"
            else -> "Completely different / Opposite colors (> 49.0)"
        }

        val report = buildString {
            appendLine("CIEDE2000 COLOR DIFFERENCE (ΔE₀₀) REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Color 1 (Reference): $hex1  ($lab1)")
            appendLine("Color 2 (Sample):    $hex2  ($lab2)")
            appendLine("--------------------------------------------------")
            appendLine(String.format(Locale.US, "CIEDE2000 (ΔE₀₀):    %.4f", dE00))
            appendLine(String.format(Locale.US, "CIE76 (ΔE*₇₆):       %.4f", dE76))
            appendLine("--------------------------------------------------")
            appendLine("PERCEPTUAL EVALUATION:")
            appendLine(assessment)
        }

        return ToolResult.Success(
            data = DeltaEOutput(
                color1Hex = hex1,
                color2Hex = hex2,
                lab1 = lab1,
                lab2 = lab2,
                deltaE2000 = dE00,
                deltaE76 = dE76,
                perceptualAssessment = assessment,
                formattedReport = report,
                summary = String.format(Locale.US, "ΔE₀₀: %.2f (%s)", dE00, if (dE00 <= 1.0) "Match" else "Differs")
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = String.format(Locale.US, "CIEDE2000: %.2f", dE00)
        )
    }

    private fun cleanHex(raw: String): String? {
        val s = raw.trim().removePrefix("#")
        return when (s.length) {
            3 -> "#" + s.map { "$it$it" }.joinToString("")
            6 -> "#$s"
            8 -> "#" + s.substring(2) // trim alpha if provided as AARRGGBB
            else -> null
        }
    }

    private fun hexToLab(hex: String): CieLab {
        val r255 = hex.substring(1, 3).toInt(16)
        val g255 = hex.substring(3, 5).toInt(16)
        val b255 = hex.substring(5, 7).toInt(16)

        // sRGB to linear RGB
        fun pivotRgb(n: Double): Double {
            val v = n / 255.0
            return if (v > 0.04045) ((v + 0.055) / 1.055).pow(2.4) else v / 12.92
        }

        val r = pivotRgb(r255.toDouble()) * 100.0
        val g = pivotRgb(g255.toDouble()) * 100.0
        val b = pivotRgb(b255.toDouble()) * 100.0

        // Linear RGB to XYZ (D65 illuminant matrix)
        val x = r * 0.4124564 + g * 0.3575761 + b * 0.1804375
        val y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750
        val z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041

        // D65 reference white points
        val xn = 95.047
        val yn = 100.000
        val zn = 108.883

        fun pivotXyz(v: Double): Double {
            return if (v > 0.008856) v.pow(1.0 / 3.0) else (7.787 * v) + (16.0 / 116.0)
        }

        val fx = pivotXyz(x / xn)
        val fy = pivotXyz(y / yn)
        val fz = pivotXyz(z / zn)

        val lStar = (116.0 * fy) - 16.0
        val aStar = 500.0 * (fx - fy)
        val bStar = 200.0 * (fy - fz)

        return CieLab(lStar, aStar, bStar)
    }

    private fun calculateCiede2000(c1: CieLab, c2: CieLab): Double {
        val kL = 1.0
        val kC = 1.0
        val kH = 1.0

        val c1Star = hypot(c1.a, c1.b)
        val c2Star = hypot(c2.a, c2.b)
        val cBar = (c1Star + c2Star) / 2.0

        val g = 0.5 * (1.0 - sqrt(cBar.pow(7) / (cBar.pow(7) + 25.0.pow(7))))

        val a1Prime = (1.0 + g) * c1.a
        val a2Prime = (1.0 + g) * c2.a

        val c1Prime = hypot(a1Prime, c1.b)
        val c2Prime = hypot(a2Prime, c2.b)

        fun computeHPrime(aPrime: Double, b: Double): Double {
            if (aPrime == 0.0 && b == 0.0) return 0.0
            var deg = Math.toDegrees(atan2(b, aPrime))
            if (deg < 0.0) deg += 360.0
            return deg
        }

        val h1Prime = computeHPrime(a1Prime, c1.b)
        val h2Prime = computeHPrime(a2Prime, c2.b)

        val deltaLPrime = c2.l - c1.l
        val deltaCPrime = c2Prime - c1Prime

        var deltaHPrime = 0.0
        if (c1Prime * c2Prime != 0.0) {
            val diff = h2Prime - h1Prime
            deltaHPrime = when {
                abs(diff) <= 180.0 -> diff
                diff > 180.0 -> diff - 360.0
                else -> diff + 360.0
            }
        }
        val deltaBigHPrime = 2.0 * sqrt(c1Prime * c2Prime) * sin(Math.toRadians(deltaHPrime / 2.0))

        val lBarPrime = (c1.l + c2.l) / 2.0
        val cBarPrime = (c1Prime + c2Prime) / 2.0

        var hBarPrime = 0.0
        if (c1Prime * c2Prime != 0.0) {
            val sum = h1Prime + h2Prime
            hBarPrime = when {
                abs(h1Prime - h2Prime) <= 180.0 -> sum / 2.0
                sum < 360.0 -> (sum + 360.0) / 2.0
                else -> (sum - 360.0) / 2.0
            }
        }

        val t = 1.0 -
                0.17 * cos(Math.toRadians(hBarPrime - 30.0)) +
                0.24 * cos(Math.toRadians(2.0 * hBarPrime)) +
                0.32 * cos(Math.toRadians(3.0 * hBarPrime + 6.0)) -
                0.20 * cos(Math.toRadians(4.0 * hBarPrime - 63.0))

        val deltaTheta = 30.0 * exp(-(((hBarPrime - 275.0) / 25.0).pow(2)))
        val rC = 2.0 * sqrt(cBarPrime.pow(7) / (cBarPrime.pow(7) + 25.0.pow(7)))
        val sL = 1.0 + (0.015 * (lBarPrime - 50.0).pow(2)) / sqrt(20.0 + (lBarPrime - 50.0).pow(2))
        val sC = 1.0 + 0.045 * cBarPrime
        val sH = 1.0 + 0.015 * cBarPrime * t
        val rT = -sin(Math.toRadians(2.0 * deltaTheta)) * rC

        val lTerm = deltaLPrime / (kL * sL)
        val cTerm = deltaCPrime / (kC * sC)
        val hTerm = deltaBigHPrime / (kH * sH)

        val dE = sqrt(lTerm.pow(2) + cTerm.pow(2) + hTerm.pow(2) + rT * cTerm * hTerm)
        return dE
    }
}
