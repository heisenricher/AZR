package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class StringSimilarityInput(
    val stringA: String = "kitten",
    val stringB: String = "sitting"
)

data class StringSimilarityOutput(
    val levenshteinDistance: Int,
    val levenshteinSimilarityPercentage: Double,
    val jaroWinklerSimilarity: Double,
    val hammingDistance: Int?,
    val sorensenDiceCoefficient: Double,
    val formattedReport: String,
    val summary: String
)

class StringSimilarityTool : Tool<StringSimilarityInput, StringSimilarityOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "string_similarity_tool",
        name = "String Distance & Similarity Comparator",
        description = "Calculate Levenshtein distance, Jaro-Winkler similarity, Hamming distance, and Sørensen-Dice bigram coefficient between text strings.",
        category = ToolCategory.TEXT,
        tags = listOf("similarity", "levenshtein", "jaro-winkler", "hamming", "dice", "distance", "diff", "text", "fuzzy"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "CompareArrows"
    )

    override suspend fun execute(input: StringSimilarityInput): ToolResult<StringSimilarityOutput> {
        val startTime = System.currentTimeMillis()
        val s1 = input.stringA
        val s2 = input.stringB

        val levDist = levenshtein(s1, s2)
        val maxLen = max(s1.length, s2.length)
        val levSimPct = if (maxLen > 0) ((maxLen - levDist).toDouble() / maxLen.toDouble()) * 100.0 else 100.0

        val jaroWinkler = jaroWinkler(s1, s2)
        val hamming = if (s1.length == s2.length) hamming(s1, s2) else null
        val dice = sorensenDice(s1, s2)

        val report = buildString {
            appendLine("STRING SIMILARITY & DISTANCE METRICS")
            appendLine("--------------------------------------------------")
            appendLine("String A:            \"$s1\" (${s1.length} chars)")
            appendLine("String B:            \"$s2\" (${s2.length} chars)")
            appendLine()
            appendLine("SIMILARITY SCORES:")
            appendLine("• Levenshtein Edit Distance:  $levDist operations")
            appendLine("• Levenshtein Similarity:     ${String.format(Locale.US, "%.2f", levSimPct)}%")
            appendLine("• Jaro-Winkler Similarity:    ${String.format(Locale.US, "%.4f", jaroWinkler)} (${String.format(Locale.US, "%.1f", jaroWinkler * 100)}%)")
            appendLine("• Sørensen-Dice (Bigrams):    ${String.format(Locale.US, "%.4f", dice)} (${String.format(Locale.US, "%.1f", dice * 100)}%)")
            if (hamming != null) {
                appendLine("• Hamming Distance:           $hamming differing positions")
            } else {
                appendLine("• Hamming Distance:           N/A (Strings have unequal lengths)")
            }
        }

        val summary = "Lev: $levDist | Sim: ${String.format(Locale.US, "%.1f", levSimPct)}% | J-W: ${String.format(Locale.US, "%.2f", jaroWinkler)}"

        return ToolResult.Success(
            data = StringSimilarityOutput(
                levenshteinDistance = levDist,
                levenshteinSimilarityPercentage = levSimPct,
                jaroWinklerSimilarity = jaroWinkler,
                hammingDistance = hamming,
                sorensenDiceCoefficient = dice,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun hamming(s1: String, s2: String): Int {
        var diff = 0
        for (i in s1.indices) {
            if (s1[i] != s2[i]) diff++
        }
        return diff
    }

    private fun jaroWinkler(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val matchDistance = max(s1.length, s2.length) / 2 - 1
        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)

        var matches = 0
        for (i in s1.indices) {
            val start = max(0, i - matchDistance)
            val end = min(i + matchDistance + 1, s2.length)
            for (j in start until end) {
                if (s2Matches[j]) continue
                if (s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0.0
        var k = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        val jaro = ((matches.toDouble() / s1.length) +
                (matches.toDouble() / s2.length) +
                ((matches - transpositions / 2.0) / matches)) / 3.0

        // Winkler prefix bonus (up to 4 chars)
        var prefix = 0
        for (i in 0 until min(4, min(s1.length, s2.length))) {
            if (s1[i] == s2[i]) prefix++ else break
        }

        return jaro + prefix * 0.1 * (1.0 - jaro)
    }

    private fun sorensenDice(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.length < 2 || s2.length < 2) return 0.0

        val bigrams1 = mutableListOf<String>()
        for (i in 0 until s1.length - 1) bigrams1.add(s1.substring(i, i + 2))

        val bigrams2 = mutableListOf<String>()
        for (i in 0 until s2.length - 1) bigrams2.add(s2.substring(i, i + 2))

        var intersection = 0
        val copy2 = ArrayList(bigrams2)
        for (bg in bigrams1) {
            if (copy2.remove(bg)) intersection++
        }

        return (2.0 * intersection) / (bigrams1.size + bigrams2.size)
    }
}
