package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class AreaUnit(val symbol: String, val displayName: String, val sqMetersFactor: Double) {
    SQUARE_METER("m²", "Square Meters", 1.0),
    SQUARE_KILOMETER("km²", "Square Kilometers", 1_000_000.0),
    SQUARE_CENTIMETER("cm²", "Square Centimeters", 0.0001),
    SQUARE_MILLIMETER("mm²", "Square Millimeters", 0.000001),
    HECTARE("ha", "Hectares", 10_000.0),
    ACRE("ac", "Acres", 4046.8564224),
    SQUARE_FOOT("sq ft", "Square Feet", 0.09290304),
    SQUARE_INCH("sq in", "Square Inches", 0.00064516),
    SQUARE_YARD("sq yd", "Square Yards", 0.83612736),
    SQUARE_MILE("sq mi", "Square Miles", 2_589_988.110336)
}

data class AreaConverterInput(
    val value: Double,
    val fromUnit: AreaUnit = AreaUnit.SQUARE_METER,
    val toUnit: AreaUnit = AreaUnit.SQUARE_FOOT
)

data class AreaConverterOutput(
    val resultValue: Double,
    val resultFormatted: String,
    val allConversions: Map<AreaUnit, Double>,
    val summary: String
)

class AreaConverterTool : Tool<AreaConverterInput, AreaConverterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "area_converter",
        name = "Area & Land Converter",
        description = "Convert area across square meters, square feet, acres, hectares, square miles, and square kilometers.",
        category = ToolCategory.CONVERTER,
        tags = listOf("area", "land", "acres", "hectares", "square feet", "sq ft", "meters", "real estate"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "CropSquare"
    )

    override suspend fun execute(input: AreaConverterInput): ToolResult<AreaConverterOutput> {
        val startTime = System.currentTimeMillis()
        val sqMeters = input.value * input.fromUnit.sqMetersFactor
        val converted = sqMeters / input.toUnit.sqMetersFactor

        val allMap = mutableMapOf<AreaUnit, Double>()
        for (unit in AreaUnit.values()) {
            allMap[unit] = sqMeters / unit.sqMetersFactor
        }

        val formatted = "%.4f %s".format(Locale.US, converted, input.toUnit.symbol)
        val summary = "${input.value} ${input.fromUnit.symbol} = $formatted"

        return ToolResult.Success(
            data = AreaConverterOutput(
                resultValue = converted,
                resultFormatted = formatted,
                allConversions = allMap,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
