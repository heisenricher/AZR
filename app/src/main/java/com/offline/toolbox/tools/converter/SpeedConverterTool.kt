package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class SpeedUnit(val symbol: String, val displayName: String, val metersPerSecFactor: Double) {
    METERS_PER_SECOND("m/s", "Meters per second", 1.0),
    KILOMETERS_PER_HOUR("km/h", "Kilometers per hour", 1.0 / 3.6),
    MILES_PER_HOUR("mph", "Miles per hour", 0.44704),
    KNOT("kn", "Knots", 0.514444444),
    FEET_PER_SECOND("ft/s", "Feet per second", 0.3048)
}

data class SpeedConverterInput(
    val value: Double,
    val fromUnit: SpeedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
    val toUnit: SpeedUnit = SpeedUnit.MILES_PER_HOUR
)

data class SpeedConverterOutput(
    val resultValue: Double,
    val resultFormatted: String,
    val allConversions: Map<SpeedUnit, Double>,
    val summary: String
)

class SpeedConverterTool : Tool<SpeedConverterInput, SpeedConverterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "speed_converter",
        name = "Speed & Velocity Converter",
        description = "Convert speeds across km/h, mph, m/s, knots, and ft/s.",
        category = ToolCategory.CONVERTER,
        tags = listOf("speed", "velocity", "km/h", "mph", "knots", "pace", "units"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Speed"
    )

    override suspend fun execute(input: SpeedConverterInput): ToolResult<SpeedConverterOutput> {
        val startTime = System.currentTimeMillis()
        val mps = input.value * input.fromUnit.metersPerSecFactor
        val converted = mps / input.toUnit.metersPerSecFactor

        val allMap = mutableMapOf<SpeedUnit, Double>()
        for (unit in SpeedUnit.values()) {
            allMap[unit] = mps / unit.metersPerSecFactor
        }

        val formattedResult = "${formatNumber(converted)} ${input.toUnit.symbol}"
        val summary = "${formatNumber(input.value)} ${input.fromUnit.symbol} = $formattedResult"

        return ToolResult.Success(
            data = SpeedConverterOutput(
                resultValue = converted,
                resultFormatted = formattedResult,
                allConversions = allMap,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun formatNumber(v: Double): String {
        return if (v == v.toLong().toDouble()) {
            v.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
        }
    }
}
