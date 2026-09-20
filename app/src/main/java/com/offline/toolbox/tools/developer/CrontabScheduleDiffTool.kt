package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class CrontabDiffInput(
    val cronA: String = "*/15 * * * *",
    val cronB: String = "0 * * * *"
)

data class CrontabDiffOutput(
    val cronA: String,
    val cronB: String,
    val overlapRelationship: String,
    val upcomingSimultaneousRuns: List<String>,
    val formattedReport: String,
    val summary: String
)

class CrontabScheduleDiffTool : Tool<CrontabDiffInput, CrontabDiffOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "crontab_schedule_diff_tool",
        name = "Cron Schedule Overlap & Collision Comparator",
        description = "Compare two cron schedules offline to identify overlapping runs, concurrency conflicts, and calculate next shared executions.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("cron", "crontab", "schedule", "diff", "overlap", "collision", "compare", "concurrency"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Schedule"
    )

    override suspend fun execute(input: CrontabDiffInput): ToolResult<CrontabDiffOutput> {
        val startTime = System.currentTimeMillis()
        val cA = input.cronA.trim()
        val cB = input.cronB.trim()

        val parsedA = parseCron(cA) ?: return ToolResult.Failure("Invalid Cron Expression A: '$cA'. Expected 5 fields.")
        val parsedB = parseCron(cB) ?: return ToolResult.Failure("Invalid Cron Expression B: '$cB'. Expected 5 fields.")

        // Find next 10 simultaneous matches
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        var current = LocalDateTime.now().withSecond(0).withNano(0).plusMinutes(1)
        val matches = mutableListOf<String>()

        var iterations = 0
        while (matches.size < 10 && iterations < 50000) {
            val minute = current.minute
            val hour = current.hour
            val dom = current.dayOfMonth
            val month = current.monthValue
            val dow = current.dayOfWeek.value % 7 // 0=Sunday..6=Saturday

            val matchA = parsedA.matches(minute, hour, dom, month, dow)
            val matchB = parsedB.matches(minute, hour, dom, month, dow)

            if (matchA && matchB) {
                matches.add(current.format(formatter))
            }
            current = current.plusMinutes(1)
            iterations++
        }

        // Determine overlap relationship
        val relationship = when {
            cA == cB -> "Identical Schedules (100% Collision on every run)"
            matches.size == 10 && iterations < 700 -> "Frequent Overlap (Shared Execution Intervals)"
            matches.isNotEmpty() -> "Partial Overlap (Periodic Shared Runs)"
            else -> "Disjoint Schedules (No Concurrent Runs Detected within projection horizon)"
        }

        val report = buildString {
            appendLine("CRON SCHEDULE COLLISION & OVERLAP REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Schedule A:      $cA")
            appendLine("Schedule B:      $cB")
            appendLine("Relationship:    $relationship")
            appendLine()
            appendLine("NEXT 10 SIMULTANEOUS RUNS (Potential Concurrency Bottlenecks):")
            if (matches.isEmpty()) {
                appendLine("• No simultaneous triggers within next 35 days.")
            } else {
                matches.forEachIndexed { i, ts -> appendLine(" [${i + 1}] $ts") }
            }
        }

        val summary = "$relationship (${matches.size} upcoming collisions)"

        return ToolResult.Success(
            data = CrontabDiffOutput(
                cronA = cA,
                cronB = cB,
                overlapRelationship = relationship,
                upcomingSimultaneousRuns = matches,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private data class ParsedCron(
        val minutes: Set<Int>,
        val hours: Set<Int>,
        val daysOfMonth: Set<Int>,
        val months: Set<Int>,
        val daysOfWeek: Set<Int>
    ) {
        fun matches(minute: Int, hour: Int, dom: Int, month: Int, dow: Int): Boolean {
            return minute in minutes && hour in hours && dom in daysOfMonth && month in months && dow in daysOfWeek
        }
    }

    private fun parseCron(cron: String): ParsedCron? {
        val parts = cron.trim().split(Regex("\\s+"))
        if (parts.size != 5) return null

        val minSet = parseField(parts[0], 0, 59) ?: return null
        val hourSet = parseField(parts[1], 0, 23) ?: return null
        val domSet = parseField(parts[2], 1, 31) ?: return null
        val monSet = parseField(parts[3], 1, 12) ?: return null
        val dowSet = parseField(parts[4], 0, 6) ?: return null

        return ParsedCron(minSet, hourSet, domSet, monSet, dowSet)
    }

    private fun parseField(field: String, min: Int, max: Int): Set<Int>? {
        if (field == "*") return (min..max).toSet()

        val result = mutableSetOf<Int>()
        val segments = field.split(",")

        for (seg in segments) {
            when {
                seg.startsWith("*/") -> {
                    val step = seg.removePrefix("*/").toIntOrNull() ?: return null
                    if (step <= 0) return null
                    for (i in min..max step step) result.add(i)
                }
                seg.contains("-") -> {
                    val range = seg.split("-")
                    if (range.size != 2) return null
                    val start = range[0].toIntOrNull() ?: return null
                    val end = range[1].toIntOrNull() ?: return null
                    if (start !in min..max || end !in min..max) return null
                    for (i in start..end) result.add(i)
                }
                else -> {
                    val num = seg.toIntOrNull() ?: return null
                    if (num in min..max) result.add(num) else return null
                }
            }
        }
        return result
    }
}
