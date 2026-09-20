package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.roundToInt

enum class ColorBlindnessType(val displayName: String, val affectedPhotoreceptor: String) {
    PROTANOPIA("Protanopia", "L-cone deficient (Red-Blind, ~1% of males)"),
    DEUTERANOPIA("Deuteranopia", "M-cone deficient (Green-Blind, ~5% of males)"),
    TRITANOPIA("Tritanopia", "S-cone deficient (Blue-Blind, rare <0.1%)"),
    ACHROMATOPSIA("Achromatopsia", "Monochromacy / Total Color Blindness")
}

data class ColorBlindnessInput(
    val hexColor: String = "#E11D48",
    val deficiency: ColorBlindnessType = ColorBlindnessType.DEUTERANOPIA
)

data class ColorBlindnessOutput(
    val originalHex: String,
    val simulatedHex: String,
    val originalRgb: String,
    val simulatedRgb: String,
    val deficiencyName: String,
    val description: String,
    val contrastDelta: Double,
    val formattedReport: String,
    val summary: String
)

class ColorBlindnessSimulatorTool : Tool<ColorBlindnessInput, ColorBlindnessOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "color_blindness_simulator_tool",
        name = "Color Vision Deficiency & Accessibility Simulator",
        description = "Simulate how colors appear to individuals with Protanopia, Deuteranopia, Tritanopia, or Achromatopsia.",
        category = ToolCategory.COLOR,
        tags = listOf("color", "blindness", "accessibility", "a11y", "wcag", "protanopia", "deuteranopia", "tritanopia", "achromatopsia"),
        inputType = ToolDataType.COLOR,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Visibility"
    )

    override suspend fun execute(input: ColorBlindnessInput): ToolResult<ColorBlindnessOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.hexColor.trim().removePrefix("#")

        if (raw.length != 6 && raw.length != 8) {
            return ToolResult.Failure("Invalid HEX color code: '${input.hexColor}'. Must be a 6 or 8 character hex code (e.g. #E11D48).")
        }

        val r = raw.substring(0, 2).toIntOrNull(16) ?: return ToolResult.Failure("Invalid hex digits.")
        val g = raw.substring(2, 4).toIntOrNull(16) ?: return ToolResult.Failure("Invalid hex digits.")
        val b = raw.substring(4, 6).toIntOrNull(16) ?: return ToolResult.Failure("Invalid hex digits.")

        val (simR, simG, simB) = simulate(r, g, b, input.deficiency)

        val origHex = String.format("#%02X%02X%02X", r, g, b)
        val simHex = String.format("#%02X%02X%02X", simR, simG, simB)
        val origRgb = "rgb($r, $g, $b)"
        val simRgb = "rgb($simR, $simG, $simB)"

        // Euclidean color distance between original and simulated
        val delta = kotlin.math.sqrt(
            ((r - simR) * (r - simR) + (g - simG) * (g - simG) + (b - simB) * (b - simB)).toDouble()
        )

        val report = buildString {
            appendLine("COLOR VISION ACCESSIBILITY AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Original Color:   $origHex ($origRgb)")
            appendLine("Deficiency Mode:  ${input.deficiency.displayName}")
            appendLine("Condition Detail: ${input.deficiency.affectedPhotoreceptor}")
            appendLine()
            appendLine("SIMULATED COLOR PERCEPTION")
            appendLine("• Simulated HEX:  $simHex")
            appendLine("• Simulated RGB:  $simRgb")
            appendLine("• Color Shift:    ${String.format(Locale.US, "%.1f", delta)} Euclidean RGB units")
        }

        val summary = "$origHex → $simHex (${input.deficiency.displayName})"

        return ToolResult.Success(
            data = ColorBlindnessOutput(
                originalHex = origHex,
                simulatedHex = simHex,
                originalRgb = origRgb,
                simulatedRgb = simRgb,
                deficiencyName = input.deficiency.displayName,
                description = input.deficiency.affectedPhotoreceptor,
                contrastDelta = delta,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun simulate(r: Int, g: Int, b: Int, type: ColorBlindnessType): Triple<Int, Int, Int> {
        val rf = r / 255.0
        val gf = g / 255.0
        val bf = b / 255.0

        val (outR, outG, outB) = when (type) {
            ColorBlindnessType.PROTANOPIA -> {
                // Vienot 1999 matrix for Protanopia
                Triple(
                    0.56667 * rf + 0.43333 * gf + 0.0 * bf,
                    0.55833 * rf + 0.44167 * gf + 0.0 * bf,
                    0.0 * rf + 0.24167 * gf + 0.75833 * bf
                )
            }
            ColorBlindnessType.DEUTERANOPIA -> {
                // Vienot 1999 matrix for Deuteranopia
                Triple(
                    0.625 * rf + 0.375 * gf + 0.0 * bf,
                    0.70 * rf + 0.30 * gf + 0.0 * bf,
                    0.0 * rf + 0.30 * gf + 0.70 * bf
                )
            }
            ColorBlindnessType.TRITANOPIA -> {
                // Brettel matrix for Tritanopia
                Triple(
                    0.95 * rf + 0.05 * gf + 0.0 * bf,
                    0.0 * rf + 0.43333 * gf + 0.56667 * bf,
                    0.0 * rf + 0.475 * gf + 0.525 * bf
                )
            }
            ColorBlindnessType.ACHROMATOPSIA -> {
                // ITU-R BT.709 Luminance
                val gray = 0.2126 * rf + 0.7152 * gf + 0.0722 * bf
                Triple(gray, gray, gray)
            }
        }

        return Triple(
            (outR.coerceIn(0.0, 1.0) * 255.0).roundToInt(),
            (outG.coerceIn(0.0, 1.0) * 255.0).roundToInt(),
            (outB.coerceIn(0.0, 1.0) * 255.0).roundToInt()
        )
    }
}
