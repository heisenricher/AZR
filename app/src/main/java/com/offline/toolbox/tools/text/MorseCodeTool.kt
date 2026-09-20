package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class MorseDirection(val label: String) {
    TEXT_TO_MORSE("Text to Morse Code"),
    MORSE_TO_TEXT("Morse Code to Text"),
    AUTO_DETECT("Auto Detect Direction")
}

data class MorseCodeInput(
    val text: String = "",
    val direction: MorseDirection = MorseDirection.AUTO_DETECT,
    val dotChar: Char = '.',
    val dashChar: Char = '-',
    val wordSeparator: String = "/"
)

data class MorseCodeOutput(
    val convertedText: String,
    val detectedDirection: MorseDirection,
    val dotCount: Int,
    val dashCount: Int,
    val totalUnits: Int,
    val timingPatternSummary: String,
    val summary: String
)

class MorseCodeTool : Tool<MorseCodeInput, MorseCodeOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "morse_code_tool",
        name = "Morse Code Translator & Timing Engine",
        description = "Translate between text and International Morse Code with ITU-R timing unit calculations.",
        category = ToolCategory.TEXT,
        tags = listOf("morse", "code", "translator", "telegraph", "itu", "sos", "timing", "cipher"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "GraphicEq"
    )

    override suspend fun execute(input: MorseCodeInput): ToolResult<MorseCodeOutput> {
        val startTime = System.currentTimeMillis()

        if (input.text.isBlank()) {
            return ToolResult.Failure(
                message = "Input text is empty.",
                userGuidance = "Provide text or Morse code (using . and -) to translate."
            )
        }

        val effectiveDirection = if (input.direction == MorseDirection.AUTO_DETECT) {
            detectDirection(input.text)
        } else {
            input.direction
        }

        return try {
            if (effectiveDirection == MorseDirection.TEXT_TO_MORSE) {
                val morse = textToMorse(input.text, input.dotChar, input.dashChar, input.wordSeparator)
                val dots = morse.count { it == input.dotChar }
                val dashes = morse.count { it == input.dashChar }
                val units = calculateTimingUnits(morse, input.dotChar, input.dashChar, input.wordSeparator)

                val summary = "Translated to Morse ($dots dots, $dashes dashes, $units units)"

                ToolResult.Success(
                    data = MorseCodeOutput(
                        convertedText = morse,
                        detectedDirection = MorseDirection.TEXT_TO_MORSE,
                        dotCount = dots,
                        dashCount = dashes,
                        totalUnits = units,
                        timingPatternSummary = "Total ITU timing duration: $units units (Dot=1, Dash=3, CharGap=3, WordGap=7)",
                        summary = summary
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            } else {
                val text = morseToText(input.text, input.dotChar, input.dashChar, input.wordSeparator)
                val dots = input.text.count { it == input.dotChar || it == '.' || it == '•' }
                val dashes = input.text.count { it == input.dashChar || it == '-' || it == '—' }
                val summary = "Decoded Morse to ${text.length} characters"

                ToolResult.Success(
                    data = MorseCodeOutput(
                        convertedText = text,
                        detectedDirection = MorseDirection.MORSE_TO_TEXT,
                        dotCount = dots,
                        dashCount = dashes,
                        totalUnits = 0,
                        timingPatternSummary = "Decoded from Morse code",
                        summary = summary
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            }
        } catch (e: Exception) {
            ToolResult.Failure("Translation error: ${e.message}", cause = e)
        }
    }

    private fun detectDirection(text: String): MorseDirection {
        val clean = text.trim()
        val morseChars = clean.count { it == '.' || it == '-' || it == '/' || it == ' ' || it == '•' || it == '—' }
        return if (morseChars.toDouble() / clean.length > 0.75) {
            MorseDirection.MORSE_TO_TEXT
        } else {
            MorseDirection.TEXT_TO_MORSE
        }
    }

    private fun textToMorse(text: String, dot: Char, dash: Char, wordSep: String): String {
        val words = text.trim().split(Regex("\\s+"))
        return words.joinToString(" $wordSep ") { word ->
            word.uppercase(Locale.US).mapNotNull { char ->
                CHAR_TO_MORSE[char]?.replace('.', dot)?.replace('-', dash)
            }.joinToString(" ")
        }
    }

    private fun morseToText(morse: String, dot: Char, dash: Char, wordSep: String): String {
        val normalized = morse
            .replace(dot, '.')
            .replace(dash, '-')
            .replace('•', '.')
            .replace('—', '-')

        val words = normalized.split(Regex("(?<!/)\\s*${Regex.escape(wordSep)}\\s*(?!/)|\\s{3,}"))
        return words.joinToString(" ") { word ->
            val codes = word.trim().split(Regex("\\s+"))
            codes.mapNotNull { code ->
                MORSE_TO_CHAR[code]?.toString() ?: if (code.isNotBlank()) "?" else ""
            }.joinToString("")
        }
    }

    private fun calculateTimingUnits(morse: String, dot: Char, dash: Char, wordSep: String): Int {
        var units = 0
        val words = morse.split(wordSep)
        for ((wIndex, word) in words.withIndex()) {
            val letters = word.trim().split(" ").filter { it.isNotBlank() }
            for ((lIndex, letter) in letters.withIndex()) {
                for (ch in letter) {
                    units += if (ch == dot) 1 else if (ch == dash) 3 else 0
                    units += 1 // intra-character gap
                }
                if (letter.isNotEmpty()) units -= 1 // remove last extra intra-char gap
                if (lIndex < letters.size - 1) units += 3 // inter-character gap
            }
            if (wIndex < words.size - 1) units += 7 // inter-word gap
        }
        return units
    }

    companion object {
        private val CHAR_TO_MORSE = mapOf(
            'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
            'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
            'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
            'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
            'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
            'Z' to "--..",
            '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
            '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
            '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
            '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
            ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
            '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-."
        )

        private val MORSE_TO_CHAR = CHAR_TO_MORSE.entries.associate { (k, v) -> v to k }
    }
}
