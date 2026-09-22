package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class AtbashInput(
    val text: String = "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG",
    val reverseDigits: Boolean = false
)

data class AtbashOutput(
    val transformedText: String,
    val characterCount: Int,
    val lettersTransformed: Int,
    val digitsTransformed: Int,
    val isSelfInverting: Boolean,
    val formattedReport: String,
    val summary: String
)

class AtbashCipherTool : Tool<AtbashInput, AtbashOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "atbash_cipher_tool",
        name = "Atbash Reciprocal Substitution Cipher",
        description = "Encrypt and decrypt text using the ancient classical Atbash reciprocal alphabet substitution cipher (A ↔ Z, B ↔ Y) with optional numeric reflection.",
        category = ToolCategory.TEXT,
        tags = listOf("atbash", "cipher", "substitution", "cryptography", "classical", "hebrew", "reciprocal", "reflection", "rot"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Repeat"
    )

    override suspend fun execute(input: AtbashInput): ToolResult<AtbashOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.text
        if (text.isBlank()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        var letterCount = 0
        var digitCount = 0

        val result = buildString {
            for (ch in text) {
                when {
                    ch in 'A'..'Z' -> {
                        append('Z' - (ch - 'A'))
                        letterCount++
                    }
                    ch in 'a'..'z' -> {
                        append('z' - (ch - 'a'))
                        letterCount++
                    }
                    input.reverseDigits && ch in '0'..'9' -> {
                        append('9' - (ch - '0'))
                        digitCount++
                    }
                    else -> append(ch)
                }
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== ATBASH RECIPROCAL SUBSTITUTION CIPHER ===")
            appendLine("Input Length:        ${text.length} characters")
            appendLine("Letters Transformed: $letterCount (A ↔ Z)")
            appendLine("Digits Transformed:  $digitCount ${if (input.reverseDigits) "(0 ↔ 9)" else "(Preserved)"}")
            appendLine("Reciprocal Property: Symmetric (f(f(x)) = x)")
            appendLine("----------------------------------------")
            appendLine("TRANSFORMED OUTPUT:")
            appendLine(result)
        }

        return ToolResult.Success(
            data = AtbashOutput(
                transformedText = result,
                characterCount = text.length,
                lettersTransformed = letterCount,
                digitsTransformed = digitCount,
                isSelfInverting = true,
                formattedReport = report,
                summary = "Atbash transformed $letterCount letters: ${result.take(25)}..."
            ),
            executionTimeMs = elapsed,
            summary = "Transformed $letterCount letters"
        )
    }
}
