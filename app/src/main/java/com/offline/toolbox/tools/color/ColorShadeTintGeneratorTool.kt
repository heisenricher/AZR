package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class ColorShadeTintInput(
    val baseHex: String = "#3F51B5", // Indigo base
    val stepCount: Int = 10          // 10 or 20 steps
)

data class ColorStep(
    val stepLabel: String,
    val hex: String,
    val red: Int,
    val green: Int,
    val blue: Int,
    val relativeLuminance: Double,
    val contrastOnWhite: Double,
    val contrastOnBlack: Double,
    val recommendedTextHex: String
)

data class ColorShadeTintOutput(
    val baseHex: String,
    val totalSteps: Int,
    val tints: List<ColorStep>,
    val baseColorStep: ColorStep,
    val shades: List<ColorStep>,
    val fullScale: List<ColorStep>,
    val formattedReport: String,
    val summary: String
)

class ColorShadeTintGeneratorTool : Tool<ColorShadeTintInput, ColorShadeTintOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "color_shade_tint_generator_tool",
        name = "Monochromatic Color Shade & Tint Scale Generator",
        description = "Generate stepped monochromatic color scales by mixing base hues with pure White (Tints) and Black (Shades) with relative luminance and WCAG text contrast ratings.",
        category = ToolCategory.COLOR,
        tags = listOf("color", "shades", "tints", "palette", "monochromatic", "hex", "contrast", "wcag", "design system", "ui"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Palette"
    )

    private fun parseHex(input: String): Triple<Int, Int, Int> {
        val clean = input.trim().removePrefix("#").trim()
        val hex = when (clean.length) {
            3 -> "${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
            6 -> clean
            8 -> clean.substring(2, 8) // Strip alpha prefix if ARGB
            else -> throw IllegalArgumentException("Invalid HEX color '#$input'. Expected 3 or 6 hex digits.")
        }
        val r = hex.substring(0, 2).toInt(16)
        val g = hex.substring(2, 4).toInt(16)
        val b = hex.substring(4, 6).toInt(16)
        return Triple(r, g, b)
    }

    private fun srgbToLinear(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun relativeLuminance(r: Int, g: Int, b: Int): Double {
        val rLin = srgbToLinear(r / 255.0)
        val gLin = srgbToLinear(g / 255.0)
        val bLin = srgbToLinear(b / 255.0)
        return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
    }

    private fun contrastRatio(lum1: Double, lum2: Double): Double {
        val l1 = max(lum1, lum2)
        val l2 = min(lum1, lum2)
        return (l1 + 0.05) / (l2 + 0.05)
    }

    private fun createColorStep(label: String, r: Int, g: Int, b: Int): ColorStep {
        val crR = r.coerceIn(0, 255)
        val crG = g.coerceIn(0, 255)
        val crB = b.coerceIn(0, 255)
        val hex = "#%02X%02X%02X".format(Locale.US, crR, crG, crB)
        val lum = relativeLuminance(crR, crG, crB)
        val crWhite = contrastRatio(lum, 1.0)
        val crBlack = contrastRatio(lum, 0.0)
        val bestText = if (crWhite >= crBlack) "#FFFFFF" else "#000000"
        return ColorStep(label, hex, crR, crG, crB, lum, crWhite, crBlack, bestText)
    }

    override suspend fun execute(input: ColorShadeTintInput): ToolResult<ColorShadeTintOutput> {
        val startTime = System.currentTimeMillis()
        val (baseR, baseG, baseB) = try {
            parseHex(input.baseHex)
        } catch (e: Exception) {
            return ToolResult.Failure("Invalid HEX color: ${e.message}")
        }

        val stepCount = input.stepCount.coerceIn(5, 20)
        val stepInterval = 1.0 / stepCount

        // Tints: mix with White (factor 0.9 down to 0.1)
        val tints = mutableListOf<ColorStep>()
        for (i in (stepCount - 1) downTo 1) {
            val factor = i * stepInterval
            val r = (baseR + (255 - baseR) * factor).roundToInt()
            val g = (baseG + (255 - baseG) * factor).roundToInt()
            val b = (baseB + (255 - baseB) * factor).roundToInt()
            val pct = (factor * 100).roundToInt()
            tints.add(createColorStep("Tint +$pct%", r, g, b))
        }

        val baseStep = createColorStep("Base 100%", baseR, baseG, baseB)

        // Shades: mix with Black (factor 0.1 up to 0.9)
        val shades = mutableListOf<ColorStep>()
        for (i in 1 until stepCount) {
            val factor = i * stepInterval
            val r = (baseR * (1.0 - factor)).roundToInt()
            val g = (baseG * (1.0 - factor)).roundToInt()
            val b = (baseB * (1.0 - factor)).roundToInt()
            val pct = (factor * 100).roundToInt()
            shades.add(createColorStep("Shade -$pct%", r, g, b))
        }

        val fullScale = tints + listOf(baseStep) + shades

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== MONOCHROMATIC SHADE & TINT SCALE ===")
            appendLine("Base Color:       ${baseStep.hex} (rgb($baseR, $baseG, $baseB))")
            appendLine("Scale Steps:      ${fullScale.size} swatches")
            appendLine("----------------------------------------")
            appendLine("%-12s | %-8s | %-15s | %-10s | %s".format(Locale.US, "Step", "HEX", "RGB", "Lum (Y)", "Text / Contrast"))
            fullScale.forEach { s ->
                val txtLabel = if (s.recommendedTextHex == "#FFFFFF") "White (${String.format(Locale.US, "%.1f", s.contrastOnWhite)}:1)" else "Black (${String.format(Locale.US, "%.1f", s.contrastOnBlack)}:1)"
                appendLine("%-12s | %-8s | rgb(%3d,%3d,%3d) | %-10.4f | %s".format(Locale.US, s.stepLabel, s.hex, s.red, s.green, s.blue, s.relativeLuminance, txtLabel))
            }
        }

        return ToolResult.Success(
            data = ColorShadeTintOutput(
                baseHex = baseStep.hex,
                totalSteps = fullScale.size,
                tints = tints,
                baseColorStep = baseStep,
                shades = shades,
                fullScale = fullScale,
                formattedReport = report,
                summary = "Generated ${fullScale.size} shade & tint swatches for ${baseStep.hex}."
            ),
            executionTimeMs = elapsed,
            summary = "Generated ${fullScale.size} swatches for ${baseStep.hex}"
        )
    }
}
