package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class HtmlToMarkdownInput(
    val html: String = "",
    val headingStyleAtx: Boolean = true // # vs underlined
)

data class HtmlToMarkdownOutput(
    val markdown: String,
    val elementsConverted: Int,
    val originalLength: Int,
    val markdownLength: Int,
    val summary: String
)

class HtmlToMarkdownTool : Tool<HtmlToMarkdownInput, HtmlToMarkdownOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "html_to_markdown_tool",
        name = "HTML to Markdown Converter",
        description = "Convert HTML markup into clean, readable GitHub-Flavored Markdown offline.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("html", "markdown", "converter", "gfm", "parser", "format", "web"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Code"
    )

    override suspend fun execute(input: HtmlToMarkdownInput): ToolResult<HtmlToMarkdownOutput> {
        val startTime = System.currentTimeMillis()

        if (input.html.isBlank()) {
            return ToolResult.Failure(
                message = "HTML input is empty.",
                userGuidance = "Paste HTML markup to convert into Markdown."
            )
        }

        return try {
            val md = convertHtml(input.html)
            val summary = "Converted ${input.html.length} chars HTML -> ${md.length} chars Markdown"

            ToolResult.Success(
                data = HtmlToMarkdownOutput(
                    markdown = md,
                    elementsConverted = 1,
                    originalLength = input.html.length,
                    markdownLength = md.length,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("HTML conversion error: ${e.message}", cause = e)
        }
    }

    private fun convertHtml(html: String): String {
        var text = html

        // 1. Remove script, style, comments
        text = text.replace(Regex("(?si)<script.*?</script>"), "")
        text = text.replace(Regex("(?si)<style.*?</style>"), "")
        text = text.replace(Regex("(?si)<head.*?</head>"), "")
        text = text.replace(Regex("(?s)<!--.*?-->"), "")

        // 2. Preformatted code blocks
        text = text.replace(Regex("(?si)<pre[^>]*><code(?:\\s+class=[\"'](?:language-)?([a-zA-Z0-9_-]+)[\"'])?[^>]*>(.*?)</code></pre>")) { match ->
            val lang = match.groupValues[1]
            val code = unescapeHtml(match.groupValues[2])
            "\n\n```$lang\n${code.trim()}\n```\n\n"
        }
        text = text.replace(Regex("(?si)<pre[^>]*>(.*?)</pre>")) { match ->
            val code = unescapeHtml(match.groupValues[1])
            "\n\n```\n${code.trim()}\n```\n\n"
        }

        // 3. Headings h1..h6
        for (i in 1..6) {
            val hashes = "#".repeat(i)
            text = text.replace(Regex("(?si)<h$i[^>]*>(.*?)</h$i>")) { match ->
                "\n\n$hashes ${match.groupValues[1].trim()}\n\n"
            }
        }

        // 4. Horizontal rules
        text = text.replace(Regex("(?i)<hr\\s*/?>"), "\n\n---\n\n")

        // 5. Blockquotes
        text = text.replace(Regex("(?si)<blockquote[^>]*>(.*?)</blockquote>")) { match ->
            val content = match.groupValues[1].trim()
            "\n\n" + content.lines().joinToString("\n") { "> $it" } + "\n\n"
        }

        // 6. Inline code
        text = text.replace(Regex("(?si)<code[^>]*>(.*?)</code>")) { match ->
            "`${unescapeHtml(match.groupValues[1])}`"
        }

        // 7. Bold and strong
        text = text.replace(Regex("(?si)<(strong|b)[^>]*>(.*?)</\\1>")) { match ->
            "**${match.groupValues[2].trim()}**"
        }

        // 8. Italic and em
        text = text.replace(Regex("(?si)<(em|i)[^>]*>(.*?)</\\1>")) { match ->
            "*${match.groupValues[2].trim()}*"
        }

        // 9. Links: <a href="url">text</a>
        text = text.replace(Regex("(?si)<a\\s+[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>")) { match ->
            val url = match.groupValues[1]
            val label = match.groupValues[2].trim()
            if (label.isEmpty()) url else "[$label]($url)"
        }

        // 10. Images: <img src="url" alt="alt">
        text = text.replace(Regex("(?si)<img\\s+[^>]*src=[\"']([^\"']*)[\"'][^>]*alt=[\"']([^\"']*)[\"'][^>]*>")) { match ->
            val src = match.groupValues[1]
            val alt = match.groupValues[2]
            "![$alt]($src)"
        }
        text = text.replace(Regex("(?si)<img\\s+[^>]*src=[\"']([^\"']*)[\"'][^>]*>")) { match ->
            val src = match.groupValues[1]
            "![image]($src)"
        }

        // 11. Unordered lists
        text = text.replace(Regex("(?si)<ul[^>]*>(.*?)</ul>")) { match ->
            val items = match.groupValues[1]
            val convertedItems = items.replace(Regex("(?si)<li[^>]*>(.*?)</li>")) { liMatch ->
                "- ${liMatch.groupValues[1].trim()}\n"
            }
            "\n\n$convertedItems\n\n"
        }

        // 12. Ordered lists
        text = text.replace(Regex("(?si)<ol[^>]*>(.*?)</ol>")) { match ->
            var idx = 1
            val items = match.groupValues[1]
            val convertedItems = items.replace(Regex("(?si)<li[^>]*>(.*?)</li>")) { liMatch ->
                "${idx++}. ${liMatch.groupValues[1].trim()}\n"
            }
            "\n\n$convertedItems\n\n"
        }

        // 13. Paragraphs & Line Breaks
        text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
        text = text.replace(Regex("(?si)<p[^>]*>(.*?)</p>")) { match ->
            "\n\n${match.groupValues[1].trim()}\n\n"
        }

        // 14. Strip any remaining unknown tags
        text = text.replace(Regex("<[^>]+>"), "")

        // 15. Unescape HTML entities and normalize whitespace
        text = unescapeHtml(text)
        return text.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&copy;", "©")
    }
}
