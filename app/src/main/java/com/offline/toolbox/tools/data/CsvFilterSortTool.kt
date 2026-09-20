package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class CsvFilterOperator(val label: String) {
    NONE("No Filter (Keep All)"),
    CONTAINS("Contains"),
    EQUALS("Equals"),
    STARTS_WITH("Starts With"),
    ENDS_WITH("Ends With"),
    GREATER_THAN("Greater Than (Numeric >)"),
    LESS_THAN("Less Than (Numeric <)"),
    REGEX("Matches Regex")
}

enum class CsvSortOrder(val label: String) {
    NONE("No Sort (Original Order)"),
    ASCENDING_TEXT("Sort A to Z (Text)"),
    DESCENDING_TEXT("Sort Z to A (Text)"),
    ASCENDING_NUMERIC("Sort 0 to 9 (Numeric)"),
    DESCENDING_NUMERIC("Sort 9 to 0 (Numeric)")
}

data class CsvFilterSortInput(
    val csvData: String = "",
    val filterColumn: String = "",
    val filterOperator: CsvFilterOperator = CsvFilterOperator.NONE,
    val filterValue: String = "",
    val sortColumn: String = "",
    val sortOrder: CsvSortOrder = CsvSortOrder.NONE,
    val deduplicateRows: Boolean = false,
    val projectColumns: String = "" // comma-separated subset of column names to keep
)

data class CsvFilterSortOutput(
    val processedCsv: String,
    val inputRowCount: Int,
    val outputRowCount: Int,
    val columnNames: List<String>,
    val tablePreview: String,
    val summary: String
)

