package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.roundToInt

data class AspectRatioInput(
    val originalWidth: Int = 1920,
    val originalHeight: Int = 1080,
    val targetWidth: Int? = null,
    val targetHeight: Int? = null
)

data class AspectRatioOutput(
    val ratioSimplified: String,
    val decimalRatio: Double,
    val standardMatch: String,
    val scaledDimensions: Pair<Int, Int>?,
    val formattedReport: String,
    val summary: String
)

class AspectRatioCalculatorTool : Tool<AspectRatioInput, AspectRatioOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "aspect_ratio_calculator_tool",
        name = "Aspect Ratio & Resolution Scaler",
        description = "Calculate aspect ratios, detect cinema/display standards, and scale dimensions proportionally.",
        category = ToolCategory.MEDIA,
        tags = listOf("aspect ratio", "resolution", "scale", "dimensions", "video", "photo", "screen", "16:9", "4:3"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "AspectRatio"
    )

    override suspend fun execute(input: AspectRatioInput): ToolResult<AspectRatioOutput> {
        val startTime = System.currentTimeMillis()

        val w = input.originalWidth
        val h = input.originalHeight

        if (w <= 0 || h <= 0) {
            return ToolResult.Failure("Width and height must both be positive integers.")
        }

        val gcdVal = gcd(w, h)
        val simpW = w / gcdVal
        val simpH = h / gcdVal
        val ratioStr = "$simpW:$simpH"
        val decimalRatio = w.toDouble() / h.toDouble()

        val standardMatch = COMMON_RATIOS.minByOrNull { kotlin.math.abs(it.ratio - decimalRatio) }?.let {
            if (kotlin.math.abs(it.ratio - decimalRatio) < 0.05) it.name else "Custom Ratio (${"%.2f".format(Locale.US, decimalRatio)}:1)"
        } ?: "Custom Ratio"

        var scaledPair: Pair<Int, Int>? = null
        if (input.targetWidth != null && input.targetWidth > 0) {
            val calcH = (input.targetWidth / decimalRatio).roundToInt()
            scaledPair = Pair(input.targetWidth, calcH)
        } else if (input.targetHeight != null && input.targetHeight > 0) {
            val calcW = (input.targetHeight * decimalRatio).roundToInt()
            scaledPair = Pair(calcW, input.targetHeight)
        }

        val report = buildString {
            appendLine("ASPECT RATIO & RESOLUTION SPECIFICATION")
            appendLine("--------------------------------")
            appendLine("Original Resolution:   ${w}x${h} px")
            appendLine("Simplified Ratio:      $ratioStr")
            appendLine("Decimal Ratio:         ${"%.4f".format(Locale.US, decimalRatio)}:1")
            appendLine("Standard Match:        $standardMatch")
            if (scaledPair != null) {
                appendLine("--------------------------------")
                appendLine("Proportional Scaling:  ${scaledPair.first}x${scaledPair.second} px")
            }
        }

        val summary = "$ratioStr ($standardMatch)"

        return ToolResult.Success(
            data = AspectRatioOutput(
                ratioSimplified = ratioStr,
                decimalRatio = decimalRatio,
                standardMatch = standardMatch,
                scaledDimensions = scaledPair,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun gcd(a: Int, b: Int): Int {
        var n1 = a
        var n2 = b
        while (n2 != 0) {
            val temp = n2
            n2 = n1 % n2
            n1 = temp
        }
        return n1
    }

    private data class RatioSpec(val ratio: Double, val name: String)

    companion object {
        private val COMMON_RATIOS = listOf(
            RatioSpec(16.0 / 9.0, "16:9 (Widescreen Full HD / 4K)"),
            RatioSpec(9.0 / 16.0, "9:16 (Vertical Video / Stories / Reels)"),
            RatioSpec(4.0 / 3.0, "4:3 (Standard Definition / Tablet)"),
            RatioSpec(3.0 / 4.0, "3:4 (Portrait Tablet)"),
            RatioSpec(1.0, "1:1 (Square / Profile)"),
            RatioSpec(3.0 / 2.0, "3:2 (35mm Photo / Surface)"),
            RatioSpec(2.0 / 3.0, "2:3 (Portrait Photo)"),
            RatioSpec(21.0 / 9.0, "21:9 (Ultrawide Cinema)"),
            RatioSpec(19.5 / 9.0, "19.5:9 (Modern Smartphone Display)")
        )
    }
}
