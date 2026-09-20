package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

data class StatisticsOutput(
    val count: Int,
    val sum: Double,
    val mean: Double,
    val median: Double,
    val mode: List<Double>,
    val min: Double,
    val max: Double,
    val range: Double,
    val standardDeviation: Double,
    val variance: Double,
    val summary: String
)

class AverageCalculatorTool : Tool<String, StatisticsOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "average_calculator",
        name = "Statistics & Average Calculator",
        description = "Calculate Mean, Median, Mode, Standard Deviation, Variance, Min, and Max from a list of numbers.",
        category = ToolCategory.MATH,
        tags = listOf("average", "mean", "median", "mode", "statistics", "math", "variance", "stddev", "min", "max"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "BarChart"
    )

    override suspend fun execute(input: String): ToolResult<StatisticsOutput> {
        val startTime = System.currentTimeMillis()
        val numbers = input.split(Regex("[,;\\s]+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.toDoubleOrNull() }

        if (numbers.isEmpty()) {
            return ToolResult.Failure(
                message = "No valid numbers found.",
                userGuidance = "Enter numbers separated by spaces, commas, or newlines (e.g. '12, 15, 22, 9, 34')."
            )
        }

        val count = numbers.size
        val sum = numbers.sum()
        val mean = sum / count

        val sorted = numbers.sorted()
        val median = if (count % 2 == 1) {
            sorted[count / 2]
        } else {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        }

        // Mode calculation
        val frequencies = numbers.groupingBy { it }.eachCount()
        val maxFreq = frequencies.values.maxOrNull() ?: 1
        val mode = if (maxFreq > 1) {
            frequencies.filter { it.value == maxFreq }.keys.toList().sorted()
        } else emptyList()

        val min = sorted.first()
        val max = sorted.last()
        val range = max - min

        // Variance & Standard Deviation (Sample)
        val variance = if (count > 1) {
            numbers.sumOf { (it - mean).pow(2) } / (count - 1)
        } else 0.0
        val stdDev = sqrt(variance)

        val summary = "Mean: ${format(mean)} • Median: ${format(median)} • Range: [${format(min)}, ${format(max)}]"

        return ToolResult.Success(
            data = StatisticsOutput(
                count = count,
                sum = round4(sum),
                mean = round4(mean),
                median = round4(median),
                mode = mode,
                min = min,
                max = max,
                range = round4(range),
                standardDeviation = round4(stdDev),
                variance = round4(variance),
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun round4(v: Double): Double = Math.round(v * 10000.0) / 10000.0
    private fun format(v: Double): String = String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
}
