package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONArray
import org.json.JSONObject

enum class YamlJsonMode(val label: String) {
    YAML_TO_JSON("YAML to JSON"),
    JSON_TO_YAML("JSON to YAML")
}

data class YamlJsonInput(
    val content: String = "",
    val mode: YamlJsonMode = YamlJsonMode.YAML_TO_JSON,
    val indentSpaces: Int = 2
)

data class YamlJsonOutput(
    val convertedContent: String,
    val mode: YamlJsonMode,
    val originalLength: Int,
    val convertedLength: Int,
    val summary: String
)

class YamlToJsonTool : Tool<YamlJsonInput, YamlJsonOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "yaml_to_json_tool",
        name = "YAML <-> JSON Converter",
        description = "Bi-directional conversion between YAML documents and structured JSON payloads offline.",
        category = ToolCategory.DATA,
        tags = listOf("yaml", "json", "converter", "format", "config", "yml", "data", "parser"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Transform"
    )

    override suspend fun execute(input: YamlJsonInput): ToolResult<YamlJsonOutput> {
        val startTime = System.currentTimeMillis()

        if (input.content.isBlank()) {
            return ToolResult.Failure(
                message = "Input data is empty.",
                userGuidance = "Provide YAML or JSON text to convert."
            )
        }

        return try {
            val resultText = when (input.mode) {
                YamlJsonMode.YAML_TO_JSON -> yamlToJson(input.content, input.indentSpaces)
                YamlJsonMode.JSON_TO_YAML -> jsonToYaml(input.content)
            }

            val summary = "Converted ${input.mode.label} (${resultText.length} chars)"

            ToolResult.Success(
                data = YamlJsonOutput(
                    convertedContent = resultText,
                    mode = input.mode,
                    originalLength = input.content.length,
                    convertedLength = resultText.length,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("Conversion failed: ${e.message}", cause = e)
        }
    }

    private fun yamlToJson(yaml: String, indent: Int): String {
        val root = parseYamlBlock(yaml.lines(), 0).first
        val jsonString = valueToJsonString(root, indent)
        return jsonString
    }

    private fun parseYamlBlock(lines: List<String>, minIndent: Int): Pair<Any?, Int> {
        val map = mutableMapOf<String, Any?>()
        val list = mutableListOf<Any?>()
        var isList = false
        var idx = 0

        while (idx < lines.size) {
            val rawLine = lines[idx]
            if (rawLine.isBlank() || rawLine.trimStart().startsWith("#")) {
                idx++
                continue
            }

            val indent = rawLine.takeWhile { it == ' ' }.length
            if (indent < minIndent) {
                break
            }

            val trimmed = rawLine.trim()
            if (trimmed.startsWith("- ")) {
                isList = true
                val valPart = trimmed.substring(2).trim()
                if (valPart.isEmpty()) {
                    val (sub, consumed) = parseYamlBlock(lines.subList(idx + 1, lines.size), indent + 2)
                    list.add(sub)
                    idx += 1 + consumed
                } else {
                    list.add(parseScalar(valPart))
                    idx++
                }
            } else if (trimmed.contains(":")) {
                val colonIdx = trimmed.indexOf(':')
                val key = trimmed.substring(0, colonIdx).trim().trim('"', '\'')
                val valPart = trimmed.substring(colonIdx + 1).trim()

                if (valPart.isEmpty()) {
                    val (sub, consumed) = parseYamlBlock(lines.subList(idx + 1, lines.size), indent + 2)
                    map[key] = sub
                    idx += 1 + consumed
                } else {
                    map[key] = parseScalar(valPart)
                    idx++
                }
            } else {
                idx++
            }
        }

        return Pair(if (isList) list else map, idx)
    }

    private fun parseScalar(v: String): Any? {
        val s = v.trim()
        if (s == "null" || s == "~") return null
        if (s.equals("true", ignoreCase = true)) return true
        if (s.equals("false", ignoreCase = true)) return false
        s.toLongOrNull()?.let { return it }
        s.toDoubleOrNull()?.let { return it }
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length - 1)
        }
        return s
    }

    private fun valueToJsonString(v: Any?, indent: Int): String {
        return when (v) {
            is Map<*, *> -> {
                val obj = JSONObject()
                for ((k, value) in v) {
                    obj.put(k.toString(), wrapForJson(value))
                }
                obj.toString(indent)
            }
            is List<*> -> {
                val arr = JSONArray()
                for (item in v) {
                    arr.put(wrapForJson(item))
                }
                arr.toString(indent)
            }
            else -> JSONObject.wrap(v)?.toString() ?: "null"
        }
    }

    private fun wrapForJson(v: Any?): Any? {
        return when (v) {
            is Map<*, *> -> {
                val obj = JSONObject()
                for ((k, value) in v) {
                    obj.put(k.toString(), wrapForJson(value))
                }
                obj
            }
            is List<*> -> {
                val arr = JSONArray()
                for (item in v) {
                    arr.put(wrapForJson(item))
                }
                arr
            }
            else -> v
        }
    }

    private fun jsonToYaml(json: String): String {
        val trimmed = json.trim()
        val sb = StringBuilder()
        if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            buildYamlFromJsonObject(obj, sb, 0)
        } else if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            buildYamlFromJsonArray(arr, sb, 0)
        } else {
            throw IllegalArgumentException("Input is neither a JSON object nor array.")
        }
        return sb.toString().trimEnd()
    }

    private fun buildYamlFromJsonObject(obj: JSONObject, sb: StringBuilder, indent: Int) {
        val pad = " ".repeat(indent)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key)
            when (value) {
                is JSONObject -> {
                    sb.append(pad).append(key).append(":\n")
                    buildYamlFromJsonObject(value, sb, indent + 2)
                }
                is JSONArray -> {
                    sb.append(pad).append(key).append(":\n")
                    buildYamlFromJsonArray(value, sb, indent + 2)
                }
                null, JSONObject.NULL -> {
                    sb.append(pad).append(key).append(": null\n")
                }
                is String -> {
                    val formatted = if (value.contains("\n") || value.contains(":")) "\"$value\"" else value
                    sb.append(pad).append(key).append(": ").append(formatted).append("\n")
                }
                else -> {
                    sb.append(pad).append(key).append(": ").append(value).append("\n")
                }
            }
        }
    }

    private fun buildYamlFromJsonArray(arr: JSONArray, sb: StringBuilder, indent: Int) {
        val pad = " ".repeat(indent)
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            when (item) {
                is JSONObject -> {
                    sb.append(pad).append("- \n")
                    buildYamlFromJsonObject(item, sb, indent + 2)
                }
                is JSONArray -> {
                    sb.append(pad).append("- \n")
                    buildYamlFromJsonArray(item, sb, indent + 2)
                }
                null, JSONObject.NULL -> {
                    sb.append(pad).append("- null\n")
                }
                is String -> {
                    val formatted = if (item.contains("\n") || item.contains(":")) "\"$item\"" else item
                    sb.append(pad).append("- ").append(formatted).append("\n")
                }
                else -> {
                    sb.append(pad).append("- ").append(item).append("\n")
                }
            }
        }
    }
}
