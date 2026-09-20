package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class TextWrapInput(
    val text: String = "AZR is an offline Android toolbox built with Jetpack Compose Material 3 and designed to provide privacy-first utilities without network permissions.",
    val columnWidth: Int = 40,
    val linePrefix: String = "",
    val firstLineIndent: String = "",
    val subsequentIndent: String = "",
    val reflowParagraphs: Boolean = true
)

data class TextWrapOutput(
    val wrappedText: String,
    val lineCount: Int,
    val maxLineWidth: Int,
    val columnWidth: Int,
    val summary: String
)

class TextWrapTool : Tool<TextWrapInput, TextWrapOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "text_wrap_tool",
        name = "Word Wrap & Paragraph Reflower",
        description = "Wrap text to specific column widths (72, 80, 100) with custom line prefixes, margins, and hanging indents.",
        category = ToolCategory.TEXT,
        tags = listOf("wrap", "word wrap", "reflow", "indent", "paragraph", "terminal", "margin", "format"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "WrapText"
    )

    override suspend fun execute(input: TextWrapInput): ToolResult<TextWrapOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.text

        if (raw.isEmpty()) {
            return ToolResult.Success(
                TextWrapOutput("", 0, 0, input.columnWidth, "Empty text")
            )
        }

        val limit = input.columnWidth.coerceIn(10, 500)
        val paragraphs = if (input.reflowParagraphs) {
            raw.split(Regex("(\r?\n){2,}")).map { p ->
                p.replace(Regex("\\s+"), " ").trim()
            }
        } else {
            raw.lines()
        }

        val wrappedLines = mutableListOf<String>()

        for ((pIdx, paragraph) in paragraphs.withIndex()) {
            if (paragraph.isEmpty()) {
                wrappedLines.add("")
                continue
            }

            val words = paragraph.split(" ").filter { it.isNotEmpty() }
            var currentLine = StringBuilder()
            var isFirstLineOfParagraph = true

            for (w in words) {
                val prefix = if (isFirstLineOfParagraph) {
                    input.linePrefix + input.firstLineIndent
                } else {
                    input.linePrefix + input.subsequentIndent
                }

                val testLine = if (currentLine.isEmpty()) "$prefix$w" else "$currentLine $w"

                if (testLine.length <= limit) {
                    if (currentLine.isEmpty()) currentLine.append(prefix) else currentLine.append(" ")
                    currentLine.append(w)
                } else {
                    if (currentLine.isNotEmpty()) {
                        wrappedLines.add(currentLine.toString())
                        isFirstLineOfParagraph = false
                    }
                    val nextPrefix = input.linePrefix + input.subsequentIndent
                    currentLine = StringBuilder("$nextPrefix$w")
                }
            }

            if (currentLine.isNotEmpty()) {
                wrappedLines.add(currentLine.toString())
            }

            if (input.reflowParagraphs && pIdx < paragraphs.size - 1) {
                wrappedLines.add("")
            }
        }

        val resultStr = wrappedLines.joinToString("\n")
        val maxLen = wrappedLines.maxOfOrNull { it.length } ?: 0
        val summary = "Wrapped into ${wrappedLines.size} line(s) at width $limit"

        return ToolResult.Success(
            data = TextWrapOutput(
                wrappedText = resultStr,
                lineCount = wrappedLines.size,
                maxLineWidth = maxLen,
                columnWidth = limit,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
