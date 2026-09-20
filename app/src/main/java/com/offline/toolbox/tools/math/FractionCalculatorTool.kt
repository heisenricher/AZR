package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.abs

enum class FractionOperator(val symbol: String) {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("×"),
    DIVIDE("÷")
}

data class FractionInput(
    val num1: Long,
    val den1: Long,
    val operator: FractionOperator = FractionOperator.ADD,
    val num2: Long,
    val den2: Long
)

data class FractionOutput(
    val simplifiedFraction: String,
    val mixedNumber: String?,
    val decimalValue: Double,
    val calculationSteps: String,
    val summary: String
)

class FractionCalculatorTool : Tool<FractionInput, FractionOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "fraction_calculator",
        name = "Fraction Calculator",
        description = "Add, subtract, multiply, and divide fractions with mixed number and decimal conversion.",
        category = ToolCategory.MATH,
        tags = listOf("fraction", "math", "calculator", "mixed numbers", "numerator", "denominator", "simplify"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Calculate"
    )

    override suspend fun execute(input: FractionInput): ToolResult<FractionOutput> {
        val startTime = System.currentTimeMillis()

        if (input.den1 == 0L || input.den2 == 0L) {
            return ToolResult.Failure(
                message = "Denominator cannot be zero.",
                userGuidance = "A fraction cannot have a denominator of 0."
            )
        }
        if (input.operator == FractionOperator.DIVIDE && input.num2 == 0L) {
            return ToolResult.Failure(
                message = "Cannot divide by a zero fraction.",
                userGuidance = "The divisor fraction (${input.num2}/${input.den2}) evaluates to 0."
            )
        }

        val (resNum, resDen) = when (input.operator) {
            FractionOperator.ADD -> {
                val n = input.num1 * input.den2 + input.num2 * input.den1
                val d = input.den1 * input.den2
                Pair(n, d)
            }
            FractionOperator.SUBTRACT -> {
                val n = input.num1 * input.den2 - input.num2 * input.den1
                val d = input.den1 * input.den2
                Pair(n, d)
            }
            FractionOperator.MULTIPLY -> {
                val n = input.num1 * input.num2
                val d = input.den1 * input.den2
                Pair(n, d)
            }
            FractionOperator.DIVIDE -> {
                val n = input.num1 * input.den2
                val d = input.den1 * input.num2
                Pair(n, d)
            }
        }

        val gcdVal = gcd(abs(resNum), abs(resDen))
        var simpNum = resNum / gcdVal
        var simpDen = resDen / gcdVal

        if (simpDen < 0) {
            simpNum = -simpNum
            simpDen = -simpDen
        }

        val simplifiedStr = if (simpDen == 1L) "$simpNum" else "$simpNum/$simpDen"

        // Mixed number
        val mixedStr = if (abs(simpNum) > simpDen && simpDen != 1L) {
            val whole = simpNum / simpDen
            val rem = abs(simpNum % simpDen)
            "$whole $rem/$simpDen"
        } else null

        val decimal = simpNum.toDouble() / simpDen.toDouble()

        val steps = buildString {
            appendLine("Formula: (${input.num1}/${input.den1}) ${input.operator.symbol} (${input.num2}/${input.den2})")
            appendLine("Unsimplified: $resNum / $resDen")
            appendLine("Greatest Common Divisor (GCD): $gcdVal")
            appendLine("Simplified: $simplifiedStr")
            if (mixedStr != null) {
                appendLine("Mixed Number: $mixedStr")
            }
            appendLine("Decimal: ${String.format(Locale.US, "%.4f", decimal).trimEnd('0').trimEnd('.')}")
        }

        val summary = "$simplifiedStr" + (if (mixedStr != null) " ($mixedStr)" else "")

        return ToolResult.Success(
            data = FractionOutput(
                simplifiedFraction = simplifiedStr,
                mixedNumber = mixedStr,
                decimalValue = decimal,
                calculationSteps = steps.trimEnd(),
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
}
