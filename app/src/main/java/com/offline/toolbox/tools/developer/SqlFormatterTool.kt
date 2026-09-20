package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class SqlFormatterInput(
    val sql: String,
    val uppercaseKeywords: Boolean = true,
    val indentSpaces: Int = 2
)

data class SqlFormatterOutput(
    val formattedSql: String,
    val lineCount: Int,
    val summary: String
)

class SqlFormatterTool : Tool<SqlFormatterInput, SqlFormatterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "sql_formatter",
        name = "SQL Query Formatter",
        description = "Format and beautify SQL queries with standard indentation, keyword casing, and clause breaks.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("sql", "query", "database", "format", "beautify", "select", "indent", "syntax"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "DataObject"
    )

    private val majorClauses = listOf(
        "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY", "LIMIT", "OFFSET",
        "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "OUTER JOIN", "CROSS JOIN", "JOIN",
        "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM", "UNION ALL", "UNION"
    )

    private val minorKeywords = listOf(
        "AND", "OR", "ON", "AS", "IN", "NOT", "NULL", "IS", "LIKE", "BETWEEN", "CASE",
        "WHEN", "THEN", "ELSE", "END", "DISTINCT", "ALL", "ASC", "DESC", "BY"
    )

    override suspend fun execute(input: SqlFormatterInput): ToolResult<SqlFormatterOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.sql.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure(
                message = "Input SQL is empty.",
                userGuidance = "Paste a SQL query (e.g. 'SELECT id, name FROM users WHERE active = 1')."
            )
        }

        val indent = " ".repeat(input.indentSpaces.coerceIn(1, 8))
        var normalized = raw.replace(Regex("\\s+"), " ")

        // Format major clauses onto new lines
        for (clause in majorClauses.sortedByDescending { it.length }) {
            val pattern = "(?i)\\b${Regex.escape(clause)}\\b"
            val replacement = if (input.uppercaseKeywords) "\n$clause" else "\n${clause.lowercase(Locale.ROOT)}"
            normalized = normalized.replace(Regex(pattern), replacement)
        }

        // Format AND / OR on indented sub-lines
        for (sub in listOf("AND", "OR")) {
            val pattern = "(?i)\\b${Regex.escape(sub)}\\b"
            val replacement = if (input.uppercaseKeywords) "\n$indent$sub" else "\n$indent${sub.lowercase(Locale.ROOT)}"
            normalized = normalized.replace(Regex(pattern), replacement)
        }

        // Case format minor keywords
        if (input.uppercaseKeywords) {
            for (kw in minorKeywords) {
                val pattern = "(?i)\\b${Regex.escape(kw)}\\b"
                normalized = normalized.replace(Regex(pattern), kw)
            }
        }

        val cleanLines = normalized.lines()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()

        val lineCount = cleanLines.lines().size
        val summary = "Formatted SQL query ($lineCount lines)"

        return ToolResult.Success(
            data = SqlFormatterOutput(
                formattedSql = cleanLines,
                lineCount = lineCount,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
