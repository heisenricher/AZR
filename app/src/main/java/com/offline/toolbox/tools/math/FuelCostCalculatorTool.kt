package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class FuelUnit(val label: String) {
    METRIC_L_PER_100KM("Liters / 100 km (Distance in km, Price per Liter)"),
    US_MPG("Miles / Gallon (Distance in miles, Price per Gallon)"),
    KM_PER_LITER("Kilometers / Liter (Distance in km, Price per Liter)")
}

data class FuelCostInput(
    val distance: Double = 500.0,
    val efficiencyValue: Double = 7.5,
    val fuelUnit: FuelUnit = FuelUnit.METRIC_L_PER_100KM,
    val fuelPricePerUnit: Double = 1.45,
    val passengers: Int = 1
)

data class FuelCostOutput(
    val totalFuelConsumed: Double,
    val totalTripCost: Double,
    val costPerDistanceUnit: Double,
    val costPerPassenger: Double,
    val fuelUnitName: String,
    val formattedReport: String,
    val summary: String
)

class FuelCostCalculatorTool : Tool<FuelCostInput, FuelCostOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "fuel_cost_calculator_tool",
        name = "Fuel Cost & Road Trip Splitter",
        description = "Calculate vehicle fuel consumption, road trip expenses, cost per mile/km, and split among passengers.",
        category = ToolCategory.MATH,
        tags = listOf("fuel", "cost", "gas", "mileage", "trip", "travel", "mpg", "split", "car", "distance"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "LocalGasStation"
    )

    override suspend fun execute(input: FuelCostInput): ToolResult<FuelCostOutput> {
        val startTime = System.currentTimeMillis()

        if (input.distance <= 0.0) {
            return ToolResult.Failure("Trip distance must be greater than 0.")
        }
        if (input.efficiencyValue <= 0.0) {
            return ToolResult.Failure("Fuel efficiency must be greater than 0.")
        }
        if (input.fuelPricePerUnit < 0.0) {
            return ToolResult.Failure("Fuel price cannot be negative.")
        }
        val passCount = input.passengers.coerceAtLeast(1)

        val fuelConsumed = when (input.fuelUnit) {
            FuelUnit.METRIC_L_PER_100KM -> (input.distance / 100.0) * input.efficiencyValue
            FuelUnit.US_MPG -> input.distance / input.efficiencyValue
            FuelUnit.KM_PER_LITER -> input.distance / input.efficiencyValue
        }

        val totalCost = fuelConsumed * input.fuelPricePerUnit
        val costPerDist = totalCost / input.distance
        val costPerPerson = totalCost / passCount

        val volumeName = if (input.fuelUnit == FuelUnit.US_MPG) "Gallons" else "Liters"
        val distName = if (input.fuelUnit == FuelUnit.US_MPG) "miles" else "km"

        val report = buildString {
            appendLine("ROAD TRIP FUEL COST BREAKDOWN")
            appendLine("--------------------------------")
            appendLine("Trip Distance:         %.1f %s".format(Locale.US, input.distance, distName))
            appendLine("Vehicle Efficiency:    %.2f (%s)".format(Locale.US, input.efficiencyValue, input.fuelUnit.name))
            appendLine("Fuel Price:            %.2f per %s".format(Locale.US, input.fuelPricePerUnit, if (input.fuelUnit == FuelUnit.US_MPG) "gal" else "L"))
            appendLine("Total Fuel Consumed:   %.2f %s".format(Locale.US, fuelConsumed, volumeName))
            appendLine("--------------------------------")
            appendLine("TOTAL TRIP EXPENSE:    %.2f".format(Locale.US, totalCost))
            appendLine("Cost per %s:            %.3f".format(Locale.US, distName, costPerDist))
            if (passCount > 1) {
                appendLine("Passengers:            $passCount people")
                appendLine("Cost per Passenger:    %.2f".format(Locale.US, costPerPerson))
            }
        }

        val summary = "Total Fuel Cost: %.2f (%.1f %s)".format(Locale.US, totalCost, fuelConsumed, volumeName)

        return ToolResult.Success(
            data = FuelCostOutput(
                totalFuelConsumed = fuelConsumed,
                totalTripCost = totalCost,
                costPerDistanceUnit = costPerDist,
                costPerPassenger = costPerPerson,
                fuelUnitName = volumeName,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
