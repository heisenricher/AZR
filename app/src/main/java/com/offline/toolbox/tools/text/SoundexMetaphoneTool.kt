package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class SoundexMetaphoneInput(
    val primaryWord: String = "Smith",
    val comparisonWord: String = "Smythe"
)

data class SoundexMetaphoneOutput(
    val primaryWord: String,
    val comparisonWord: String,
    val soundex1: String,
    val soundex2: String,
    val soundexMatch: Boolean,
    val metaphone1: String,
    val metaphone2: String,
    val metaphoneMatch: Boolean,
    val overallPhoneticSimilarity: String,
    val formattedReport: String,
    val summary: String
)

class SoundexMetaphoneTool : Tool<SoundexMetaphoneInput, SoundexMetaphoneOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "soundex_metaphone_tool",
        name = "Soundex & Metaphone Phonetic Name Comparator",
        description = "Generate American Soundex (e.g. S530) and Philips Metaphone phonetic sound-alike keys for names and words with match scoring.",
        category = ToolCategory.TEXT,
        tags = listOf("soundex", "metaphone", "phonetics", "sound alike", "nlp", "fuzzy matching", "linguistics", "names", "spelling"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FileText"
    )

    override suspend fun execute(input: SoundexMetaphoneInput): ToolResult<SoundexMetaphoneOutput> {
        val startTime = System.currentTimeMillis()
        val w1 = input.primaryWord.trim()
        val w2 = input.comparisonWord.trim()

        if (w1.isEmpty() && w2.isEmpty()) {
            return ToolResult.Failure("At least one word must be provided.")
        }

        val s1 = computeSoundex(w1)
        val s2 = if (w2.isNotEmpty()) computeSoundex(w2) else ""
        val m1 = computeMetaphone(w1)
        val m2 = if (w2.isNotEmpty()) computeMetaphone(w2) else ""

        val soundexMatch = w2.isNotEmpty() && s1 == s2
        val metaphoneMatch = w2.isNotEmpty() && m1 == m2

        val matchAssessment = if (w2.isEmpty()) {
            "Single Word Phonetic Extraction"
        } else if (soundexMatch && metaphoneMatch) {
            "EXACT PHONETIC MATCH (Identical in Soundex & Metaphone)"
        } else if (metaphoneMatch) {
            "STRONG METAPHONE MATCH (Likely Homophone)"
        } else if (soundexMatch) {
            "MODERATE SOUNDEX MATCH (Family Name Sound-Alike)"
        } else {
            "NO PHONETIC MATCH (Phonetically Distinct)"
        }

        val report = buildString {
            appendLine("PHONETIC ALGORITHM COMPARISON REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Word 1:               \"$w1\"")
            appendLine(" * American Soundex:  $s1")
            appendLine(" * Philips Metaphone: $m1")
            if (w2.isNotEmpty()) {
                appendLine("--------------------------------------------------")
                appendLine("Word 2:               \"$w2\"")
                appendLine(" * American Soundex:  $s2")
                appendLine(" * Philips Metaphone: $m2")
                appendLine("--------------------------------------------------")
                appendLine("COMPARISON ASSESSMENT:")
                appendLine(" * Soundex Match:     ${if (soundexMatch) "YES ($s1 == $s2)" else "NO ($s1 != $s2)"}")
                appendLine(" * Metaphone Match:   ${if (metaphoneMatch) "YES ($m1 == $m2)" else "NO ($m1 != $m2)"}")
                appendLine(" * Verdict:           $matchAssessment")
            }
        }

        val output = SoundexMetaphoneOutput(
            primaryWord = w1,
            comparisonWord = w2,
            soundex1 = s1,
            soundex2 = s2,
            soundexMatch = soundexMatch,
            metaphone1 = m1,
            metaphone2 = m2,
            metaphoneMatch = metaphoneMatch,
            overallPhoneticSimilarity = matchAssessment,
            formattedReport = report,
            summary = if (w2.isEmpty()) "$w1 → Soundex: $s1, Metaphone: $m1" else "$w1 vs $w2: $matchAssessment"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Computed phonetic keys for words"
        )
    }

    private fun computeSoundex(raw: String): String {
        val clean = raw.uppercase(Locale.US).filter { it in 'A'..'Z' }
        if (clean.isEmpty()) return ""

        val firstLetter = clean[0]
        val codes = StringBuilder()
        codes.append(firstLetter)

        var lastDigit = soundexDigit(firstLetter)

        for (i in 1 until clean.length) {
            val ch = clean[i]
            val digit = soundexDigit(ch)
            if (digit != '0' && digit != lastDigit) {
                codes.append(digit)
                if (codes.length == 4) break
            }
            if (ch !in "HW") {
                lastDigit = digit
            }
        }

        while (codes.length < 4) {
            codes.append('0')
        }

        return codes.substring(0, 4)
    }

    private fun soundexDigit(c: Char): Char {
        return when (c) {
            'B', 'F', 'P', 'V' -> '1'
            'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2'
            'D', 'T' -> '3'
            'L' -> '4'
            'M', 'N' -> '5'
            'R' -> '6'
            else -> '0' // Vowels and H, W, Y
        }
    }

    private fun computeMetaphone(raw: String): String {
        var word = raw.uppercase(Locale.US).filter { it in 'A'..'Z' }
        if (word.isEmpty()) return ""

        // Initial transformations
        if (word.startsWith("KN") || word.startsWith("GN") || word.startsWith("PN") || word.startsWith("AE") || word.startsWith("WR")) {
            word = word.substring(1)
        } else if (word.startsWith("X")) {
            word = "S" + word.substring(1)
        } else if (word.startsWith("WH")) {
            word = "W" + word.substring(2)
        }

        val result = StringBuilder()
        val len = word.length
        var i = 0

        while (i < len && result.length < 6) {
            val c = word[i]
            val next = if (i + 1 < len) word[i + 1] else ' '
            val prev = if (i > 0) word[i - 1] else ' '

            // Skip duplicate letters except 'C'
            if (c != 'C' && c == prev) {
                i++
                continue
            }

            when (c) {
                'A', 'E', 'I', 'O', 'U' -> {
                    // Only preserve vowels at start of word
                    if (i == 0) result.append(c)
                }
                'B' -> {
                    // Drop silent B after M at end of word
                    if (!(prev == 'M' && i == len - 1)) {
                        result.append('B')
                    }
                }
                'C' -> {
                    if (next == 'H') {
                        result.append('X')
                        i++
                    } else if (next == 'I' && i + 2 < len && word[i + 2] == 'A') {
                        result.append('X')
                        i += 2
                    } else if (next in "EIY") {
                        result.append('S')
                    } else {
                        result.append('K')
                    }
                }
                'D' -> {
                    if (next == 'G' && i + 2 < len && word[i + 2] in "EIY") {
                        result.append('J')
                        i += 2
                    } else {
                        result.append('T')
                    }
                }
                'F' -> result.append('F')
                'G' -> {
                    if (next == 'H') {
                        if (i + 2 < len && word[i + 2] in "AEIOU") {
                            result.append('K')
                            i++
                        }
                    } else if (next == 'N' && i + 1 == len - 1) {
                        // Silent G before N at end
                    } else if (next in "EIY") {
                        result.append('J')
                    } else {
                        result.append('K')
                    }
                }
                'H' -> {
                    // Drop H if after vowel and not followed by vowel
                    if (!(prev in "AEIOU") && next in "AEIOU") {
                        result.append('H')
                    }
                }
                'J' -> result.append('J')
                'K' -> {
                    if (prev != 'C') result.append('K')
                }
                'L' -> result.append('L')
                'M' -> result.append('M')
                'N' -> result.append('N')
                'P' -> {
                    if (next == 'H') {
                        result.append('F')
                        i++
                    } else {
                        result.append('P')
                    }
                }
                'Q' -> result.append('K')
                'R' -> result.append('R')
                'S' -> {
                    if (next == 'H') {
                        result.append('X')
                        i++
                    } else if (next == 'I' && i + 2 < len && word[i + 2] in "AO") {
                        result.append('X')
                        i += 2
                    } else {
                        result.append('S')
                    }
                }
                'T' -> {
                    if (next == 'I' && i + 2 < len && word[i + 2] in "AO") {
                        result.append('X')
                        i += 2
                    } else if (next == 'H') {
                        result.append('0') // '0' represents theta 'th'
                        i++
                    } else if (next == 'C' && i + 2 < len && word[i + 2] == 'H') {
                        // TCH: drop T
                    } else {
                        result.append('T')
                    }
                }
                'V' -> result.append('F')
                'W', 'Y' -> {
                    if (next in "AEIOU") {
                        result.append(c)
                    }
                }
                'X' -> {
                    result.append("KS")
                }
                'Z' -> result.append('S')
            }
            i++
        }

        return result.toString()
    }
}
