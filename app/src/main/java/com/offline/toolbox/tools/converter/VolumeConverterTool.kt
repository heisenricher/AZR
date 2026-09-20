package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class VolumeUnit(val symbol: String, val displayName: String, val litersFactor: Double) {
    MILLILITER("mL", "Milliliters", 0.001),
    LITER("L", "Liters", 1.0),
    CUBIC_METER("m³", "Cubic Meters", 1000.0),
    GALLON_US("gal", "Gallons (US)", 3.785411784),
    GALLON_UK("gal (UK)", "Gallons (UK)", 4.54609),
    FLUID_OUNCE_US("fl oz", "Fluid Ounces (US)", 0.0295735295625),
    CUP_US("cup", "Cups (US)", 0.2365882365),
    PINT_US("pt", "Pints (US)", 0.473176473),
    QUART_US("qt", "Quarts (US)", 0.946352946),
    TABLESPOON("tbsp", "Tablespoons (US)", 0.01478676478125),
    TEASPOON("tsp", "Teaspoons (US)", 0.00492892159375),
    CUBIC_FOOT("cu ft", "Cubic Feet", 28.316846592)
}

data class VolumeConverterInput(
    val value: Double,
    val fromUnit: VolumeUnit = VolumeUnit.LITER,
    val toUnit: VolumeUnit = VolumeUnit.GALLON_US
)

data class VolumeConverterOutput(
    val resultValue: Double,
    val resultFormatted: String,
    val allConversions: Map<VolumeUnit, Double>,
    val summary: String
)

class VolumeConverterTool : Tool<VolumeConverterInput, VolumeConverterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "volume_converter",
        name = "Volume & Liquid Capacity Converter",
        description = "Convert volume and liquid capacity across Liters, Gallons, Fluid Ounces, Cups, Milliliters, and Cubic Meters.",
        category = ToolCategory.CONVERTER,
        tags = listOf("volume", "liquid", "gallons", "liters", "cups", "ounces", "capacity", "cooking", "units"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "LocalDrink"
    )

    override suspend fun execute(input: VolumeConverterInput): ToolResult<VolumeConverterOutput> {
        val startTime = System.currentTimeMillis()
        val liters = input.value * input.fromUnit.litersFactor
        val converted = liters / input.toUnit.litersFactor

        val allMap = mutableMapOf<VolumeUnit, Double>()
        for (unit in VolumeUnit.values()) {
            allMap[unit] = liters / unit.litersFactor
        }

        val formatted = "%.4f %s".format(Locale.US, converted, input.toUnit.symbol)
        val summary = "${input.value} ${input.fromUnit.symbol} = $formatted"

        return ToolResult.Success(
            data = VolumeConverterOutput(
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
