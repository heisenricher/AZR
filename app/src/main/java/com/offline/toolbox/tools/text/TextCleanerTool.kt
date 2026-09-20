package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class TextCleanerOptions(
    val removeExtraSpaces: Boolean = true,
    val trimLines: Boolean = true,
    val removeEmptyLines: Boolean = true,
    val stripHtmlTags: Boolean = false,
    val normalizeLineBreaks: Boolean = true,
    val removeSpecialCharacters: Boolean = false
)

data class TextCleanerInput(
    val text: String,
    val options: TextCleanerOptions = TextCleanerOptions()
)

data class TextCleanerOutput(
    val cleanedText: String,
    val charactersRemoved: Int,
    val linesRemoved: Int,
    val summary: String
)

class TextCleanerTool : Tool<TextCleanerInput, TextCleanerOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "text_cleaner",
        name = "Text Cleaner",
        description = "Clean up messy text: remove duplicate spaces, trim lines, remove blank lines, and strip HTML.",
        category = ToolCategory.TEXT,
        tags = listOf("clean", "trim", "spaces", "empty lines", "strip html", "format", "whitespace"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "CleaningServices"
    )

    override suspend fun execute(input: TextCleanerInput): ToolResult<TextCleanerOutput> {
        val startTime = System.currentTimeMillis()
        var current = input.text
        val originalCharCount = current.length
        val originalLineCount = if (current.isEmpty()) 0 else current.lines().size

        val opts = input.options

        if (opts.normalizeLineBreaks) {
            current = current.replace("\r\n", "\n").replace("\r", "\n")
        }

        if (opts.stripHtmlTags) {
            current = current.replace(Regex("<[^>]*>"), "")
        }

        if (opts.removeSpecialCharacters) {
            // Keep letters, digits, whitespace, and standard punctuation
            current = current.filter { it.isLetterOrDigit() || it.isWhitespace() || it in ".,!?;:'\"-()[]{}@#%&*+=/\\" }
        }

        val lines = current.lines()
        val processedLines = mutableListOf<String>()

        for (line in lines) {
            var l = line
            if (opts.trimLines) {
                l = l.trim()
            }
            if (opts.removeExtraSpaces) {
                l = l.replace(Regex("[ \\t]+"), " ")
            }
            if (opts.removeEmptyLines) {
                if (l.isNotBlank()) {
                    processedLines.add(l)
                }
            } else {
                processedLines.add(l)
            }
        }

        val result = processedLines.joinToString("\n")
        val charsRemoved = (originalCharCount - result.length).coerceAtLeast(0)
        val linesRemoved = (originalLineCount - processedLines.size).coerceAtLeast(0)

        val summary = "Removed $charsRemoved chars and $linesRemoved lines"

        return ToolResult.Success(
            data = TextCleanerOutput(
                cleanedText = result,
                charactersRemoved = charsRemoved,
                linesRemoved = linesRemoved,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
