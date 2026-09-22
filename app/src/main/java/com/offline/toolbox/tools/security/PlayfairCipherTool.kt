package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class PlayfairCipherInput(
    val operation: String = "ENCRYPT", // ENCRYPT or DECRYPT
    val text: String = "INSTRUMENTS",
    val keyword: String = "MONARCHY",
    val fillerChar: Char = 'X'
)

data class DigraphPair(
    val original: String,
    val transformed: String,
    val ruleApplied: String // SAME_ROW, SAME_COLUMN, RECTANGLE
)

data class PlayfairCipherOutput(
    val operation: String,
    val resultText: String,
    val keywordUsed: String,
    val digraphPairs: List<DigraphPair>,
    val visualKeySquare: String,
    val formattedReport: String,
    val summary: String
)

class PlayfairCipherTool : Tool<PlayfairCipherInput, PlayfairCipherOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "playfair_cipher_tool",
        name = "Wheatstone-Playfair Digraph Substitution Cipher",
        description = "Encrypt and decrypt text using the historic 5x5 Playfair digram substitution key square with circular row/column shifting and rectangle coordinate swapping.",
        category = ToolCategory.SECURITY,
        tags = listOf("playfair", "cipher", "wheatstone", "cryptography", "digraph", "polybius", "classical", "wwi", "wwii"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Shield"
    )

    private fun buildKeySquare(keyword: String): Pair<Array<CharArray>, Map<Char, Pair<Int, Int>>> {
        val cleanKey = keyword.uppercase(Locale.US).replace("J", "I").filter { it in 'A'..'Z' }
        val seen = mutableSetOf<Char>()
        val matrixLetters = mutableListOf<Char>()

        for (c in cleanKey) {
            if (seen.add(c)) matrixLetters.add(c)
        }

        for (c in 'A'..'Z') {
            if (c == 'J') continue
            if (seen.add(c)) matrixLetters.add(c)
        }

        val grid = Array(5) { CharArray(5) }
        val charPositions = mutableMapOf<Char, Pair<Int, Int>>()

        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val letter = matrixLetters[r * 5 + c]
                grid[r][c] = letter
                charPositions[letter] = Pair(r, c)
            }
        }
        charPositions['J'] = charPositions['I']!!

        return Pair(grid, charPositions)
    }

    private fun prepareDigraphs(rawText: String, filler: Char): List<Pair<Char, Char>> {
        val clean = rawText.uppercase(Locale.US).replace("J", "I").filter { it in 'A'..'Z' }
        val pairs = mutableListOf<Pair<Char, Char>>()
        val cleanFiller = if (filler.uppercaseChar() == 'J') 'I' else filler.uppercaseChar()

        var i = 0
        while (i < clean.length) {
            val first = clean[i]
            if (i + 1 < clean.length) {
                val second = clean[i + 1]
                if (first == second) {
                    val actualFiller = if (first == cleanFiller) 'Q' else cleanFiller
                    pairs.add(Pair(first, actualFiller))
                    i++
                } else {
                    pairs.add(Pair(first, second))
                    i += 2
                }
            } else {
                val actualFiller = if (first == cleanFiller) 'Q' else cleanFiller
                pairs.add(Pair(first, actualFiller))
                i++
            }
        }
        return pairs
    }

    override suspend fun execute(input: PlayfairCipherInput): ToolResult<PlayfairCipherOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.text.trim()
        if (text.isBlank()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        val op = input.operation.trim().uppercase(Locale.US)
        if (op != "ENCRYPT" && op != "DECRYPT") {
            return ToolResult.Failure("Unknown operation '$op'. Supported: ENCRYPT, DECRYPT.")
        }

        val (grid, charPos) = buildKeySquare(input.keyword)

        val visualGrid = buildString {
            appendLine("    1 2 3 4 5")
            appendLine("  +-----------+")
            for (r in 0 until 5) {
                append("${r + 1} | ")
                for (c in 0 until 5) {
                    append("${grid[r][c]} ")
                }
                appendLine("|")
            }
            appendLine("  +-----------+")
        }

        val digraphs = prepareDigraphs(text, input.fillerChar)
        if (digraphs.isEmpty()) {
            return ToolResult.Failure("Text must contain at least one alphabetic character.")
        }

        val pairs = mutableListOf<DigraphPair>()
        val resultLetters = StringBuilder()

        for ((ch1, ch2) in digraphs) {
            val (r1, c1) = charPos[ch1] ?: continue
            val (r2, c2) = charPos[ch2] ?: continue

            val out1: Char
            val out2: Char
            val rule: String

            if (r1 == r2) {
                // Same Row
                rule = "SAME_ROW"
                if (op == "ENCRYPT") {
                    out1 = grid[r1][(c1 + 1) % 5]
                    out2 = grid[r2][(c2 + 1) % 5]
                } else {
                    out1 = grid[r1][(c1 + 4) % 5]
                    out2 = grid[r2][(c2 + 4) % 5]
                }
            } else if (c1 == c2) {
                // Same Column
                rule = "SAME_COLUMN"
                if (op == "ENCRYPT") {
                    out1 = grid[(r1 + 1) % 5][c1]
                    out2 = grid[(r2 + 1) % 5][c2]
                } else {
                    out1 = grid[(r1 + 4) % 5][c1]
                    out2 = grid[(r2 + 4) % 5][c2]
                }
            } else {
                // Rectangle
                rule = "RECTANGLE"
                out1 = grid[r1][c2]
                out2 = grid[r2][c1]
            }

            pairs.add(DigraphPair("$ch1$ch2", "$out1$out2", rule))
            resultLetters.append(out1).append(out2)
        }

        val finalString = resultLetters.toString()
        val elapsed = System.currentTimeMillis() - startTime

        val report = buildString {
            appendLine("=== WHEATSTONE-PLAYFAIR DIGRAPH CIPHER ===")
            appendLine("Operation:     $op")
            appendLine("Keyword:       ${if (input.keyword.isNotBlank()) input.keyword.uppercase(Locale.US) else "NONE (Standard Alphabet)"}")
            appendLine("Filler Char:   ${input.fillerChar.uppercaseChar()}")
            appendLine("Total Digraphs:${pairs.size} pairs (${pairs.size * 2} chars)")
            appendLine("Result Output: $finalString")
            appendLine("----------------------------------------")
            appendLine("5x5 Playfair Key Matrix (I/J Merged):")
            append(visualGrid)
            appendLine("DIGRAPH SUBSTITUTION AUDIT:")
            appendLine("%-8s -> %-8s | %s".format(Locale.US, "Plain", "Cipher", "Rule"))
            appendLine("----------------------------------------")
            pairs.forEach { p ->
                appendLine("%-8s -> %-8s | %s".format(Locale.US, p.original, p.transformed, p.ruleApplied))
            }
        }

        val summaryText = "$op successful: ${pairs.size} digraphs -> $finalString"

        return ToolResult.Success(
            data = PlayfairCipherOutput(
                operation = op,
                resultText = finalString,
                keywordUsed = input.keyword.uppercase(Locale.US),
                digraphPairs = pairs,
                visualKeySquare = visualGrid,
                formattedReport = report,
                summary = summaryText
            ),
            executionTimeMs = elapsed,
            summary = summaryText
        )
    }
}
