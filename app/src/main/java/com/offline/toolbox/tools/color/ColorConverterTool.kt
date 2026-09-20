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
import kotlin.math.roundToInt

data class ColorOutput(
    val hex6: String,
    val hex8: String,
    val rgbString: String,
    val hslString: String,
    val cmykString: String,
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Float,
    val relativeLuminance: Double,
    val contrastWhite: Double,
    val contrastBlack: Double,
    val wcagWhiteLevel: String,
    val wcagBlackLevel: String,
    val recommendedTextColor: String
)

class ColorConverterTool : Tool<String, ColorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "color_converter",
        name = "Color Converter & Contrast",
        description = "Convert between HEX, RGB, HSL, CMYK, with WCAG 2.1 contrast ratio and accessibility checks.",
        category = ToolCategory.COLOR,
        tags = listOf("color", "hex", "rgb", "hsl", "cmyk", "contrast", "wcag", "accessibility", "palette"),
        inputType = ToolDataType.COLOR,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Palette"
    )

    override suspend fun execute(input: String): ToolResult<ColorOutput> {
        val startTime = System.currentTimeMillis()
        val cleanInput = input.trim().removePrefix("#")

        val (r, g, b, a) = parseHex(cleanInput)
            ?: return ToolResult.Failure(
                message = "Invalid color format: '$input'",
                userGuidance = "Enter a valid 3-character, 6-character, or 8-character HEX color code (e.g. #3B82F6 or 3B82F6)."
            )

        val hex6 = String.format(Locale.US, "#%02X%02X%02X", r, g, b)
        val alphaInt = (a * 255).roundToInt()
        val hex8 = String.format(Locale.US, "#%02X%02X%02X%02X", alphaInt, r, g, b)

        val rgbString = "rgb($r, $g, $b)"

        // HSL calculation
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val maxVal = max(rf, max(gf, bf))
        val minVal = min(rf, min(gf, bf))
        val delta = maxVal - minVal

        val l = (maxVal + minVal) / 2f
        val s = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * l - 1f))
        var h = when {
            delta == 0f -> 0f
            maxVal == rf -> ((gf - bf) / delta) % 6f
            maxVal == gf -> ((bf - rf) / delta) + 2f
            else -> ((rf - gf) / delta) + 4f
        } * 60f
        if (h < 0) h += 360f

        val hslString = String.format(
            Locale.US,
            "hsl(%d, %d%%, %d%%)",
            h.roundToInt(),
            (s * 100).roundToInt(),
            (l * 100).roundToInt()
        )

        // CMYK calculation
        val k = 1f - maxVal
        val c = if (k == 1f) 0f else (1f - rf - k) / (1f - k)
        val m = if (k == 1f) 0f else (1f - gf - k) / (1f - k)
        val y = if (k == 1f) 0f else (1f - bf - k) / (1f - k)

        val cmykString = String.format(
            Locale.US,
            "cmyk(%d%%, %d%%, %d%%, %d%%)",
            (c * 100).roundToInt(),
            (m * 100).roundToInt(),
            (y * 100).roundToInt(),
            (k * 100).roundToInt()
        )

        // WCAG 2.1 Luminance and Contrast
        val luminance = calculateLuminance(r, g, b)
        val whiteLuminance = 1.0
        val blackLuminance = 0.0

        val contrastWhite = (whiteLuminance + 0.05) / (luminance + 0.05)
        val contrastBlack = (luminance + 0.05) / (blackLuminance + 0.05)

        val wcagWhiteLevel = getWcagLevel(contrastWhite)
        val wcagBlackLevel = getWcagLevel(contrastBlack)
        val recommendedText = if (contrastWhite >= contrastBlack) "#FFFFFF (White)" else "#000000 (Black)"

        return ToolResult.Success(
            data = ColorOutput(
                hex6 = hex6,
                hex8 = hex8,
                rgbString = rgbString,
                hslString = hslString,
                cmykString = cmykString,
                red = r,
                green = g,
                blue = b,
                alpha = a,
                relativeLuminance = Math.round(luminance * 1000.0) / 1000.0,
                contrastWhite = Math.round(contrastWhite * 100.0) / 100.0,
                contrastBlack = Math.round(contrastBlack * 100.0) / 100.0,
                wcagWhiteLevel = wcagWhiteLevel,
                wcagBlackLevel = wcagBlackLevel,
                recommendedTextColor = recommendedText
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "$hex6 • $rgbString • $hslString"
        )
    }

    private fun parseHex(hex: String): ColorComponents? {
        return try {
            when (hex.length) {
                3 -> {
                    val r = hex.substring(0, 1).repeat(2).toInt(16)
                    val g = hex.substring(1, 2).repeat(2).toInt(16)
                    val b = hex.substring(2, 3).repeat(2).toInt(16)
                    ColorComponents(r, g, b, 1.0f)
                }
                6 -> {
                    val r = hex.substring(0, 2).toInt(16)
                    val g = hex.substring(2, 4).toInt(16)
                    val b = hex.substring(4, 6).toInt(16)
                    ColorComponents(r, g, b, 1.0f)
                }
                8 -> {
                    val a = hex.substring(0, 2).toInt(16) / 255.0f
                    val r = hex.substring(2, 4).toInt(16)
                    val g = hex.substring(4, 6).toInt(16)
                    val b = hex.substring(6, 8).toInt(16)
                    ColorComponents(r, g, b, a)
                }
                else -> null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun calculateLuminance(r: Int, g: Int, b: Int): Double {
        val sR = r / 255.0
        val sG = g / 255.0
        val sB = b / 255.0

        val rL = if (sR <= 0.03928) sR / 12.92 else Math.pow((sR + 0.055) / 1.055, 2.4)
        val gL = if (sG <= 0.03928) sG / 12.92 else Math.pow((sG + 0.055) / 1.055, 2.4)
        val bL = if (sB <= 0.03928) sB / 12.92 else Math.pow((sB + 0.055) / 1.055, 2.4)

        return 0.2126 * rL + 0.7152 * gL + 0.0722 * bL
    }

    private fun getWcagLevel(ratio: Double): String = when {
        ratio >= 7.0 -> "AAA (Pass: Normal & Large Text)"
        ratio >= 4.5 -> "AA (Pass: Normal & Large Text)"
        ratio >= 3.0 -> "AA Large (Pass: Large Text Only)"
        else -> "Fail (< 3:1)"
    }

    private data class ColorComponents(val r: Int, val g: Int, val b: Int, val a: Float)
}
