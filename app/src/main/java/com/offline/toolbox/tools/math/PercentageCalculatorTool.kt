package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class PercentageCalculationType(val label: String) {
    PERCENT_OF("What is X% of Y?"),
    IS_WHAT_PERCENT("X is what % of Y?"),
    PERCENT_CHANGE("Percentage change from X to Y"),
    ADD_PERCENT("Add X% to Y (e.g., Tax / Tip)"),
    SUBTRACT_PERCENT("Subtract X% from Y (e.g., Discount)")
}

data class PercentageInput(
    val type: PercentageCalculationType,
    val valueX: Double,
    val valueY: Double
)

data class PercentageOutput(
    val resultFormatted: String,
    val explanation: String,
    val numericValue: Double
)

class PercentageCalculatorTool : Tool<PercentageInput, PercentageOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "percentage_calculator",
        name = "Percentage Calculator",
        description = "Compute percentages, discounts, tips, margins, and percent changes.",
        category = ToolCategory.MATH,
        tags = listOf("percentage", "calculator", "math", "discount", "tip", "tax", "ratio", "change"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Percent"
    )

    override suspend fun execute(input: PercentageInput): ToolResult<PercentageOutput> {
        val startTime = System.currentTimeMillis()
        val x = input.valueX
        val y = input.valueY

        return try {
            val (resultVal, explanation) = when (input.type) {
                PercentageCalculationType.PERCENT_OF -> {
                    val res = (x / 100.0) * y
                    Pair(res, "$x% of $y = ${formatNumber(res)}")
                }
                PercentageCalculationType.IS_WHAT_PERCENT -> {
                    if (y == 0.0) {
                        return ToolResult.Failure(
                            message = "Cannot divide by zero.",
                            userGuidance = "The denominator Y cannot be 0 when calculating what percent X is of Y."
                        )
                    }
                    val res = (x / y) * 100.0
                    Pair(res, "$x is ${formatNumber(res)}% of $y")
                }
                PercentageCalculationType.PERCENT_CHANGE -> {
                    if (x == 0.0) {
                        return ToolResult.Failure(
                            message = "Original value X cannot be zero for percent change.",
                            userGuidance = "Percentage change from 0 is undefined. Enter a non-zero starting value."
                        )
                    }
                    val change = ((y - x) / x) * 100.0
                    val sign = if (change > 0) "+ (Increase)" else if (change < 0) "- (Decrease)" else "No change"
                    Pair(change, "Change from $x to $y is ${formatNumber(change)}% $sign")
                }
                PercentageCalculationType.ADD_PERCENT -> {
                    val addAmount = (x / 100.0) * y
                    val total = y + addAmount
                    Pair(total, "$y + $x% (${formatNumber(addAmount)}) = ${formatNumber(total)}")
                }
                PercentageCalculationType.SUBTRACT_PERCENT -> {
                    val discountAmount = (x / 100.0) * y
                    val total = y - discountAmount
                    Pair(total, "$y - $x% discount (${formatNumber(discountAmount)}) = ${formatNumber(total)}")
                }
            }

            ToolResult.Success(
                data = PercentageOutput(
                    resultFormatted = formatNumber(resultVal),
                    explanation = explanation,
                    numericValue = resultVal
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = explanation
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Calculation error: ${e.message}",
                cause = e
            )
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }
}
