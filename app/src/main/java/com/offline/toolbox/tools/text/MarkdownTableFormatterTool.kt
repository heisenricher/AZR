package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class TableAlignment {
    LEFT,
    CENTER,
    RIGHT,
    AUTO
}

data class TableFormatterInput(
    val content: String = """
        Name | Role | Salary | Department
        Alice | Senior Architect | $160,000 | Engineering
        Bob | Product Designer | $125,000 | Design
        Charlie | SecOps Lead | $150,000 | Security
    """.trimIndent(),
    val alignment: TableAlignment = TableAlignment.AUTO
)

data class TableFormatterOutput(
    val formattedTable: String,
    val rowCount: Int,
    val columnCount: Int,
    val formattedReport: String,
    val summary: String
)

class MarkdownTableFormatterTool : Tool<TableFormatterInput, TableFormatterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "markdown_table_formatter_tool",
        name = "Markdown Table Formatter & Padded Aligner",
        description = "Format raw tabular data, CSV, TSV, or misaligned pipes into perfectly padded, aligned GitHub-Flavored Markdown tables.",
        category = ToolCategory.TEXT,
        tags = listOf("markdown", "table", "format", "align", "pad", "gfm", "ascii", "csv", "tsv"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "TableChart"
    )

    override suspend fun execute(input: TableFormatterInput): ToolResult<TableFormatterOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.content.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input table content cannot be empty.")
        }

        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val parsedRows = mutableListOf<List<String>>()

        for (line in lines) {
            // Ignore separator rows like |---|---|
            if (line.matches(Regex("^[|\\s\\-:]+$"))) continue

            val cells: List<String> = when {
                line.contains("|") -> {
                    line.split("|")
                        .map { it.trim() }
                        .filterIndexed { idx, s -> !(idx == 0 && s.isEmpty()) }
                        .let { if (it.isNotEmpty() && it.last().isEmpty()) it.dropLast(1) else it }
                }
                line.contains("\t") -> line.split("\t").map { it.trim() }
                line.contains(",") -> line.split(",").map { it.trim() }
                else -> listOf(line)
            }
            if (cells.isNotEmpty()) {
                parsedRows.add(cells)
            }
        }

        if (parsedRows.isEmpty()) {
            return ToolResult.Failure("No valid rows could be extracted.")
        }

        val numCols = parsedRows.maxOf { it.size }
        val colWidths = IntArray(numCols) { 3 }

        // Find max width for each column
        for (row in parsedRows) {
            for (c in 0 until numCols) {
                val text = if (c < row.size) row[c] else ""
                colWidths[c] = maxOf(colWidths[c], text.length)
            }
        }

        // Build header, separator, and data rows
        val sb = StringBuilder()

        fun formatRow(cells: List<String>): String {
            return "| " + (0 until numCols).joinToString(" | ") { c ->
                val text = if (c < cells.size) cells[c] else ""
                val pad = colWidths[c] - text.length
                when (input.alignment) {
                    TableAlignment.RIGHT -> " ".repeat(pad) + text
                    TableAlignment.CENTER -> {
                        val leftPad = pad / 2
                        val rightPad = pad - leftPad
                        " ".repeat(leftPad) + text + " ".repeat(rightPad)
                    }
                    TableAlignment.LEFT -> text + " ".repeat(pad)
                    TableAlignment.AUTO -> {
                        // Numeric right align, text left align
                        if (text.removePrefix("$").removeSuffix("%").replace(",", "").toDoubleOrNull() != null) {
                            " ".repeat(pad) + text
                        } else {
                            text + " ".repeat(pad)
                        }
                    }
                }
            } + " |"
        }

        // Header
        sb.appendLine(formatRow(parsedRows[0]))

        // Separator
        val sepRow = "| " + (0 until numCols).joinToString(" | ") { c ->
            val w = colWidths[c]
            when (input.alignment) {
                TableAlignment.CENTER -> ":" + "-".repeat(maxOf(1, w - 2)) + ":"
                TableAlignment.RIGHT -> "-".repeat(maxOf(2, w - 1)) + ":"
                TableAlignment.LEFT, TableAlignment.AUTO -> ":" + "-".repeat(maxOf(2, w - 1))
            }
        } + " |"
        sb.appendLine(sepRow)

        // Data Rows
        for (i in 1 until parsedRows.size) {
            sb.appendLine(formatRow(parsedRows[i]))
        }

        val resultTable = sb.toString().trimEnd()

        val report = buildString {
            appendLine("GITHUB-FLAVORED MARKDOWN TABLE")
            appendLine("--------------------------------------------------")
            appendLine("Rows:        ${parsedRows.size}")
            appendLine("Columns:     $numCols")
            appendLine("Alignment:   ${input.alignment.name}")
            appendLine()
            appendLine(resultTable)
        }

        val summary = "Formatted ${parsedRows.size}x$numCols Markdown table"

        return ToolResult.Success(
            data = TableFormatterOutput(
                formattedTable = resultTable,
                rowCount = parsedRows.size,
                columnCount = numCols,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
