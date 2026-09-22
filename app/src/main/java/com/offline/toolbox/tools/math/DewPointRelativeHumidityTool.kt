package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln

data class DewPointInput(
    val temperature: Double = 25.0,
    val unit: String = "CELSIUS", // CELSIUS or FAHRENHEIT
    val relativeHumidityPercent: Double = 60.0
)

data class DewPointOutput(
    val inputTempC: Double,
    val inputTempF: Double,
    val relativeHumidity: Double,
    val dewPointC: Double,
    val dewPointF: Double,
    val saturationVaporPressureHpa: Double,
    val actualVaporPressureHpa: Double,
    val absoluteHumidityGm3: Double,
    val comfortLevel: String,
    val formattedReport: String,
    val summary: String
)

class DewPointRelativeHumidityTool : Tool<DewPointInput, DewPointOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "dew_point_relative_humidity_tool",
        name = "Dew Point & Vapor Pressure Calculator",
        description = "Calculate meteorological dew point temperature, saturation vapor pressure, and absolute humidity using the Magnus-Tetens approximation with human comfort ratings.",
        category = ToolCategory.MATH,
        tags = listOf("dew point", "weather", "humidity", "vapor pressure", "meteorology", "magnus tetens", "comfort", "atmosphere"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "CloudRain"
    )

    private fun fToC(f: Double): Double = (f - 32.0) * 5.0 / 9.0
    private fun cToF(c: Double): Double = (c * 9.0 / 5.0) + 32.0

    override suspend fun execute(input: DewPointInput): ToolResult<DewPointOutput> {
        val startTime = System.currentTimeMillis()
        val isMetric = !input.unit.trim().uppercase(Locale.US).startsWith("F")
        val tC = if (isMetric) input.temperature else fToC(input.temperature)
        val tF = if (isMetric) cToF(input.temperature) else input.temperature
        val rh = input.relativeHumidityPercent.coerceIn(1.0, 100.0)

        // Magnus-Tetens coefficients:
        val a = 6.112 // hPa
        val b = 17.67
        val c = 243.5 // deg C

        val gamma = (b * tC) / (c + tC) + ln(rh / 100.0)
        val dpC = (c * gamma) / (b - gamma)
        val dpF = cToF(dpC)

        // Saturation vapor pressure es in hPa
        val es = a * exp((b * tC) / (c + tC))
        // Actual vapor pressure e in hPa
        val e = es * (rh / 100.0)
        // Absolute humidity AH in g/m^3 = 216.7 * e / (T + 273.15)
        val ah = (216.7 * e) / (tC + 273.15)

        val comfort = when {
            dpC < 10.0 -> "A bit dry / Very comfortable"
            dpC in 10.0..15.9 -> "Comfortable / Pleasant"
            dpC in 16.0..19.9 -> "Slightly humid / Noticeable moisture"
            dpC in 20.0..23.9 -> "Uncomfortable / Muggy"
            dpC in 24.0..26.0 -> "Very humid / Oppressive"
            else -> "Severely oppressive / Extreme heat stress risk"
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== DEW POINT & VAPOR PRESSURE ANALYSIS ===")
            appendLine("Air Temperature:     ${String.format(Locale.US, "%.1f", tC)}°C (${String.format(Locale.US, "%.1f", tF)}°F)")
            appendLine("Relative Humidity:   ${String.format(Locale.US, "%.1f", rh)}%")
            appendLine("----------------------------------------")
            appendLine("DEW POINT:           ${String.format(Locale.US, "%.1f", dpC)}°C (${String.format(Locale.US, "%.1f", dpF)}°F)")
            appendLine("Comfort Level:       $comfort")
            appendLine("----------------------------------------")
            appendLine("ATMOSPHERIC VAPOR METRICS:")
            appendLine("  Saturation Vapor:  ${String.format(Locale.US, "%.2f", es)} hPa (mbar)")
            appendLine("  Actual Vapor:      ${String.format(Locale.US, "%.2f", e)} hPa (mbar)")
            appendLine("  Absolute Humidity: ${String.format(Locale.US, "%.2f", ah)} g/m³")
        }

        return ToolResult.Success(
            data = DewPointOutput(
                inputTempC = tC,
                inputTempF = tF,
                relativeHumidity = rh,
                dewPointC = dpC,
                dewPointF = dpF,
                saturationVaporPressureHpa = es,
                actualVaporPressureHpa = e,
                absoluteHumidityGm3 = ah,
                comfortLevel = comfort,
                formattedReport = report,
                summary = "Dew Point: ${String.format(Locale.US, "%.1f", dpC)}°C (${String.format(Locale.US, "%.1f", dpF)}°F) [$comfort]."
            ),
            executionTimeMs = elapsed,
            summary = "Dew Point: ${String.format(Locale.US, "%.1f", dpC)}°C / ${String.format(Locale.US, "%.1f", dpF)}°F"
        )
    }
}
