package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.max

data class ReadabilityInput(
    val text: String = """
        The offline architecture provides complete privacy and zero data leakage. 
        Engineers designed the toolbox to compute cryptographic primitives directly on the local device.
        Every utility functions seamlessly without requiring an internet connection or external servers.
    """.trimIndent()
)

data class ReadabilityOutput(
    val fleschReadingEase: Double,
    val fleschReadingEaseRating: String,
    val fleschKincaidGradeLevel: Double,
    val gunningFogIndex: Double,
    val colemanLiauIndex: Double,
    val wordCount: Int,
    val sentenceCount: Int,
    val syllableCount: Int,
    val complexWordCount: Int,
    val formattedReport: String,
    val summary: String
)

class ReadabilityScoreTool : Tool<ReadabilityInput, ReadabilityOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "readability_score_tool",
        name = "Readability & Grade Level Analyzer",
        description = "Analyze text comprehension and grade level using Flesch Reading Ease, Flesch-Kincaid, Gunning Fog, and Coleman-Liau indexes.",
        category = ToolCategory.TEXT,
        tags = listOf("readability", "flesch", "kincaid", "gunning fog", "coleman liau", "grade", "reading", "text", "writing"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Spellcheck"
    )

    override suspend fun execute(input: ReadabilityInput): ToolResult<ReadabilityOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.text.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        val sentences = raw.split(Regex("[.!?]+\\s*")).filter { it.isNotBlank() }
        val words = raw.split(Regex("[\\s,;:\"'()\\[\\]{}]+")).filter { it.any { c -> c.isLetter() } }

        if (words.isEmpty() || sentences.isEmpty()) {
            return ToolResult.Failure("Text must contain at least one complete sentence with words.")
        }

        val totalWords = words.size
        val totalSentences = max(1, sentences.size)
        val totalLetters = words.sumOf { w -> w.count { it.isLetter() } }

        var totalSyllables = 0
        var complexWords = 0

        for (w in words) {
            val sCount = countSyllables(w)
            totalSyllables += sCount
            if (sCount >= 3) complexWords++
        }

        val asl = totalWords.toDouble() / totalSentences.toDouble() // Average sentence length
        val asw = totalSyllables.toDouble() / totalWords.toDouble() // Average syllables per word

        // 1. Flesch Reading Ease
        val fre = 206.835 - (1.015 * asl) - (84.6 * asw)
        val freRating = when {
            fre >= 90 -> "Very Easy (5th grade reading level)"
            fre >= 80 -> "Easy (6th grade reading level)"
            fre >= 70 -> "Fairly Easy (7th grade reading level)"
            fre >= 60 -> "Standard / Plain English (8th-9th grade level)"
            fre >= 50 -> "Fairly Difficult (10th-12th grade / High School)"
            fre >= 30 -> "Difficult (College level text)"
            else -> "Very Difficult / Academic (University Graduate level)"
        }

        // 2. Flesch-Kincaid Grade Level
        val fkgl = (0.39 * asl) + (11.8 * asw) - 15.59

        // 3. Gunning Fog Index
        val pctComplex = (complexWords.toDouble() / totalWords.toDouble()) * 100.0
        val fog = 0.4 * (asl + pctComplex)

        // 4. Coleman-Liau Index
        val l = (totalLetters.toDouble() / totalWords.toDouble()) * 100.0 // letters per 100 words
        val s = (totalSentences.toDouble() / totalWords.toDouble()) * 100.0 // sentences per 100 words
        val cl = (0.0588 * l) - (0.296 * s) - 15.8

        val report = buildString {
            appendLine("TEXT READABILITY & COMPREHENSION AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Corpus Size:         $totalWords words | $totalSentences sentences | $totalLetters characters")
            appendLine("Syllable Density:    $totalSyllables syllables (${String.format(Locale.US, "%.2f", asw)} syl/word)")
            appendLine("Sentence Length:     ${String.format(Locale.US, "%.1f", asl)} words/sentence")
            appendLine("Complex Words (≥3):  $complexWords (${String.format(Locale.US, "%.1f", pctComplex)}%)")
            appendLine()
            appendLine("READABILITY INDEXES:")
            appendLine("• Flesch Reading Ease:       ${String.format(Locale.US, "%.1f", fre)} / 100")
            appendLine("  Grade Assessment:          $freRating")
            appendLine("• Flesch-Kincaid Grade:      Grade ${String.format(Locale.US, "%.1f", max(1.0, fkgl))}")
            appendLine("• Gunning Fog Index:         ${String.format(Locale.US, "%.1f", max(1.0, fog))} (Years of formal education)")
            appendLine("• Coleman-Liau Index:        Grade ${String.format(Locale.US, "%.1f", max(1.0, cl))}")
        }

        val summary = "Flesch: ${String.format(Locale.US, "%.0f", fre)} | Grade: ${String.format(Locale.US, "%.1f", max(1.0, fkgl))}"

        return ToolResult.Success(
            data = ReadabilityOutput(
                fleschReadingEase = fre,
                fleschReadingEaseRating = freRating,
                fleschKincaidGradeLevel = fkgl,
                gunningFogIndex = fog,
                colemanLiauIndex = cl,
                wordCount = totalWords,
                sentenceCount = totalSentences,
                syllableCount = totalSyllables,
                complexWordCount = complexWords,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun countSyllables(word: String): Int {
        val w = word.lowercase(Locale.ROOT).filter { it.isLetter() }
        if (w.length <= 3) return 1

        val clean = if (w.endsWith("e") && !w.endsWith("le")) w.dropLast(1) else w
        val vowels = "aeiouy"
        var syllables = 0
        var prevWasVowel = false

        for (c in clean) {
            val isVowel = c in vowels
            if (isVowel && !prevWasVowel) {
                syllables++
            }
            prevWasVowel = isVowel
        }
        return max(1, syllables)
    }
}
