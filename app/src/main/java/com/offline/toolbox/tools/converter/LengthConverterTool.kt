package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class LengthUnit(val symbol: String, val displayName: String, val metersFactor: Double) {
    MILLIMETER("mm", "Millimeters", 0.001),
    CENTIMETER("cm", "Centimeters", 0.01),
    METER("m", "Meters", 1.0),
    KILOMETER("km", "Kilometers", 1000.0),
    INCH("in", "Inches", 0.0254),
    FOOT("ft", "Feet", 0.3048),
    YARD("yd", "Yards", 0.9144),
    MILE("mi", "Miles", 1609.344),
    NAUTICAL_MILE("NM", "Nautical Miles", 1852.0)
}

data class LengthConverterInput(
    val value: Double,
    val fromUnit: LengthUnit = LengthUnit.METER,
    val toUnit: LengthUnit = LengthUnit.FOOT
)

data class LengthConverterOutput(
    val resultValue: Double,
    val resultFormatted: String,
    val allConversions: Map<LengthUnit, Double>,
    val summary: String
)

class LengthConverterTool : Tool<LengthConverterInput, LengthConverterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "length_converter",
        name = "Length & Distance Converter",
        description = "Convert metric and imperial lengths: meters, feet, inches, miles, and kilometers.",
        category = ToolCategory.CONVERTER,
        tags = listOf("length", "distance", "meters", "feet", "inches", "miles", "kilometers", "yards", "units"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Straighten"
    )

    override suspend fun execute(input: LengthConverterInput): ToolResult<LengthConverterOutput> {
        val startTime = System.currentTimeMillis()
        val meters = input.value * input.fromUnit.metersFactor
        val converted = meters / input.toUnit.metersFactor

        val allMap = mutableMapOf<LengthUnit, Double>()
        for (unit in LengthUnit.values()) {
            allMap[unit] = meters / unit.metersFactor
        }

        val formattedResult = formatNumber(converted)
        val summary = "${formatNumber(input.value)} ${input.fromUnit.symbol} = $formattedResult ${input.toUnit.symbol}"

        return ToolResult.Success(
            data = LengthConverterOutput(
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
