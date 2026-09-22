package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class StemmerInput(
    val text: String = "The developer was connecting multiple microservices, debugging connections, and writing scalable applications."
)

data class WordStemPair(
    val original: String,
    val stem: String,
    val changed: Boolean
)

data class StemmerOutput(
    val originalText: String,
    val stemmedText: String,
    val totalWords: Int,
    val uniqueWords: Int,
    val uniqueStems: Int,
    val compressionRatioPercent: Double,
    val stemMappings: List<WordStemPair>,
    val formattedReport: String,
    val summary: String
)

class PorterStemmerTool : Tool<StemmerInput, StemmerOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "porter_stemmer_tool",
        name = "Porter Stemmer Morphological Analyzer",
        description = "Algorithmic English Porter Stemmer (Steps 1a through 5b) reducing inflected words to morphological base stems for NLP and search indexing.",
        category = ToolCategory.TEXT,
        tags = listOf("stemmer", "porter", "nlp", "linguistics", "morphology", "tokenization", "information retrieval", "indexing", "text mining"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Type"
    )

    private fun isConsonant(str: String, i: Int): Boolean {
        val ch = str[i]
        return when (ch) {
            'a', 'e', 'i', 'o', 'u' -> false
            'y' -> if (i == 0) true else !isConsonant(str, i - 1)
            else -> true
        }
    }

    private fun measure(str: String): Int {
        var m = 0
        var i = 0
        val len = str.length
        while (i < len && isConsonant(str, i)) i++
        while (i < len) {
            while (i < len && !isConsonant(str, i)) i++
            if (i >= len) break
            while (i < len && isConsonant(str, i)) i++
            m++
        }
        return m
    }

    private fun containsVowel(str: String): Boolean {
        for (i in str.indices) {
            if (!isConsonant(str, i)) return true
        }
        return false
    }

    private fun endsWithDoubleConsonant(str: String): Boolean {
        val len = str.length
        if (len < 2) return false
        return str[len - 1] == str[len - 2] && isConsonant(str, len - 1)
    }

    private fun cvc(str: String): Boolean {
        val len = str.length
        if (len < 3) return false
        val c1 = isConsonant(str, len - 3)
        val v = !isConsonant(str, len - 2)
        val c2 = isConsonant(str, len - 1)
        val lastChar = str[len - 1]
        return c1 && v && c2 && lastChar != 'w' && lastChar != 'x' && lastChar != 'y'
    }

    fun stemWord(word: String): String {
        var w = word.lowercase(Locale.US)
        if (w.length <= 2) return w

        // Step 1a
        when {
            w.endsWith("sses") -> w = w.dropLast(2)
            w.endsWith("ies") -> w = w.dropLast(2)
            w.endsWith("ss") -> { /* leave intact */ }
            w.endsWith("s") -> w = w.dropLast(1)
        }

        // Step 1b
        var step1bExtra = false
        if (w.endsWith("eed")) {
            val stem = w.dropLast(3)
            if (measure(stem) > 0) w = stem + "ee"
        } else if (w.endsWith("ed")) {
            val stem = w.dropLast(2)
            if (containsVowel(stem)) {
                w = stem
                step1bExtra = true
            }
        } else if (w.endsWith("ing")) {
            val stem = w.dropLast(3)
            if (containsVowel(stem)) {
                w = stem
                step1bExtra = true
            }
        }

        if (step1bExtra) {
            when {
                w.endsWith("at") || w.endsWith("bl") || w.endsWith("iz") -> w += "e"
                endsWithDoubleConsonant(w) -> {
                    val last = w.last()
                    if (last != 'l' && last != 's' && last != 'z') {
                        w = w.dropLast(1)
                    }
                }
                measure(w) == 1 && cvc(w) -> w += "e"
            }
        }

        // Step 1c
        if (w.endsWith("y")) {
            val stem = w.dropLast(1)
            if (containsVowel(stem)) {
                w = stem + "i"
            }
        }

        // Step 2
        fun replaceSuffix(suffix: String, replacement: String): Boolean {
            if (w.endsWith(suffix)) {
                val stem = w.dropLast(suffix.length)
                if (measure(stem) > 0) {
                    w = stem + replacement
                    return true
                }
            }
            return false
        }

        val step2Done = replaceSuffix("ational", "ate") ||
            replaceSuffix("tional", "tion") ||
            replaceSuffix("enci", "ence") ||
            replaceSuffix("anci", "ance") ||
            replaceSuffix("izer", "ize") ||
            replaceSuffix("abli", "able") ||
            replaceSuffix("alli", "al") ||
            replaceSuffix("entli", "ent") ||
            replaceSuffix("eli", "e") ||
            replaceSuffix("ousli", "ous") ||
            replaceSuffix("ization", "ize") ||
            replaceSuffix("ation", "ate") ||
            replaceSuffix("ator", "ate") ||
            replaceSuffix("alism", "al") ||
            replaceSuffix("iveness", "ive") ||
            replaceSuffix("fulness", "ful") ||
            replaceSuffix("ousness", "ous") ||
            replaceSuffix("aliti", "al") ||
            replaceSuffix("iviti", "ive") ||
            replaceSuffix("biliti", "ble")

        // Step 3
        if (!step2Done) {
            val step3Done = replaceSuffix("icate", "ic") ||
                replaceSuffix("ative", "") ||
                replaceSuffix("alize", "al") ||
                replaceSuffix("iciti", "ic") ||
                replaceSuffix("ical", "ic") ||
                replaceSuffix("ful", "") ||
                replaceSuffix("ness", "")
        }

        // Step 4
        val step4Suffixes = listOf(
            "al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement",
            "ment", "ent", "ou", "ism", "ate", "iti", "ous", "ive", "ize"
        )
        var s4Matched = false
        for (suffix in step4Suffixes) {
            if (w.endsWith(suffix)) {
                val stem = w.dropLast(suffix.length)
                if (measure(stem) > 1) {
                    w = stem
                    s4Matched = true
                    break
                }
            }
        }
        if (!s4Matched && w.endsWith("ion")) {
            val stem = w.dropLast(3)
            if (measure(stem) > 1 && (stem.endsWith("s") || stem.endsWith("t"))) {
                w = stem
            }
        }

        // Step 5a
        if (w.endsWith("e")) {
            val stem = w.dropLast(1)
            val m = measure(stem)
            if (m > 1 || (m == 1 && !cvc(stem))) {
                w = stem
            }
        }

        // Step 5b
        if (measure(w) > 1 && endsWithDoubleConsonant(w) && w.endsWith("l")) {
            w = w.dropLast(1)
        }

        return w
    }

    override suspend fun execute(input: StemmerInput): ToolResult<StemmerOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.text.trim()
        if (text.isBlank()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        val wordRegex = Regex("\\b[a-zA-Z]+\\b")
        val pairs = mutableListOf<WordStemPair>()

        val stemmed = wordRegex.replace(text) { match ->
            val orig = match.value
            val s = stemWord(orig)
            val finalStem = if (orig.first().isUpperCase()) s.replaceFirstChar { it.uppercase() } else s
            pairs.add(WordStemPair(orig, finalStem, orig.lowercase(Locale.US) != s))
            finalStem
        }

        val totalWords = pairs.size
        val uniqueWords = pairs.map { it.original.lowercase(Locale.US) }.distinct().size
        val uniqueStems = pairs.map { it.stem.lowercase(Locale.US) }.distinct().size
        val compression = if (uniqueWords > 0) ((uniqueWords - uniqueStems).toDouble() / uniqueWords) * 100.0 else 0.0

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== PORTER STEMMER MORPHOLOGICAL ANALYSIS ===")
            appendLine("Total Tokens:   $totalWords")
            appendLine("Unique Words:   $uniqueWords")
            appendLine("Unique Stems:   $uniqueStems")
            appendLine("Vocab Reduction: ${String.format(Locale.US, "%.1f", compression)}%")
            appendLine("----------------------------------------")
            appendLine("Stemmed Text Output:")
            appendLine(stemmed)
            appendLine("----------------------------------------")
            appendLine("Sample Morphological Reductions:")
            appendLine("%-20s -> %-20s (Changed: %s)".format(Locale.US, "Original", "Stem", "Status"))
            pairs.distinctBy { it.original.lowercase(Locale.US) }.take(15).forEach { p ->
                appendLine("%-20s -> %-20s (%s)".format(Locale.US, p.original, p.stem, if (p.changed) "REDUCED" else "UNCHANGED"))
            }
        }

        return ToolResult.Success(
            data = StemmerOutput(
                originalText = text,
                stemmedText = stemmed,
                totalWords = totalWords,
                uniqueWords = uniqueWords,
                uniqueStems = uniqueStems,
                compressionRatioPercent = compression,
                stemMappings = pairs.distinctBy { it.original.lowercase(Locale.US) },
                formattedReport = report,
                summary = "Stemmed $totalWords tokens ($uniqueWords words -> $uniqueStems unique stems, ${String.format(Locale.US, "%.1f", compression)}% reduction)."
            ),
            executionTimeMs = elapsed,
            summary = "Stemmed $totalWords words to $uniqueStems stems"
        )
    }
}
