package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

data class ColorBlendInput(
    val foregroundHex: String = "#80FF5722", // 50% opacity Orange
    val backgroundHex: String = "#3F51B5",   // Indigo
    val hexFormat: String = "ARGB", // ARGB or RGBA
    val colorSpace: String = "SRGB" // SRGB or LINEAR
)

data class ColorBlendOutput(
    val blendedHexArgb: String,
    val blendedHexRgba: String,
    val cssRgba: String,
    val red: Int,
    val green: Int,
    val blue: Int,
    val alphaPercent: Double,
    val contrastRatioOnWhite: Double,
    val contrastRatioOnBlack: Double,
    val formattedReport: String,
    val summary: String
)

class ColorHexAlphaBlenderTool : Tool<ColorBlendInput, ColorBlendOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "color_hex_alpha_blender_tool",
        name = "HEX Alpha Color Porter-Duff Blender",
        description = "Blend translucent foreground colors over background layers using Porter-Duff 'Source Over' compositing in sRGB or linear color spaces with WCAG contrast checks.",
        category = ToolCategory.COLOR,
        tags = listOf("color", "hex", "alpha", "blender", "compositing", "porter duff", "transparency", "argb", "rgba", "wcag"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Layers"
    )

    private data class RgbaColor(val r: Double, val g: Double, val b: Double, val a: Double)

    private fun parseHex(input: String, isRgbaOrder: Boolean): RgbaColor {
        val clean = input.trim().removePrefix("#").trim()
        val hex = when (clean.length) {
            3 -> "${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
            4 -> if (isRgbaOrder) {
                "${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}${clean[3]}${clean[3]}"
            } else {
                "${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}${clean[3]}${clean[3]}"
            }
            else -> clean
        }

        return when (hex.length) {
            6 -> {
                val r = hex.substring(0, 2).toInt(16) / 255.0
                val g = hex.substring(2, 4).toInt(16) / 255.0
                val b = hex.substring(4, 6).toInt(16) / 255.0
                RgbaColor(r, g, b, 1.0)
            }
            8 -> {
                if (isRgbaOrder) {
                    val r = hex.substring(0, 2).toInt(16) / 255.0
                    val g = hex.substring(2, 4).toInt(16) / 255.0
                    val b = hex.substring(4, 6).toInt(16) / 255.0
                    val a = hex.substring(6, 8).toInt(16) / 255.0
                    RgbaColor(r, g, b, a)
                } else {
                    val a = hex.substring(0, 2).toInt(16) / 255.0
                    val r = hex.substring(2, 4).toInt(16) / 255.0
                    val g = hex.substring(4, 6).toInt(16) / 255.0
                    val b = hex.substring(6, 8).toInt(16) / 255.0
                    RgbaColor(r, g, b, a)
                }
            }
            else -> throw IllegalArgumentException("Invalid HEX string '#$input'. Expected 3, 6, or 8 hex characters.")
        }
    }

    private fun srgbToLinear(c: Double): Double = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun linearToSrgb(c: Double): Double = if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055

    private fun relativeLuminance(r: Double, g: Double, b: Double): Double {
        val rLin = srgbToLinear(r)
        val gLin = srgbToLinear(g)
        val bLin = srgbToLinear(b)
        return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
    }

    private fun contrastRatio(lum1: Double, lum2: Double): Double {
        val l1 = maxOf(lum1, lum2)
        val l2 = minOf(lum1, lum2)
        return (l1 + 0.05) / (l2 + 0.05)
    }

    override suspend fun execute(input: ColorBlendInput): ToolResult<ColorBlendOutput> {
        val startTime = System.currentTimeMillis()
        val isRgbaOrder = input.hexFormat.trim().uppercase(Locale.US) == "RGBA"
        val isLinear = input.colorSpace.trim().uppercase(Locale.US) == "LINEAR"

        val fg: RgbaColor
        val bg: RgbaColor
        try {
            fg = parseHex(input.foregroundHex, isRgbaOrder)
            bg = parseHex(input.backgroundHex, isRgbaOrder)
        } catch (e: Exception) {
            return ToolResult.Failure("Color parsing error: ${e.message}")
        }

        // Porter-Duff "Source Over":
        // a_out = a_src + a_dst * (1 - a_src)
        // c_out = (c_src * a_src + c_dst * a_dst * (1 - a_src)) / a_out
        val outAlpha = fg.a + bg.a * (1.0 - fg.a)

        fun blendChannel(srcC: Double, dstC: Double): Double {
            if (outAlpha == 0.0) return 0.0
            val sC = if (isLinear) srgbToLinear(srcC) else srcC
            val dC = if (isLinear) srgbToLinear(dstC) else dstC
            val blendedLin = (sC * fg.a + dC * bg.a * (1.0 - fg.a)) / outAlpha
            return if (isLinear) linearToSrgb(blendedLin) else blendedLin
        }

        val outR = blendChannel(fg.r, bg.r).coerceIn(0.0, 1.0)
        val outG = blendChannel(fg.g, bg.g).coerceIn(0.0, 1.0)
        val outB = blendChannel(fg.b, bg.b).coerceIn(0.0, 1.0)

        val rInt = (outR * 255.0).roundToInt()
        val gInt = (outG * 255.0).roundToInt()
        val bInt = (outB * 255.0).roundToInt()
        val aInt = (outAlpha * 255.0).roundToInt()

        val hexArgb = "#%02X%02X%02X%02X".format(Locale.US, aInt, rInt, gInt, bInt)
        val hexRgba = "#%02X%02X%02X%02X".format(Locale.US, rInt, gInt, bInt, aInt)
        val cssRgba = "rgba($rInt, $gInt, $bInt, ${String.format(Locale.US, "%.2f", outAlpha)})"

        val lum = relativeLuminance(outR, outG, outB)
        val crWhite = contrastRatio(lum, 1.0)
        val crBlack = contrastRatio(lum, 0.0)

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== PORTER-DUFF ALPHA COMPOSITING ===")
            appendLine("Foreground: ${input.foregroundHex} (Opacity: ${(fg.a * 100).roundToInt()}%)")
            appendLine("Background: ${input.backgroundHex} (Opacity: ${(bg.a * 100).roundToInt()}%)")
            appendLine("Color Space: ${if (isLinear) "Linear RGB (Gamma Corrected)" else "Standard sRGB"}")
            appendLine("----------------------------------------")
            appendLine("Blended HEX (ARGB): $hexArgb")
            appendLine("Blended HEX (RGBA): $hexRgba")
            appendLine("CSS Notation:       $cssRgba")
            appendLine("RGB Decimals:       rgb($rInt, $gInt, $bInt)")
            appendLine("Alpha Channel:      ${String.format(Locale.US, "%.1f", outAlpha * 100.0)}%")
            appendLine("----------------------------------------")
            appendLine("WCAG Contrast on Pure White (#FFF): ${String.format(Locale.US, "%.2f", crWhite)}:1 ${if (crWhite >= 4.5) "(PASS AA)" else "(FAIL AA)"}")
            appendLine("WCAG Contrast on Pure Black (#000): ${String.format(Locale.US, "%.2f", crBlack)}:1 ${if (crBlack >= 4.5) "(PASS AA)" else "(FAIL AA)"}")
        }

        return ToolResult.Success(
            data = ColorBlendOutput(
                blendedHexArgb = hexArgb,
                blendedHexRgba = hexRgba,
                cssRgba = cssRgba,
                red = rInt,
                green = gInt,
                blue = bInt,
                alphaPercent = outAlpha * 100.0,
                contrastRatioOnWhite = crWhite,
                contrastRatioOnBlack = crBlack,
                formattedReport = report,
                summary = "Blended to $hexArgb ($cssRgba)."
            ),
            executionTimeMs = elapsed,
            summary = "Blended: $hexArgb ($cssRgba)"
        )
    }
}
