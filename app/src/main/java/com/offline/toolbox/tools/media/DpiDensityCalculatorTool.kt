package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class DpiDensityInput(
    val widthPixels: Int = 1080,
    val heightPixels: Int = 2400,
    val diagonalInches: Double = 6.5,
    val sampleDpToConvert: Double = 16.0
)

data class DpiDensityOutput(
    val calculatedPpi: Double,
    val densityBucket: String,
    val densityScaleFactor: Double,
    val convertedPixelsForSampleDp: Double,
    val physicalWidthInches: Double,
    val physicalHeightInches: Double,
    val screenAspectRatio: String,
    val formattedReport: String,
    val summary: String
)

class DpiDensityCalculatorTool : Tool<DpiDensityInput, DpiDensityOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "dpi_density_calculator_tool",
        name = "Screen DPI & Android Density Bucket Scaler",
        description = "Calculate PPI/DPI from screen resolution and diagonal, classify Android density buckets (mdpi to xxxhdpi), and convert dp <-> px.",
        category = ToolCategory.MEDIA,
        tags = listOf("dpi", "ppi", "density", "screen", "display", "bucket", "android", "resolution", "pixel", "dp"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Smartphone"
    )

    override suspend fun execute(input: DpiDensityInput): ToolResult<DpiDensityOutput> {
        val startTime = System.currentTimeMillis()
        val w = input.widthPixels
        val h = input.heightPixels
        val diag = input.diagonalInches

        if (w <= 0 || h <= 0) {
            return ToolResult.Failure("Resolution width and height must be positive integers.")
        }
        if (diag <= 0.0) {
            return ToolResult.Failure("Diagonal screen size must be greater than 0 inches.")
        }

        val diagonalPixels = sqrt((w.toDouble() * w + h.toDouble() * h))
        val ppi = diagonalPixels / diag

        // Android Density Buckets (Baseline mdpi = 160 dpi = 1.0x)
        val (bucketName, scaleFactor) = when {
            ppi < 140.0 -> Pair("ldpi (~120 dpi)", 0.75)
            ppi < 200.0 -> Pair("mdpi (~160 dpi - Baseline)", 1.0)
            ppi < 280.0 -> Pair("hdpi (~240 dpi)", 1.5)
            ppi < 400.0 -> Pair("xhdpi (~320 dpi)", 2.0)
            ppi < 560.0 -> Pair("xxhdpi (~480 dpi)", 3.0)
            else -> Pair("xxxhdpi (~640 dpi)", 4.0)
        }

        val convertedPx = input.sampleDpToConvert * scaleFactor
        val physWidth = (w / ppi)
        val physHeight = (h / ppi)

        val gcdVal = gcd(w, h)
        val ratioStr = "${w / gcdVal}:${h / gcdVal}"

        val report = buildString {
            appendLine("SCREEN DENSITY & DPI ANALYSIS")
            appendLine("--------------------------------------------------")
            appendLine("Resolution:           ${w} × ${h} px ($ratioStr)")
            appendLine("Diagonal Size:        ${diag}\" inches")
            appendLine("Calculated PPI:       ${String.format(Locale.US, "%.1f", ppi)} pixels per inch")
            appendLine("Android Bucket:       $bucketName")
            appendLine("Density Scale Factor: ${scaleFactor}x (1 dp = $scaleFactor px)")
            appendLine("Sample Conversion:    ${input.sampleDpToConvert} dp = ${convertedPx.roundToInt()} px")
            appendLine("Physical Dimensions:  ${String.format(Locale.US, "%.2f", physWidth)}\" × ${String.format(Locale.US, "%.2f", physHeight)}\"")
        }

        val summary = "${String.format(Locale.US, "%.0f", ppi)} PPI ($bucketName)"

        return ToolResult.Success(
            data = DpiDensityOutput(
                calculatedPpi = ppi,
                densityBucket = bucketName,
                densityScaleFactor = scaleFactor,
                convertedPixelsForSampleDp = convertedPx,
                physicalWidthInches = physWidth,
                physicalHeightInches = physHeight,
                screenAspectRatio = ratioStr,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
