package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class HtmlEntityMode(val label: String) {
    ENCODE("Encode HTML Entities"),
    DECODE("Decode HTML Entities")
}

data class HtmlEntityInput(
    val text: String,
    val mode: HtmlEntityMode = HtmlEntityMode.ENCODE
)

class HtmlEntityTool : Tool<HtmlEntityInput, String> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "html_entity",
        name = "HTML Entity Encoder & Decoder",
        description = "Escape special characters into HTML entities or unescape them back into readable text.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("html", "entities", "escape", "unescape", "xml", "web", "ampersand"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Html"
    )

    private val entityMap = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&apos;" to "'",
        "&nbsp;" to " ",
        "&copy;" to "©",
        "&reg;" to "®",
        "&trade;" to "™",
        "&euro;" to "€",
        "&pound;" to "£",
        "&yen;" to "¥",
        "&cent;" to "¢",
        "&deg;" to "°",
        "&plusmn;" to "±",
        "&times;" to "×",
        "&divide;" to "÷"
    )

    override suspend fun execute(input: HtmlEntityInput): ToolResult<String> {
        val startTime = System.currentTimeMillis()
        val text = input.text
        if (text.isEmpty()) {
            return ToolResult.Success("", System.currentTimeMillis() - startTime, "Empty input")
        }

        val result = if (input.mode == HtmlEntityMode.ENCODE) {
            encodeHtml(text)
        } else {
            decodeHtml(text)
        }

        return ToolResult.Success(
            data = result,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "${input.mode.label} completed"
        )
    }

    private fun encodeHtml(text: String): String {
        val sb = StringBuilder(text.length * 2)
        for (c in text) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> {
                    if (c.code > 127) {
                        sb.append("&#").append(c.code).append(";")
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun decodeHtml(text: String): String {
        var current = text
        for ((entity, replacement) in entityMap) {
            current = current.replace(entity, replacement)
        }

        // Decimal numeric entities: &#123;
        current = current.replace(Regex("&#([0-9]+);")) { match ->
            try {
                val code = match.groupValues[1].toInt()
                code.toChar().toString()
            } catch (e: Exception) {
                match.value
            }
        }

        // Hex numeric entities: &#x1F600;
        current = current.replace(Regex("&#[xX]([0-9a-fA-F]+);")) { match ->
            try {
                val code = match.groupValues[1].toInt(16)
                code.toChar().toString()
            } catch (e: Exception) {
                match.value
            }
        }

        return current
    }
}
