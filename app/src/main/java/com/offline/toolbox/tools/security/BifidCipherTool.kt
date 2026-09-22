package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class BifidCipherInput(
    val operation: String = "ENCRYPT", // ENCRYPT or DECRYPT
    val text: String = "DEFEND THE EAST WALL",
    val period: Int = 5,
    val keyword: String = "BIFID"
)

data class BifidCipherOutput(
    val operation: String,
    val resultText: String,
    val period: Int,
    val keywordUsed: String,
    val visualGrid: String,
    val formattedReport: String,
    val summary: String
)

class BifidCipherTool : Tool<BifidCipherInput, BifidCipherOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "bifid_cipher_tool",
        name = "Bifid Delastelle Fractionated Cipher",
        description = "Encrypt and decrypt using Felix Delastelle's classical Bifid fractionated transposition cipher with Polybius coordinate interleaving and custom block periods.",
        category = ToolCategory.SECURITY,
        tags = listOf("bifid", "cipher", "delastelle", "cryptography", "transposition", "polybius", "fractionation", "classical"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Shield"
    )

    private fun buildGrid(keyword: String): Pair<Map<Char, Pair<Int, Int>>, Array<CharArray>> {
        val seen = mutableSetOf<Char>()
        val letters = mutableListOf<Char>()
        val cleanKey = keyword.uppercase(Locale.US).replace("J", "I").filter { it in 'A'..'Z' }
        for (c in cleanKey) {
            if (seen.add(c)) letters.add(c)
        }
        for (c in 'A'..'Z') {
            if (c == 'J') continue
            if (seen.add(c)) letters.add(c)
        }

        val charToCoord = mutableMapOf<Char, Pair<Int, Int>>()
        val grid = Array(5) { CharArray(5) }
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val ch = letters[r * 5 + c]
                grid[r][c] = ch
                charToCoord[ch] = Pair(r + 1, c + 1)
            }
        }
        charToCoord['J'] = charToCoord['I']!!
        return Pair(charToCoord, grid)
    }

    override suspend fun execute(input: BifidCipherInput): ToolResult<BifidCipherOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.text.trim()
        if (text.isBlank()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        val op = input.operation.trim().uppercase(Locale.US)
        val period = input.period.coerceAtLeast(1)
        val (charToCoord, grid) = buildGrid(input.keyword)

        val visualGrid = buildString {
            appendLine("    1 2 3 4 5")
            appendLine("  +-----------+")
            for (r in 0 until 5) {
                append("${r + 1} | ")
                for (c in 0 until 5) {
                    val ch = grid[r][c]
                    append("$ch ")
                }
                appendLine("|")
            }
            appendLine("  +-----------+")
        }

        // Filter letters, preserving non-letters in place
        val lettersOnly = mutableListOf<Char>()
        for (ch in text.uppercase(Locale.US)) {
            if (ch in 'A'..'Z') {
                lettersOnly.add(if (ch == 'J') 'I' else ch)
            }
        }

        if (lettersOnly.isEmpty()) {
            return ToolResult.Failure("Text must contain at least one alphabetic character.")
        }

        val transformedLetters = mutableListOf<Char>()

        when (op) {
            "ENCRYPT" -> {
                // Chunk into blocks of period size
                for (chunk in lettersOnly.chunked(period)) {
                    val rows = mutableListOf<Int>()
                    val cols = mutableListOf<Int>()
                    for (ch in chunk) {
                        val (r, c) = charToCoord[ch]!!
                        rows.add(r)
                        cols.add(c)
                    }
                    val combined = rows + cols
                    for (i in combined.indices step 2) {
                        val r = combined[i]
                        val c = combined[i + 1]
                        transformedLetters.add(grid[r - 1][c - 1])
                    }
                }
            }
            "DECRYPT" -> {
                for (chunk in lettersOnly.chunked(period)) {
                    val coords = mutableListOf<Int>()
                    for (ch in chunk) {
                        val (r, c) = charToCoord[ch]!!
                        coords.add(r)
                        coords.add(c)
                    }
                    val half = coords.size / 2
                    val rows = coords.subList(0, half)
                    val cols = coords.subList(half, coords.size)
                    for (i in 0 until half) {
                        val r = rows[i]
                        val c = cols[i]
                        transformedLetters.add(grid[r - 1][c - 1])
                    }
                }
            }
            else -> return ToolResult.Failure("Unknown operation '$op'. Supported: ENCRYPT, DECRYPT.")
        }

        // Reconstruct text with original non-letter characters and casing
        var letterIdx = 0
        val finalResult = buildString {
            for (ch in text) {
                if (ch.uppercaseChar() in 'A'..'Z') {
                    val rep = transformedLetters[letterIdx++]
                    append(if (ch.isLowerCase()) rep.lowercaseChar() else rep)
                } else {
                    append(ch)
                }
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== BIFID DELASTELLE FRACTIONATED CIPHER ===")
            appendLine("Operation:     $op")
            appendLine("Block Period:  $period")
            appendLine("Keyword:       ${if (input.keyword.isNotBlank()) input.keyword else "Standard A-Z"}")
            appendLine("----------------------------------------")
            appendLine("5x5 Polybius Matrix:")
            append(visualGrid)
            appendLine("----------------------------------------")
            appendLine("Input Text:    $text")
            appendLine("Result Text:   $finalResult")
        }

        return ToolResult.Success(
            data = BifidCipherOutput(
                operation = op,
                resultText = finalResult,
                period = period,
                keywordUsed = input.keyword,
                visualGrid = visualGrid,
                formattedReport = report,
                summary = "Bifid ${op.lowercase(Locale.US)}ed ${lettersOnly.size} letters (period=$period)."
            ),
            executionTimeMs = elapsed,
            summary = "Bifid $op: ${finalResult.take(24)}"
        )
    }
}
