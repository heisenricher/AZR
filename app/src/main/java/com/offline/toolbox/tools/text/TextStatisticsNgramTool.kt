package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class NgramFrequencyItem(
    val ngram: String,
    val count: Int,
    val percentage: Double
)

data class TextStatisticsNgramInput(
    val text: String = """
        The quick brown fox jumps over the lazy dog. The dog barks at the fox, and the fox jumps high into the air.
    """.trimIndent(),
    val caseSensitive: Boolean = false,
    val filterStopwords: Boolean = false,
    val topN: Int = 5
)

data class TextStatisticsNgramOutput(
    val totalWordCount: Int,
    val uniqueWordCount: Int,
    val typeTokenRatio: Double,
    val hapaxLegomenaCount: Int, // Words appearing exactly once
    val topUnigrams: List<NgramFrequencyItem>,
    val topBigrams: List<NgramFrequencyItem>,
    val topTrigrams: List<NgramFrequencyItem>,
    val formattedReport: String,
    val summary: String
)

class TextStatisticsNgramTool : Tool<TextStatisticsNgramInput, TextStatisticsNgramOutput> {
    companion object {
        private val BASIC_STOPWORDS = setOf(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
            "any", "are", "as", "at", "be", "because", "been", "before", "being", "below",
            "between", "both", "but", "by", "for", "from", "further", "had", "has", "have",
            "he", "her", "here", "him", "his", "how", "i", "if", "in", "into", "is", "it",
            "its", "me", "my", "no", "nor", "not", "of", "off", "on", "once", "only", "or",
            "other", "our", "out", "over", "own", "same", "so", "some", "such", "than",
            "that", "the", "their", "them", "then", "there", "these", "they", "this", "those",
            "through", "to", "too", "under", "until", "up", "very", "was", "we", "were",
            "what", "when", "where", "which", "while", "who", "whom", "why", "with", "you", "your"
        )
    }

    override val metadata: ToolMetadata = ToolMetadata(
        id = "text_statistics_ngram_tool",
        name = "N-Gram Frequency & Lexical Diversity Analyzer",
        description = "Tokenize prose into unigrams, bigrams, and trigrams, compute frequency rankings, and calculate Type-Token Ratio (TTR) lexical richness.",
        category = ToolCategory.TEXT,
        tags = listOf("ngram", "unigram", "bigram", "trigram", "nlp", "text statistics", "frequency", "lexical diversity", "ttr", "corpus"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "BarChart2"
    )

    override suspend fun execute(input: TextStatisticsNgramInput): ToolResult<TextStatisticsNgramOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.text.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Text cannot be empty.")
        }

        // Tokenize text into words
        val processed = if (input.caseSensitive) raw else raw.lowercase(Locale.US)
        val rawTokens = processed.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.isNotBlank() }

        if (rawTokens.isEmpty()) {
            return ToolResult.Failure("No valid alphanumeric words found in text.")
        }

        val tokens = if (input.filterStopwords) {
            rawTokens.filter { it.lowercase(Locale.US) !in BASIC_STOPWORDS }
        } else {
            rawTokens
        }

        if (tokens.isEmpty()) {
            return ToolResult.Failure("All words were filtered out by stopword list.")
        }

        val totalTokens = tokens.size
        val limit = input.topN.coerceIn(1, 50)

        // 1. Unigrams
        val unigramCounts = mutableMapOf<String, Int>()
        tokens.forEach { unigramCounts[it] = unigramCounts.getOrDefault(it, 0) + 1 }

        val uniqueTokens = unigramCounts.size
        val ttr = uniqueTokens.toDouble() / totalTokens
        val hapax = unigramCounts.count { it.value == 1 }

        val topUnigrams = unigramCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { NgramFrequencyItem(it.key, it.value, (it.value.toDouble() / totalTokens) * 100.0) }

        // 2. Bigrams
        val bigramCounts = mutableMapOf<String, Int>()
        for (i in 0 until tokens.size - 1) {
            val bg = "${tokens[i]} ${tokens[i + 1]}"
            bigramCounts[bg] = bigramCounts.getOrDefault(bg, 0) + 1
        }
        val totalBigrams = (tokens.size - 1).coerceAtLeast(1)
        val topBigrams = bigramCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { NgramFrequencyItem(it.key, it.value, (it.value.toDouble() / totalBigrams) * 100.0) }

        // 3. Trigrams
        val trigramCounts = mutableMapOf<String, Int>()
        for (i in 0 until tokens.size - 2) {
            val tg = "${tokens[i]} ${tokens[i + 1]} ${tokens[i + 2]}"
            trigramCounts[tg] = trigramCounts.getOrDefault(tg, 0) + 1
        }
        val totalTrigrams = (tokens.size - 2).coerceAtLeast(1)
        val topTrigrams = trigramCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { NgramFrequencyItem(it.key, it.value, (it.value.toDouble() / totalTrigrams) * 100.0) }

        val report = buildString {
            appendLine("N-GRAM FREQUENCY & LEXICAL RICHNESS REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Total Analyzed Tokens:    $totalTokens")
            appendLine("Unique Vocabulary Types:  $uniqueTokens")
            appendLine("Type-Token Ratio (TTR):   ${String.format(Locale.US, "%.4f (%.1f%%)", ttr, ttr * 100.0)}")
            appendLine("Hapax Legomena (Single):  $hapax words (${String.format(Locale.US, "%.1f%% of types", (hapax.toDouble() / uniqueTokens) * 100.0)})")
            appendLine("Case Sensitive:           ${input.caseSensitive}")
            appendLine("Stopwords Excluded:       ${input.filterStopwords}")
            appendLine("--------------------------------------------------")
            appendLine("TOP UNIGRAMS (1-Word Frequency):")
            topUnigrams.forEach { appendLine(" - \"${it.ngram}\": ${it.count} (${String.format(Locale.US, "%.1f%%", it.percentage)})") }
            appendLine("--------------------------------------------------")
            appendLine("TOP BIGRAMS (2-Word Collocations):")
            if (topBigrams.isEmpty()) appendLine(" (Insufficient word length for bigrams)")
            topBigrams.forEach { appendLine(" - \"${it.ngram}\": ${it.count} (${String.format(Locale.US, "%.1f%%", it.percentage)})") }
            appendLine("--------------------------------------------------")
            appendLine("TOP TRIGRAMS (3-Word Phrases):")
            if (topTrigrams.isEmpty()) appendLine(" (Insufficient word length for trigrams)")
            topTrigrams.forEach { appendLine(" - \"${it.ngram}\": ${it.count} (${String.format(Locale.US, "%.1f%%", it.percentage)})") }
        }

        val output = TextStatisticsNgramOutput(
            totalWordCount = totalTokens,
            uniqueWordCount = uniqueTokens,
            typeTokenRatio = ttr,
            hapaxLegomenaCount = hapax,
            topUnigrams = topUnigrams,
            topBigrams = topBigrams,
            topTrigrams = topTrigrams,
            formattedReport = report,
            summary = "Tokens: $totalTokens, TTR: ${String.format(Locale.US, "%.2f", ttr)}, Top word: \"${topUnigrams.firstOrNull()?.ngram ?: ""}\""
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Extracted n-grams and calculated lexical diversity"
        )
    }
}
