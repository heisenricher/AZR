package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class JsonToCsvTool : Tool<String, String> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "json_to_csv",
        name = "JSON to CSV Converter",
        description = "Convert JSON arrays of objects into structured, quote-escaped CSV spreadsheets.",
        category = ToolCategory.DATA,
        tags = listOf("json", "csv", "convert", "table", "export", "spreadsheet", "data"),
        inputType = ToolDataType.JSON,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "TableRows"
    )

    override suspend fun execute(input: String): ToolResult<String> {
        val startTime = System.currentTimeMillis()
        val text = input.trim()

        if (text.isEmpty()) {
            return ToolResult.Failure("Input JSON is empty.", "Paste a JSON array of objects to convert to CSV.")
        }

        return try {
            val jsonArray = when (val parsed = JSONTokener(text).nextValue()) {
                is JSONArray -> parsed
                is JSONObject -> JSONArray().put(parsed)
                else -> return ToolResult.Failure(
                    message = "Input is not a JSON Array or Object.",
                    userGuidance = "JSON must represent a list of objects like '[{\"id\":1, \"name\":\"Tool\"}]'."
                )
            }

            if (jsonArray.length() == 0) {
                return ToolResult.Failure("JSON array is empty.")
            }

            // Gather all distinct keys across items to preserve full columns
            val headers = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i)
                if (item != null) {
                    val keys = item.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (!headers.contains(k)) {
                            headers.add(k)
                        }
                    }
                }
            }

            if (headers.isEmpty()) {
                return ToolResult.Failure("No object properties found in JSON items.")
            }

            val sb = StringBuilder()
            // Header row
            sb.appendLine(headers.joinToString(",") { escapeCsv(it) })

            // Data rows
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i)
                if (item != null) {
                    val rowVals = headers.map { key ->
                        val v = if (item.has(key)) item.opt(key)?.toString() ?: "" else ""
                        escapeCsv(v)
                    }
                    sb.appendLine(rowVals.joinToString(","))
                }
            }

            val result = sb.toString().trimEnd()
            val summary = "Converted ${jsonArray.length()} item(s) to CSV with ${headers.size} column(s)"

            ToolResult.Success(
                data = result,
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Failed to convert JSON to CSV: ${e.message}",
                cause = e
            )
        }
    }

    private fun escapeCsv(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}
