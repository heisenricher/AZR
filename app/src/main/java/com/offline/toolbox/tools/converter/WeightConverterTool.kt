package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class WeightUnit(val symbol: String, val displayName: String, val gramsFactor: Double) {
    MILLIGRAM("mg", "Milligrams", 0.001),
    GRAM("g", "Grams", 1.0),
    KILOGRAM("kg", "Kilograms", 1000.0),
    METRIC_TON("t", "Metric Tons", 1_000_000.0),
    OUNCE("oz", "Ounces", 28.349523125),
    POUND("lb", "Pounds", 453.59237),
    STONE("st", "Stones", 6350.29318),
    CARAT("ct", "Carats", 0.2)
}

data class WeightConverterInput(
    val value: Double,
    val fromUnit: WeightUnit = WeightUnit.KILOGRAM,
    val toUnit: WeightUnit = WeightUnit.POUND
)

data class WeightConverterOutput(
    val resultValue: Double,
    val resultFormatted: String,
    val allConversions: Map<WeightUnit, Double>,
    val summary: String
)

class WeightConverterTool : Tool<WeightConverterInput, WeightConverterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "weight_converter",
        name = "Weight & Mass Converter",
        description = "Convert between kilograms, pounds, ounces, grams, stones, and metric tons.",
        category = ToolCategory.CONVERTER,
        tags = listOf("weight", "mass", "kg", "lbs", "pounds", "grams", "ounces", "stones", "units"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Scale"
    )

    override suspend fun execute(input: WeightConverterInput): ToolResult<WeightConverterOutput> {
        val startTime = System.currentTimeMillis()
        val grams = input.value * input.fromUnit.gramsFactor
        val converted = grams / input.toUnit.gramsFactor

        val allMap = mutableMapOf<WeightUnit, Double>()
        for (unit in WeightUnit.values()) {
            allMap[unit] = grams / unit.gramsFactor
        }

        val formattedResult = formatNumber(converted)
        val summary = "${formatNumber(input.value)} ${input.fromUnit.symbol} = $formattedResult ${input.toUnit.symbol}"

        return ToolResult.Success(
            data = WeightConverterOutput(
                resultValue = converted,
                resultFormatted = "$formattedResult ${input.toUnit.symbol}",
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
            String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
        }
    }
}
