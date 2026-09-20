package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class MarkdownStats(
    val headingCount: Int,
    val linkCount: Int,
    val imageCount: Int,
    val codeBlockCount: Int,
    val tableRowCount: Int,
    val wordCount: Int,
    val estimatedReadTimeMinutes: Double
)

data class MarkdownPreviewOutput(
    val plainText: String,
    val headingsOutline: List<String>,
    val stats: MarkdownStats,
    val summary: String
)

class MarkdownPreviewTool : Tool<String, MarkdownPreviewOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "markdown_preview",
        name = "Markdown Inspector & Parser",
        description = "Analyze Markdown documents: extract table of contents outline, count links/images/code blocks, and convert to clean text.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("markdown", "md", "preview", "outline", "headings", "links", "text", "read time"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Description"
    )

    override suspend fun execute(input: String): ToolResult<MarkdownPreviewOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.trim()

        if (text.isEmpty()) {
            return ToolResult.Success(
                data = MarkdownPreviewOutput(
                    plainText = "",
                    headingsOutline = emptyList(),
                    stats = MarkdownStats(0, 0, 0, 0, 0, 0, 0.0),
                    summary = "Empty document"
                ),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        val lines = text.lines()
        val headings = mutableListOf<String>()
        var codeBlockCount = 0
        var tableRowCount = 0
        var inCodeBlock = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (!inCodeBlock) codeBlockCount++
                inCodeBlock = !inCodeBlock
                continue
            }
            if (inCodeBlock) continue

            if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length
                if (level in 1..6 && trimmed.length > level && trimmed[level] == ' ') {
                    val title = trimmed.substring(level).trim()
                    val indent = "  ".repeat(level - 1)
                    headings.add("$indent• H$level: $title")
                }
            }

            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                tableRowCount++
            }
        }

        val linkCount = Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)").findAll(text).count()
        val imageCount = Regex("!\\[([^\\]]*)\\]\\(([^\\)]+)\\)").findAll(text).count()

        // Strip basic markdown syntax for clean text preview
        var clean = text
            .replace(Regex("```[\\s\\S]*?```"), "[Code Block]")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("!\\[([^\\]]*)\\]\\([^\\)]+\\)"), "[Image: $1]")
            .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")
            .replace(Regex("(?m)^#{1,6}\\s+"), "")
            .replace(Regex("(?m)^[\\*\\-\\+]\\s+"), "• ")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")

        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val readTime = (words / 200.0).coerceAtLeast(0.1)

        val stats = MarkdownStats(
            headingCount = headings.size,
            linkCount = linkCount,
            imageCount = imageCount,
            codeBlockCount = codeBlockCount,
            tableRowCount = tableRowCount,
            wordCount = words,
            estimatedReadTimeMinutes = readTime
        )

        val summary = "${headings.size} headings • $words words • ~${"%.1f".format(readTime)} min read"

        return ToolResult.Success(
            data = MarkdownPreviewOutput(
                plainText = clean,
                headingsOutline = headings,
                stats = stats,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
