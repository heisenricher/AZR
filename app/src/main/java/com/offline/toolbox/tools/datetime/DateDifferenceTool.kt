package com.offline.toolbox.tools.datetime

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

data class DateDifferenceInput(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class DateDifferenceOutput(
    val periodYears: Int,
    val periodMonths: Int,
    val periodDays: Int,
    val totalDays: Long,
    val totalWeeks: Long,
    val remainingDaysInWeek: Long,
    val businessDays: Long,
    val weekendDays: Long,
    val formattedSummary: String
)

class DateDifferenceTool : Tool<DateDifferenceInput, DateDifferenceOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "date_difference",
        name = "Date Difference Calculator",
        description = "Calculate exact difference between two dates in years, months, days, weeks, and business days.",
        category = ToolCategory.DATETIME,
        tags = listOf("date", "difference", "days", "calendar", "business days", "working days", "time"),
        inputType = ToolDataType.DATE_PAIR,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "CalendarToday"
    )

    override suspend fun execute(input: DateDifferenceInput): ToolResult<DateDifferenceOutput> {
        val startTime = System.currentTimeMillis()

        return try {
            val (start, end) = if (input.startDate.isAfter(input.endDate)) {
                Pair(input.endDate, input.startDate)
            } else {
                Pair(input.startDate, input.endDate)
            }

            val period = Period.between(start, end)
            val totalDays = ChronoUnit.DAYS.between(start, end)
            val totalWeeks = totalDays / 7
            val remainingDays = totalDays % 7

            // Calculate business days
            var businessDays = 0L
            var weekendDays = 0L
            var current = start
            while (current.isBefore(end)) {
                if (current.dayOfWeek == DayOfWeek.SATURDAY || current.dayOfWeek == DayOfWeek.SUNDAY) {
                    weekendDays++
                } else {
                    businessDays++
                }
                current = current.plusDays(1)
            }

            val summaryParts = mutableListOf<String>()
            if (period.years > 0) summaryParts.add("${period.years} ${if (period.years == 1) "year" else "years"}")
            if (period.months > 0) summaryParts.add("${period.months} ${if (period.months == 1) "month" else "months"}")
            if (period.days > 0 || summaryParts.isEmpty()) summaryParts.add("${period.days} ${if (period.days == 1) "day" else "days"}")

            val formattedSummary = summaryParts.joinToString(", ")

            ToolResult.Success(
                data = DateDifferenceOutput(
                    periodYears = period.years,
                    periodMonths = period.months,
                    periodDays = period.days,
                    totalDays = totalDays,
                    totalWeeks = totalWeeks,
                    remainingDaysInWeek = remainingDays,
                    businessDays = businessDays,
                    weekendDays = weekendDays,
                    formattedSummary = formattedSummary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "$formattedSummary ($totalDays total days)"
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Failed to calculate date difference: ${e.message}",
                cause = e
            )
        }
    }
}
