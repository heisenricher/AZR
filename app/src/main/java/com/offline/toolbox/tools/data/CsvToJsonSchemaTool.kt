package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONArray
import org.json.JSONObject

data class CsvSchemaInput(
    val csvContent: String = """
        id,name,email,is_active,score,created_at
        1,Alice,alice@example.com,true,95.5,2026-01-15T08:30:00Z
        2,Bob,bob@example.com,false,82.0,2026-02-20T14:15:00Z
        3,Charlie,charlie@example.com,true,99.2,2026-03-01T19:45:00Z
    """.trimIndent()
)

data class CsvSchemaOutput(
    val jsonSchema: String,
    val columnCount: Int,
    val rowCount: Int,
    val inferredTypes: Map<String, String>,
    val formattedReport: String,
    val summary: String
)

class CsvToJsonSchemaTool : Tool<CsvSchemaInput, CsvSchemaOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "csv_to_json_schema_tool",
        name = "CSV to JSON Schema Inferer",
        description = "Infer standard Draft-07 JSON Schema specifications from tabular CSV data with automatic type detection.",
        category = ToolCategory.DATA,
        tags = listOf("csv", "json", "schema", "infer", "types", "table", "data", "validator"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.JSON,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "DataObject"
    )

    override suspend fun execute(input: CsvSchemaInput): ToolResult<CsvSchemaOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.csvContent.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("CSV content cannot be empty.")
        }

        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) {
            return ToolResult.Failure("CSV must contain at least a header line and one data row.")
        }

        val headers = lines[0].split(",").map { it.trim().trim('"', '\'') }
        val dataRows = lines.drop(1).map { line ->
            line.split(",").map { it.trim().trim('"', '\'') }
        }

        val colTypes = mutableMapOf<String, String>()
        val requiredList = mutableListOf<String>()

        val schema = JSONObject()
        schema.put("\$schema", "http://json-schema.org/draft-07/schema#")
        schema.put("type", "object")
        schema.put("title", "InferredSchema")

        val properties = JSONObject()

        for (colIdx in headers.indices) {
            val colName = headers[colIdx]
            val values = dataRows.mapNotNull { if (colIdx < it.size) it[colIdx] else null }.filter { it.isNotEmpty() }

            val inferredType = detectType(values)
            colTypes[colName] = inferredType

            val propDef = JSONObject()
            when (inferredType) {
                "integer" -> propDef.put("type", "integer")
                "number" -> propDef.put("type", "number")
                "boolean" -> propDef.put("type", "boolean")
                "date-time" -> {
                    propDef.put("type", "string")
                    propDef.put("format", "date-time")
                }
                else -> propDef.put("type", "string")
            }

            // Check if column has 0 empty values across rows
            if (values.size == dataRows.size) {
                requiredList.add(colName)
            }

            properties.put(colName, propDef)
        }

        schema.put("properties", properties)
        if (requiredList.isNotEmpty()) {
            val reqArray = JSONArray()
            requiredList.forEach { reqArray.put(it) }
            schema.put("required", reqArray)
        }

        val schemaJson = schema.toString(2)

        val report = buildString {
            appendLine("INFERRED JSON SCHEMA (DRAFT-07)")
            appendLine("--------------------------------------------------")
            appendLine("Columns Inferred: ${headers.size}")
            appendLine("Sample Rows:      ${dataRows.size}")
            appendLine()
            appendLine("DETECTED COLUMN TYPES:")
            colTypes.forEach { (col, type) -> appendLine("• $col: $type") }
            appendLine()
            appendLine("SCHEMA SPECIFICATION:")
            appendLine(schemaJson)
        }

        val summary = "Inferred schema for ${headers.size} cols from ${dataRows.size} rows"

        return ToolResult.Success(
            data = CsvSchemaOutput(
                jsonSchema = schemaJson,
                columnCount = headers.size,
                rowCount = dataRows.size,
                inferredTypes = colTypes,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun detectType(values: List<String>): String {
        if (values.isEmpty()) return "string"

        if (values.all { it.equals("true", ignoreCase = true) || it.equals("false", ignoreCase = true) }) {
            return "boolean"
        }
        if (values.all { it.toIntOrNull() != null }) {
            return "integer"
        }
        if (values.all { it.toDoubleOrNull() != null }) {
            return "number"
        }
        if (values.all { it.contains("T") && it.contains(":") && it.length >= 19 }) {
            return "date-time"
        }
        return "string"
    }
}
