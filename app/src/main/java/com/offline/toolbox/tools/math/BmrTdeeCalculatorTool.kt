package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.roundToInt

enum class BiologicalGender {
    MALE,
    FEMALE
}

enum class ActivityLevel(val displayName: String, val multiplier: Double) {
    SEDENTARY("Sedentary (Little or no exercise, desk job)", 1.2),
    LIGHTLY_ACTIVE("Lightly Active (Exercise 1-3 days/week)", 1.375),
    MODERATELY_ACTIVE("Moderately Active (Exercise 3-5 days/week)", 1.55),
    VERY_ACTIVE("Very Active (Hard exercise 6-7 days/week)", 1.725),
    EXTRA_ACTIVE("Extra Active (Very hard exercise, physical job)", 1.9)
}

data class BmrInput(
    val weightKg: Double = 75.0,
    val heightCm: Double = 178.0,
    val ageYears: Int = 28,
    val gender: BiologicalGender = BiologicalGender.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE
)

data class BmrOutput(
    val bmrCalories: Int,
    val tdeeCalories: Int,
    val cuttingCalories: Int,
    val bulkingCalories: Int,
    val maintenanceProteinGrams: Int,
    val maintenanceCarbsGrams: Int,
    val maintenanceFatGrams: Int,
    val formattedReport: String,
    val summary: String
)

class BmrTdeeCalculatorTool : Tool<BmrInput, BmrOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "bmr_tdee_calculator_tool",
        name = "BMR & Daily Caloric Expenditure (TDEE)",
        description = "Calculate Basal Metabolic Rate and Total Daily Energy Expenditure using Mifflin-St Jeor formula with macro targets.",
        category = ToolCategory.MATH,
        tags = listOf("bmr", "tdee", "calories", "metabolism", "fitness", "nutrition", "health", "macro", "diet"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FitnessCenter"
    )

    override suspend fun execute(input: BmrInput): ToolResult<BmrOutput> {
        val startTime = System.currentTimeMillis()
        val w = input.weightKg
        val h = input.heightCm
        val a = input.ageYears

        if (w <= 0.0 || h <= 0.0 || a <= 0) {
            return ToolResult.Failure("Weight, height, and age must be strictly positive values.")
        }

        // Mifflin-St Jeor Equation
        // BMR = 10 * weight (kg) + 6.25 * height (cm) - 5 * age (y) + s
        // s = +5 for males, -161 for females
        val s = if (input.gender == BiologicalGender.MALE) 5.0 else -161.0
        val bmr = (10.0 * w + 6.25 * h - 5.0 * a + s).roundToInt()

        val tdee = (bmr * input.activityLevel.multiplier).roundToInt()
        val cutting = (tdee - 500).coerceAtLeast(1200)
        val bulking = tdee + 500

        // Macro targets (Maintenance: 30% protein, 40% carbs, 30% fats)
        // Protein: 4 kcal/g, Carbs: 4 kcal/g, Fat: 9 kcal/g
        val proteinGrams = ((tdee * 0.30) / 4.0).roundToInt()
        val carbsGrams = ((tdee * 0.40) / 4.0).roundToInt()
        val fatGrams = ((tdee * 0.30) / 9.0).roundToInt()

        val report = buildString {
            appendLine("METABOLIC & CALORIC ENERGY EXPENDITURE (TDEE)")
            appendLine("--------------------------------------------------")
            appendLine("Bio Profile:         ${input.gender.name.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }}, ${a}y, ${w}kg, ${h}cm")
            appendLine("Activity Level:      ${input.activityLevel.displayName}")
            appendLine()
            appendLine("ENERGY REQUIREMENTS:")
            appendLine("• Basal Metabolic Rate (BMR): $bmr kcal/day (Base survival)")
            appendLine("• Total Daily Expenditure:    $tdee kcal/day (Maintenance)")
            appendLine("• Moderate Fat Loss (Cut):    $cutting kcal/day (-500 kcal deficit)")
            appendLine("• Muscle Gain (Bulk):         $bulking kcal/day (+500 kcal surplus)")
            appendLine()
            appendLine("MAINTENANCE MACRONUTRIENT SPLIT (30/40/30):")
            appendLine("• Protein (30%): $proteinGrams g ($((proteinGrams * 4))} kcal)")
            appendLine("• Carbs (40%):   $carbsGrams g ($((carbsGrams * 4))} kcal)")
            appendLine("• Fats (30%):    $fatGrams g ($((fatGrams * 9))} kcal)")
        }

        val summary = "BMR: $bmr kcal | TDEE: $tdee kcal/day"

        return ToolResult.Success(
            data = BmrOutput(
                bmrCalories = bmr,
                tdeeCalories = tdee,
                cuttingCalories = cutting,
                bulkingCalories = bulking,
                maintenanceProteinGrams = proteinGrams,
                maintenanceCarbsGrams = carbsGrams,
                maintenanceFatGrams = fatGrams,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
