package com.offline.toolbox.tools.generator

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.SecureRandom
import java.util.Locale

data class RandomNumberConfig(
    val min: Long = 1,
    val max: Long = 100,
    val count: Int = 5,
    val uniqueOnly: Boolean = false,
    val sortResults: Boolean = false
)

data class RandomNumberOutput(
    val numbers: List<Long>,
    val numbersFormatted: String,
    val count: Int,
    val sum: Long,
    val average: Double,
    val summary: String
)

class RandomNumberTool : Tool<RandomNumberConfig, RandomNumberOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "random_number_generator",
        name = "Random Number Generator",
        description = "Generate cryptographically secure random numbers with custom bounds, count, and uniqueness.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("random", "number", "generator", "crypto", "secure", "dice", "integer", "lottery"),
        inputType = ToolDataType.NONE,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Casino"
    )

    private val secureRandom = SecureRandom()

    override suspend fun execute(input: RandomNumberConfig): ToolResult<RandomNumberOutput> {
        val startTime = System.currentTimeMillis()
        val min = minOf(input.min, input.max)
        val max = maxOf(input.min, input.max)
        val rangeSize = max - min + 1

        val count = input.count.coerceIn(1, 500)

        if (input.uniqueOnly && rangeSize < count) {
            return ToolResult.Failure(
                message = "Cannot generate $count unique numbers in range [$min, $max] (only $rangeSize unique values exist).",
                userGuidance = "Expand the [min, max] range or decrease the quantity of numbers."
            )
        }

        val results = mutableListOf<Long>()
        val uniqueSet = mutableSetOf<Long>()

        while (results.size < count) {
            val randomOffset = if (rangeSize <= Int.MAX_VALUE) {
                secureRandom.nextInt(rangeSize.toInt()).toLong()
            } else {
                (secureRandom.nextDouble() * rangeSize).toLong()
            }
            val num = min + randomOffset

            if (input.uniqueOnly) {
                if (uniqueSet.add(num)) {
                    results.add(num)
                }
            } else {
                results.add(num)
            }
        }

        val finalNumbers = if (input.sortResults) results.sorted() else results
        val sum = finalNumbers.sum()
        val avg = sum.toDouble() / count

        val formatted = finalNumbers.joinToString(", ")
        val summary = "Generated $count numbers in [$min, $max]"

        return ToolResult.Success(
            data = RandomNumberOutput(
                numbers = finalNumbers,
                numbersFormatted = formatted,
                count = count,
                sum = sum,
                average = Math.round(avg * 100.0) / 100.0,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
