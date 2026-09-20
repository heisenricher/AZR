package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

enum class JsonOperation {
    FORMAT_2_SPACES,
    FORMAT_4_SPACES,
    MINIFY,
    VALIDATE_ONLY
}

data class JsonFormatterInput(
    val jsonString: String,
    val operation: JsonOperation = JsonOperation.FORMAT_2_SPACES
)

data class JsonFormatterOutput(
    val processedText: String,
    val isValid: Boolean,
    val rootType: String,
    val sizeBytes: Int,
    val infoMessage: String
)

class JsonFormatterTool : Tool<JsonFormatterInput, JsonFormatterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "json_formatter",
        name = "JSON Formatter & Validator",
        description = "Beautify, minify, and validate JSON with diagnostic error detection and structure inspection.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("json", "format", "beautify", "minify", "validate", "syntax", "lint"),
        inputType = ToolDataType.JSON,
        outputType = ToolDataType.JSON,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "DataObject"
    )

    override suspend fun execute(input: JsonFormatterInput): ToolResult<JsonFormatterOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.jsonString.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure(
                message = "Input JSON is empty.",
                userGuidance = "Paste or enter valid JSON starting with '{' or '['."
            )
        }

        return try {
            val tokener = JSONTokener(raw)
            val nextValue = tokener.nextValue()

            val rootType: String
            val formatted: String

            val indent = when (input.operation) {
                JsonOperation.FORMAT_2_SPACES -> 2
                JsonOperation.FORMAT_4_SPACES -> 4
                JsonOperation.MINIFY -> 0
                JsonOperation.VALIDATE_ONLY -> 2
            }

            when (nextValue) {
                is JSONObject -> {
                    rootType = "JSONObject (${nextValue.length()} keys)"
                    formatted = if (input.operation == JsonOperation.MINIFY) {
                        nextValue.toString()
                    } else {
                        nextValue.toString(indent)
                    }
                }
                is JSONArray -> {
                    rootType = "JSONArray (${nextValue.length()} items)"
                    formatted = if (input.operation == JsonOperation.MINIFY) {
                        nextValue.toString()
                    } else {
                        nextValue.toString(indent)
                    }
                }
                else -> {
                    return ToolResult.Failure(
                        message = "Root element is a primitive value ($nextValue), not a valid JSON Object or Array.",
                        userGuidance = "JSON specification requires a valid document to represent an object {...} or array [...]."
                    )
                }
            }

            // Check if there are trailing leftover characters
            if (tokener.more()) {
                val leftover = tokener.nextClean()
                if (leftover != 0.toChar()) {
                    return ToolResult.Failure(
                        message = "Unexpected extra character '$leftover' after root JSON structure.",
                        userGuidance = "Ensure you do not have trailing text, commas, or extra characters after the closing brace/bracket."
                    )
                }
            }

            val displayText = if (input.operation == JsonOperation.VALIDATE_ONLY) raw else formatted
            ToolResult.Success(
                data = JsonFormatterOutput(
                    processedText = displayText,
                    isValid = true,
                    rootType = rootType,
                    sizeBytes = displayText.toByteArray(Charsets.UTF_8).size,
                    infoMessage = "Valid $rootType"
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "Valid $rootType"
            )
        } catch (e: JSONException) {
            val guidance = diagnoseJsonError(raw, e)
            ToolResult.Failure(
                message = "Invalid JSON: ${e.message ?: "Syntax error"}",
                userGuidance = guidance,
                cause = e
            )
        }
    }

    private fun diagnoseJsonError(raw: String, e: JSONException): String {
        val message = e.message?.lowercase() ?: ""
        val suggestions = mutableListOf<String>()

        if (raw.contains("'")) {
            suggestions.add("Replace single quotes (') with double quotes (\"). JSON requires double-quoted strings and keys.")
        }
        if (raw.matches(Regex(".*,\\s*[}\\]].*"))) {
            suggestions.add("Remove trailing comma before '}' or ']'. JSON does not allow trailing commas.")
        }
        if (raw.count { it == '{' } != raw.count { it == '}' }) {
            suggestions.add("Check curly braces '{ }' count. Found ${raw.count { it == '{' }} opening vs ${raw.count { it == '}' }} closing.")
        }
        if (raw.count { it == '[' } != raw.count { it == ']' }) {
            suggestions.add("Check square brackets '[ ]' count. Found ${raw.count { it == '[' }} opening vs ${raw.count { it == ']' }} closing.")
        }
        if (message.contains("unterminated") || message.contains("expected")) {
            suggestions.add("Check for unescaped characters, missing colons between keys and values, or missing commas.")
        }

        return if (suggestions.isNotEmpty()) {
            "Diagnostic suggestions:\n• " + suggestions.joinToString("\n• ")
        } else {
            "Check that all property keys are enclosed in double quotes and strings are properly escaped."
        }
    }
}
