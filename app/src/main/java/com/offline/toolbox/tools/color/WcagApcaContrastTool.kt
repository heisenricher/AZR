package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

data class ApcaContrastInput(
    val textColorHex: String = "#0F172A",      // Slate 900
    val backgroundColorHex: String = "#F8FAFC" // Slate 50
)

data class ApcaContrastOutput(
    val wcag2ContrastRatio: Double,
    val wcag2AaPass: Boolean,
    val wcag2AaaPass: Boolean,
    val apcaLcValue: Double,
    val apcaPolarity: String,
    val apcaRating: String,
    val recommendedMinFontSizePx: String,
    val formattedReport: String,
    val summary: String
)

class WcagApcaContrastTool : Tool<ApcaContrastInput, ApcaContrastOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "wcag_apca_contrast_tool",
        name = "WCAG 3 APCA & Perceptual Contrast Engine",
        description = "Evaluate color contrast using the Advanced Perceptual Contrast Algorithm (APCA / WCAG 3) alongside WCAG 2.1 AA/AAA metrics.",
        category = ToolCategory.COLOR,
        tags = listOf("apca", "wcag", "contrast", "accessibility", "a11y", "color", "lightness", "typography", "design"),
        inputType = ToolDataType.COLOR,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Contrast"
    )

    override suspend fun execute(input: ApcaContrastInput): ToolResult<ApcaContrastOutput> {
        val startTime = System.currentTimeMillis()

        val textRgb = parseHex(input.textColorHex) ?: return ToolResult.Failure("Invalid text color hex: '${input.textColorHex}'")
        val bgRgb = parseHex(input.backgroundColorHex) ?: return ToolResult.Failure("Invalid background color hex: '${input.backgroundColorHex}'")

        // 1. WCAG 2.1 Relative Luminance & Ratio
        val lumText = getRelativeLuminance(textRgb)
        val lumBg = getRelativeLuminance(bgRgb)
        val ratio = if (lumText > lumBg) (lumText + 0.05) / (lumBg + 0.05) else (lumBg + 0.05) / (lumText + 0.05)
        val wcagAa = ratio >= 4.5
        val wcagAaa = ratio >= 7.0

        // 2. APCA 0.98G Lightness Contrast (Lc)
        val yText = apcaLuminance(textRgb)
        val yBg = apcaLuminance(bgRgb)

        // APCA Constants
        val mainTRC = 2.4
        val normBgExp = 0.56
        val normTxtExp = 0.57
        val revBgExp = 0.62
        val revTxtExp = 0.65
        val scaleBoW = 1.1414
        val scaleWoB = 1.1414
        val offsetBoW = 0.027
        val offsetWoB = 0.027

        val lc: Double
        val polarity: String

        if (yBg >= yText) {
            // Dark text on light background (Positive Lc)
            polarity = "Dark Text on Light Background (Standard Polarity)"
            val sapc = (yBg.pow(normBgExp) - yText.pow(normTxtExp)) * scaleBoW
            lc = if (sapc < offsetBoW) 0.0 else (sapc - offsetBoW) * 100.0
        } else {
            // Light text on dark background (Negative Lc)
            polarity = "Light Text on Dark Background (Reverse Polarity)"
            val sapc = (yBg.pow(revBgExp) - yText.pow(revTxtExp)) * scaleWoB
            lc = if (sapc > -offsetWoB) 0.0 else (sapc + offsetWoB) * 100.0
        }

        val absLc = abs(lc)
        val rating: String
        val minFont: String

        when {
            absLc >= 90.0 -> {
                rating = "PREFERRED (Exceptional Readability for All Text)"
                minFont = "12px Regular / Any weight"
            }
            absLc >= 75.0 -> {
                rating = "OPTIMAL (Fluent Body Text & Paragraphs)"
                minFont = "14px Regular / 12px Bold"
            }
            absLc >= 60.0 -> {
                rating = "ADEQUATE (Secondary Content & Larger Body)"
                minFont = "16px Regular / 14px Bold"
            }
            absLc >= 45.0 -> {
                rating = "MINIMUM (Large Headers, Labels, and UI Components)"
                minFont = "24px Regular / 18px Bold"
            }
            absLc >= 30.0 -> {
                rating = "NON-TEXT (Spot Icons, Dividers, Inactive Controls Only)"
                minFont = "Not recommended for readable text"
            }
            else -> {
                rating = "INSUFFICIENT (Fails Accessibility Standards)"
                minFont = "Not accessible"
            }
        }

        val report = buildString {
            appendLine("ACCESSIBILITY CONTRAST AUDIT (WCAG 2.1 & APCA)")
            appendLine("--------------------------------------------------")
            appendLine("Text Color:       ${input.textColorHex.uppercase()} (rgb(${textRgb.first}, ${textRgb.second}, ${textRgb.third}))")
            appendLine("Background Color: ${input.backgroundColorHex.uppercase()} (rgb(${bgRgb.first}, ${bgRgb.second}, ${bgRgb.third}))")
            appendLine("Polarity:         $polarity")
            appendLine()
            appendLine("WCAG 3 APCA METRICS:")
            appendLine("• Lightness Contrast (Lc): ${String.format(Locale.US, "%+.1f", lc)}")
            appendLine("• Usability Grade:         $rating")
            appendLine("• Recommended Min Font:    $minFont")
            appendLine()
            appendLine("WCAG 2.1 CLASSIC METRICS:")
            appendLine("• Contrast Ratio:          ${String.format(Locale.US, "%.2f:1", ratio)}")
            appendLine("• Level AA (4.5:1):        ${if (wcagAa) "PASS" else "FAIL"}")
            appendLine("• Level AAA (7.0:1):       ${if (wcagAaa) "PASS" else "FAIL"}")
        }

        val summary = "APCA Lc ${String.format(Locale.US, "%+.1f", lc)} | WCAG ${String.format(Locale.US, "%.1f:1", ratio)}"

        return ToolResult.Success(
            data = ApcaContrastOutput(
                wcag2ContrastRatio = ratio,
                wcag2AaPass = wcagAa,
                wcag2AaaPass = wcagAaa,
                apcaLcValue = lc,
                apcaPolarity = polarity,
                apcaRating = rating,
                recommendedMinFontSizePx = minFont,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun parseHex(hexStr: String): Triple<Int, Int, Int>? {
        val clean = hexStr.trim().removePrefix("#")
        if (clean.length != 6 && clean.length != 8) return null
        val r = clean.substring(0, 2).toIntOrNull(16) ?: return null
        val g = clean.substring(2, 4).toIntOrNull(16) ?: return null
        val b = clean.substring(4, 6).toIntOrNull(16) ?: return null
        return Triple(r, g, b)
    }

    private fun getRelativeLuminance(rgb: Triple<Int, Int, Int>): Double {
        fun channel(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(rgb.first) + 0.7152 * channel(rgb.second) + 0.0722 * channel(rgb.third)
    }

    private fun apcaLuminance(rgb: Triple<Int, Int, Int>): Double {
        val r = (rgb.first / 255.0).pow(2.4)
        val g = (rgb.second / 255.0).pow(2.4)
        val b = (rgb.third / 255.0).pow(2.4)
        // APCA sRGB spectral weighting
        return 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
    }
}
