package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class PolybiusInput(
    val operation: String = "ENCODE", // ENCODE or DECODE
    val text: String = "DEFEND THE EAST WALL",
    val keyword: String = "" // Optional custom key to permute square
)

data class PolybiusOutput(
    val operation: String,
    val resultText: String,
    val gridMatrix: List<List<Char>>,
    val keywordUsed: String,
    val visualGrid: String,
    val formattedReport: String,
    val summary: String
)

class PolybiusSquareCipherTool : Tool<PolybiusInput, PolybiusOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "polybius_square_cipher_tool",
        name = "Polybius Square 5x5 Matrix Cipher",
        description = "Encode and decode text into 2D grid coordinates (1-5, 1-5) using the classical Polybius Square cipher with optional keyword alphabet permutation.",
        category = ToolCategory.SECURITY,
        tags = listOf("polybius", "cipher", "cryptography", "classical", "matrix", "grid", "substitution", "bifid", "adfgvx"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Grid"
    )

    override suspend fun execute(input: PolybiusInput): ToolResult<PolybiusOutput> {
        val startTime = System.currentTimeMillis()
        val op = input.operation.trim().uppercase(Locale.US)
        val text = input.text.trim()

        if (text.isBlank()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        // Build 5x5 grid (I/J combined)
        val alphabetChars = mutableListOf<Char>()
        val seen = mutableSetOf<Char>()

        val kw = input.keyword.uppercase(Locale.US).replace("J", "I").filter { it in 'A'..'Z' }
        for (ch in kw) {
            if (seen.add(ch)) alphabetChars.add(ch)
        }
        for (ch in 'A'..'Z') {
            if (ch == 'J') continue
            if (seen.add(ch)) alphabetChars.add(ch)
        }

        val grid = Array(5) { r ->
            List(5) { c -> alphabetChars[r * 5 + c] }
        }.toList()

        val charToCoord = mutableMapOf<Char, String>()
        val coordToChar = mutableMapOf<String, Char>()

        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val ch = grid[r][c]
                val coord = "${r + 1}${c + 1}"
                charToCoord[ch] = coord
                coordToChar[coord] = ch
            }
        }
        // J maps to I's coordinate
        charToCoord['J'] = charToCoord['I']!!

        val visualGrid = buildString {
            appendLine("    1   2   3   4   5")
            appendLine("  +---+---+---+---+---+")
            for (r in 0 until 5) {
                append("${r + 1} |")
                for (c in 0 until 5) {
                    val ch = grid[r][c]
                    val display = if (ch == 'I') "I/J" else " $ch "
                    append("$display|")
                }
                appendLine()
                appendLine("  +---+---+---+---+---+")
            }
        }

        return try {
            if (op == "ENCODE") {
                val encoded = StringBuilder()
                val upper = text.uppercase(Locale.US)
                for (ch in upper) {
                    if (ch in 'A'..'Z') {
                        encoded.append(charToCoord[ch]).append(" ")
                    } else if (ch.isWhitespace()) {
                        encoded.append("/ ")
                    }
                }
                val result = encoded.toString().trim()

                val report = buildString {
                    appendLine("POLYBIUS SQUARE 5×5 ENCODING REPORT")
                    appendLine("--------------------------------------------------")
                    appendLine("Operation:      ENCODE")
                    appendLine("Keyword:        ${if (kw.isNotEmpty()) kw else "None (Standard A-Z)"}")
                    appendLine("Plaintext:      $text")
                    appendLine("--------------------------------------------------")
                    appendLine("5×5 COORDINATE MATRIX:")
                    append(visualGrid)
                    appendLine("--------------------------------------------------")
                    appendLine("ENCODED COORDINATES:")
                    appendLine(result)
                }

                ToolResult.Success(
                    data = PolybiusOutput(
                        operation = "ENCODE",
                        resultText = result,
                        gridMatrix = grid,
                        keywordUsed = kw,
                        visualGrid = visualGrid,
                        formattedReport = report,
                        summary = "Polybius Encoded: ${text.take(20)}"
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = "Encoded Polybius Square"
                )
            } else {
                // DECODE
                val words = text.split("/")
                val decodedWords = words.map { wordToken ->
                    val digits = wordToken.filter { it in '1'..'5' }
                    if (digits.length % 2 != 0) {
                        throw IllegalArgumentException("Odd number of coordinate digits in token '$wordToken'. Coordinates must be pairs.")
                    }
                    val sb = StringBuilder()
                    for (i in digits.indices step 2) {
                        val coord = digits.substring(i, i + 2)
                        val ch = coordToChar[coord]
                            ?: throw IllegalArgumentException("Invalid Polybius coordinate '$coord'. Row and column digits must be between 1 and 5.")
                        sb.append(ch)
                    }
                    sb.toString()
                }
                val result = decodedWords.joinToString(" ").trim()

                val report = buildString {
                    appendLine("POLYBIUS SQUARE 5×5 DECODING REPORT")
                    appendLine("--------------------------------------------------")
                    appendLine("Operation:      DECODE")
                    appendLine("Keyword:        ${if (kw.isNotEmpty()) kw else "None (Standard A-Z)"}")
                    appendLine("Coordinates:    $text")
                    appendLine("--------------------------------------------------")
                    appendLine("5×5 COORDINATE MATRIX:")
                    append(visualGrid)
                    appendLine("--------------------------------------------------")
                    appendLine("DECODED PLAINTEXT:")
                    appendLine(result)
                }

                ToolResult.Success(
                    data = PolybiusOutput(
                        operation = "DECODE",
                        resultText = result,
                        gridMatrix = grid,
                        keywordUsed = kw,
                        visualGrid = visualGrid,
                        formattedReport = report,
                        summary = "Polybius Decoded: ${result.take(20)}"
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = "Decoded Polybius Square"
                )
            }
        } catch (e: Exception) {
            ToolResult.Failure("Polybius cipher error: ${e.message}")
        }
    }
}
