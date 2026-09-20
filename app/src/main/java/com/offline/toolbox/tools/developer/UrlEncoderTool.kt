package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class UrlOperation(val displayName: String) {
    ENCODE("Encode URL Component"),
    DECODE("Decode URL Component"),
    EXTRACT_QUERY_PARAMS("Extract Query Parameters")
}

data class UrlEncoderInput(
    val text: String,
    val operation: UrlOperation = UrlOperation.ENCODE
)

class UrlEncoderTool : Tool<UrlEncoderInput, String> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "url_encoder",
        name = "URL Encoder & Decoder",
        description = "Percent-encode and decode URLs and inspect query parameters offline.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("url", "encode", "decode", "uri", "percent encoding", "query params", "http"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Link"
    )

    override suspend fun execute(input: UrlEncoderInput): ToolResult<String> {
        val startTime = System.currentTimeMillis()
        val text = input.text.trim()

        if (text.isEmpty()) {
            return ToolResult.Success("", System.currentTimeMillis() - startTime, "Empty input")
        }

        return try {
            val result = when (input.operation) {
                UrlOperation.ENCODE -> {
                    URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
                }
                UrlOperation.DECODE -> {
                    URLDecoder.decode(text, StandardCharsets.UTF_8.toString())
                }
                UrlOperation.EXTRACT_QUERY_PARAMS -> {
                    extractQueryParams(text)
                }
            }

            ToolResult.Success(
                data = result,
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "${input.operation.displayName} complete"
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "URL processing failed: ${e.message}",
                cause = e
            )
        }
    }

    private fun extractQueryParams(url: String): String {
        val queryPart = if (url.contains("?")) {
            url.substringAfter("?").substringBefore("#")
        } else {
            url
        }

        if (queryPart.isEmpty()) return "No query parameters found."

        val pairs = queryPart.split("&").filter { it.isNotBlank() }
        val sb = StringBuilder()
        sb.appendLine("Found ${pairs.size} parameter(s):")
        sb.appendLine("--------------------------------")

        for (pair in pairs) {
            val idx = pair.indexOf("=")
            val key = if (idx >= 0) pair.substring(0, idx) else pair
            val value = if (idx >= 0) pair.substring(idx + 1) else ""
            val decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8.toString())
            val decodedVal = URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
            sb.appendLine("$decodedKey = $decodedVal")
        }

        return sb.toString().trimEnd()
    }
}
