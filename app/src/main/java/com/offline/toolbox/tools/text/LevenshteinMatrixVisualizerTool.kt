package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class EditStep(
    val operation: String, // MATCH, SUBSTITUTE, INSERT, DELETE
    val sourceChar: Char?,
    val targetChar: Char?,
    val cost: Int
)

data class LevenshteinMatrixInput(
    val sourceText: String = "kitten",
    val targetText: String = "sitting"
)

data class LevenshteinMatrixOutput(
    val distance: Int,
    val similarityScorePercent: Double,
    val sourceLength: Int,
    val targetLength: Int,
    val editSteps: List<EditStep>,
    val alignedSource: String,
    val alignedTarget: String,
    val matrixGridText: String,
    val formattedReport: String,
    val summary: String
)

class LevenshteinMatrixVisualizerTool : Tool<LevenshteinMatrixInput, LevenshteinMatrixOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "levenshtein_matrix_visualizer_tool",
        name = "Levenshtein DP Matrix & Alignment Visualizer",
        description = "Compute Wagner-Fischer 2D dynamic programming matrix, Levenshtein distance, similarity score, and optimal edit alignment.",
        category = ToolCategory.TEXT,
        tags = listOf("levenshtein", "matrix", "edit distance", "dynamic programming", "wagner fischer", "diff", "alignment", "text similarity"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Grid"
    )

    override suspend fun execute(input: LevenshteinMatrixInput): ToolResult<LevenshteinMatrixOutput> {
        val startTime = System.currentTimeMillis()
        val s = input.sourceText
        val t = input.targetText

        if (s.length > 100 || t.length > 100) {
            return ToolResult.Failure("Strings must be 100 characters or fewer for optimal 2D matrix rendering.")
        }

        val m = s.length
        val n = t.length

        // Initialize (m+1) x (n+1) matrix
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                val del = dp[i - 1][j] + 1
                val ins = dp[i][j - 1] + 1
                val sub = dp[i - 1][j - 1] + cost
                dp[i][j] = min(sub, min(del, ins))
            }
        }

        val distance = dp[m][n]
        val maxLen = max(m, n)
        val similarityScore = if (maxLen == 0) 100.0 else (1.0 - (distance.toDouble() / maxLen)) * 100.0

        // Backtrack for optimal alignment
        var currI = m
        var currJ = n
        val stepsReversed = mutableListOf<EditStep>()
        val alignedSReversed = StringBuilder()
        val alignedTReversed = StringBuilder()

        while (currI > 0 || currJ > 0) {
            if (currI > 0 && currJ > 0 && dp[currI][currJ] == dp[currI - 1][currJ - 1] + (if (s[currI - 1] == t[currJ - 1]) 0 else 1)) {
                val isMatch = s[currI - 1] == t[currJ - 1]
                stepsReversed.add(
                    EditStep(
                        operation = if (isMatch) "MATCH" else "SUBSTITUTE",
                        sourceChar = s[currI - 1],
                        targetChar = t[currJ - 1],
                        cost = if (isMatch) 0 else 1
                    )
                )
                alignedSReversed.append(s[currI - 1])
                alignedTReversed.append(t[currJ - 1])
                currI--
                currJ--
            } else if (currI > 0 && dp[currI][currJ] == dp[currI - 1][currJ] + 1) {
                stepsReversed.add(
                    EditStep(
                        operation = "DELETE",
                        sourceChar = s[currI - 1],
                        targetChar = null,
                        cost = 1
                    )
                )
                alignedSReversed.append(s[currI - 1])
                alignedTReversed.append('-')
                currI--
            } else {
                stepsReversed.add(
                    EditStep(
                        operation = "INSERT",
                        sourceChar = null,
                        targetChar = t[currJ - 1],
                        cost = 1
                    )
                )
                alignedSReversed.append('-')
                alignedTReversed.append(t[currJ - 1])
                currJ--
            }
        }

        val editSteps = stepsReversed.reversed()
        val alignedSource = alignedSReversed.reverse().toString()
        val alignedTarget = alignedTReversed.reverse().toString()

        // Generate matrix visual grid
        val grid = buildString {
            append("      ε ")
            for (j in 0 until n) {
                append("  ${t[j]} ")
            }
            appendLine()
            for (i in 0..m) {
                val rowLabel = if (i == 0) " ε" else " ${s[i - 1]}"
                append("$rowLabel |")
                for (j in 0..n) {
                    append(String.format(Locale.US, "%3d ", dp[i][j]))
                }
                appendLine()
            }
        }

        val report = buildString {
            appendLine("LEVENSHTEIN DYNAMIC PROGRAMMING ANALYSIS")
            appendLine("--------------------------------------------------")
            appendLine("Source:      \"$s\" (length: $m)")
            appendLine("Target:      \"$t\" (length: $n)")
            appendLine("Distance:    $distance edit operations")
            appendLine(String.format(Locale.US, "Similarity:  %.2f%%", similarityScore))
            appendLine("--------------------------------------------------")
            appendLine("OPTIMAL ALIGNMENT:")
            appendLine("Source: $alignedSource")
            appendLine("Target: $alignedTarget")
            appendLine("--------------------------------------------------")
            appendLine("EDIT SCRIPT STEPS:")
            editSteps.forEachIndexed { idx, step ->
                appendLine(" #${idx + 1}: ${step.operation} '${step.sourceChar ?: ' '}' -> '${step.targetChar ?: ' '}' (cost ${step.cost})")
            }
            appendLine("--------------------------------------------------")
            appendLine("2D WAGNER-FISCHER COST MATRIX:")
            append(grid)
        }

        return ToolResult.Success(
            data = LevenshteinMatrixOutput(
                distance = distance,
                similarityScorePercent = similarityScore,
                sourceLength = m,
                targetLength = n,
                editSteps = editSteps,
                alignedSource = alignedSource,
                alignedTarget = alignedTarget,
                matrixGridText = grid,
                formattedReport = report,
                summary = "Distance: $distance | Sim: ${String.format(Locale.US, "%.1f", similarityScore)}%"
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Computed Levenshtein distance: $distance"
        )
    }
}
