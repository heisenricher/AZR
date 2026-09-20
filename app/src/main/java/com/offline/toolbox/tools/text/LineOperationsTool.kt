package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class LineOperation(val displayName: String) {
    SORT_AZ("Sort Alphabetically (A → Z)"),
    SORT_ZA("Sort Alphabetically (Z → A)"),
    SORT_LENGTH_ASC("Sort by Length (Shortest first)"),
    SORT_LENGTH_DESC("Sort by Length (Longest first)"),
    REMOVE_DUPLICATES("Remove Duplicate Lines"),
    REVERSE_ORDER("Reverse Line Order"),
    ADD_LINE_NUMBERS("Add Line Numbers")
}

data class LineOperationsInput(
    val text: String,
    val operation: LineOperation = LineOperation.SORT_AZ,
    val ignoreCase: Boolean = true
)

class LineOperationsTool : Tool<LineOperationsInput, String> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "line_operations",
        name = "Sort & Line Operations",
        description = "Sort lines A-Z or by length, remove duplicate lines, reverse order, and number lines.",
        category = ToolCategory.TEXT,
        tags = listOf("sort", "lines", "duplicates", "unique", "reverse", "numbering", "order"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FormatLineSpacing"
    )

    override suspend fun execute(input: LineOperationsInput): ToolResult<String> {
        val startTime = System.currentTimeMillis()
        if (input.text.isEmpty()) {
            return ToolResult.Success("", System.currentTimeMillis() - startTime, "Empty input")
        }

        val lines = input.text.lines()
        val resultLines = when (input.operation) {
            LineOperation.SORT_AZ -> {
                if (input.ignoreCase) lines.sortedWith(String.CASE_INSENSITIVE_ORDER)
                else lines.sorted()
            }
            LineOperation.SORT_ZA -> {
                if (input.ignoreCase) lines.sortedWith(String.CASE_INSENSITIVE_ORDER.reversed())
                else lines.sortedDescending()
            }
            LineOperation.SORT_LENGTH_ASC -> lines.sortedBy { it.length }
            LineOperation.SORT_LENGTH_DESC -> lines.sortedByDescending { it.length }
            LineOperation.REMOVE_DUPLICATES -> {
                if (input.ignoreCase) {
                    val seen = mutableSetOf<String>()
                    lines.filter { seen.add(it.lowercase()) }
                } else {
                    lines.distinct()
                }
            }
            LineOperation.REVERSE_ORDER -> lines.reversed()
            LineOperation.ADD_LINE_NUMBERS -> {
                val padLength = lines.size.toString().length
                lines.mapIndexed { idx, line ->
                    val num = (idx + 1).toString().padStart(padLength, ' ')
                    "$num. $line"
                }
            }
        }

        val output = resultLines.joinToString("\n")
        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "${input.operation.displayName} (${resultLines.size} lines)"
        )
    }
}
