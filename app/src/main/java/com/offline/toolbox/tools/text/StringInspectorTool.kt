package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.nio.charset.StandardCharsets

data class StringInspectorOutput(
    val charCount: Int,
    val codePointCount: Int,
    val utf8Bytes: Int,
    val utf16Bytes: Int,
    val lineCount: Int,
    val asciiCount: Int,
    val nonAsciiCount: Int,
    val digitCount: Int,
    val whitespaceCount: Int,
    val punctuationCount: Int,
    val detailsFormatted: String,
    val summary: String
)

class StringInspectorTool : Tool<String, StringInspectorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "string_inspector",
        name = "Unicode & String Inspector",
        description = "Inspect character counts, Unicode code points, UTF-8/UTF-16 byte lengths, ASCII and whitespace breakdown.",
        category = ToolCategory.TEXT,
        tags = listOf("unicode", "inspect", "string", "bytes", "codepoints", "utf8", "ascii", "length", "analyzer"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Search"
    )

    override suspend fun execute(input: String): ToolResult<StringInspectorOutput> {
        val startTime = System.currentTimeMillis()
        val charCount = input.length
        val codePointCount = input.codePointCount(0, input.length)
        val utf8Bytes = input.toByteArray(StandardCharsets.UTF_8).size
        val utf16Bytes = input.toByteArray(StandardCharsets.UTF_16).size
        val lineCount = if (input.isEmpty()) 0 else input.lines().size

        var asciiCount = 0
        var nonAsciiCount = 0
        var digitCount = 0
        var whitespaceCount = 0
        var punctuationCount = 0

        for (ch in input) {
            if (ch.code in 0..127) asciiCount++ else nonAsciiCount++
            if (ch.isDigit()) digitCount++
            if (ch.isWhitespace()) whitespaceCount++
            if (ch in "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~") punctuationCount++
        }

        val details = buildString {
            appendLine("Characters (Length):     $charCount")
            appendLine("Unicode Code Points:     $codePointCount")
            appendLine("UTF-8 Size:              $utf8Bytes bytes")
            appendLine("UTF-16 Size:             $utf16Bytes bytes")
            appendLine("Lines:                   $lineCount")
            appendLine("--------------------------------")
            appendLine("ASCII Characters:        $asciiCount")
            appendLine("Non-ASCII / Unicode:     $nonAsciiCount")
            appendLine("Numeric Digits:          $digitCount")
            appendLine("Whitespace:              $whitespaceCount")
            appendLine("Punctuation Marks:       $punctuationCount")
        }

        val summary = "$charCount chars • $codePointCount code points • $utf8Bytes UTF-8 bytes"

        return ToolResult.Success(
            data = StringInspectorOutput(
                charCount = charCount,
                codePointCount = codePointCount,
                utf8Bytes = utf8Bytes,
                utf16Bytes = utf16Bytes,
                lineCount = lineCount,
                asciiCount = asciiCount,
                nonAsciiCount = nonAsciiCount,
                digitCount = digitCount,
                whitespaceCount = whitespaceCount,
                punctuationCount = punctuationCount,
                detailsFormatted = details,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
