package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class AnagramInput(
    val primaryWord: String = "listen",
    val secondaryWord: String = "silent"
)

data class AnagramOutput(
    val isAnagram: Boolean,
    val isPrimaryPalindrome: Boolean,
    val isSecondaryPalindrome: Boolean,
    val letterFrequenciesPrimary: Map<Char, Int>,
    val letterFrequenciesSecondary: Map<Char, Int>,
    val samplePermutations: List<String>,
    val formattedReport: String,
    val summary: String
)

class AnagramSolverTool : Tool<AnagramInput, AnagramOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "anagram_solver_tool",
        name = "Anagram & Palindrome Solver",
        description = "Verify anagrams, detect palindromes, inspect letter frequency signatures, and generate permutations.",
        category = ToolCategory.TEXT,
        tags = listOf("anagram", "palindrome", "words", "letters", "frequency", "permutation", "puzzle", "linguistics"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Spellcheck"
    )

    override suspend fun execute(input: AnagramInput): ToolResult<AnagramOutput> {
        val startTime = System.currentTimeMillis()
        val s1 = input.primaryWord.trim()
        val s2 = input.secondaryWord.trim()

        if (s1.isEmpty()) {
            return ToolResult.Failure("Primary word cannot be empty.")
        }

        val clean1 = s1.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
        val clean2 = s2.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

        val freq1 = clean1.groupingBy { it }.eachCount()
        val freq2 = clean2.groupingBy { it }.eachCount()

        val isAnagram = (clean1.isNotEmpty() && clean2.isNotEmpty() && freq1 == freq2)
        val isPal1 = (clean1.isNotEmpty() && clean1 == clean1.reversed())
        val isPal2 = (clean2.isNotEmpty() && clean2 == clean2.reversed())

        val permutations = if (clean1.length in 2..6) {
            val perms = mutableListOf<String>()
            generatePermutations("", clean1, perms, maxCount = 20)
            perms.distinct()
        } else {
            emptyList()
        }

        val report = buildString {
            appendLine("ANAGRAM & PALINDROME ANALYSIS")
            appendLine("--------------------------------------------------")
            appendLine("Primary Text:   \"$s1\"")
            appendLine("Secondary Text: \"$s2\"")
            appendLine()
            appendLine("Are Anagrams:   ${if (isAnagram) "YES (Exact letter-for-letter match)" else "NO"}")
            appendLine("Primary is Palindrome:   ${if (isPal1) "YES" else "NO"}")
            if (s2.isNotEmpty()) {
                appendLine("Secondary is Palindrome: ${if (isPal2) "YES" else "NO"}")
            }
            appendLine()
            appendLine("Letter Frequencies (Primary):")
            freq1.entries.sortedByDescending { it.value }.forEach { (ch, count) ->
                appendLine("  '$ch' : $count")
            }
            if (permutations.isNotEmpty()) {
                appendLine()
                appendLine("Sample Word Permutations (${permutations.size}):")
                appendLine(permutations.joinToString(", "))
            }
        }

        val summary = if (isAnagram) "\"$s1\" & \"$s2\" are Anagrams" else "Analysis completed (Anagram: $isAnagram)"

        return ToolResult.Success(
            data = AnagramOutput(
                isAnagram = isAnagram,
                isPrimaryPalindrome = isPal1,
                isSecondaryPalindrome = isPal2,
                letterFrequenciesPrimary = freq1,
                letterFrequenciesSecondary = freq2,
                samplePermutations = permutations,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun generatePermutations(prefix: String, str: String, result: MutableList<String>, maxCount: Int) {
        if (result.size >= maxCount) return
        val n = str.length
        if (n == 0) {
            result.add(prefix)
        } else {
            for (i in 0 until n) {
                generatePermutations(
                    prefix + str[i],
                    str.substring(0, i) + str.substring(i + 1, n),
                    result,
                    maxCount
                )
                if (result.size >= maxCount) return
            }
        }
    }
}
