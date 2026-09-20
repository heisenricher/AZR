package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class UnixTimestampOutput(
    val epochSeconds: Long,
    val epochMilliseconds: Long,
    val utcDateTime: String,
    val localDateTime: String,
    val relativeTime: String,
    val dayOfWeek: String,
    val dayOfYear: Int,
    val isLeapYear: Boolean,
    val summary: String
)

class UnixTimestampTool : Tool<String, UnixTimestampOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "unix_timestamp",
        name = "Unix Timestamp Converter",
        description = "Convert between Unix epoch timestamps (seconds/millis) and human-readable UTC and local dates.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("unix", "timestamp", "epoch", "date", "time", "seconds", "milliseconds", "iso8601"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Schedule"
    )

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override suspend fun execute(input: String): ToolResult<UnixTimestampOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.trim()

        val instant = try {
            if (raw.isEmpty() || raw.equals("now", ignoreCase = true)) {
                Instant.now()
            } else if (raw.all { it.isDigit() || it == '-' }) {
                val num = raw.toLong()
                // If larger than 10 digits, assume milliseconds, else seconds
                if (kotlin.math.abs(num) > 99_999_999_999L) {
                    Instant.ofEpochMilli(num)
                } else {
                    Instant.ofEpochSecond(num)
                }
            } else {
                // Try parsing ISO date
                Instant.parse(raw)
            }
        } catch (e: Exception) {
            return ToolResult.Failure(
                message = "Invalid timestamp or date format: '$input'",
                userGuidance = "Enter Unix epoch seconds (e.g. 1725840000), milliseconds, 'now', or an ISO-8601 string."
            )
        }

        val epochSec = instant.epochSecond
        val epochMilli = instant.toEpochMilli()

        val utcZdt = instant.atZone(ZoneOffset.UTC)
        val localZdt = instant.atZone(ZoneId.systemDefault())

        val utcStr = utcZdt.format(isoFormatter) + " UTC"
        val localStr = localZdt.format(isoFormatter) + " (" + localZdt.zone.id + ")"

        val relative = calculateRelativeTime(epochSec)
        val dayOfWeek = localZdt.dayOfWeek.name
        val dayOfYear = localZdt.dayOfYear
        val isLeapYear = localZdt.toLocalDate().isLeapYear

        val summary = "$localStr ($relative)"

        return ToolResult.Success(
            data = UnixTimestampOutput(
                epochSeconds = epochSec,
                epochMilliseconds = epochMilli,
                utcDateTime = utcStr,
                localDateTime = localStr,
                relativeTime = relative,
                dayOfWeek = dayOfWeek,
                dayOfYear = dayOfYear,
                isLeapYear = isLeapYear,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun calculateRelativeTime(epochSec: Long): String {
        val nowSec = Instant.now().epochSecond
        val diff = nowSec - epochSec

        return when {
            diff == 0L -> "just now"
            diff in 1..59 -> "$diff seconds ago"
            diff in 60..3599 -> "${diff / 60} minute(s) ago"
            diff in 3600..86399 -> "${diff / 3600} hour(s) ago"
            diff >= 86400 -> "${diff / 86400} day(s) ago"
            diff in -59..-1 -> "in ${-diff} seconds"
            diff in -3599..-60 -> "in ${-diff / 60} minute(s)"
            diff in -86399..-3600 -> "in ${-diff / 3600} hour(s)"
            else -> "in ${-diff / 86400} day(s)"
        }
    }
}
