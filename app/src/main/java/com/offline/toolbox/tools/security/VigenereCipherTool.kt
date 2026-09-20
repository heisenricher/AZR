package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class VigenereMode {
    ENCRYPT,
    DECRYPT
}

data class VigenereInput(
    val text: String = "ATTACK AT DAWN ON THE SHORELINE",
    val key: String = "LEMON",
    val mode: VigenereMode = VigenereMode.ENCRYPT
)

data class VigenereOutput(
    val result: String,
    val mode: VigenereMode,
    val keyLength: Int,
    val indexOfCoincidence: Double,
    val formattedReport: String,
    val summary: String
)

class VigenereCipherTool : Tool<VigenereInput, VigenereOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "vigenere_cipher_tool",
        name = "Vigenère Polyalphabetic Cipher & Cryptanalysis",
        description = "Encrypt or decrypt text using the classical Vigenère cipher and compute Index of Coincidence (IoC) for key length estimation.",
        category = ToolCategory.SECURITY,
        tags = listOf("vigenere", "cipher", "crypto", "encryption", "historical", "polyalphabetic", "ioc", "cryptanalysis"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Lock"
    )

    override suspend fun execute(input: VigenereInput): ToolResult<VigenereOutput> {
        val startTime = System.currentTimeMillis()
        val rawText = input.text
        val rawKey = input.key.filter { it.isLetter() }.uppercase(Locale.ROOT)

        if (rawText.isEmpty()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }
        if (rawKey.isEmpty()) {
            return ToolResult.Failure("Cipher key must contain at least one alphabetic letter.")
        }

        val sb = StringBuilder()
        var keyIdx = 0

        for (c in rawText) {
            if (c.isLetter()) {
                val isUpper = c.isUpperCase()
                val base = if (isUpper) 'A' else 'a'
                val charVal = c - base
                val shift = rawKey[keyIdx % rawKey.length] - 'A'

                val newCharVal = if (input.mode == VigenereMode.ENCRYPT) {
                    (charVal + shift) % 26
                } else {
                    (charVal - shift + 26) % 26
                }
                sb.append((base + newCharVal))
                keyIdx++
            } else {
                sb.append(c)
            }
        }

        val resultStr = sb.toString()

        // Calculate Index of Coincidence (IoC) on result letters
        val alphaOnly = resultStr.filter { it.isLetter() }.uppercase(Locale.ROOT)
        val ioc = calculateIoc(alphaOnly)

        val report = buildString {
            appendLine("VIGENÈRE POLYALPHABETIC CIPHER")
            appendLine("--------------------------------------------------")
            appendLine("Operation:           ${if (input.mode == VigenereMode.ENCRYPT) "Encryption" else "Decryption"}")
            appendLine("Key:                 $rawKey (Length: ${rawKey.length})")
            appendLine("Index of Coincidence: ${String.format(Locale.US, "%.4f", ioc)} (English normal: ~0.066, Random: ~0.038)")
            appendLine()
            appendLine("RESULT:")
            appendLine(resultStr)
        }

        val summary = "${input.mode.name}: \"${resultStr.take(24)}...\" (Key: $rawKey)"

        return ToolResult.Success(
            data = VigenereOutput(
                result = resultStr,
                mode = input.mode,
                keyLength = rawKey.length,
                indexOfCoincidence = ioc,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun calculateIoc(text: String): Double {
        val n = text.length
        if (n <= 1) return 0.0
        val counts = IntArray(26)
        for (c in text) counts[c - 'A']++
        var sum = 0.0
        for (cnt in counts) {
            sum += cnt * (cnt - 1)
        }
        return sum / (n * (n - 1).toDouble())
    }
}
