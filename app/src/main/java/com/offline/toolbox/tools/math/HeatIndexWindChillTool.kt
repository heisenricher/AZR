package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

data class WeatherIndexInput(
    val calculationMode: String = "AUTO", // AUTO, HEAT_INDEX, WIND_CHILL
    val temperature: Double = 92.0,
    val unit: String = "FAHRENHEIT", // FAHRENHEIT or CELSIUS
    val relativeHumidityPercent: Double = 65.0,
    val windSpeedMph: Double = 15.0
)

data class WeatherIndexOutput(
    val calculationMode: String,
    val inputTempF: Double,
    val inputTempC: Double,
    val relativeHumidity: Double,
    val windSpeedMph: Double,
    val windSpeedKmh: Double,
    val apparentTempF: Double,
    val apparentTempC: Double,
    val dangerCategory: String,
    val advisoryNotice: String,
    val formattedReport: String,
    val summary: String
)

class HeatIndexWindChillTool : Tool<WeatherIndexInput, WeatherIndexOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "heat_index_wind_chill_tool",
        name = "Heat Index & Wind Chill Apparent Temp Calculator",
        description = "Calculate apparent 'feels-like' temperature using the NOAA Heat Index (Rothfusz equation) and NWS Wind Chill Index with clinical risk categories.",
        category = ToolCategory.MATH,
        tags = listOf("heat index", "wind chill", "weather", "apparent temperature", "feels like", "meteorology", "humidity", "rothefusz", "noaa", "nws"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Sun"
    )

    private fun fToC(f: Double): Double = (f - 32.0) * 5.0 / 9.0
    private fun cToF(c: Double): Double = (c * 9.0 / 5.0) + 32.0

    private fun computeHeatIndex(tF: Double, rh: Double): Pair<Double, Pair<String, String>> {
        val simpleHi = 0.5 * (tF + 61.0 + ((tF - 68.0) * 1.2) + (rh * 0.094))
        var hi = if ((simpleHi + tF) / 2.0 < 80.0) {
            simpleHi
        } else {
            var fullHi = -42.379 +
                2.04901523 * tF +
                10.14333127 * rh -
                0.22475541 * tF * rh -
                0.00683783 * tF * tF -
                0.05481717 * rh * rh +
                0.00122874 * tF * tF * rh +
                0.00085282 * tF * rh * rh -
                0.00000199 * tF * tF * rh * rh

            if (rh < 13.0 && tF in 80.0..112.0) {
                val adj = -((13.0 - rh) / 4.0) * sqrt((17.0 - kotlin.math.abs(tF - 95.0)) / 17.0)
                fullHi += adj
            } else if (rh > 85.0 && tF in 80.0..87.0) {
                val adj = ((rh - 85.0) / 10.0) * ((87.0 - tF) / 5.0)
                fullHi += adj
            }
            fullHi
        }

        val (cat, advice) = when {
            hi < 80.0 -> "Normal" to "Comfortable or mild conditions. Minimal heat-related fatigue risk."
            hi in 80.0..89.9 -> "Caution" to "Fatigue is possible with prolonged exposure and physical activity."
            hi in 90.0..102.9 -> "Extreme Caution" to "Heat stroke, heat cramps, and heat exhaustion are possible with prolonged exposure."
            hi in 103.0..124.9 -> "Danger" to "Heat cramps or heat exhaustion likely; heat stroke is possible with continued activity."
            else -> "Extreme Danger" to "Heat stroke or sunstroke is highly likely with prolonged outdoor exposure."
        }
        return hi to (cat to advice)
    }

    private fun computeWindChill(tF: Double, vMph: Double): Pair<Double, Pair<String, String>> {
        val wc = if (vMph < 3.0 || tF > 50.0) {
            tF
        } else {
            35.74 + 0.6215 * tF - 35.75 * vMph.pow(0.16) + 0.4275 * tF * vMph.pow(0.16)
        }

        val (cat, advice) = when {
            wc > 32.0 -> "Mild Cold" to "Minimal risk of hypothermia or frostbite with standard clothing."
            wc in 16.0..32.0 -> "Caution" to "Cold temperature; dress in layers and wear warm headwear."
            wc in -15.0..15.9 -> "Extreme Caution" to "Frostbite possible in 30 minutes of continuous exposed skin."
            wc in -45.0..-15.1 -> "Danger" to "Frostbite possible in 10 minutes of exposed skin. High hypothermia risk."
            else -> "Extreme Danger" to "Frostbite occurs in under 5 minutes. Outdoor exposure should be strictly avoided."
        }
        return wc to (cat to advice)
    }

    override suspend fun execute(input: WeatherIndexInput): ToolResult<WeatherIndexOutput> {
        val startTime = System.currentTimeMillis()
        val isMetric = input.unit.trim().uppercase(Locale.US).startsWith("C")

        val tempF = if (isMetric) cToF(input.temperature) else input.temperature
        val tempC = if (isMetric) input.temperature else fToC(input.temperature)

        val rh = input.relativeHumidityPercent.coerceIn(0.0, 100.0)
        val windMph = input.windSpeedMph.coerceAtLeast(0.0)
        val windKmh = windMph * 1.609344

        val mode = input.calculationMode.trim().uppercase(Locale.US)
        val isHeatIndex = when (mode) {
            "HEAT_INDEX" -> true
            "WIND_CHILL" -> false
            else -> tempF >= 65.0
        }

        val apparentF: Double
        val apparentC: Double
        val category: String
        val advice: String
        val activeMode: String

        if (isHeatIndex) {
            activeMode = "HEAT_INDEX"
            val (hi, danger) = computeHeatIndex(tempF, rh)
            apparentF = hi
            apparentC = fToC(hi)
            category = danger.first
            advice = danger.second
        } else {
            activeMode = "WIND_CHILL"
            val (wc, danger) = computeWindChill(tempF, windMph)
            apparentF = wc
            apparentC = fToC(wc)
            category = danger.first
            advice = danger.second
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== APPARENT TEMPERATURE EVALUATION ===")
            appendLine("Mode: ${if (activeMode == "HEAT_INDEX") "NOAA Heat Index" else "NWS Wind Chill Index"}")
            appendLine("Observed Temperature: ${String.format(Locale.US, "%.1f", tempF)}°F (${String.format(Locale.US, "%.1f", tempC)}°C)")
            if (activeMode == "HEAT_INDEX") {
                appendLine("Relative Humidity:   ${String.format(Locale.US, "%.1f", rh)}%")
            } else {
                appendLine("Wind Speed:          ${String.format(Locale.US, "%.1f", windMph)} mph (${String.format(Locale.US, "%.1f", windKmh)} km/h)")
            }
            appendLine("----------------------------------------")
            appendLine("Apparent 'Feels Like': ${String.format(Locale.US, "%.1f", apparentF)}°F (${String.format(Locale.US, "%.1f", apparentC)}°C)")
            appendLine("Danger Level:        $category")
            appendLine("Advisory Notice:     $advice")
        }

        return ToolResult.Success(
            data = WeatherIndexOutput(
                calculationMode = activeMode,
                inputTempF = tempF,
                inputTempC = tempC,
                relativeHumidity = rh,
                windSpeedMph = windMph,
                windSpeedKmh = windKmh,
                apparentTempF = apparentF,
                apparentTempC = apparentC,
                dangerCategory = category,
                advisoryNotice = advice,
                formattedReport = report,
                summary = "Feels like ${String.format(Locale.US, "%.1f", apparentF)}°F (${String.format(Locale.US, "%.1f", apparentC)}°C) [$category]."
            ),
            executionTimeMs = elapsed,
            summary = "Apparent temp: ${String.format(Locale.US, "%.1f", apparentF)}°F [$category]"
        )
    }
}
