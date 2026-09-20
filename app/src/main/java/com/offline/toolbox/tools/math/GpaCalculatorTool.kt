package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class GpaScale(val maxGrade: Double, val label: String) {
    SCALE_4_0(4.0, "Standard 4.0 Scale (US / Collegiate)"),
    SCALE_5_0(5.0, "Weighted 5.0 Scale (Honors / AP)"),
    SCALE_10_0(10.0, "10.0 CGPA Scale (International)")
}

data class CourseEntry(
    val courseName: String,
    val credits: Double,
    val gradeLetter: String
)

data class GpaInput(
    val courses: List<CourseEntry> = listOf(
        CourseEntry("Mathematics", 4.0, "A"),
        CourseEntry("Computer Science", 4.0, "A-"),
        CourseEntry("Physics", 3.0, "B+"),
        CourseEntry("English", 3.0, "A")
    ),
    val scale: GpaScale = GpaScale.SCALE_4_0,
    val priorCumulativeGpa: Double = 0.0,
    val priorCredits: Double = 0.0
)

data class GpaOutput(
    val semesterGpa: Double,
    val cumulativeGpa: Double,
    val semesterCredits: Double,
    val totalCreditsOverall: Double,
    val honorsStatus: String,
    val formattedReport: String,
    val summary: String
)

class GpaCalculatorTool : Tool<GpaInput, GpaOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "gpa_calculator_tool",
        name = "GPA & Academic Grade Calculator",
        description = "Calculate semester GPA, cumulative CGPA, credit-weighted averages, and honors status across 4.0, 5.0, and 10.0 scales.",
        category = ToolCategory.MATH,
        tags = listOf("gpa", "cgpa", "grade", "college", "school", "academic", "average", "calculator", "student"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "School"
    )

    override suspend fun execute(input: GpaInput): ToolResult<GpaOutput> {
        val startTime = System.currentTimeMillis()

        if (input.courses.isEmpty()) {
            return ToolResult.Failure(
                message = "Course list is empty.",
                userGuidance = "Add at least one course with credit hours and letter grade."
            )
        }

        var semesterTotalCredits = 0.0
        var semesterTotalPoints = 0.0

        for (c in input.courses) {
            if (c.credits <= 0) continue
            val points = letterToPoints(c.gradeLetter, input.scale)
            semesterTotalCredits += c.credits
            semesterTotalPoints += (c.credits * points)
        }

        if (semesterTotalCredits <= 0.0) {
            return ToolResult.Failure("Total course credits must be greater than 0.")
        }

        val semesterGpa = semesterTotalPoints / semesterTotalCredits

        val totalOverallCredits = semesterTotalCredits + input.priorCredits.coerceAtLeast(0.0)
        val priorPoints = input.priorCumulativeGpa.coerceAtLeast(0.0) * input.priorCredits.coerceAtLeast(0.0)
        val cumulativeGpa = if (totalOverallCredits > 0.0) {
            (semesterTotalPoints + priorPoints) / totalOverallCredits
        } else semesterGpa

        val honors = when {
            input.scale == GpaScale.SCALE_4_0 && cumulativeGpa >= 3.9 -> "Summa Cum Laude (Highest Honors)"
            input.scale == GpaScale.SCALE_4_0 && cumulativeGpa >= 3.7 -> "Magna Cum Laude (High Honors)"
            input.scale == GpaScale.SCALE_4_0 && cumulativeGpa >= 3.5 -> "Cum Laude (Honors)"
            input.scale == GpaScale.SCALE_10_0 && cumulativeGpa >= 9.0 -> "First Class with Distinction"
            input.scale == GpaScale.SCALE_10_0 && cumulativeGpa >= 7.5 -> "First Class"
            else -> "Good Standing"
        }

        val report = buildString {
            appendLine("ACADEMIC GPA & CGPA REPORT")
            appendLine("--------------------------------")
            appendLine("Grading Scale:         ${input.scale.label}")
            appendLine("Semester Credits:      %.1f".format(Locale.US, semesterTotalCredits))
            appendLine("SEMESTER GPA:          %.3f / %.1f".format(Locale.US, semesterGpa, input.scale.maxGrade))
            if (input.priorCredits > 0.0) {
                appendLine("Prior Credits:         %.1f (Prior GPA: %.2f)".format(Locale.US, input.priorCredits, input.priorCumulativeGpa))
                appendLine("Total Credits Overall: %.1f".format(Locale.US, totalOverallCredits))
                appendLine("CUMULATIVE CGPA:       %.3f / %.1f".format(Locale.US, cumulativeGpa, input.scale.maxGrade))
            }
            appendLine("Academic Honors:       $honors")
            appendLine("--------------------------------")
            appendLine("Course Breakdown:")
            input.courses.forEach { c ->
                val p = letterToPoints(c.gradeLetter, input.scale)
                appendLine("• %-20s | %4.1f credits | Grade: %-3s (%.2f pts)".format(
                    Locale.US, c.courseName, c.credits, c.gradeLetter, p
                ))
            }
        }

        val summary = "Semester GPA: %.2f | Cumulative: %.2f".format(Locale.US, semesterGpa, cumulativeGpa)

        return ToolResult.Success(
            data = GpaOutput(
                semesterGpa = semesterGpa,
                cumulativeGpa = cumulativeGpa,
                semesterCredits = semesterTotalCredits,
                totalCreditsOverall = totalOverallCredits,
                honorsStatus = honors,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun letterToPoints(grade: String, scale: GpaScale): Double {
        val g = grade.trim().uppercase(Locale.US)
        val base4 = when (g) {
            "A+", "A" -> 4.0
            "A-" -> 3.7
            "B+" -> 3.3
            "B" -> 3.0
            "B-" -> 2.7
            "C+" -> 2.3
            "C" -> 2.0
            "C-" -> 1.7
            "D+" -> 1.3
            "D" -> 1.0
            "D-" -> 0.7
            "F" -> 0.0
            else -> g.toDoubleOrNull() ?: 0.0
        }

        return when (scale) {
            GpaScale.SCALE_4_0 -> base4
            GpaScale.SCALE_5_0 -> (base4 / 4.0) * 5.0
            GpaScale.SCALE_10_0 -> (base4 / 4.0) * 10.0
        }
    }
}
