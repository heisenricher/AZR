package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class SqlIndexAdvisorInput(
    val sqlQuery: String = """
        SELECT u.id, u.email, o.order_date, o.total_amount
        FROM users u
        INNER JOIN orders o ON u.id = o.user_id
        WHERE u.status = 'active'
          AND o.order_date >= '2026-01-01'
        ORDER BY o.order_date DESC;
    """.trimIndent(),
    val dialect: String = "SQLITE" // SQLITE, POSTGRES, MYSQL
)

data class SuggestedIndex(
    val tableName: String,
    val columns: List<String>,
    val indexType: String,
    val ddlStatement: String,
    val rationale: String
)

data class QueryOptimizationFinding(
    val severity: String, // INFO, WARNING, CRITICAL
    val message: String,
    val recommendation: String
)

data class SqlIndexAdvisorOutput(
    val targetDialect: String,
    val detectedTables: List<String>,
    val whereFilterColumns: List<String>,
    val joinColumns: List<String>,
    val orderGroupColumns: List<String>,
    val suggestedIndexes: List<SuggestedIndex>,
    val findings: List<QueryOptimizationFinding>,
    val formattedReport: String,
    val summary: String
)

class SqlIndexAdvisorTool : Tool<SqlIndexAdvisorInput, SqlIndexAdvisorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "sql_index_advisor_tool",
        name = "SQL Query Index Advisor & DDL Generator",
        description = "Analyze SQL queries (WHERE, JOIN, ORDER BY, GROUP BY) to suggest optimal composite B-Tree indexes, detect unindexed full table scans, and generate CREATE INDEX statements.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("sql", "index", "database", "sqlite", "postgres", "mysql", "query optimization", "btree", "performance", "explain"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Database"
    )

    override suspend fun execute(input: SqlIndexAdvisorInput): ToolResult<SqlIndexAdvisorOutput> {
        val startTime = System.currentTimeMillis()
        val query = input.sqlQuery.trim()
        if (query.isBlank()) {
            return ToolResult.Failure("SQL query cannot be empty.")
        }

        val dialect = input.dialect.trim().uppercase(Locale.US)
        val upperQuery = query.uppercase(Locale.US)

        // Parse tables
        val tableMatches = Regex("\\b(?:FROM|JOIN)\\s+([a-zA-Z0-9_]+)(?:\\s+(?:AS\\s+)?([a-zA-Z0-9_]+))?", RegexOption.IGNORE_CASE)
            .findAll(query)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        val detectedTables = if (tableMatches.isNotEmpty()) tableMatches else listOf("main_table")

        // Parse WHERE clause columns
        val whereFilterCols = mutableListOf<String>()
        val findings = mutableListOf<QueryOptimizationFinding>()

        val whereIdx = upperQuery.indexOf("WHERE")
        val orderIdx = upperQuery.indexOf("ORDER BY")
        val groupIdx = upperQuery.indexOf("GROUP BY")

        val endWhereIdx = when {
            groupIdx != -1 -> groupIdx
            orderIdx != -1 -> orderIdx
            else -> query.length
        }

        if (whereIdx != -1 && endWhereIdx > whereIdx) {
            val whereClause = query.substring(whereIdx + 5, endWhereIdx)
            // Extract col = val, col >, col <, col LIKE, col IN, etc.
            val colPredicates = Regex("([a-zA-Z0-9_.]+)\\s*(=|!=|<>|<=|>=|<|>|LIKE|IN|BETWEEN)", RegexOption.IGNORE_CASE)
                .findAll(whereClause)

            for (m in colPredicates) {
                val fullCol = m.groupValues[1]
                val op = m.groupValues[2].uppercase(Locale.US)
                val cleanCol = fullCol.substringAfterLast(".")
                if (cleanCol !in whereFilterCols) {
                    whereFilterCols.add(cleanCol)
                }

                if (op == "LIKE") {
                    val afterOp = whereClause.substring(m.range.last + 1).trim()
                    if (afterOp.startsWith("'%") || afterOp.startsWith("\"%")) {
                        findings.add(
                            QueryOptimizationFinding(
                                severity = "WARNING",
                                message = "Leading wildcard LIKE operator detected on column '$fullCol'.",
                                recommendation = "Leading wildcards (e.g. '%term') prevent B-Tree index lookups and cause full table scans. Consider Full-Text Search (FTS5 / GIN / GIST) or postfix search."
                            )
                        )
                    }
                }
            }

            // Check function on column
            if (Regex("(?:LOWER|UPPER|DATE|SUBSTR|TRIM)\\s*\\([a-zA-Z0-9_.]+\\)", RegexOption.IGNORE_CASE).containsMatchIn(whereClause)) {
                findings.add(
                    QueryOptimizationFinding(
                        severity = "WARNING",
                        message = "Function call detected on indexed column in WHERE clause.",
                        recommendation = "Wrapping a column in a function (e.g. LOWER(col)) invalidates standard B-Tree indexes unless a functional / expression index is defined."
                    )
                )
            }
        }

        // Parse JOIN columns
        val joinCols = mutableListOf<String>()
        val onMatches = Regex("\\bON\\s+([a-zA-Z0-9_.]+)\\s*=\\s*([a-zA-Z0-9_.]+)", RegexOption.IGNORE_CASE)
            .findAll(query)

        for (m in onMatches) {
            val c1 = m.groupValues[1].substringAfterLast(".")
            val c2 = m.groupValues[2].substringAfterLast(".")
            if (c1 !in joinCols) joinCols.add(c1)
            if (c2 !in joinCols) joinCols.add(c2)
        }

        // Parse ORDER BY / GROUP BY columns
        val orderGroupCols = mutableListOf<String>()
        if (orderIdx != -1) {
            val orderClause = query.substring(orderIdx + 8).substringBefore(";").substringBefore("LIMIT")
            val orderTokens = orderClause.split(",").map { it.trim().split("\\s+".toRegex())[0].substringAfterLast(".") }
            orderGroupCols.addAll(orderTokens.filter { it.isNotBlank() && it !in orderGroupCols })
        }
        if (groupIdx != -1) {
            val groupClause = query.substring(groupIdx + 8).substringBefore("ORDER BY").substringBefore("HAVING").substringBefore(";")
            val groupTokens = groupClause.split(",").map { it.trim().split("\\s+".toRegex())[0].substringAfterLast(".") }
            orderGroupCols.addAll(groupTokens.filter { it.isNotBlank() && it !in orderGroupCols })
        }

        // Build suggested composite indexes
        val suggestedIndexes = mutableListOf<SuggestedIndex>()

        val primaryTable = detectedTables.firstOrNull() ?: "table_name"
        val secondaryTable = detectedTables.getOrNull(1)

        val table1Cols = mutableListOf<String>()
        table1Cols.addAll(whereFilterCols.take(2))
        table1Cols.addAll(orderGroupCols.filter { it !in table1Cols }.take(1))

        if (table1Cols.isNotEmpty()) {
            val idxName = "idx_${primaryTable}_" + table1Cols.joinToString("_")
            val ddl = "CREATE INDEX $idxName ON $primaryTable (${table1Cols.joinToString(", ")});"
            suggestedIndexes.add(
                SuggestedIndex(
                    tableName = primaryTable,
                    columns = table1Cols,
                    indexType = "Composite B-Tree",
                    ddlStatement = ddl,
                    rationale = "Covers high-cardinality equality/range filters and sorting order to eliminate filesort."
                )
            )
        }

        if (joinCols.isNotEmpty() && secondaryTable != null) {
            val idxName = "idx_${secondaryTable}_" + joinCols.take(2).joinToString("_")
            val ddl = "CREATE INDEX $idxName ON $secondaryTable (${joinCols.take(2).joinToString(", ")});"
            suggestedIndexes.add(
                SuggestedIndex(
                    tableName = secondaryTable,
                    columns = joinCols.take(2),
                    indexType = "B-Tree Join Index",
                    ddlStatement = ddl,
                    rationale = "Accelerates hash/nested-loop join lookup on foreign key foreign table."
                )
            )
        }

        if (findings.isEmpty()) {
            findings.add(
                QueryOptimizationFinding(
                    severity = "INFO",
                    message = "Query structure is well-formed for index coverage.",
                    recommendation = "Ensure table statistics are up-to-date with ANALYZE (or VACUUM) so the query planner uses composite indexes."
                )
            )
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== SQL QUERY INDEX ADVISOR ===")
            appendLine("Dialect Target:    $dialect")
            appendLine("Identified Tables: ${detectedTables.joinToString(", ")}")
            appendLine("Filter Columns:    ${if (whereFilterCols.isNotEmpty()) whereFilterCols.joinToString(", ") else "None (Potential Full Table Scan)"}")
            appendLine("Join Keys:         ${if (joinCols.isNotEmpty()) joinCols.joinToString(", ") else "None"}")
            appendLine("Sort/Group By:     ${if (orderGroupCols.isNotEmpty()) orderGroupCols.joinToString(", ") else "None"}")
            appendLine("----------------------------------------")
            appendLine("RECOMMENDED DDL INDEX STATEMENTS:")
            if (suggestedIndexes.isEmpty()) {
                appendLine("  -- No high-value index candidates identified.")
            } else {
                suggestedIndexes.forEachIndexed { i, idx ->
                    appendLine("-- Recommendation #${i + 1}: ${idx.rationale}")
                    appendLine(idx.ddlStatement)
                    appendLine()
                }
            }
            appendLine("----------------------------------------")
            appendLine("OPTIMIZATION FINDINGS & BEST PRACTICES:")
            findings.forEach { f ->
                appendLine("[${f.severity}] ${f.message}")
                appendLine("  Action: ${f.recommendation}")
            }
        }

        return ToolResult.Success(
            data = SqlIndexAdvisorOutput(
                targetDialect = dialect,
                detectedTables = detectedTables,
                whereFilterColumns = whereFilterCols,
                joinColumns = joinCols,
                orderGroupColumns = orderGroupCols,
                suggestedIndexes = suggestedIndexes,
                findings = findings,
                formattedReport = report,
                summary = "Generated ${suggestedIndexes.size} index recommendations across ${detectedTables.size} tables."
            ),
            executionTimeMs = elapsed,
            summary = "Suggested ${suggestedIndexes.size} SQL indexes"
        )
    }
}
