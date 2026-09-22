package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class TsvAggregateInput(
    val tsvContent: String = """
category	item	quantity	price
Electronics	Laptop	5	999.50
Furniture	Chair	12	89.00
Electronics	Mouse	25	29.99
Furniture	Desk	4	250.00
Electronics	Monitor	8	199.99
Office	Paper	50	5.50
Office	Pen	100	1.25
    """.trimIndent(),
    val filterColumn: String = "",
    val filterOperator: String = "ALL", // ALL, EQUALS, GREATER_THAN, LESS_THAN, CONTAINS
    val filterValue: String = "",
    val aggregateColumn: String = "price",
    val groupByColumn: String = "category"
)

data class GroupSummary(
    val groupKey: String,
    val rowCount: Int,
    val sum: Double,
    val mean: Double,
    val min: Double,
    val max: Double
)

data class TsvAggregateOutput(
    val totalInputRows: Int,
    val filteredRowCount: Int,
    val headers: List<String>,
    val overallCount: Int,
    val overallSum: Double,
    val overallMean: Double,
    val overallMedian: Double,
    val overallMin: Double,
    val overallMax: Double,
    val groupSummaries: List<GroupSummary>,
    val formattedReport: String,
    val summary: String
)

class TsvFilterAggregateTool : Tool<TsvAggregateInput, TsvAggregateOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "tsv_filter_aggregate_tool",
        name = "TSV Stream Filter & Aggregator",
        description = "Filter and aggregate Tab-Separated Values (TSV) tabular datasets with column-wise statistics (Sum, Mean, Median, Min, Max) and Group-By summaries.",
        category = ToolCategory.DATA,
        tags = listOf("tsv", "tab", "filter", "aggregate", "group by", "statistics", "data", "csv", "table"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Table"
    )

    override suspend fun execute(input: TsvAggregateInput): ToolResult<TsvAggregateOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.tsvContent.trim()
        if (text.isBlank()) {
            return ToolResult.Failure("TSV content cannot be empty.")
        }

        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 2) {
            return ToolResult.Failure("TSV content must contain a header line and at least one data row.")
        }

        val headers = lines[0].split("\t").map { it.trim() }
        val headerMap = headers.withIndex().associate { it.value.lowercase(Locale.US) to it.index }

        val filterColIdx = if (input.filterColumn.isNotBlank()) {
            headerMap[input.filterColumn.trim().lowercase(Locale.US)] ?: -1
        } else -1

        val aggColIdx = headerMap[input.aggregateColumn.trim().lowercase(Locale.US)] ?: -1
        if (aggColIdx == -1) {
            return ToolResult.Failure("Aggregate column '${input.aggregateColumn}' not found in headers: ${headers.joinToString(", ")}.")
        }

        val groupColIdx = if (input.groupByColumn.isNotBlank()) {
            headerMap[input.groupByColumn.trim().lowercase(Locale.US)] ?: -1
        } else -1

        val dataRows = lines.drop(1).map { it.split("\t") }

        // Filter rows
        val op = input.filterOperator.trim().uppercase(Locale.US)
        val fVal = input.filterValue.trim()
        val fNum = fVal.toDoubleOrNull()

        val filteredRows = dataRows.filter { row ->
            if (filterColIdx == -1 || op == "ALL") true
            else {
                val cell = row.getOrNull(filterColIdx)?.trim() ?: ""
                val cellNum = cell.toDoubleOrNull()
                when (op) {
                    "EQUALS" -> cell.equals(fVal, ignoreCase = true)
                    "CONTAINS" -> cell.contains(fVal, ignoreCase = true)
                    "GREATER_THAN" -> if (cellNum != null && fNum != null) cellNum > fNum else false
                    "LESS_THAN" -> if (cellNum != null && fNum != null) cellNum < fNum else false
                    else -> true
                }
            }
        }

        if (filteredRows.isEmpty()) {
            return ToolResult.Failure("No rows matched the filter criteria.")
        }

        val aggValues = mutableListOf<Double>()
        for (r in filteredRows) {
            val v = r.getOrNull(aggColIdx)?.trim()?.toDoubleOrNull()
            if (v != null) aggValues.add(v)
        }

        if (aggValues.isEmpty()) {
            return ToolResult.Failure("No numeric values found in column '${headers[aggColIdx]}'.")
        }

        aggValues.sort()
        val count = aggValues.size
        val sum = aggValues.sum()
        val mean = sum / count
        val median = if (count % 2 == 1) aggValues[count / 2] else (aggValues[count / 2 - 1] + aggValues[count / 2]) / 2.0
        val min = aggValues.first()
        val max = aggValues.last()

        // Group-By calculations
        val groupSummaries = mutableListOf<GroupSummary>()
        if (groupColIdx != -1) {
            val grouped = filteredRows.groupBy { it.getOrNull(groupColIdx)?.trim() ?: "UNKNOWN" }
            for ((key, rows) in grouped) {
                val gVals = rows.mapNotNull { it.getOrNull(aggColIdx)?.trim()?.toDoubleOrNull() }
                if (gVals.isNotEmpty()) {
                    val gSum = gVals.sum()
                    groupSummaries.add(
                        GroupSummary(
                            groupKey = key,
                            rowCount = gVals.size,
                            sum = gSum,
                            mean = gSum / gVals.size,
                            min = gVals.minOrNull() ?: 0.0,
                            max = gVals.maxOrNull() ?: 0.0
                        )
                    )
                }
            }
            groupSummaries.sortByDescending { it.sum }
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== TSV AGGREGATION & FILTER REPORT ===")
            appendLine("Headers:          ${headers.joinToString(" | ")}")
            appendLine("Total Input Rows: ${dataRows.size}")
            appendLine("Filtered Rows:    ${filteredRows.size}")
            appendLine("Target Column:    ${headers[aggColIdx]}")
            appendLine("----------------------------------------")
            appendLine("OVERALL METRICS:")
            appendLine("  Count:          $count")
            appendLine("  Sum:            ${String.format(Locale.US, "%,.2f", sum)}")
            appendLine("  Mean (Average): ${String.format(Locale.US, "%.2f", mean)}")
            appendLine("  Median:         ${String.format(Locale.US, "%.2f", median)}")
            appendLine("  Min:            ${String.format(Locale.US, "%.2f", min)}")
            appendLine("  Max:            ${String.format(Locale.US, "%.2f", max)}")

            if (groupSummaries.isNotEmpty()) {
                appendLine("----------------------------------------")
                appendLine("GROUP-BY: ${headers[groupColIdx]}")
                appendLine("%-16s | %-6s | %-12s | %-10s | %-8s | %-8s".format(Locale.US, "Group", "Rows", "Sum", "Mean", "Min", "Max"))
                groupSummaries.forEach { g ->
                    appendLine("%-16s | %-6d | %-12.2f | %-10.2f | %-8.2f | %-8.2f".format(Locale.US, g.groupKey, g.rowCount, g.sum, g.mean, g.min, g.max))
                }
            }
        }

        return ToolResult.Success(
            data = TsvAggregateOutput(
                totalInputRows = dataRows.size,
                filteredRowCount = filteredRows.size,
                headers = headers,
                overallCount = count,
                overallSum = sum,
                overallMean = mean,
                overallMedian = median,
                overallMin = min,
                overallMax = max,
                groupSummaries = groupSummaries,
                formattedReport = report,
                summary = "Aggregated ${filteredRows.size} TSV rows: Sum = ${String.format(Locale.US, "%,.2f", sum)}, Mean = ${String.format(Locale.US, "%.2f", mean)}."
            ),
            executionTimeMs = elapsed,
            summary = "Sum: ${String.format(Locale.US, "%,.2f", sum)}, Mean: ${String.format(Locale.US, "%.2f", mean)}"
        )
    }
}
