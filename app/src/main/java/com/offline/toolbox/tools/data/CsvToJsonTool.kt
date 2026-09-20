package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONArray
import org.json.JSONObject

enum class CsvDelimiter(val char: Char, val label: String) {
    AUTO(',', "Auto-detect"),
    COMMA(',', "Comma (,)"),
    TAB('\t', "Tab (\\t)"),
    SEMICOLON(';', "Semicolon (;)"),
    PIPE('|', "Pipe (|)")
}

data class CsvToJsonInput(
    val csvText: String,
    val delimiter: CsvDelimiter = CsvDelimiter.AUTO,
    val indentSpaces: Int = 2
)

data class CsvToJsonOutput(
    val jsonString: String,
    val rowCount: Int,
    val columnCount: Int,
    val summary: String
)

class CsvToJsonTool : Tool<CsvToJsonInput, CsvToJsonOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "csv_to_json",
        name = "CSV to JSON Converter",
        description = "Parse CSV, TSV, or delimited text into structured JSON arrays with quote escaping.",
        category = ToolCategory.DATA,
        tags = listOf("csv", "json", "convert", "tsv", "delimited", "table", "data", "parser"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.JSON,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "TableChart"
    )

    override suspend fun execute(input: CsvToJsonInput): ToolResult<CsvToJsonOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.csvText.trim()

        if (text.isEmpty()) {
            return ToolResult.Failure(
                message = "Input CSV is empty.",
                userGuidance = "Paste CSV data containing a header row and data rows."
            )
        }

        val delim = if (input.delimiter == CsvDelimiter.AUTO) detectDelimiter(text) else input.delimiter.char

        return try {
            val rows = parseCsvRows(text, delim)
            if (rows.isEmpty()) {
                return ToolResult.Failure("No valid rows found in CSV.")
            }

            val headers = rows[0]
            if (headers.isEmpty()) {
                return ToolResult.Failure("Header row has no columns.")
            }

            val jsonArray = JSONArray()
            for (i in 1 until rows.size) {
                val row = rows[i]
                val obj = JSONObject()
                for (j in headers.indices) {
                    val key = headers[j]
                    val value = if (j < row.size) row[j] else ""
                    // Try parsing numbers / booleans
                    when {
                        value.equals("true", ignoreCase = true) -> obj.put(key, true)
                        value.equals("false", ignoreCase = true) -> obj.put(key, false)
                        value.toLongOrNull() != null -> obj.put(key, value.toLong())
                        value.toDoubleOrNull() != null -> obj.put(key, value.toDouble())
                        else -> obj.put(key, value)
                    }
                }
                jsonArray.put(obj)
            }

            val jsonFormatted = jsonArray.toString(input.indentSpaces)
            val dataRows = rows.size - 1
            val summary = "Converted $dataRows row(s) and ${headers.size} column(s) to JSON"

            ToolResult.Success(
                data = CsvToJsonOutput(
                    jsonString = jsonFormatted,
                    rowCount = dataRows,
                    columnCount = headers.size,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Failed to parse CSV: ${e.message}",
                cause = e
            )
        }
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lines().firstOrNull() ?: ""
        val commaCount = firstLine.count { it == ',' }
        val tabCount = firstLine.count { it == '\t' }
        val semiCount = firstLine.count { it == ';' }
        val pipeCount = firstLine.count { it == '|' }

        return when {
            tabCount > commaCount && tabCount > semiCount -> '\t'
            semiCount > commaCount && semiCount > pipeCount -> ';'
            pipeCount > commaCount -> '|'
            else -> ','
        }
    }

    private fun parseCsvRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < text.length) {
            val c = text[i]

            if (c == '"') {
                if (insideQuotes && i + 1 < text.length && text[i + 1] == '"') {
                    currentField.append('"')
                    i++ // skip escaped quote
                } else {
                    insideQuotes = !insideQuotes
                }
            } else if (c == delimiter && !insideQuotes) {
                currentRow.add(currentField.toString().trim())
                currentField.clear()
            } else if ((c == '\n' || c == '\r') && !insideQuotes) {
                if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
                    i++
                }
                currentRow.add(currentField.toString().trim())
                currentField.clear()
                if (currentRow.any { it.isNotEmpty() }) {
                    rows.add(currentRow.toList())
                }
                currentRow.clear()
            } else {
                currentField.append(c)
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotEmpty() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }
}
