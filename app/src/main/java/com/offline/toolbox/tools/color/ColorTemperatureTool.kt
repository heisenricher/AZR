package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

data class ColorTempInput(
    val temperatureKelvin: Int = 6500 // D65 Daylight standard
)

data class ColorTempOutput(
    val kelvin: Int,
    val hexColor: String,
    val rgbString: String,
    val red: Int,
    val green: Int,
    val blue: Int,
    val lightingClassification: String,
    val formattedReport: String,
    val summary: String
)

class ColorTemperatureTool : Tool<ColorTempInput, ColorTempOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "color_temperature_tool",
        name = "Color Temperature (Kelvin) to RGB Converter",
        description = "Convert Correlated Color Temperature (1,000K to 40,000K) to sRGB chromaticity using Planckian blackbody locus approximations.",
        category = ToolCategory.COLOR,
        tags = listOf("kelvin", "color temperature", "cct", "cct to rgb", "blackbody", "lighting", "d65", "white balance", "photography"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.COLOR,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "WbSunny"
    )

    override suspend fun execute(input: ColorTempInput): ToolResult<ColorTempOutput> {
        val startTime = System.currentTimeMillis()
        val k = input.temperatureKelvin

        if (k < 1000 || k > 40000) {
            return ToolResult.Failure("Color temperature must be between 1,000K and 40,000K (got $k K).")
        }

        val temp = k / 100.0
        val r: Double
        val g: Double
        val b: Double

        // Red channel
        r = if (temp <= 66) {
            255.0
        } else {
            329.698727446 * (temp - 60.0).pow(-0.1332047592)
        }

        // Green channel
        g = if (temp <= 66) {
            99.4708025861 * ln(temp) - 161.1195681661
        } else {
            288.1221695283 * (temp - 60.0).pow(-0.0755148492)
        }

        // Blue channel
        b = when {
            temp >= 66 -> 255.0
            temp <= 19 -> 0.0
            else -> 138.5177312231 * ln(temp - 10.0) - 305.0447927307
        }

        val rInt = r.coerceIn(0.0, 255.0).roundToInt()
        val gInt = g.coerceIn(0.0, 255.0).roundToInt()
        val bInt = b.coerceIn(0.0, 255.0).roundToInt()

        val hex = String.format("#%02X%02X%02X", rInt, gInt, bInt)
        val rgbStr = "rgb($rInt, $gInt, $bInt)"

        val classification = when {
            k < 2000 -> "Candlelight / Fireplace (Deep Warm Amber)"
            k in 2000..2999 -> "Warm White (Incandescent bulb, cozy residential)"
            k in 3000..3999 -> "Soft White / Neutral (Halogen, hospitality)"
            k in 4000..4999 -> "Cool White (Fluorescent office lighting)"
            k in 5000..6499 -> "Direct Sunlight (Crisp commercial daylight)"
            k in 6500..7500 -> "Standard Daylight D65 (Overcast sky, sRGB calibration)"
            k in 7501..11999 -> "Cloudy Sky / Light Shade"
            else -> "Clear Blue Sky (Extreme High Kelvin Cool Light)"
        }

        val report = buildString {
            appendLine("CORRELATED COLOR TEMPERATURE (CCT) AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Temperature:         $k Kelvin (K)")
            appendLine("sRGB Equivalent:     $hex ($rgbStr)")
            appendLine("Atmosphere/Source:   $classification")
            appendLine()
            appendLine("CHANNEL DECOMPOSITION:")
            appendLine("• Red:   $rInt / 255 (${String.format("%.1f", (rInt / 255.0) * 100)}%)")
            appendLine("• Green: $gInt / 255 (${String.format("%.1f", (gInt / 255.0) * 100)}%)")
            appendLine("• Blue:  $bInt / 255 (${String.format("%.1f", (bInt / 255.0) * 100)}%)")
        }

        val summary = "$k K → $hex ($classification)"

        return ToolResult.Success(
            data = ColorTempOutput(
                kelvin = k,
                hexColor = hex,
                rgbString = rgbStr,
                red = rInt,
                green = gInt,
                blue = bInt,
                lightingClassification = classification,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
