package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class BmiUnitSystem {
    METRIC,    // kg and cm
    IMPERIAL   // lbs and inches
}

data class BmiInput(
    val weight: Double,
    val height: Double,
    val unitSystem: BmiUnitSystem = BmiUnitSystem.METRIC
)

data class BmiOutput(
    val bmiScore: Double,
    val category: String,
    val healthyWeightMin: Double,
    val healthyWeightMax: Double,
    val weightUnit: String,
    val summary: String
)

class BmiCalculatorTool : Tool<BmiInput, BmiOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "bmi_calculator",
        name = "BMI & Body Health Calculator",
        description = "Compute Body Mass Index (BMI), WHO weight category, and healthy weight range.",
        category = ToolCategory.MATH,
        tags = listOf("bmi", "body mass index", "health", "fitness", "weight", "height", "calculator"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FitnessCenter"
    )

    override suspend fun execute(input: BmiInput): ToolResult<BmiOutput> {
        val startTime = System.currentTimeMillis()

        if (input.weight <= 0.0 || input.height <= 0.0) {
            return ToolResult.Failure(
                message = "Invalid height or weight values.",
                userGuidance = "Height and weight must be greater than zero."
            )
        }

        // Convert to standard metric for formula: weight in kg, height in meters
        val (weightKg, heightM, weightUnit) = when (input.unitSystem) {
            BmiUnitSystem.METRIC -> Triple(input.weight, input.height / 100.0, "kg")
            BmiUnitSystem.IMPERIAL -> Triple(input.weight * 0.45359237, input.height * 0.0254, "lbs")
        }

        val bmi = weightKg / (heightM * heightM)
        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal Weight (Healthy)"
            bmi < 30.0 -> "Overweight"
            bmi < 35.0 -> "Obesity (Class 1)"
            bmi < 40.0 -> "Obesity (Class 2)"
            else -> "Severe Obesity (Class 3)"
        }

        // Healthy weight range (BMI 18.5 to 24.9)
        val minHealthyKg = 18.5 * (heightM * heightM)
        val maxHealthyKg = 24.9 * (heightM * heightM)

        val (minHealthyDisp, maxHealthyDisp) = when (input.unitSystem) {
            BmiUnitSystem.METRIC -> Pair(round1(minHealthyKg), round1(maxHealthyKg))
            BmiUnitSystem.IMPERIAL -> Pair(round1(minHealthyKg / 0.45359237), round1(maxHealthyKg / 0.45359237))
        }

        val roundedBmi = round1(bmi)
        val summary = "BMI: $roundedBmi • $category (Ideal: $minHealthyDisp - $maxHealthyDisp $weightUnit)"

        return ToolResult.Success(
            data = BmiOutput(
                bmiScore = roundedBmi,
                category = category,
                healthyWeightMin = minHealthyDisp,
                healthyWeightMax = maxHealthyDisp,
                weightUnit = weightUnit,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0
}
