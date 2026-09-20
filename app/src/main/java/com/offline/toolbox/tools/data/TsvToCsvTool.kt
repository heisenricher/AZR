package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class TsvCsvMode {
    TSV_TO_CSV,
    CSV_TO_TSV
}

data class TsvCsvInput(
    val content: String = "Name\tRole\tDepartment\nAlice\tArchitect\tEngineering\nBob\tDesigner\tProduct",
    val mode: TsvCsvMode = TsvCsvMode.TSV_TO_CSV
)

data class TsvCsvOutput(
    val convertedContent: String,
    val mode: TsvCsvMode,
    val rowCount: Int,
    val columnCount: Int,
    val formattedReport: String,
    val summary: String
)

class TsvToCsvTool : Tool<TsvCsvInput, TsvCsvOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "tsv_to_csv_tool",
        name = "TSV <-> CSV Bi-Directional Converter",
        description = "Bi-directional conversion between Tab-Separated Values (TSV) and Comma-Separated Values (CSV) with proper quote escaping.",
        category = ToolCategory.DATA,
        tags = listOf("tsv", "csv", "tab", "comma", "convert", "table", "excel", "sheets", "data"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "TableChart"
    )

    override suspend fun execute(input: TsvCsvInput): ToolResult<TsvCsvOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.content.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input tabular text cannot be empty.")
        }

        val lines = raw.lines()
        val convertedLines = mutableListOf<String>()
        var maxCols = 0

        for (line in lines) {
            if (line.isBlank()) continue
            val fields = if (input.mode == TsvCsvMode.TSV_TO_CSV) {
                parseTsvLine(line)
            } else {
                parseCsvLine(line)
            }

            if (fields.size > maxCols) maxCols = fields.size

            val outLine = if (input.mode == TsvCsvMode.TSV_TO_CSV) {
                formatCsvLine(fields)
            } else {
                formatTsvLine(fields)
            }
            convertedLines.add(outLine)
        }

        val resultStr = convertedLines.joinToString("\n")
        val report = buildString {
            appendLine("TSV <-> CSV CONVERSION AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Direction:     ${if (input.mode == TsvCsvMode.TSV_TO_CSV) "TSV → CSV" else "CSV → TSV"}")
            appendLine("Total Rows:    ${convertedLines.size}")
            appendLine("Total Columns: $maxCols")
            appendLine()
            appendLine("Output Preview:")
            appendLine(resultStr)
        }

        val summary = "Converted ${convertedLines.size} rows ($maxCols cols) to ${if (input.mode == TsvCsvMode.TSV_TO_CSV) "CSV" else "TSV"}"

        return ToolResult.Success(
            data = TsvCsvOutput(
                convertedContent = resultStr,
                mode = input.mode,
                rowCount = convertedLines.size,
                columnCount = maxCols,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun parseTsvLine(line: String): List<String> {
        return line.split("\t").map { it.trim() }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private fun formatCsvLine(fields: List<String>): String {
        return fields.joinToString(",") { field ->
            if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                "\"${field.replace("\"", "\"\"")}\""
            } else {
                field
            }
        }
    }

    private fun formatTsvLine(fields: List<String>): String {
        return fields.joinToString("\t") { field ->
            field.replace("\t", " ").replace("\n", " ")
        }
    }
}
