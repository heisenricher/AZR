package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class ColumnProfile(
    val name: String,
    val inferredType: String,
    val totalRows: Int,
    val nonNullCount: Int,
    val nullCount: Int,
    val uniqueValuesCount: Int,
    val minNumeric: Double?,
    val maxNumeric: Double?,
    val meanNumeric: Double?
)

data class CsvStatsInput(
    val csvData: String = ""
)

data class CsvStatsOutput(
    val totalRowCount: Int,
    val totalColumnCount: Int,
    val columns: List<ColumnProfile>,
    val formattedReport: String,
    val summary: String
)

class CsvStatsTool : Tool<CsvStatsInput, CsvStatsOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "csv_stats_tool",
        name = "CSV Data Profiler & Column Statistics",
        description = "Analyze CSV datasets with column data type inference, null counts, unique cardinality, and numeric averages.",
        category = ToolCategory.DATA,
        tags = listOf("csv", "stats", "profile", "data", "summary", "columns", "mean", "min", "max", "nulls"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "BarChart"
    )

    override suspend fun execute(input: CsvStatsInput): ToolResult<CsvStatsOutput> {
        val startTime = System.currentTimeMillis()

        if (input.csvData.isBlank()) {
            return ToolResult.Failure(
                message = "CSV input data is empty.",
                userGuidance = "Provide CSV data to profile columns and statistical distributions."
            )
        }

        return try {
            val rows = parseCsv(input.csvData)
            if (rows.isEmpty()) {
                return ToolResult.Failure("No valid CSV rows parsed.")
            }

            val headers = rows.first()
            val dataRows = rows.drop(1)
            val numRows = dataRows.size

            val profiles = headers.mapIndexed { colIdx, headerName ->
                val values = dataRows.map { if (colIdx < it.size) it[colIdx] else "" }
                val nonNullValues = values.filter { it.isNotBlank() }
                val nullCount = values.size - nonNullValues.size
                val uniqueCount = nonNullValues.toSet().size

                val numericValues = nonNullValues.mapNotNull { it.toDoubleOrNull() }
                val isNumeric = numericValues.size == nonNullValues.size && nonNullValues.isNotEmpty()
                val isBoolean = nonNullValues.all { it.equals("true", ignoreCase = true) || it.equals("false", ignoreCase = true) } && nonNullValues.isNotEmpty()

                val inferredType = when {
                    isBoolean -> "Boolean"
                    isNumeric && numericValues.all { it % 1.0 == 0.0 } -> "Integer"
                    isNumeric -> "Decimal / Float"
                    else -> "Text / String"
                }

                val minVal = if (isNumeric) numericValues.minOrNull() else null
                val maxVal = if (isNumeric) numericValues.maxOrNull() else null
                val meanVal = if (isNumeric) numericValues.average() else null

                ColumnProfile(
                    name = headerName,
                    inferredType = inferredType,
                    totalRows = numRows,
                    nonNullCount = nonNullValues.size,
                    nullCount = nullCount,
                    uniqueValuesCount = uniqueCount,
                    minNumeric = minVal,
                    maxNumeric = maxVal,
                    meanNumeric = meanVal
                )
            }

            val report = buildString {
                appendLine("CSV DATASET PROFILE & STATISTICS")
                appendLine("Total Rows:    $numRows data rows")
                appendLine("Total Columns: ${headers.size}")
                appendLine("--------------------------------")
                appendLine("%-16s | %-12s | %-6s | %-6s | %-8s | %-8s | %-8s".format(
                    "Column", "Type", "Nulls", "Uniq", "Min", "Max", "Mean"
                ))
                appendLine("-----------------+--------------+--------+--------+----------+----------+----------")
                profiles.forEach { p ->
                    val minStr = p.minNumeric?.let { formatNum(it) } ?: "-"
                    val maxStr = p.maxNumeric?.let { formatNum(it) } ?: "-"
                    val meanStr = p.meanNumeric?.let { formatNum(it) } ?: "-"

                    appendLine("%-16s | %-12s | %-6d | %-6d | %-8s | %-8s | %-8s".format(
                        Locale.US,
                        p.name.take(16),
                        p.inferredType.take(12),
                        p.nullCount,
                        p.uniqueValuesCount,
                        minStr,
                        maxStr,
                        meanStr
                    ))
                }
            }

            val summary = "Profiled $numRows rows x ${headers.size} columns"

            ToolResult.Success(
                data = CsvStatsOutput(
                    totalRowCount = numRows,
                    totalColumnCount = headers.size,
                    columns = profiles,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("CSV statistical profiling error: ${e.message}", cause = e)
        }
    }

    private fun formatNum(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else "%.2f".format(Locale.US, v)
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
}
