package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class JustifierInput(
    val text: String = """
        The offline architecture provides complete privacy and zero data leakage. 
        Engineers designed the toolbox to compute cryptographic primitives directly on the local device. 
        Every utility functions seamlessly without requiring an internet connection.
    """.trimIndent(),
    val lineWidth: Int = 45
)

data class JustifierOutput(
    val justifiedText: String,
    val totalLines: Int,
    val lineWidth: Int,
    val formattedReport: String,
    val summary: String
)

class TextJustifierTool : Tool<JustifierInput, JustifierOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "text_justifier_tool",
        name = "Typographic Full Text Justifier",
        description = "Format and align paragraphs to exact column widths with typographic space distribution (book/typeset justification).",
        category = ToolCategory.TEXT,
        tags = listOf("justify", "typography", "align", "typeset", "format", "column", "width", "paragraph", "text"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FormatAlignJustify"
    )

    override suspend fun execute(input: JustifierInput): ToolResult<JustifierOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.text.trim()
        val width = input.lineWidth.coerceIn(20, 120)

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        val paragraphs = raw.split(Regex("\n\n+"))
        val justifiedParagraphs = mutableListOf<String>()

        for (para in paragraphs) {
            val words = para.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isEmpty()) continue

            val lines = mutableListOf<List<String>>()
            var currentLine = mutableListOf<String>()
            var currentLen = 0

            for (w in words) {
                if (currentLine.isEmpty()) {
                    currentLine.add(w)
                    currentLen = w.length
                } else if (currentLen + 1 + w.length <= width) {
                    currentLine.add(w)
                    currentLen += 1 + w.length
                } else {
                    lines.add(currentLine)
                    currentLine = mutableListOf(w)
                    currentLen = w.length
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }

            // Format lines with justification
            val paraBuilder = StringBuilder()
            for (i in lines.indices) {
                val lineWords = lines[i]
                val isLastLine = (i == lines.size - 1)

                if (isLastLine || lineWords.size == 1) {
                    // Left align
                    paraBuilder.appendLine(lineWords.joinToString(" "))
                } else {
                    val totalCharLen = lineWords.sumOf { it.length }
                    val totalSpaces = width - totalCharLen
                    val gaps = lineWords.size - 1
                    val baseSpace = totalSpaces / gaps
                    val extraSpaces = totalSpaces % gaps

                    val lineStr = buildString {
                        for (j in lineWords.indices) {
                            append(lineWords[j])
                            if (j < gaps) {
                                val spacesToAdd = baseSpace + if (j < extraSpaces) 1 else 0
                                append(" ".repeat(spacesToAdd))
                            }
                        }
                    }
                    paraBuilder.appendLine(lineStr)
                }
            }
            justifiedParagraphs.add(paraBuilder.toString().trimEnd())
        }

        val fullJustified = justifiedParagraphs.joinToString("\n\n")
        val lineCount = fullJustified.lines().size

        val report = buildString {
            appendLine("TYPOGRAPHIC FULL JUSTIFICATION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Target Width:  $width columns")
            appendLine("Output Lines:  $lineCount lines")
            appendLine("Paragraphs:    ${justifiedParagraphs.size}")
            appendLine()
            appendLine("JUSTIFIED OUTPUT PREVIEW:")
            appendLine("┌" + "─".repeat(width) + "┐")
            fullJustified.lines().forEach { line ->
                appendLine("│" + line.padEnd(width) + "│")
            }
            appendLine("└" + "─".repeat(width) + "┘")
        }

        val summary = "Justified to $width cols ($lineCount lines)"

        return ToolResult.Success(
            data = JustifierOutput(
                justifiedText = fullJustified,
                totalLines = lineCount,
                lineWidth = width,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
