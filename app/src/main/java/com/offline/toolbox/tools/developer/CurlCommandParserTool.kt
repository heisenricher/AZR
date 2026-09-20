package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class CurlParserInput(
    val curlCommand: String = """
        curl -X POST "https://api.example.com/v1/users?source=mobile" \
          -H "Authorization: Bearer secret-token-xyz" \
          -H "Content-Type: application/json" \
          -d '{"name": "Alice Vance", "role": "Scientist"}'
    """.trimIndent()
)

data class CurlParserOutput(
    val method: String,
    val url: String,
    val queryParams: Map<String, String>,
    val headers: Map<String, String>,
    val body: String?,
    val kotlinSnippet: String,
    val pythonSnippet: String,
    val javascriptSnippet: String,
    val formattedReport: String,
    val summary: String
)

class CurlCommandParserTool : Tool<CurlParserInput, CurlParserOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "curl_command_parser_tool",
        name = "cURL Command Parser & Code Generator",
        description = "Parse offline cURL shell commands into HTTP components and generate equivalent Kotlin, Python, and JavaScript snippets.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("curl", "http", "api", "request", "headers", "kotlin", "python", "javascript", "fetch", "rest"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Code"
    )

    override suspend fun execute(input: CurlParserInput): ToolResult<CurlParserOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.curlCommand.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("cURL command cannot be empty.")
        }

        // Tokenize handling quotes and backslashes
        val tokens = tokenizeCommand(raw)
        if (tokens.isEmpty() || !tokens[0].equals("curl", ignoreCase = true)) {
            return ToolResult.Failure("Command must start with 'curl'.")
        }

        var method = "GET"
        var url = ""
        val headers = mutableMapOf<String, String>()
        var body: String? = null

        var i = 1
        while (i < tokens.size) {
            val t = tokens[i]
            when (t) {
                "-X", "--request" -> {
                    if (i + 1 < tokens.size) method = tokens[++i].uppercase()
                }
                "-H", "--header" -> {
                    if (i + 1 < tokens.size) {
                        val headerStr = tokens[++i]
                        val colon = headerStr.indexOf(':')
                        if (colon > 0) {
                            val k = headerStr.substring(0, colon).trim()
                            val v = headerStr.substring(colon + 1).trim()
                            headers[k] = v
                        }
                    }
                }
                "-d", "--data", "--data-raw", "--data-binary" -> {
                    if (i + 1 < tokens.size) {
                        body = tokens[++i]
                        if (method == "GET") method = "POST"
                    }
                }
                else -> {
                    if (t.startsWith("http://") || t.startsWith("https://")) {
                        url = t
                    } else if (url.isEmpty() && !t.startsWith("-") && t.contains(".")) {
                        url = t
                    }
                }
            }
            i++
        }

        if (url.isEmpty()) {
            return ToolResult.Failure("No valid target URL found in cURL command.")
        }

        // Parse query params from URL
        val queryMap = mutableMapOf<String, String>()
        if (url.contains("?")) {
            val queryStr = url.substringAfter("?")
            for (param in queryStr.split("&")) {
                val pair = param.split("=", limit = 2)
                if (pair.isNotEmpty()) {
                    queryMap[pair[0]] = if (pair.size > 1) pair[1] else ""
                }
            }
        }

        // Generate Code Snippets
        val pythonSnippet = buildString {
            appendLine("import requests")
            appendLine()
            appendLine("url = \"$url\"")
            if (headers.isNotEmpty()) {
                appendLine("headers = {")
                headers.forEach { (k, v) -> appendLine("    \"$k\": \"$v\",") }
                appendLine("}")
            }
            if (body != null) {
                appendLine("payload = $body")
                appendLine("response = requests.${method.lowercase()}(url, headers=headers, json=payload)")
            } else {
                val hParam = if (headers.isNotEmpty()) ", headers=headers" else ""
                appendLine("response = requests.${method.lowercase()}(url$hParam)")
            }
            appendLine("print(response.status_code, response.text)")
        }

        val jsSnippet = buildString {
            appendLine("const url = \"$url\";")
            appendLine("const options = {")
            appendLine("  method: \"$method\",")
            if (headers.isNotEmpty()) {
                appendLine("  headers: {")
                headers.forEach { (k, v) -> appendLine("    \"$k\": \"$v\",") }
                appendLine("  },")
            }
            if (body != null) {
                appendLine("  body: JSON.stringify($body),")
            }
            appendLine("};")
            appendLine("const response = await fetch(url, options);")
            appendLine("const data = await response.json();")
        }

        val kotlinSnippet = buildString {
            appendLine("val url = java.net.URL(\"$url\")")
            appendLine("val conn = url.openConnection() as java.net.HttpURLConnection")
            appendLine("conn.requestMethod = \"$method\"")
            headers.forEach { (k, v) ->
                appendLine("conn.setRequestProperty(\"$k\", \"$v\")")
            }
            if (body != null) {
                appendLine("conn.doOutput = true")
                appendLine("conn.outputStream.use { it.write(\"\"\"$body\"\"\".toByteArray()) }")
            }
            appendLine("val responseCode = conn.responseCode")
        }

        val report = buildString {
            appendLine("cURL COMMAND PARSING AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("HTTP Method:   $method")
            appendLine("Target URL:    $url")
            appendLine("Headers Count: ${headers.size}")
            appendLine("Query Params:  ${queryMap.size}")
            appendLine("Payload Size:  ${body?.length ?: 0} chars")
            appendLine()
            appendLine("HEADERS:")
            if (headers.isEmpty()) appendLine("• (None)")
            else headers.forEach { (k, v) -> appendLine("• $k: $v") }
            appendLine()
            if (body != null) {
                appendLine("REQUEST BODY:")
                appendLine(body)
                appendLine()
            }
            appendLine("PYTHON CODE SNIPPET:")
            appendLine(pythonSnippet)
        }

        val summary = "$method $url (${headers.size} headers)"

        return ToolResult.Success(
            data = CurlParserOutput(
                method = method,
                url = url,
                queryParams = queryMap,
                headers = headers,
                body = body,
                kotlinSnippet = kotlinSnippet,
                pythonSnippet = pythonSnippet,
                javascriptSnippet = jsSnippet,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun tokenizeCommand(cmd: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inSingle = false
        var inDouble = false
        var escape = false

        for (c in cmd) {
            if (escape) {
                sb.append(c)
                escape = false
                continue
            }
            when (c) {
                '\\' -> {
                    escape = true
                }
                '\'' -> {
                    if (!inDouble) inSingle = !inSingle else sb.append(c)
                }
                '\"' -> {
                    if (!inSingle) inDouble = !inDouble else sb.append(c)
                }
                ' ', '\t', '\n', '\r' -> {
                    if (inSingle || inDouble) {
                        sb.append(c)
                    } else if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> sb.append(c)
            }
        }
        if (sb.isNotEmpty()) tokens.add(sb.toString())
        return tokens
    }
}
