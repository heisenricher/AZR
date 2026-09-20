package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class TemperatureUnit(val symbol: String, val displayName: String) {
    CELSIUS("°C", "Celsius"),
    FAHRENHEIT("°F", "Fahrenheit"),
    KELVIN("K", "Kelvin"),
    RANKINE("°R", "Rankine")
}

data class TemperatureConverterInput(
    val value: Double,
    val fromUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val toUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT
)

data class TemperatureConverterOutput(
    val resultValue: Double,
    val resultFormatted: String,
    val celsius: Double,
    val fahrenheit: Double,
    val kelvin: Double,
    val rankine: Double,
    val summary: String
)

class TemperatureConverterTool : Tool<TemperatureConverterInput, TemperatureConverterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "temperature_converter",
        name = "Temperature Converter",
        description = "Convert temperatures across Celsius, Fahrenheit, Kelvin, and Rankine scales.",
        category = ToolCategory.CONVERTER,
        tags = listOf("temperature", "celsius", "fahrenheit", "kelvin", "rankine", "weather", "heat", "units"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Thermostat"
    )

    override suspend fun execute(input: TemperatureConverterInput): ToolResult<TemperatureConverterOutput> {
        val startTime = System.currentTimeMillis()

        // 1. Convert source to Celsius
        val c = when (input.fromUnit) {
            TemperatureUnit.CELSIUS -> input.value
            TemperatureUnit.FAHRENHEIT -> (input.value - 32.0) * (5.0 / 9.0)
            TemperatureUnit.KELVIN -> input.value - 273.15
            TemperatureUnit.RANKINE -> (input.value - 491.67) * (5.0 / 9.0)
        }

        // 2. Derive all target values from Celsius
        val f = c * (9.0 / 5.0) + 32.0
        val k = c + 273.15
        val r = (c + 273.15) * (9.0 / 5.0)

        val targetValue = when (input.toUnit) {
            TemperatureUnit.CELSIUS -> c
            TemperatureUnit.FAHRENHEIT -> f
            TemperatureUnit.KELVIN -> k
            TemperatureUnit.RANKINE -> r
        }

        val formattedResult = "${formatNumber(targetValue)} ${input.toUnit.symbol}"
        val summary = "${formatNumber(input.value)} ${input.fromUnit.symbol} = $formattedResult"

        return ToolResult.Success(
            data = TemperatureConverterOutput(
                resultValue = targetValue,
                resultFormatted = formattedResult,
                celsius = round2(c),
                fahrenheit = round2(f),
                kelvin = round2(k),
                rankine = round2(r),
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0

    private fun formatNumber(v: Double): String {
        return if (v == v.toLong().toDouble()) {
            v.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
        }
    }
}
