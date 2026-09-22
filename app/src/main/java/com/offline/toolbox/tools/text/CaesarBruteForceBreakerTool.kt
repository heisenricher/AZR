package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class CaesarBreakerInput(
    val ciphertext: String = "Khoor Zruog! Wklv lv dq hqfubswhg phvvdjh xvlqj fdhvdu flskhu."
)

data class CaesarShiftCandidate(
    val shift: Int,
    val chiSquaredScore: Double,
    val decryptedText: String,
    val rank: Int
)

data class CaesarBreakerOutput(
    val bestShift: Int,
    val bestDecryptedText: String,
    val bestScore: Double,
    val totalCandidatesTested: Int,
    val allShifts: List<CaesarShiftCandidate>,
    val formattedReport: String,
    val summary: String
)

class CaesarBruteForceBreakerTool : Tool<CaesarBreakerInput, CaesarBreakerOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "caesar_brute_force_breaker_tool",
        name = "Caesar Cipher Automated Frequency Breaker",
        description = "Automated cryptanalysis of Caesar cipher rotations (1-25) using Chi-squared (χ²) English letter frequency matching to instantly recover plaintext without a key.",
        category = ToolCategory.TEXT,
        tags = listOf("caesar", "cipher", "cryptanalysis", "frequency", "chi-squared", "brute force", "breaker", "rot", "rot13", "security"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Unlock"
    )

    // Standard English expected letter frequencies (0.0 to 1.0)
    private val englishFrequencies = mapOf(
        'A' to 0.08167, 'B' to 0.01492, 'C' to 0.02782, 'D' to 0.04253, 'E' to 0.12702,
        'F' to 0.02228, 'G' to 0.02015, 'H' to 0.06094, 'I' to 0.06966, 'J' to 0.00153,
        'K' to 0.00772, 'L' to 0.04025, 'M' to 0.02406, 'N' to 0.06749, 'O' to 0.07507,
        'P' to 0.01929, 'Q' to 0.00095, 'R' to 0.05987, 'S' to 0.06327, 'T' to 0.09056,
        'U' to 0.02758, 'V' to 0.00978, 'W' to 0.02360, 'X' to 0.00150, 'Y' to 0.01974,
        'Z' to 0.00074
    )

    private fun rotateText(text: String, shift: Int): String {
        val s = (shift % 26 + 26) % 26
        return buildString {
            for (ch in text) {
                when {
                    ch in 'A'..'Z' -> append('A' + ((ch - 'A' - s + 26) % 26))
                    ch in 'a'..'z' -> append('a' + ((ch - 'a' - s + 26) % 26))
                    else -> append(ch)
                }
            }
        }
    }

    private fun computeChiSquared(text: String): Double {
        val counts = mutableMapOf<Char, Int>()
        var totalLetters = 0
        for (ch in text.uppercase(Locale.US)) {
            if (ch in 'A'..'Z') {
                counts[ch] = (counts[ch] ?: 0) + 1
                totalLetters++
            }
        }
        if (totalLetters == 0) return 99999.0

        var chiSq = 0.0
        for (letter in 'A'..'Z') {
            val observed = (counts[letter] ?: 0).toDouble()
            val expected = totalLetters * (englishFrequencies[letter] ?: 0.0)
            if (expected > 0.0) {
                val diff = observed - expected
                chiSq += (diff * diff) / expected
            }
        }
        return chiSq
    }

    override suspend fun execute(input: CaesarBreakerInput): ToolResult<CaesarBreakerOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.ciphertext.trim()
        if (text.isBlank()) {
            return ToolResult.Failure("Ciphertext cannot be empty.")
        }

        val lettersOnly = text.filter { it.isLetter() }
        if (lettersOnly.isEmpty()) {
            return ToolResult.Failure("Ciphertext must contain at least one alphabetic character.")
        }

        val candidates = mutableListOf<Pair<Int, Double>>()
        // Test shifts 0 to 25
        for (s in 0..25) {
            val decrypted = rotateText(text, s)
            val score = computeChiSquared(decrypted)
            candidates.add(s to score)
        }

        candidates.sortBy { it.second } // Lower chi-squared is better match

        val ranked = candidates.mapIndexed { index, (shift, score) ->
            CaesarShiftCandidate(
                shift = shift,
                chiSquaredScore = score,
                decryptedText = rotateText(text, shift),
                rank = index + 1
            )
        }

        val best = ranked.first()
        val elapsed = System.currentTimeMillis() - startTime

        val report = buildString {
            appendLine("=== CAESAR CIPHER AUTOMATED FREQUENCY BREAKER ===")
            appendLine("Best Detected Shift: ROT-${best.shift}")
            appendLine("Chi-Squared Statistic (χ²): ${String.format(Locale.US, "%.2f", best.chiSquaredScore)} (Lower = closer to English)")
            appendLine("Ciphertext Length: ${text.length} characters (${lettersOnly.length} letters)")
            appendLine("----------------------------------------")
            appendLine("Recovered Plaintext:")
            appendLine(best.decryptedText)
            appendLine("----------------------------------------")
            appendLine("Top 5 Candidate Shifts:")
            appendLine("%-5s | %-6s | %-10s | %s".format(Locale.US, "Rank", "Shift", "χ² Score", "Preview"))
            ranked.take(5).forEach { c ->
                val preview = c.decryptedText.take(45).replace("\n", " ")
                appendLine("%-5d | ROT-%-2d | %-10.2f | %s".format(Locale.US, c.rank, c.shift, c.chiSquaredScore, preview))
            }
        }

        return ToolResult.Success(
            data = CaesarBreakerOutput(
                bestShift = best.shift,
                bestDecryptedText = best.decryptedText,
                bestScore = best.chiSquaredScore,
                totalCandidatesTested = 26,
                allShifts = ranked,
                formattedReport = report,
                summary = "Cracked Caesar Cipher: ROT-${best.shift} recovered plaintext (χ²: ${String.format(Locale.US, "%.1f", best.chiSquaredScore)})."
            ),
            executionTimeMs = elapsed,
            summary = "Recovered plaintext with ROT-${best.shift}"
        )
    }
}
