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

data class JsonPathInput(
    val json: String = """
        {
          "store": {
            "book": [
              { "category": "reference", "author": "Nigel Rees", "title": "Sayings of the Century", "price": 8.95 },
              { "category": "fiction", "author": "Evelyn Waugh", "title": "Sword of Honour", "price": 12.99 },
              { "category": "fiction", "author": "Herman Melville", "title": "Moby Dick", "isbn": "0-553-21311-3", "price": 8.99 }
            ],
            "bicycle": {
              "color": "red",
              "price": 19.95
            }
          }
        }
    """.trimIndent(),
    val jsonPath: String = "$.store.book[*].title"
)

data class JsonPathOutput(
    val queryPath: String,
    val matchedCount: Int,
    val matchedElementsJson: String,
    val formattedReport: String,
    val summary: String
)

class JsonPathEvaluatorTool : Tool<JsonPathInput, JsonPathOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "json_path_evaluator_tool",
        name = "JSONPath Query & Value Extractor",
        description = "Extract targeted elements, fields, or array slices from nested JSON documents using lightweight offline JSONPath expressions.",
        category = ToolCategory.DATA,
        tags = listOf("json", "jsonpath", "query", "filter", "extract", "data", "array", "slice"),
        inputType = ToolDataType.JSON,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FilterList"
    )

    override suspend fun execute(input: JsonPathInput): ToolResult<JsonPathOutput> {
        val startTime = System.currentTimeMillis()
        val rawJson = input.json.trim()
        val path = input.jsonPath.trim()

        if (rawJson.isEmpty()) {
            return ToolResult.Failure("JSON input cannot be empty.")
        }
        if (path.isEmpty()) {
            return ToolResult.Failure("JSONPath expression cannot be empty.")
        }

        val root: Any = try {
            val tokener = JSONTokener(rawJson)
            tokener.nextValue()
        } catch (e: Exception) {
            return ToolResult.Failure("Invalid JSON structure: ${e.message}")
        }

        val results = evaluatePath(root, path)

        val outArray = JSONArray()
        for (r in results) {
            outArray.put(r)
        }
        val outJson = outArray.toString(2)

        val report = buildString {
            appendLine("JSONPATH QUERY EVALUATION")
            appendLine("--------------------------------------------------")
            appendLine("Query:          $path")
            appendLine("Matches Found:  ${results.size}")
            appendLine()
            appendLine("Matched Results:")
            appendLine(outJson)
        }

        val summary = "Path '$path' returned ${results.size} match(es)"

        return ToolResult.Success(
            data = JsonPathOutput(
                queryPath = path,
                matchedCount = results.size,
                matchedElementsJson = outJson,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun evaluatePath(root: Any, rawPath: String): List<Any?> {
        val clean = rawPath.removePrefix("$").removePrefix(".").trim()
        if (clean.isEmpty()) return listOf(root)

        val tokens = tokenizePath(clean)
        var currentLevel = listOf<Any?>(root)

        for (token in tokens) {
            val nextLevel = mutableListOf<Any?>()
            for (node in currentLevel) {
                if (node == null) continue
                when {
                    token == "*" -> {
                        if (node is JSONObject) {
                            val keys = node.keys()
                            while (keys.hasNext()) {
                                nextLevel.add(node.get(keys.next()))
                            }
                        } else if (node is JSONArray) {
                            for (i in 0 until node.length()) {
                                nextLevel.add(node.get(i))
                            }
                        }
                    }
                    token.startsWith("[") && token.endsWith("]") -> {
                        val inner = token.substring(1, token.length - 1).trim()
                        if (inner == "*") {
                            if (node is JSONArray) {
                                for (i in 0 until node.length()) nextLevel.add(node.get(i))
                            }
                        } else {
                            val idx = inner.toIntOrNull()
                            if (idx != null && node is JSONArray) {
                                val actualIdx = if (idx < 0) node.length() + idx else idx
                                if (actualIdx in 0 until node.length()) {
                                    nextLevel.add(node.get(actualIdx))
                                }
                            }
                        }
                    }
                    node is JSONObject -> {
                        if (node.has(token)) {
                            nextLevel.add(node.get(token))
                        }
                    }
                }
            }
            currentLevel = nextLevel
        }

        return currentLevel
    }

    private fun tokenizePath(path: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inBracket = false

        for (c in path) {
            when {
                c == '[' -> {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                    inBracket = true
                    sb.append(c)
                }
                c == ']' -> {
                    sb.append(c)
                    tokens.add(sb.toString())
                    sb.clear()
                    inBracket = false
                }
                c == '.' && !inBracket -> {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> {
                    sb.append(c)
                }
            }
        }
        if (sb.isNotEmpty()) {
            tokens.add(sb.toString())
        }
        return tokens
    }
}
