package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.roundToInt

data class ColorMixingInput(
    val color1Hex: String = "#FFFF00", // Yellow
    val color2Hex: String = "#0000FF", // Blue
    val ratioColor1Percent: Int = 50   // 50% Color 1, 50% Color 2
)

data class ColorMixingOutput(
    val color1Hex: String,
    val color2Hex: String,
    val ratioPercent: Int,
    val additiveRgbHex: String,
    val subtractiveCmyHex: String,
    val pigmentRybHex: String,
    val additiveRgbValues: String,
    val subtractiveCmyValues: String,
    val pigmentRybValues: String,
    val formattedReport: String,
    val summary: String
)

class ColorHarmonyMixingTool : Tool<ColorMixingInput, ColorMixingOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "color_harmony_mixing_tool",
        name = "Subtractive & Additive Color Mixer",
        description = "Simulate physical pigment subtractive color mixing (CMY/RYB absorption) and digital screen additive RGB light mixing.",
        category = ToolCategory.COLOR,
        tags = listOf("color mix", "subtractive", "additive", "cmyk", "rgb", "pigment", "paint", "blending", "palette"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Palette"
    )

    override suspend fun execute(input: ColorMixingInput): ToolResult<ColorMixingOutput> {
        val startTime = System.currentTimeMillis()

        val rgb1 = parseHexToRgb(input.color1Hex)
            ?: return ToolResult.Failure("Invalid hexadecimal color 1: '${input.color1Hex}'. Format: #RRGGBB.")
        val rgb2 = parseHexToRgb(input.color2Hex)
            ?: return ToolResult.Failure("Invalid hexadecimal color 2: '${input.color2Hex}'. Format: #RRGGBB.")

        val w1 = input.ratioColor1Percent.coerceIn(0, 100) / 100.0
        val w2 = 1.0 - w1

        // 1. Digital Additive Mixing (RGB light addition)
        val addR = (rgb1.first * w1 + rgb2.first * w2).roundToInt().coerceIn(0, 255)
        val addG = (rgb1.second * w1 + rgb2.second * w2).roundToInt().coerceIn(0, 255)
        val addB = (rgb1.third * w1 + rgb2.third * w2).roundToInt().coerceIn(0, 255)
        val addHex = String.format(Locale.US, "#%02X%02X%02X", addR, addG, addB)

        // 2. Subtractive Mixing (CMY light absorption)
        // C = 1 - R/255, M = 1 - G/255, Y = 1 - B/255
        val c1 = 1.0 - (rgb1.first / 255.0)
        val m1 = 1.0 - (rgb1.second / 255.0)
        val y1 = 1.0 - (rgb1.third / 255.0)

        val c2 = 1.0 - (rgb2.first / 255.0)
        val m2 = 1.0 - (rgb2.second / 255.0)
        val y2 = 1.0 - (rgb2.third / 255.0)

        val cMix = (c1 * w1 + c2 * w2).coerceIn(0.0, 1.0)
        val mMix = (m1 * w1 + m2 * w2).coerceIn(0.0, 1.0)
        val yMix = (y1 * w1 + y2 * w2).coerceIn(0.0, 1.0)

        val subR = ((1.0 - cMix) * 255.0).roundToInt().coerceIn(0, 255)
        val subG = ((1.0 - mMix) * 255.0).roundToInt().coerceIn(0, 255)
        val subB = ((1.0 - yMix) * 255.0).roundToInt().coerceIn(0, 255)
        val subHex = String.format(Locale.US, "#%02X%02X%02X", subR, subG, subB)

        // 3. Artist Pigment Mixing (RYB color space approximation)
        val ryb1 = rgbToRyb(rgb1.first, rgb1.second, rgb1.third)
        val ryb2 = rgbToRyb(rgb2.first, rgb2.second, rgb2.third)
        val rybR = (ryb1.first * w1 + ryb2.first * w2).roundToInt().coerceIn(0, 255)
        val rybY = (ryb1.second * w1 + ryb2.second * w2).roundToInt().coerceIn(0, 255)
        val rybB = (ryb1.third * w1 + ryb2.third * w2).roundToInt().coerceIn(0, 255)
        val pigmentRgb = rybToRgb(rybR, rybY, rybB)
        val pigHex = String.format(Locale.US, "#%02X%02X%02X", pigmentRgb.first, pigmentRgb.second, pigmentRgb.third)

        val report = buildString {
            appendLine("COLOR MIXING & SPECTRAL HARMONY ANALYSIS")
            appendLine("--------------------------------------------------")
            appendLine("Color 1:             ${input.color1Hex.uppercase()} (${input.ratioColor1Percent}%)")
            appendLine("Color 2:             ${input.color2Hex.uppercase()} (${100 - input.ratioColor1Percent}%)")
            appendLine("--------------------------------------------------")
            appendLine("1. DIGITAL ADDITIVE MIX (RGB Screen Light):")
            appendLine("   Hex:              $addHex")
            appendLine("   RGB:              rgb($addR, $addG, $addB)")
            appendLine("   Physics:          Photons combined; emits more light wavelengths.")
            appendLine("--------------------------------------------------")
            appendLine("2. SUBTRACTIVE MIX (CMY Printing Inks):")
            appendLine("   Hex:              $subHex")
            appendLine("   RGB:              rgb($subR, $subG, $subB)")
            appendLine("   Absorption:       C: ${String.format(Locale.US, "%.0f%%", cMix * 100)}, M: ${String.format(Locale.US, "%.0f%%", mMix * 100)}, Y: ${String.format(Locale.US, "%.0f%%", yMix * 100)}")
            appendLine("--------------------------------------------------")
            appendLine("3. ARTIST PIGMENT MIX (RYB Paint Model):")
            appendLine("   Hex:              $pigHex")
            appendLine("   RGB:              rgb(${pigmentRgb.first}, ${pigmentRgb.second}, ${pigmentRgb.third})")
            appendLine("   Application:      Real-world oil, acrylic, and watercolor paint simulation.")
        }

        val output = ColorMixingOutput(
            color1Hex = input.color1Hex.uppercase(),
            color2Hex = input.color2Hex.uppercase(),
            ratioPercent = input.ratioColor1Percent,
            additiveRgbHex = addHex,
            subtractiveCmyHex = subHex,
            pigmentRybHex = pigHex,
            additiveRgbValues = "rgb($addR, $addG, $addB)",
            subtractiveCmyValues = "rgb($subR, $subG, $subB)",
            pigmentRybValues = "rgb(${pigmentRgb.first}, ${pigmentRgb.second}, ${pigmentRgb.third})",
            formattedReport = report,
            summary = "Pigment: $pigHex | Additive: $addHex | Subtractive: $subHex"
        )

        return ToolResult.Success(output, System.currentTimeMillis() - startTime, "Simulated color mixing models")
    }

    private fun parseHexToRgb(raw: String): Triple<Int, Int, Int>? {
        val clean = raw.trim().removePrefix("#")
        if (clean.length != 6) return null
        return try {
            val r = clean.substring(0, 2).toInt(16)
            val g = clean.substring(2, 4).toInt(16)
            val b = clean.substring(4, 6).toInt(16)
            Triple(r, g, b)
        } catch (_: Exception) {
            null
        }
    }

    // Gossett and Chen RYB <-> RGB transform approximation
    private fun rgbToRyb(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        val w = minOf(r, g, b)
        var red = r - w
        var green = g - w
        var blue = b - w

        val maxG = maxOf(red, green, blue)
        var yellow = minOf(red, green)
        red -= yellow
        green -= yellow

        if (blue > 0 && green > 0) {
            blue /= 2
            green /= 2
        }

        yellow += green
        blue += green

        val maxRyb = maxOf(red, yellow, blue)
        if (maxRyb > 0 && maxG > 0) {
            val factor = maxG.toDouble() / maxRyb
            red = (red * factor).roundToInt()
            yellow = (yellow * factor).roundToInt()
            blue = (blue * factor).roundToInt()
        }

        return Triple(red + w, yellow + w, blue + w)
    }

    private fun rybToRgb(r: Int, y: Int, b: Int): Triple<Int, Int, Int> {
        val w = minOf(r, y, b)
        var red = r - w
        var yellow = y - w
        var blue = b - w

        val maxRyb = maxOf(red, yellow, blue)
        var green = minOf(yellow, blue)
        yellow -= green
        blue -= green

        if (blue > 0 && green > 0) {
            blue *= 2
            green *= 2
        }

        red += yellow
        green += yellow

        val maxRgb = maxOf(red, green, blue)
        if (maxRgb > 0 && maxRyb > 0) {
            val factor = maxRyb.toDouble() / maxRgb
            red = (red * factor).roundToInt()
            green = (green * factor).roundToInt()
            blue = (blue * factor).roundToInt()
        }

        return Triple((red + w).coerceIn(0, 255), (green + w).coerceIn(0, 255), (blue + w).coerceIn(0, 255))
    }
}
