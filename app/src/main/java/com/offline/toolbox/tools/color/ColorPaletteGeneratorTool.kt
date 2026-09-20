package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.roundToInt

enum class PaletteHarmony(val label: String) {
    COMPLEMENTARY("Complementary (Opposite 180°)"),
    ANALOGOUS("Analogous (Adjacent ±30°)"),
    TRIADIC("Triadic (Even 120° Triangle)"),
    TETRADIC("Tetradic (Square 90°)"),
    SPLIT_COMPLEMENTARY("Split-Complementary (150° & 210°)"),
    MONOCHROMATIC("Monochromatic (Shades & Tints)")
}

data class PaletteColor(
    val hex: String,
    val rgb: Triple<Int, Int, Int>,
    val hsl: Triple<Float, Float, Float>,
    val isLight: Boolean
)

data class ColorPaletteInput(
    val baseHex: String = "#3F51B5",
    val harmony: PaletteHarmony = PaletteHarmony.COMPLEMENTARY
)

data class ColorPaletteOutput(
    val baseColor: PaletteColor,
    val palette: List<PaletteColor>,
    val harmony: PaletteHarmony,
    val formattedReport: String,
    val summary: String
)

class ColorPaletteGeneratorTool : Tool<ColorPaletteInput, ColorPaletteOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "color_palette_generator_tool",
        name = "Harmonious Color Palette Generator",
        description = "Generate color palettes (Complementary, Triadic, Analogous, Monochromatic) in HSL space.",
        category = ToolCategory.COLOR,
        tags = listOf("color", "palette", "harmony", "design", "hsl", "complementary", "triadic", "scheme"),
        inputType = ToolDataType.COLOR,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Palette"
    )

    override suspend fun execute(input: ColorPaletteInput): ToolResult<ColorPaletteOutput> {
        val startTime = System.currentTimeMillis()

        val cleanHex = input.baseHex.trim().removePrefix("#")
        if (cleanHex.length != 6 && cleanHex.length != 3) {
            return ToolResult.Failure(
                message = "Invalid HEX color format: '${input.baseHex}'",
                userGuidance = "Provide a 6-digit hex color code (e.g. #3F51B5 or #FF5722)."
            )
        }

        val fullHex = if (cleanHex.length == 3) {
            cleanHex.map { "$it$it" }.joinToString("")
        } else cleanHex

        val r = fullHex.substring(0, 2).toIntOrNull(16) ?: return ToolResult.Failure("Invalid red channel.")
        val g = fullHex.substring(2, 4).toIntOrNull(16) ?: return ToolResult.Failure("Invalid green channel.")
        val b = fullHex.substring(4, 6).toIntOrNull(16) ?: return ToolResult.Failure("Invalid blue channel.")

        val (h, s, l) = rgbToHsl(r, g, b)
        val basePaletteColor = createPaletteColor(h, s, l)

        val colors = mutableListOf<PaletteColor>()
        colors.add(basePaletteColor)

        when (input.harmony) {
            PaletteHarmony.COMPLEMENTARY -> {
                colors.add(createPaletteColor((h + 180f) % 360f, s, l))
                colors.add(createPaletteColor(h, (s * 0.8f).coerceIn(0f, 1f), (l * 1.25f).coerceIn(0f, 1f)))
                colors.add(createPaletteColor((h + 180f) % 360f, (s * 0.8f).coerceIn(0f, 1f), (l * 0.75f).coerceIn(0f, 1f)))
            }
            PaletteHarmony.ANALOGOUS -> {
                colors.add(createPaletteColor((h + 30f) % 360f, s, l))
                colors.add(createPaletteColor((h + 60f) % 360f, s, l))
                colors.add(createPaletteColor((h - 30f + 360f) % 360f, s, l))
                colors.add(createPaletteColor((h - 60f + 360f) % 360f, s, l))
            }
            PaletteHarmony.TRIADIC -> {
                colors.add(createPaletteColor((h + 120f) % 360f, s, l))
                colors.add(createPaletteColor((h + 240f) % 360f, s, l))
            }
            PaletteHarmony.TETRADIC -> {
                colors.add(createPaletteColor((h + 90f) % 360f, s, l))
                colors.add(createPaletteColor((h + 180f) % 360f, s, l))
                colors.add(createPaletteColor((h + 270f) % 360f, s, l))
            }
            PaletteHarmony.SPLIT_COMPLEMENTARY -> {
                colors.add(createPaletteColor((h + 150f) % 360f, s, l))
                colors.add(createPaletteColor((h + 210f) % 360f, s, l))
            }
            PaletteHarmony.MONOCHROMATIC -> {
                colors.add(createPaletteColor(h, s, (l * 0.4f).coerceIn(0f, 1f)))
                colors.add(createPaletteColor(h, (s * 0.7f).coerceIn(0f, 1f), (l * 0.7f).coerceIn(0f, 1f)))
                colors.add(createPaletteColor(h, (s * 0.5f).coerceIn(0f, 1f), (l * 1.3f).coerceIn(0f, 1f)))
                colors.add(createPaletteColor(h, (s * 0.3f).coerceIn(0f, 1f), (l * 1.5f).coerceIn(0f, 1f)))
            }
        }

        val report = buildString {
            appendLine("COLOR PALETTE HARMONY REPORT")
            appendLine("Base Color:    #${fullHex.uppercase(Locale.US)}")
            appendLine("Harmony Type:  ${input.harmony.label}")
            appendLine("--------------------------------")
            colors.forEachIndexed { idx, col ->
                val role = if (idx == 0) "Base" else "Accent $idx"
                appendLine("%-10s %s | RGB(%3d,%3d,%3d) | HSL(%3.0f°,%2.0f%%,%2.0f%%)".format(
                    Locale.US,
                    role,
                    col.hex,
                    col.rgb.first, col.rgb.second, col.rgb.third,
                    col.hsl.first, col.hsl.second * 100f, col.hsl.third * 100f
                ))
            }
        }

        val summary = "${input.harmony.name}: ${colors.size} colors generated from #${fullHex.uppercase(Locale.US)}"

        return ToolResult.Success(
            data = ColorPaletteOutput(
                baseColor = basePaletteColor,
                palette = colors,
                harmony = input.harmony,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun createPaletteColor(h: Float, s: Float, l: Float): PaletteColor {
        val (r, g, b) = hslToRgb(h, s, l)
        val hex = "#%02X%02X%02X".format(Locale.US, r, g, b)
        val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return PaletteColor(
            hex = hex,
            rgb = Triple(r, g, b),
            hsl = Triple(h, s, l),
            isLight = lum > 0.5
        )
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        val l = (max + min) / 2f
        val s = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * l - 1f))
        var h = when {
            delta == 0f -> 0f
            max == rf -> ((gf - bf) / delta) % 6f
            max == gf -> ((bf - rf) / delta) + 2f
            else -> ((rf - gf) / delta) + 4f
        } * 60f
        if (h < 0f) h += 360f

        return Triple(h, s, l)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Int, Int, Int> {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (rPrime, gPrime, bPrime) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((rPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bPrime + m) * 255f).roundToInt().coerceIn(0, 255)

        return Triple(r, g, b)
    }
}
