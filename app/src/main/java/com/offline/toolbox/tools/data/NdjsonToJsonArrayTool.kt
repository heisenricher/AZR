package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONArray
import org.json.JSONTokener

enum class NdjsonMode {
    NDJSON_TO_JSON_ARRAY,
    JSON_ARRAY_TO_NDJSON
}

data class NdjsonInput(
    val content: String = "{\"id\": 1, \"name\": \"Sensor Alpha\", \"active\": true}\n{\"id\": 2, \"name\": \"Sensor Beta\", \"active\": false}",
    val mode: NdjsonMode = NdjsonMode.NDJSON_TO_JSON_ARRAY,
    val indentSpaces: Int = 2
)

data class NdjsonOutput(
    val convertedContent: String,
    val mode: NdjsonMode,
    val recordCount: Int,
    val invalidLineCount: Int,
    val formattedReport: String,
    val summary: String
)

class NdjsonToJsonArrayTool : Tool<NdjsonInput, NdjsonOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "ndjson_to_json_array_tool",
        name = "NDJSON / JSON Lines Converter",
        description = "Bi-directional conversion between newline-delimited JSON (NDJSON/JSONL) streams and standard JSON arrays.",
        category = ToolCategory.DATA,
        tags = listOf("ndjson", "jsonl", "json lines", "streaming", "data", "array", "log", "convert"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FormatListBulleted"
    )

    override suspend fun execute(input: NdjsonInput): ToolResult<NdjsonOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.content.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input content cannot be empty.")
        }

        return try {
            when (input.mode) {
                NdjsonMode.NDJSON_TO_JSON_ARRAY -> {
                    val lines = raw.lines()
                    val array = JSONArray()
                    var validCount = 0
                    var invalidCount = 0

                    for ((idx, line) in lines.withIndex()) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        try {
                            val tokener = JSONTokener(trimmed)
                            val value = tokener.nextValue()
                            array.put(value)
                            validCount++
                        } catch (_: Exception) {
                            invalidCount++
                        }
                    }

                    val formatted = array.toString(input.indentSpaces)
                    val report = buildString {
                        appendLine("NDJSON TO JSON ARRAY REPORT")
                        appendLine("--------------------------------------------------")
                        appendLine("Total Input Lines:   ${lines.size}")
                        appendLine("Valid JSON Records:  $validCount")
                        appendLine("Invalid / Skipped:   $invalidCount")
                        appendLine("Output Character Length: ${formatted.length}")
                    }

                    val summary = "Converted $validCount NDJSON record(s) to JSON Array"

                    ToolResult.Success(
                        data = NdjsonOutput(
                            convertedContent = formatted,
                            mode = input.mode,
                            recordCount = validCount,
                            invalidLineCount = invalidCount,
                            formattedReport = report,
                            summary = summary
                        ),
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        summary = summary
                    )
                }
                NdjsonMode.JSON_ARRAY_TO_NDJSON -> {
                    val tokener = JSONTokener(raw)
                    val jsonVal = tokener.nextValue()

                    if (jsonVal !is JSONArray) {
                        return ToolResult.Failure("Input is not a valid JSON Array. Must start with '[' and end with ']'.")
                    }

                    val sb = StringBuilder()
                    for (i in 0 until jsonVal.length()) {
                        val item = jsonVal.get(i)
                        sb.append(item.toString()).append("\n")
                    }

                    val ndjsonResult = sb.toString().trimEnd()
                    val report = buildString {
                        appendLine("JSON ARRAY TO NDJSON REPORT")
                        appendLine("--------------------------------------------------")
                        appendLine("Array Elements:      ${jsonVal.length()}")
                        appendLine("NDJSON Lines:        ${jsonVal.length()}")
                        appendLine("Output Character Length: ${ndjsonResult.length}")
                    }

                    val summary = "Converted ${jsonVal.length()} Array items to NDJSON"

                    ToolResult.Success(
                        data = NdjsonOutput(
                            convertedContent = ndjsonResult,
                            mode = input.mode,
                            recordCount = jsonVal.length(),
                            invalidLineCount = 0,
                            formattedReport = report,
                            summary = summary
                        ),
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        summary = summary
                    )
                }
            }
        } catch (e: Exception) {
            ToolResult.Failure("NDJSON conversion failed: ${e.message}", cause = e)
        }
    }
}
