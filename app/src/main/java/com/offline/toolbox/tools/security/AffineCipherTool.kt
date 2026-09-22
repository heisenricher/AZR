package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class AffineCipherInput(
    val operation: String = "ENCRYPT", // ENCRYPT, DECRYPT, BRUTE_FORCE
    val text: String = "AFFINE CIPHER",
    val a: Int = 5,
    val b: Int = 8
)

data class AffineBruteCandidate(
    val a: Int,
    val b: Int,
    val aInverse: Int,
    val decryptedText: String,
    val score: Double
)

data class AffineCipherOutput(
    val operation: String,
    val resultText: String,
    val a: Int,
    val b: Int,
    val aInverse: Int,
    val isCoprime: Boolean,
    val bruteCandidates: List<AffineBruteCandidate>,
    val formattedReport: String,
    val summary: String
)

class AffineCipherTool : Tool<AffineCipherInput, AffineCipherOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "affine_cipher_tool",
        name = "Affine Modular Arithmetic Cipher",
        description = "Encrypt, decrypt, and brute-force solve the classical Affine substitution cipher using modular arithmetic E(x) = (ax + b) mod 26 and coprime validation.",
        category = ToolCategory.SECURITY,
        tags = listOf("affine", "cipher", "cryptography", "modular", "arithmetic", "gcd", "coprime", "substitution", "brute force"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Lock"
    )

    private val validCoprimes = listOf(1, 3, 5, 7, 9, 11, 15, 17, 19, 21, 23, 25)

    // Standard English letter frequencies (%)
    private val englishFreq = mapOf(
        'A' to 8.2, 'B' to 1.5, 'C' to 2.8, 'D' to 4.3, 'E' to 12.7,
        'F' to 2.2, 'G' to 2.0, 'H' to 6.1, 'I' to 7.0, 'J' to 0.15,
        'K' to 0.77, 'L' to 4.0, 'M' to 2.4, 'N' to 6.7, 'O' to 7.5,
        'P' to 1.9, 'Q' to 0.095, 'R' to 6.0, 'S' to 6.3, 'T' to 9.1,
        'U' to 2.8, 'V' to 0.98, 'W' to 2.4, 'X' to 0.15, 'Y' to 2.0, 'Z' to 0.074
    )

    private fun gcd(a: Int, b: Int): Int {
        var x = kotlin.math.abs(a)
        var y = kotlin.math.abs(b)
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }

    private fun modInverse(a: Int, m: Int = 26): Int {
        val normA = ((a % m) + m) % m
        for (x in 1 until m) {
            if ((normA * x) % m == 1) return x
        }
        return -1
    }

    private fun calculateFitness(text: String): Double {
        val upper = text.uppercase(Locale.US).filter { it in 'A'..'Z' }
        val n = upper.length
        if (n == 0) return -99999.0
        val counts = mutableMapOf<Char, Int>()
        for (ch in upper) counts[ch] = (counts[ch] ?: 0) + 1

        var chiSq = 0.0
        for (ch in 'A'..'Z') {
            val observed = (counts[ch] ?: 0).toDouble()
            val expected = n * ((englishFreq[ch] ?: 0.0) / 100.0)
            if (expected > 0.0) {
                val diff = observed - expected
                chiSq += (diff * diff) / expected
            }
        }

        // Common English word recognition bonus
        val words = text.uppercase(Locale.US).split(Regex("[^A-Z]+")).filter { it.length >= 2 }
        val commonWords = setOf(
            "THE", "BE", "TO", "OF", "AND", "A", "IN", "THAT", "HAVE", "I", "IT",
            "FOR", "NOT", "ON", "WITH", "HE", "AS", "YOU", "DO", "AT", "THIS", "BUT",
            "HIS", "BY", "FROM", "THEY", "WE", "SAY", "HER", "SHE", "OR", "AN", "WILL",
            "MY", "ONE", "ALL", "WOULD", "THERE", "THEIR", "WHAT", "SO", "UP", "OUT",
            "IF", "ABOUT", "WHO", "GET", "WHICH", "GO", "ME", "WHEN", "MAKE", "CAN",
            "LIKE", "TIME", "NO", "JUST", "HIM", "KNOW", "TAKE", "PEOPLE", "INTO",
            "YEAR", "YOUR", "GOOD", "SOME", "COULD", "THEM", "SEE", "OTHER", "THAN",
            "THEN", "NOW", "LOOK", "ONLY", "COME", "ITS", "OVER", "THINK", "ALSO",
            "BACK", "AFTER", "USE", "TWO", "HOW", "OUR", "WORK", "FIRST", "WELL", "WAY",
            "EVEN", "NEW", "WANT", "BECAUSE", "ANY", "THESE", "GIVE", "DAY", "MOST", "US",
            "IS", "ARE", "WAS", "WERE", "PRIVACY", "FOUNDATION"
        )
        var wordMatches = 0
        for (w in words) {
            if (w in commonWords) wordMatches++
        }

        return -chiSq + (wordMatches * 60.0)
    }

    private fun encryptChar(ch: Char, a: Int, b: Int): Char {
        if (ch in 'A'..'Z') {
            val x = ch - 'A'
            val enc = ((a * x + b) % 26 + 26) % 26
            return 'A' + enc
        }
        if (ch in 'a'..'z') {
            val x = ch - 'a'
            val enc = ((a * x + b) % 26 + 26) % 26
            return 'a' + enc
        }
        return ch
    }

    private fun decryptChar(ch: Char, aInv: Int, b: Int): Char {
        if (ch in 'A'..'Z') {
            val y = ch - 'A'
            val dec = ((aInv * (y - b)) % 26 + 26) % 26
            return 'A' + dec
        }
        if (ch in 'a'..'z') {
            val y = ch - 'a'
            val dec = ((aInv * (y - b)) % 26 + 26) % 26
            return 'a' + dec
        }
        return ch
    }

    override suspend fun execute(input: AffineCipherInput): ToolResult<AffineCipherOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.text.trim()
        if (text.isBlank()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        val op = input.operation.trim().uppercase(Locale.US)
        val a = input.a
        val b = ((input.b % 26) + 26) % 26

        val g = gcd(a, 26)
        val isCoprime = (g == 1)
        val aInv = if (isCoprime) modInverse(a, 26) else -1

        when (op) {
            "ENCRYPT" -> {
                if (!isCoprime) {
                    return ToolResult.Failure("Key 'a' ($a) must be coprime to 26 (gcd($a, 26) = $g). Valid keys: ${validCoprimes.joinToString()}.")
                }
                val result = buildString {
                    for (ch in text) {
                        append(encryptChar(ch, a, b))
                    }
                }
                val elapsed = System.currentTimeMillis() - startTime
                val report = buildString {
                    appendLine("=== AFFINE CIPHER ENCRYPTION ===")
                    appendLine("Operation: Encryption")
                    appendLine("Parameters: a = $a, b = $b (shift)")
                    appendLine("Coprime Check: gcd($a, 26) = 1 (VALID)")
                    appendLine("Modular Inverse a⁻¹: $aInv mod 26")
                    appendLine("Encryption Formula: E(x) = ($a * x + $b) mod 26")
                    appendLine("Plaintext Length: ${text.length} chars")
                    appendLine("----------------------------------------")
                    appendLine("Ciphertext:")
                    appendLine(result)
                }
                return ToolResult.Success(
                    data = AffineCipherOutput(
                        operation = "ENCRYPT",
                        resultText = result,
                        a = a,
                        b = b,
                        aInverse = aInv,
                        isCoprime = true,
                        bruteCandidates = emptyList(),
                        formattedReport = report,
                        summary = "Encrypted ${text.length} characters using Affine Cipher (a=$a, b=$b)."
                    ),
                    executionTimeMs = elapsed,
                    summary = "Encrypted text with a=$a, b=$b"
                )
            }
            "DECRYPT" -> {
                if (!isCoprime) {
                    return ToolResult.Failure("Key 'a' ($a) must be coprime to 26 (gcd($a, 26) = $g). Valid keys: ${validCoprimes.joinToString()}.")
                }
                val result = buildString {
                    for (ch in text) {
                        append(decryptChar(ch, aInv, b))
                    }
                }
                val elapsed = System.currentTimeMillis() - startTime
                val report = buildString {
                    appendLine("=== AFFINE CIPHER DECRYPTION ===")
                    appendLine("Operation: Decryption")
                    appendLine("Parameters: a = $a, b = $b")
                    appendLine("Coprime Check: gcd($a, 26) = 1 (VALID)")
                    appendLine("Modular Inverse a⁻¹: $aInv mod 26")
                    appendLine("Decryption Formula: D(y) = $aInv * (y - $b) mod 26")
                    appendLine("Ciphertext Length: ${text.length} chars")
                    appendLine("----------------------------------------")
                    appendLine("Plaintext:")
                    appendLine(result)
                }
                return ToolResult.Success(
                    data = AffineCipherOutput(
                        operation = "DECRYPT",
                        resultText = result,
                        a = a,
                        b = b,
                        aInverse = aInv,
                        isCoprime = true,
                        bruteCandidates = emptyList(),
                        formattedReport = report,
                        summary = "Decrypted ${text.length} characters using Affine Cipher (a=$a, b=$b, a⁻¹=$aInv)."
                    ),
                    executionTimeMs = elapsed,
                    summary = "Decrypted text with a=$a, b=$b"
                )
            }
            "BRUTE_FORCE" -> {
                val candidates = mutableListOf<AffineBruteCandidate>()
                for (candA in validCoprimes) {
                    val candAInv = modInverse(candA, 26)
                    for (candB in 0 until 26) {
                        val dec = buildString {
                            for (ch in text) {
                                append(decryptChar(ch, candAInv, candB))
                            }
                        }
                        val fitness = calculateFitness(dec)
                        candidates.add(
                            AffineBruteCandidate(
                                a = candA,
                                b = candB,
                                aInverse = candAInv,
                                decryptedText = dec,
                                score = fitness
                            )
                        )
                    }
                }
                candidates.sortByDescending { it.score }
                val topCandidate = candidates.firstOrNull()
                val topText = topCandidate?.decryptedText ?: text
                val elapsed = System.currentTimeMillis() - startTime

                val report = buildString {
                    appendLine("=== AFFINE CIPHER BRUTE-FORCE SOLVER ===")
                    appendLine("Total Keys Tested: ${candidates.size} (12 coprimes × 26 shifts)")
                    appendLine("Top Match: a = ${topCandidate?.a}, b = ${topCandidate?.b} (Score: ${String.format(Locale.US, "%.2f", topCandidate?.score ?: 0.0)})")
                    appendLine("----------------------------------------")
                    appendLine("Top Plaintext:")
                    appendLine(topText)
                    appendLine("----------------------------------------")
                    appendLine("Top 5 Candidate Keys:")
                    candidates.take(5).forEachIndexed { idx, c ->
                        appendLine("#${idx + 1}: a=${c.a}, b=${c.b} | Score: ${String.format(Locale.US, "%.2f", c.score)} -> ${c.decryptedText.take(40)}")
                    }
                }
                return ToolResult.Success(
                    data = AffineCipherOutput(
                        operation = "BRUTE_FORCE",
                        resultText = topText,
                        a = topCandidate?.a ?: a,
                        b = topCandidate?.b ?: b,
                        aInverse = topCandidate?.aInverse ?: aInv,
                        isCoprime = true,
                        bruteCandidates = candidates.take(10),
                        formattedReport = report,
                        summary = "Tested 312 key combinations. Best fit: a=${topCandidate?.a}, b=${topCandidate?.b}."
                    ),
                    executionTimeMs = elapsed,
                    summary = "Affine brute-force complete (312 combinations)"
                )
            }
            else -> {
                return ToolResult.Failure("Unknown operation '$op'. Supported: ENCRYPT, DECRYPT, BRUTE_FORCE.")
            }
        }
    }
}
