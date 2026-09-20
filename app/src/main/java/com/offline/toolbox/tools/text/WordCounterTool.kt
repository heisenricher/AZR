package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class WordCountStats(
    val wordCount: Int,
    val charCountWithSpaces: Int,
    val charCountWithoutSpaces: Int,
    val lineCount: Int,
    val nonEmptyLineCount: Int,
    val sentenceCount: Int,
    val paragraphCount: Int,
    val averageWordLength: Double,
    val longestWord: String,
    val readingTimeSeconds: Int,
    val speakingTimeSeconds: Int
)

class WordCounterTool : Tool<String, WordCountStats> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "word_counter",
        name = "Word & Character Counter",
        description = "Compute real-time statistics on text: words, characters, sentences, paragraphs, and reading time.",
        category = ToolCategory.TEXT,
        tags = listOf("count", "words", "characters", "statistics", "reading time", "lines"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Numbers"
    )

    override suspend fun execute(input: String): ToolResult<WordCountStats> {
        val startTime = System.currentTimeMillis()
        if (input.isEmpty()) {
            return ToolResult.Success(
                data = WordCountStats(0, 0, 0, 0, 0, 0, 0, 0.0, "", 0, 0),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "0 words, 0 characters"
            )
        }

        val charCountWithSpaces = input.length
        val charCountWithoutSpaces = input.count { !it.isWhitespace() }

        val lines = input.lines()
        val lineCount = lines.size
        val nonEmptyLineCount = lines.count { it.trim().isNotEmpty() }

        // Words extraction
        val words = input.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size

        // Sentences extraction (separated by ., !, ?)
        val sentences = input.split(Regex("[.!?]+")).filter { it.isNotBlank() }
        val sentenceCount = sentences.size

        // Paragraphs extraction (separated by consecutive newlines)
        val paragraphs = input.split(Regex("(\\r?\\n){2,}")).filter { it.isNotBlank() }
        val paragraphCount = if (nonEmptyLineCount == 0) 0 else maxOf(1, paragraphs.size)

        var totalWordLength = 0
        var longest = ""
        for (w in words) {
            val cleanWord = w.filter { it.isLetterOrDigit() }
            totalWordLength += cleanWord.length
            if (cleanWord.length > longest.length) {
                longest = cleanWord
            }
        }
        val avgWordLength = if (wordCount > 0) totalWordLength.toDouble() / wordCount else 0.0

        // Reading speed: ~200 words per minute -> 200 / 60 words per sec
        val readingTimeSec = (wordCount * 60) / 200
        // Speaking speed: ~130 words per minute -> 130 / 60 words per sec
        val speakingTimeSec = (wordCount * 60) / 130

        val stats = WordCountStats(
            wordCount = wordCount,
            charCountWithSpaces = charCountWithSpaces,
            charCountWithoutSpaces = charCountWithoutSpaces,
            lineCount = lineCount,
            nonEmptyLineCount = nonEmptyLineCount,
            sentenceCount = sentenceCount,
            paragraphCount = paragraphCount,
            averageWordLength = String.format(Locale.US, "%.1f", avgWordLength).toDouble(),
            longestWord = longest,
            readingTimeSeconds = readingTimeSec,
            speakingTimeSeconds = speakingTimeSec
        )

        return ToolResult.Success(
            data = stats,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "$wordCount words, $charCountWithSpaces characters"
        )
    }
}
