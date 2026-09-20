package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CronInput(
    val cronExpression: String = "*/15 9-17 * * 1-5"
)

data class CronOutput(
    val humanDescription: String,
    val minuteExplanation: String,
    val hourExplanation: String,
    val dayOfMonthExplanation: String,
    val monthExplanation: String,
    val dayOfWeekExplanation: String,
    val nextRuns: List<String>,
    val formattedReport: String,
    val summary: String
)

class CronExpressionTool : Tool<CronInput, CronOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "cron_expression_tool",
        name = "Cron Expression Explainer & Schedule Simulator",
        description = "Translate 5-field cron schedules into plain English and simulate future execution timestamps.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("cron", "schedule", "crontab", "time", "interval", "timer", "job", "parser"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Schedule"
    )

    override suspend fun execute(input: CronInput): ToolResult<CronOutput> {
        val startTime = System.currentTimeMillis()

        val clean = input.cronExpression.trim()
        val parts = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }

        if (parts.size != 5) {
            return ToolResult.Failure(
                message = "Invalid cron format. Expected 5 fields, found ${parts.size}.",
                userGuidance = "Standard cron format is: <minute> <hour> <day-of-month> <month> <day-of-week> (e.g. '*/15 9-17 * * 1-5')"
            )
        }

        val (minField, hourField, domField, monthField, dowField) = parts

        val minExp = explainField(minField, "minute", 0, 59)
        val hourExp = explainField(hourField, "hour", 0, 23)
        val domExp = explainField(domField, "day of month", 1, 31)
        val monthExp = explainMonth(monthField)
        val dowExp = explainDayOfWeek(dowField)

        val fullDescription = buildSummarySentence(minField, hourField, domField, monthField, dowField)
        val simulatedRuns = simulateNextRuns(parts, 5)

        val report = buildString {
            appendLine("CRON SCHEDULE SPECIFICATION")
            appendLine("Expression: $clean")
            appendLine("--------------------------------")
            appendLine("Plain English: $fullDescription")
            appendLine("--------------------------------")
            appendLine("Field Analysis:")
            appendLine("• Minute (0-59):        $minExp")
            appendLine("• Hour (0-23):          $hourExp")
            appendLine("• Day of Month (1-31):  $domExp")
            appendLine("• Month (1-12):         $monthExp")
            appendLine("• Day of Week (0-6):    $dowExp")
            appendLine("--------------------------------")
            appendLine("Next Projected Executions:")
            simulatedRuns.forEachIndexed { i, run ->
                appendLine("${i + 1}. $run")
            }
        }

        return ToolResult.Success(
            data = CronOutput(
                humanDescription = fullDescription,
                minuteExplanation = minExp,
                hourExplanation = hourExp,
                dayOfMonthExplanation = domExp,
                monthExplanation = monthExp,
                dayOfWeekExplanation = dowExp,
                nextRuns = simulatedRuns,
                formattedReport = report,
                summary = fullDescription
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = fullDescription
        )
    }

    private fun explainField(field: String, name: String, min: Int, max: Int): String {
        return when {
            field == "*" -> "Every $name"
            field.startsWith("*/") -> "Every ${field.substring(2)} ${name}s"
            field.contains("-") -> {
                val p = field.split("-")
                "Every $name from ${p[0]} through ${p[1]}"
            }
            field.contains(",") -> "At ${name}s ${field.replace(",", ", ")}"
            else -> "At $name $field"
        }
    }

    private fun explainMonth(field: String): String {
        val monthNames = arrayOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        return when {
            field == "*" -> "Every month"
            field.startsWith("*/") -> "Every ${field.substring(2)} months"
            field.toIntOrNull() in 1..12 -> "In ${monthNames[field.toInt()]}"
            else -> "In month $field"
        }
    }

    private fun explainDayOfWeek(field: String): String {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        return when {
            field == "*" -> "Every day of the week"
            field == "1-5" -> "Monday through Friday (Weekdays)"
            field == "0,6" || field == "6,0" -> "Saturday and Sunday (Weekends)"
            field.contains("-") -> {
                val p = field.split("-").mapNotNull { it.toIntOrNull() }
                if (p.size == 2 && p[0] in 0..6 && p[1] in 0..6) "${days[p[0]]} through ${days[p[1]]}" else "Days $field"
            }
            field.toIntOrNull() in 0..6 -> "Only on ${days[field.toInt()]}"
            else -> "Days $field"
        }
    }

    private fun buildSummarySentence(m: String, h: String, dom: String, mon: String, dow: String): String {
        val timePart = when {
            m == "*" && h == "*" -> "Every minute"
            m.startsWith("*/") && h == "*" -> "Every ${m.substring(2)} minutes"
            m.startsWith("*/") && h != "*" -> "Every ${m.substring(2)} minutes past hours $h"
            m == "0" && h == "*" -> "Every hour on the hour"
            m == "0" && h == "0" -> "At midnight (00:00)"
            else -> "At minute $m past hour $h"
        }

        val dayPart = when {
            dom == "*" && dow == "*" -> "every day"
            dow == "1-5" -> "on weekdays (Mon-Fri)"
            dow == "0,6" || dow == "6,0" -> "on weekends (Sat-Sun)"
            dow != "*" -> "on day-of-week $dow"
            dom != "*" -> "on day $dom of the month"
            else -> ""
        }

        val monPart = if (mon == "*") "" else " in month $mon"

        return "$timePart $dayPart$monPart".trim().replace(Regex("\\s+"), " ")
    }

    private fun simulateNextRuns(fields: List<String>, count: Int): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm (EEE)", Locale.US)
        val runs = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        var safety = 0
        while (runs.size < count && safety < 10000) {
            cal.add(Calendar.MINUTE, 1)
            safety++

            val m = cal.get(Calendar.MINUTE)
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val dom = cal.get(Calendar.DAY_OF_MONTH)
            val mon = cal.get(Calendar.MONTH) + 1
            val dow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday

            if (matchPart(fields[0], m, 0, 59) &&
                matchPart(fields[1], h, 0, 23) &&
                matchPart(fields[2], dom, 1, 31) &&
                matchPart(fields[3], mon, 1, 12) &&
                matchPart(fields[4], dow, 0, 6)) {
                runs.add(sdf.format(cal.time))
            }
        }
        return runs
    }

    private fun matchPart(expr: String, value: Int, min: Int, max: Int): Boolean {
        if (expr == "*") return true
        if (expr.startsWith("*/")) {
            val step = expr.substring(2).toIntOrNull() ?: return false
            return step > 0 && (value % step == 0)
        }
        if (expr.contains(",")) {
            val list = expr.split(",").mapNotNull { it.toIntOrNull() }
            return list.contains(value)
        }
        if (expr.contains("-")) {
            val parts = expr.split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size == 2) {
                return value in parts[0]..parts[1]
            }
        }
        return expr.toIntOrNull() == value
    }
}
