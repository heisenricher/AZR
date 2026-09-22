package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class RailFenceMode(val displayName: String) {
    ENCRYPT("Encrypt (Transposition)"),
    DECRYPT("Decrypt (Reconstruct Plaintext)")
}

data class RailFenceInput(
    val text: String = "WE ARE DISCOVERED FLEE AT ONCE",
    val rails: Int = 3,
    val mode: RailFenceMode = RailFenceMode.ENCRYPT,
    val preserveSpaces: Boolean = true
)

data class RailFenceOutput(
    val originalText: String,
    val resultText: String,
    val rails: Int,
    val mode: String,
    val cycleLength: Int,
    val asciiMatrixVisualization: String,
    val formattedReport: String,
    val summary: String
)

class RailFenceCipherTool : Tool<RailFenceInput, RailFenceOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "rail_fence_cipher_tool",
        name = "Rail Fence (Zig-Zag) Cipher & Visualizer",
        description = "Encode and decode classical Rail Fence transposition ciphers with ASCII zig-zag matrix visualization.",
        category = ToolCategory.SECURITY,
        tags = listOf("rail fence", "zigzag", "cipher", "transposition", "cryptography", "classical", "encryption", "matrix"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Security"
    )

    override suspend fun execute(input: RailFenceInput): ToolResult<RailFenceOutput> {
        val startTime = System.currentTimeMillis()
        val textToProcess = if (input.preserveSpaces) input.text else input.text.replace(" ", "")

        if (textToProcess.trim().isEmpty()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        val k = input.rails.coerceIn(2, 20)
        val n = textToProcess.length

        if (k >= n) {
            // Trivial case where rails >= text length
            val matrix = Array(k) { CharArray(n) { '.' } }
            for (i in 0 until n) {
                matrix[i][i] = textToProcess[i]
            }
            val viz = buildMatrixVisualization(matrix, k, n)
            val output = RailFenceOutput(
                originalText = input.text,
                resultText = textToProcess,
                rails = k,
                mode = input.mode.name,
                cycleLength = (2 * k - 2),
                asciiMatrixVisualization = viz,
                formattedReport = "Rails ($k) >= Text Length ($n). Text remains unchanged:\n$textToProcess",
                summary = "Rail Fence ($k rails): $textToProcess"
            )
            return ToolResult.Success(output, System.currentTimeMillis() - startTime, "Processed Rail Fence cipher")
        }

        val cycle = 2 * k - 2
        val matrix = Array(k) { CharArray(n) { '.' } }

        val result: String

        if (input.mode == RailFenceMode.ENCRYPT) {
            var row = 0
            var goingDown = false

            for (col in 0 until n) {
                matrix[row][col] = textToProcess[col]
                if (row == 0 || row == k - 1) {
                    goingDown = !goingDown
                }
                row += if (goingDown) 1 else -1
            }

            val cipherBuilder = StringBuilder()
            for (r in 0 until k) {
                for (c in 0 until n) {
                    if (matrix[r][c] != '.') {
                        cipherBuilder.append(matrix[r][c])
                    }
                }
            }
            result = cipherBuilder.toString()
        } else {
            // DECRYPT
            // Step 1: Mark zig-zag positions with '*'
            var row = 0
            var goingDown = false
            for (col in 0 until n) {
                matrix[row][col] = '*'
                if (row == 0 || row == k - 1) {
                    goingDown = !goingDown
                }
                row += if (goingDown) 1 else -1
            }

            // Step 2: Fill rows with ciphertext characters
            var index = 0
            for (r in 0 until k) {
                for (c in 0 until n) {
                    if (matrix[r][c] == '*' && index < textToProcess.length) {
                        matrix[r][c] = textToProcess[index++]
                    }
                }
            }

            // Step 3: Read zig-zag path to reconstruct plaintext
            val plainBuilder = StringBuilder()
            row = 0
            goingDown = false
            for (col in 0 until n) {
                plainBuilder.append(matrix[row][col])
                if (row == 0 || row == k - 1) {
                    goingDown = !goingDown
                }
                row += if (goingDown) 1 else -1
            }
            result = plainBuilder.toString()
        }

        val viz = buildMatrixVisualization(matrix, k, n)

        val report = buildString {
            appendLine("RAIL FENCE (ZIG-ZAG) TRANSPOSITION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Operation Mode:       ${input.mode.displayName}")
            appendLine("Number of Rails (k):  $k")
            appendLine("Cycle Length (2k-2):  $cycle")
            appendLine("Text Length:          $n chars")
            appendLine("Preserve Spaces:      ${input.preserveSpaces}")
            appendLine("--------------------------------------------------")
            appendLine("ORIGINAL:")
            appendLine(input.text)
            appendLine("--------------------------------------------------")
            appendLine("RESULT (${input.mode.name}):")
            appendLine(result)
            appendLine("--------------------------------------------------")
            appendLine("ASCII RAIL MATRIX PATH VISUALIZATION:")
            appendLine(viz)
        }

        val output = RailFenceOutput(
            originalText = input.text,
            resultText = result,
            rails = k,
            mode = input.mode.name,
            cycleLength = cycle,
            asciiMatrixVisualization = viz,
            formattedReport = report,
            summary = "${input.mode.name} ($k rails): $result"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Completed Rail Fence ${input.mode.name.lowercase()}"
        )
    }

    private fun buildMatrixVisualization(matrix: Array<CharArray>, rails: Int, length: Int): String {
        val maxCol = length.coerceAtMost(60) // Show up to 60 columns for readability
        return buildString {
            for (r in 0 until rails) {
                append("Rail ${r + 1}: ")
                for (c in 0 until maxCol) {
                    val ch = matrix[r][c]
                    append(if (ch == '.') " " else ch)
                    append(" ")
                }
                if (maxCol < length) append("...")
                appendLine()
            }
        }
    }
}
