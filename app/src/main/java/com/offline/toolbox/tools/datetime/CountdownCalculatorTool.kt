package com.offline.toolbox.tools.datetime

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class CountdownOutput(
    val isPastEvent: Boolean,
    val totalSeconds: Long,
    val totalDays: Long,
    val totalHours: Long,
    val totalMinutes: Long,
    val breakdownString: String,
    val summary: String
)

class CountdownCalculatorTool : Tool<LocalDateTime, CountdownOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "countdown_calculator",
        name = "Countdown & Milestone Calculator",
        description = "Compute exact days, hours, minutes, and seconds until an event or since a milestone.",
        category = ToolCategory.DATETIME,
        tags = listOf("countdown", "timer", "event", "milestone", "days until", "anniversary", "deadline"),
        inputType = ToolDataType.DATE_PAIR,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "HourglassBottom"
    )

    override suspend fun execute(input: LocalDateTime): ToolResult<CountdownOutput> {
        val startTime = System.currentTimeMillis()
        val now = LocalDateTime.now()

        val isPast = input.isBefore(now)
        val duration = Duration.between(if (isPast) input else now, if (isPast) now else input)

        val totalSecs = duration.seconds
        val days = duration.toDays()
        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()

        val breakdown = buildString {
            if (days > 0) append("$days day(s), ")
            if (hours > 0 || days > 0) append("$hours hour(s), ")
            if (minutes > 0 || hours > 0 || days > 0) append("$minutes minute(s), ")
            append("$seconds second(s)")
        }

        val prefix = if (isPast) "Time since milestone:" else "Countdown remaining:"
        val summary = "$prefix $breakdown"

        return ToolResult.Success(
            data = CountdownOutput(
                isPastEvent = isPast,
                totalSeconds = totalSecs,
                totalDays = days,
                totalHours = duration.toHours(),
                totalMinutes = duration.toMinutes(),
                breakdownString = breakdown,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