class CsvFilterSortTool : Tool<CsvFilterSortInput, CsvFilterSortOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "csv_filter_sort_tool",
        name = "CSV Filter, Query & Sort Engine",
        description = "Query, filter rows by numeric or text criteria, sort columns, deduplicate, and reshape CSV data offline.",
        category = ToolCategory.DATA,
        tags = listOf("csv", "filter", "sort", "query", "deduplicate", "table", "data", "columns"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FilterList"
    )

    override suspend fun execute(input: CsvFilterSortInput): ToolResult<CsvFilterSortOutput> {
        val startTime = System.currentTimeMillis()

        if (input.csvData.isBlank()) {
            return ToolResult.Failure(
                message = "CSV input data is empty.",
                userGuidance = "Provide CSV data with headers to filter and sort."
            )
        }

        return try {
            val parsedTable = parseCsv(input.csvData)
            if (parsedTable.isEmpty()) {
                return ToolResult.Failure("No valid CSV rows parsed.")
            }

            val headers = parsedTable.first()
            var rows = parsedTable.drop(1)
            val initialRowCount = rows.size

            // 1. Row Filtering
            if (input.filterOperator != CsvFilterOperator.NONE && input.filterColumn.isNotBlank()) {
                val colIdx = findColumnIndex(headers, input.filterColumn)
                if (colIdx != -1) {
                    rows = rows.filter { row ->
                        val cellValue = if (colIdx < row.size) row[colIdx].trim() else ""
                        matchesFilter(cellValue, input.filterOperator, input.filterValue.trim())
                    }
                }
            }

            // 2. Row Deduplication
            if (input.deduplicateRows) {
                val seen = mutableSetOf<List<String>>()
                rows = rows.filter { seen.add(it) }
            }

            // 3. Sorting
            if (input.sortOrder != CsvSortOrder.NONE && input.sortColumn.isNotBlank()) {
                val colIdx = findColumnIndex(headers, input.sortColumn)
                if (colIdx != -1) {
                    rows = when (input.sortOrder) {
                        CsvSortOrder.ASCENDING_TEXT -> rows.sortedBy { if (colIdx < it.size) it[colIdx].lowercase(Locale.US) else "" }
                        CsvSortOrder.DESCENDING_TEXT -> rows.sortedByDescending { if (colIdx < it.size) it[colIdx].lowercase(Locale.US) else "" }
                        CsvSortOrder.ASCENDING_NUMERIC -> rows.sortedBy { if (colIdx < it.size) it[colIdx].toDoubleOrNull() ?: Double.MAX_VALUE else Double.MAX_VALUE }
                        CsvSortOrder.DESCENDING_NUMERIC -> rows.sortedByDescending { if (colIdx < it.size) it[colIdx].toDoubleOrNull() ?: Double.MIN_VALUE else Double.MIN_VALUE }
                        CsvSortOrder.NONE -> rows
                    }
                }
            }

            // 4. Column Projection
            val finalHeaders: List<String>
            val finalRows: List<List<String>>

            val projectionList = input.projectColumns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (projectionList.isNotEmpty()) {
                val keptIndices = projectionList.mapNotNull { colName ->
                    val idx = findColumnIndex(headers, colName)
                    if (idx != -1) idx else null
                }
                if (keptIndices.isNotEmpty()) {
                    finalHeaders = keptIndices.map { headers[it] }
                    finalRows = rows.map { row ->
                        keptIndices.map { idx -> if (idx < row.size) row[idx] else "" }
                    }
                } else {
                    finalHeaders = headers
                    finalRows = rows
                }
            } else {
                finalHeaders = headers
                finalRows = rows
            }

            // 5. Build output CSV
            val outputCsv = buildString {
                appendLine(finalHeaders.joinToString(",") { escapeCsvCell(it) })
                finalRows.forEach { row ->
                    appendLine(row.joinToString(",") { escapeCsvCell(it) })
                }
            }

            // 6. Build preview table
            val previewTable = buildTablePreview(finalHeaders, finalRows.take(10))
            val summary = "Filtered CSV: $initialRowCount rows -> ${finalRows.size} rows (${finalHeaders.size} cols)"

            ToolResult.Success(
                data = CsvFilterSortOutput(
                    processedCsv = outputCsv.trimEnd(),
                    inputRowCount = initialRowCount,
                    outputRowCount = finalRows.size,
                    columnNames = finalHeaders,
                    tablePreview = previewTable,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("CSV Processing failed: ${e.message}", cause = e)
        }
    }

    private fun findColumnIndex(headers: List<String>, colName: String): Int {
        val trimmed = colName.trim()
        val directIndex = headers.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
        if (directIndex != -1) return directIndex
        return trimmed.toIntOrNull()?.let { if (it in headers.indices) it else null } ?: -1
    }

    private fun matchesFilter(cell: String, op: CsvFilterOperator, query: String): Boolean {
        return when (op) {
            CsvFilterOperator.NONE -> true
            CsvFilterOperator.CONTAINS -> cell.contains(query, ignoreCase = true)
            CsvFilterOperator.EQUALS -> cell.equals(query, ignoreCase = true)
            CsvFilterOperator.STARTS_WITH -> cell.startsWith(query, ignoreCase = true)
            CsvFilterOperator.ENDS_WITH -> cell.endsWith(query, ignoreCase = true)
            CsvFilterOperator.GREATER_THAN -> {
                val num = cell.toDoubleOrNull()
                val qNum = query.toDoubleOrNull()
                if (num != null && qNum != null) num > qNum else false
            }
            CsvFilterOperator.LESS_THAN -> {
                val num = cell.toDoubleOrNull()
                val qNum = query.toDoubleOrNull()
                if (num != null && qNum != null) num < qNum else false
            }
            CsvFilterOperator.REGEX -> {
                try {
                    Regex(query, RegexOption.IGNORE_CASE).containsMatchIn(cell)
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    private fun parseCsv(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentField = StringBuilder()
        var currentRow = mutableListOf<String>()
        var inQuotes = false
        var i = 0

        while (i < csv.length) {
            val c = csv[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length && csv[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    '\r' -> {}
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow)
                        }
                        currentRow = mutableListOf()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow)
            }
        }

        return rows
    }

    private fun escapeCsvCell(cell: String): String {
        return if (cell.contains(',') || cell.contains('"') || cell.contains('\n')) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }
    }

    private fun buildTablePreview(headers: List<String>, sampleRows: List<List<String>>): String {
        val colWidths = headers.mapIndexed { idx, h ->
            val maxRowVal = sampleRows.mapNotNull { if (idx < it.size) it[idx].length else null }.maxOrNull() ?: 0
            maxOf(h.length, maxRowVal).coerceIn(4, 30)
        }

        return buildString {
            // Header
            appendLine(headers.mapIndexed { i, h -> h.padEnd(colWidths[i]) }.joinToString(" | "))
            // Separator
            appendLine(colWidths.joinToString("-+-") { "-".repeat(it) })
            // Rows
            sampleRows.forEach { row ->
                appendLine(headers.indices.map { i ->
                    val cell = if (i < row.size) row[i] else ""
                    val truncated = if (cell.length > colWidths[i]) cell.take(colWidths[i] - 3) + "..." else cell
                    truncated.padEnd(colWidths[i])
                }.joinToString(" | "))
            }
        }
    }
}
