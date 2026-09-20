package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class SqlDialect(val displayName: String) {
    POSTGRESQL("PostgreSQL"),
    MYSQL("MySQL"),
    SQLITE("SQLite")
}

data class SqlDialectInput(
    val sqlContent: String = """
        CREATE TABLE users (
            id SERIAL PRIMARY KEY,
            username VARCHAR(50) NOT NULL,
            is_active BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    """.trimIndent(),
    val sourceDialect: SqlDialect = SqlDialect.POSTGRESQL,
    val targetDialect: SqlDialect = SqlDialect.MYSQL
)

data class SqlDialectOutput(
    val convertedSql: String,
    val sourceDialect: SqlDialect,
    val targetDialect: SqlDialect,
    val transformationsApplied: List<String>,
    val formattedReport: String,
    val summary: String
)

class SqlDialectConverterTool : Tool<SqlDialectInput, SqlDialectOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "sql_dialect_converter_tool",
        name = "SQL Dialect Translator (Postgres, MySQL, SQLite)",
        description = "Translate DDL schemas and queries between PostgreSQL, MySQL, and SQLite dialects with automatic type and syntax adaptation.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("sql", "dialect", "postgres", "mysql", "sqlite", "ddl", "database", "query", "convert"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Storage"
    )

    override suspend fun execute(input: SqlDialectInput): ToolResult<SqlDialectOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.sqlContent.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("SQL content cannot be empty.")
        }

        if (input.sourceDialect == input.targetDialect) {
            return ToolResult.Success(
                data = SqlDialectOutput(
                    convertedSql = raw,
                    sourceDialect = input.sourceDialect,
                    targetDialect = input.targetDialect,
                    transformationsApplied = listOf("Source and target dialects are identical; no modifications needed."),
                    formattedReport = "Identical dialect: No translation required.",
                    summary = "No translation required (same dialect)"
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "Source and Target dialects are both ${input.sourceDialect.displayName}"
            )
        }

        var sql = raw
        val transforms = mutableListOf<String>()

        // 1. Primary key & Auto-increment transformations
        when (input.targetDialect) {
            SqlDialect.MYSQL -> {
                if (sql.contains("SERIAL PRIMARY KEY", ignoreCase = true)) {
                    sql = sql.replace(Regex("(?i)\\bSERIAL PRIMARY KEY\\b"), "INT AUTO_INCREMENT PRIMARY KEY")
                    transforms.add("Converted 'SERIAL PRIMARY KEY' to 'INT AUTO_INCREMENT PRIMARY KEY'")
                } else if (sql.contains("SERIAL", ignoreCase = true)) {
                    sql = sql.replace(Regex("(?i)\\bSERIAL\\b"), "INT AUTO_INCREMENT")
                    transforms.add("Converted 'SERIAL' to 'INT AUTO_INCREMENT'")
                }
                if (sql.contains("INTEGER PRIMARY KEY AUTOINCREMENT", ignoreCase = true)) {
                    sql = sql.replace(Regex("(?i)\\bINTEGER PRIMARY KEY AUTOINCREMENT\\b"), "INT AUTO_INCREMENT PRIMARY KEY")
                    transforms.add("Converted SQLite AUTOINCREMENT to MySQL INT AUTO_INCREMENT PRIMARY KEY")
                }
                // Booleans to TINYINT(1)
                if (sql.contains("BOOLEAN", ignoreCase = true)) {
                    sql = sql.replace(Regex("(?i)\\bBOOLEAN\\b"), "TINYINT(1)")
                    transforms.add("Adapted 'BOOLEAN' type to 'TINYINT(1)' for MySQL")
                }
                // Convert double quote identifiers to backticks
                if (sql.contains("\"")) {
                    sql = sql.replace(Regex("\"([a-zA-Z0-9_]+)\""), "`$1`")
                    transforms.add("Converted double-quoted identifiers to backticks (`...`)")
                }
            }
            SqlDialect.SQLITE -> {
                if (sql.contains("SERIAL PRIMARY KEY", ignoreCase = true)) {
                    sql = sql.replace(Regex("(?i)\\bSERIAL PRIMARY KEY\\b"), "INTEGER PRIMARY KEY AUTOINCREMENT")
                    transforms.add("Converted 'SERIAL PRIMARY KEY' to 'INTEGER PRIMARY KEY AUTOINCREMENT'")
                }
                if (sql.contains("AUTO_INCREMENT", ignoreCase = true)) {
                    sql = sql.replace(Regex("(?i)\\bINT\\s+AUTO_INCREMENT\\s+PRIMARY\\s+KEY\\b"), "INTEGER PRIMARY KEY AUTOINCREMENT")
                    sql = sql.replace(Regex("(?i)\\bAUTO_INCREMENT\\b"), "AUTOINCREMENT")
                    transforms.add("Converted MySQL AUTO_INCREMENT to SQLite AUTOINCREMENT")
                }
                // Convert backticks to double quotes or plain
                if (sql.contains("`")) {
                    sql = sql.replace("`", "\"")
                    transforms.add("Normalized backticks to standard SQL double quotes")
                }
            }
            SqlDialect.POSTGRESQL -> {
                if (sql.contains("AUTO_INCREMENT", ignoreCase = true) || sql.contains("AUTOINCREMENT", ignoreCase = true)) {
                    sql = sql.replace(Regex("(?i)\\b(INT|INTEGER)\\s+(AUTO_INCREMENT|AUTOINCREMENT)\\s+PRIMARY\\s+KEY\\b"), "SERIAL PRIMARY KEY")
                    sql = sql.replace(Regex("(?i)\\b(INT|INTEGER)\\s+(AUTO_INCREMENT|AUTOINCREMENT)\\b"), "SERIAL")
                    transforms.add("Converted AUTO_INCREMENT primary key to PostgreSQL 'SERIAL PRIMARY KEY'")
                }
                if (sql.contains("TINYINT(1)", ignoreCase = true)) {
                    sql = sql.replace(Regex("(?i)\\bTINYINT\\(1\\)\\b"), "BOOLEAN")
                    transforms.add("Converted TINYINT(1) flag to PostgreSQL 'BOOLEAN'")
                }
                if (sql.contains("`")) {
                    sql = sql.replace("`", "\"")
                    transforms.add("Converted backticks to double quotes for PostgreSQL identifiers")
                }
            }
        }

        // Limit / Offset syntax
        if (input.targetDialect == SqlDialect.POSTGRESQL && sql.contains(Regex("(?i)LIMIT\\s+(\\d+)\\s*,\\s*(\\d+)"))) {
            sql = sql.replace(Regex("(?i)LIMIT\\s+(\\d+)\\s*,\\s*(\\d+)"), "LIMIT $2 OFFSET $1")
            transforms.add("Converted MySQL 'LIMIT offset, count' to standard 'LIMIT count OFFSET offset'")
        }

        val report = buildString {
            appendLine("SQL DIALECT TRANSLATION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Source Dialect:    ${input.sourceDialect.displayName}")
            appendLine("Target Dialect:    ${input.targetDialect.displayName}")
            appendLine("Rules Applied:     ${transforms.size}")
            appendLine()
            appendLine("TRANSFORMATIONS:")
            if (transforms.isEmpty()) {
                appendLine("• Direct syntax compatible (no dialect conversions necessary)")
            } else {
                transforms.forEach { appendLine("• $it") }
            }
            appendLine()
            appendLine("TRANSLATED SQL:")
            appendLine(sql)
        }

        val summary = "${input.sourceDialect.name} → ${input.targetDialect.name} (${transforms.size} rules applied)"

        return ToolResult.Success(
            data = SqlDialectOutput(
                convertedSql = sql,
                sourceDialect = input.sourceDialect,
                targetDialect = input.targetDialect,
                transformationsApplied = transforms,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
