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

data class JaroWinklerInput(
    val string1: String = "martha",
    val string2: String = "marhta",
    val caseSensitive: Boolean = false,
    val prefixScalingFactor: Double = 0.1 // Standard Winkler p = 0.1
)

data class JaroWinklerOutput(
    val jaroSimilarity: Double,
    val jaroWinklerSimilarity: Double,
    val jaroWinklerDistance: Double,
    val matchingCharacters: Int,
    val transpositions: Int,
    val commonPrefixLength: Int,
    val similarityTier: String,
    val formattedReport: String,
    val summary: String
)

class JaroWinklerDistanceTool : Tool<JaroWinklerInput, JaroWinklerOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "jaro_winkler_distance_tool",
        name = "Jaro-Winkler String Similarity & Distance Engine",
        description = "Compute Jaro and Jaro-Winkler similarity metrics, matching windows, transposition counts, and prefix bonuses for record linkage and fuzzy matching.",
        category = ToolCategory.TEXT,
        tags = listOf("jaro", "winkler", "similarity", "distance", "fuzzy", "string metrics", "deduplication", "record linkage", "nlp"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "GitCompare"
    )

    override suspend fun execute(input: JaroWinklerInput): ToolResult<JaroWinklerOutput> {
        val startTime = System.currentTimeMillis()
        var s1 = input.string1
        var s2 = input.string2

        if (!input.caseSensitive) {
            s1 = s1.lowercase(Locale.US)
            s2 = s2.lowercase(Locale.US)
        }

        if (s1.isEmpty() && s2.isEmpty()) {
            return ToolResult.Success(
                data = JaroWinklerOutput(
                    jaroSimilarity = 1.0,
                    jaroWinklerSimilarity = 1.0,
                    jaroWinklerDistance = 0.0,
                    matchingCharacters = 0,
                    transpositions = 0,
                    commonPrefixLength = 0,
                    similarityTier = "Exact Match (100%)",
                    formattedReport = "Both strings are empty.",
                    summary = "Exact match: 1.000"
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "Exact match (1.000)"
            )
        }

        if (s1.isEmpty() || s2.isEmpty()) {
            return ToolResult.Success(
                data = JaroWinklerOutput(
                    jaroSimilarity = 0.0,
                    jaroWinklerSimilarity = 0.0,
                    jaroWinklerDistance = 1.0,
                    matchingCharacters = 0,
                    transpositions = 0,
                    commonPrefixLength = 0,
                    similarityTier = "No Match (0%)",
                    formattedReport = "One string is empty.",
                    summary = "No match: 0.000"
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "No match (0.000)"
            )
        }

        if (s1 == s2) {
            val len = s1.length
            val prefixLen = min(len, 4)
            return ToolResult.Success(
                data = JaroWinklerOutput(
                    jaroSimilarity = 1.0,
                    jaroWinklerSimilarity = 1.0,
                    jaroWinklerDistance = 0.0,
                    matchingCharacters = len,
                    transpositions = 0,
                    commonPrefixLength = prefixLen,
                    similarityTier = "Exact Match (100%)",
                    formattedReport = "Identical strings: \"$s1\"",
                    summary = "Exact match: 1.000"
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "Exact match (1.000)"
            )
        }

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

        if (matches == 0) {
            val elapsed = System.currentTimeMillis() - startTime
            return ToolResult.Success(
                data = JaroWinklerOutput(
                    jaroSimilarity = 0.0,
                    jaroWinklerSimilarity = 0.0,
                    jaroWinklerDistance = 1.0,
                    matchingCharacters = 0,
                    transpositions = 0,
                    commonPrefixLength = 0,
                    similarityTier = "No Match (0%)",
                    formattedReport = "No matching characters found within window $matchDistance.",
                    summary = "No similarity: 0.000"
                ),
                executionTimeMs = elapsed,
                summary = "Similarity: 0.000"
            )
        }

        var transpositions = 0
        var k = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }
        val halfTranspositions = transpositions / 2

        val jaro = (matches.toDouble() / s1.length +
            matches.toDouble() / s2.length +
            (matches - halfTranspositions).toDouble() / matches) / 3.0

        var prefix = 0
        val maxPrefix = min(4, min(s1.length, s2.length))
        for (i in 0 until maxPrefix) {
            if (s1[i] == s2[i]) prefix++ else break
        }

        val p = input.prefixScalingFactor.coerceIn(0.0, 0.25)
        val jaroWinkler = jaro + (prefix * p * (1.0 - jaro))
        val distance = 1.0 - jaroWinkler

        val tier = when {
            jaroWinkler >= 0.90 -> "Very High Similarity / Probable Duplicate (≥90%)"
            jaroWinkler >= 0.80 -> "High Similarity / Likely Typo (80-89%)"
            jaroWinkler >= 0.70 -> "Moderate Similarity (70-79%)"
            jaroWinkler >= 0.50 -> "Low / Weak Similarity (50-69%)"
            else -> "Dissimilar / Distinct Strings (<50%)"
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== JARO-WINKLER SIMILARITY REPORT ===")
            appendLine("String 1:           \"${input.string1}\" (Length: ${input.string1.length})")
            appendLine("String 2:           \"${input.string2}\" (Length: ${input.string2.length})")
            appendLine("Case Sensitive:     ${input.caseSensitive}")
            appendLine("----------------------------------------")
            appendLine("Jaro Similarity:         ${String.format(Locale.US, "%.4f", jaro)} (${String.format(Locale.US, "%.2f", jaro * 100)}%)")
            appendLine("Jaro-Winkler Similarity: ${String.format(Locale.US, "%.4f", jaroWinkler)} (${String.format(Locale.US, "%.2f", jaroWinkler * 100)}%)")
            appendLine("Jaro-Winkler Distance:   ${String.format(Locale.US, "%.4f", distance)}")
            appendLine("----------------------------------------")
            appendLine("MATCHING ANALYSIS:")
            appendLine("  Matching Window:       $matchDistance characters")
            appendLine("  Matching Characters:   $matches")
            appendLine("  Half-Transpositions:   $halfTranspositions ($transpositions mismatches)")
            appendLine("  Common Prefix (max 4): $prefix")
            appendLine("  Scaling Factor (p):    $p")
            appendLine("----------------------------------------")
            appendLine("Rating: $tier")
        }

        return ToolResult.Success(
            data = JaroWinklerOutput(
                jaroSimilarity = jaro,
                jaroWinklerSimilarity = jaroWinkler,
                jaroWinklerDistance = distance,
                matchingCharacters = matches,
                transpositions = halfTranspositions,
                commonPrefixLength = prefix,
                similarityTier = tier,
                formattedReport = report,
                summary = "Jaro-Winkler: ${String.format(Locale.US, "%.4f", jaroWinkler)} (${String.format(Locale.US, "%.1f", jaroWinkler * 100)}% match) [$tier]."
            ),
            executionTimeMs = elapsed,
            summary = "Score: ${String.format(Locale.US, "%.4f", jaroWinkler)} (${String.format(Locale.US, "%.0f", jaroWinkler * 100)}%)"
        )
    }
}
