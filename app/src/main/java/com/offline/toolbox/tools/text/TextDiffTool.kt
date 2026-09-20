package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class TextDiffInput(
    val originalText: String,
    val modifiedText: String,
    val ignoreWhitespace: Boolean = false
)

data class TextDiffOutput(
    val formattedDiff: String,
    val additions: Int,
    val deletions: Int,
    val unchanged: Int,
    val summary: String
)

class TextDiffTool : Tool<TextDiffInput, TextDiffOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "text_diff",
        name = "Text Diff Comparator",
        description = "Compare two text snippets line-by-line and inspect additions, deletions, and differences.",
        category = ToolCategory.TEXT,
        tags = listOf("diff", "compare", "compare text", "delta", "changes", "git diff"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Difference"
    )

    override suspend fun execute(input: TextDiffInput): ToolResult<TextDiffOutput> {
        val startTime = System.currentTimeMillis()

        val origLines = input.originalText.lines()
        val modLines = input.modifiedText.lines()

        val diffLines = mutableListOf<String>()
        var additions = 0
        var deletions = 0
        var unchanged = 0

        // Longest Common Subsequence (LCS) line-by-line diff algorithm
        val n = origLines.size
        val m = modLines.size
        val dp = Array(n + 1) { IntArray(m + 1) }

        for (i in 0 until n) {
            for (j in 0 until m) {
                val line1 = if (input.ignoreWhitespace) origLines[i].trim() else origLines[i]
                val line2 = if (input.ignoreWhitespace) modLines[j].trim() else modLines[j]
                if (line1 == line2) {
                    dp[i + 1][j + 1] = dp[i][j] + 1
                } else {
                    dp[i + 1][j + 1] = maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        var i = n
        var j = m
        val reverseDiff = mutableListOf<String>()

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val line1 = if (input.ignoreWhitespace) origLines[i - 1].trim() else origLines[i - 1]
                val line2 = if (input.ignoreWhitespace) modLines[j - 1].trim() else modLines[j - 1]
                if (line1 == line2) {
                    reverseDiff.add("  ${origLines[i - 1]}")
                    unchanged++
                    i--
                    j--
                    continue
                }
            }
            if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                reverseDiff.add("+ ${modLines[j - 1]}")
                additions++
                j--
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                reverseDiff.add("- ${origLines[i - 1]}")
                deletions++
                i--
            }
        }

        diffLines.addAll(reverseDiff.reversed())
        val outputString = diffLines.joinToString("\n")
        val summary = "+$additions / -$deletions ($unchanged unchanged)"

        return ToolResult.Success(
            data = TextDiffOutput(
                formattedDiff = outputString,
                additions = additions,
                deletions = deletions,
                unchanged = unchanged,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
