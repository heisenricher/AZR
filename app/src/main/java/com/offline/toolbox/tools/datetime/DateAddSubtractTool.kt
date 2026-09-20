package com.offline.toolbox.tools.datetime

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class DateOperation {
    ADD,
    SUBTRACT
}

enum class DateUnit(val label: String) {
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months"),
    YEARS("Years"),
    BUSINESS_DAYS("Business Days (Mon-Fri)")
}

data class DateAddSubtractInput(
    val startDate: LocalDate = LocalDate.now(),
    val operation: DateOperation = DateOperation.ADD,
    val amount: Int = 30,
    val unit: DateUnit = DateUnit.DAYS
)

data class DateAddSubtractOutput(
    val resultDate: LocalDate,
    val formattedResult: String,
    val dayOfWeek: String,
    val isLeapYear: Boolean,
    val summary: String
)

class DateAddSubtractTool : Tool<DateAddSubtractInput, DateAddSubtractOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "date_add_subtract",
        name = "Add / Subtract Date",
        description = "Add or subtract days, weeks, months, years, or business days to any starting date.",
        category = ToolCategory.DATETIME,
        tags = listOf("date", "add date", "subtract date", "calendar", "business days", "deadline", "time"),
        inputType = ToolDataType.DATE_PAIR,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Event"
    )

    private val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

    override suspend fun execute(input: DateAddSubtractInput): ToolResult<DateAddSubtractOutput> {
        val startTime = System.currentTimeMillis()
        val amt = input.amount.coerceAtLeast(0)
        val sign = if (input.operation == DateOperation.ADD) 1 else -1

        val resultDate = when (input.unit) {
            DateUnit.DAYS -> input.startDate.plusDays((amt * sign).toLong())
            DateUnit.WEEKS -> input.startDate.plusWeeks((amt * sign).toLong())
            DateUnit.MONTHS -> input.startDate.plusMonths((amt * sign).toLong())
            DateUnit.YEARS -> input.startDate.plusYears((amt * sign).toLong())
            DateUnit.BUSINESS_DAYS -> {
                var current = input.startDate
                var counted = 0
                while (counted < amt) {
                    current = if (input.operation == DateOperation.ADD) current.plusDays(1) else current.minusDays(1)
                    if (current.dayOfWeek != DayOfWeek.SATURDAY && current.dayOfWeek != DayOfWeek.SUNDAY) {
                        counted++
                    }
                }
                current
            }
        }

        val formatted = resultDate.format(formatter)
        val opVerb = if (input.operation == DateOperation.ADD) "after" else "before"
        val summary = "$formatted ($amt ${input.unit.label} $opVerb ${input.startDate})"

        return ToolResult.Success(
            data = DateAddSubtractOutput(
                resultDate = resultDate,
                formattedResult = formatted,
                dayOfWeek = resultDate.dayOfWeek.name,
                isLeapYear = resultDate.isLeapYear,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
