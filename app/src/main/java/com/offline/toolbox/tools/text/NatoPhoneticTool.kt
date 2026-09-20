package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class NatoMode {
    TEXT_TO_PHONETIC,
    PHONETIC_TO_TEXT
}

data class NatoInput(
    val text: String = "AZR Offline 2026",
    val mode: NatoMode = NatoMode.TEXT_TO_PHONETIC
)

data class NatoOutput(
    val result: String,
    val mode: NatoMode,
    val wordCount: Int,
    val characterCount: Int,
    val formattedReport: String,
    val summary: String
)

class NatoPhoneticTool : Tool<NatoInput, NatoOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "nato_phonetic_tool",
        name = "NATO & ICAO Radio Phonetic Alphabet",
        description = "Translate words into standard NATO/ICAO radio spelling (Alpha, Bravo, Charlie) and decode phonetics back to text.",
        category = ToolCategory.TEXT,
        tags = listOf("nato", "icao", "phonetic", "alphabet", "radio", "aviation", "spelling", "alpha", "bravo"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "RecordVoiceOver"
    )

    private val charToPhonetic = mapOf(
        'A' to "Alfa", 'B' to "Bravo", 'C' to "Charlie", 'D' to "Delta", 'E' to "Echo",
        'F' to "Foxtrot", 'G' to "Golf", 'H' to "Hotel", 'I' to "India", 'J' to "Juliett",
        'K' to "Kilo", 'L' to "Lima", 'M' to "Mike", 'N' to "November", 'O' to "Oscar",
        'P' to "Papa", 'Q' to "Quebec", 'R' to "Romeo", 'S' to "Sierra", 'T' to "Tango",
        'U' to "Uniform", 'V' to "Victor", 'W' to "Whiskey", 'X' to "X-ray", 'Y' to "Yankee",
        'Z' to "Zulu",
        '0' to "Zero", '1' to "One", '2' to "Two", '3' to "Three", '4' to "Four",
        '5' to "Five", '6' to "Six", '7' to "Seven", '8' to "Eight", '9' to "Nine"
    )

    private val phoneticToChar = charToPhonetic.entries.associate { it.value.lowercase(Locale.ROOT) to it.key }.toMutableMap().apply {
        put("alpha", 'A')
        put("juliet", 'J')
        put("xray", 'X')
    }

    override suspend fun execute(input: NatoInput): ToolResult<NatoOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.text.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        val resultStr: String
        val wordsCount: Int

        if (input.mode == NatoMode.TEXT_TO_PHONETIC) {
            val words = raw.split(" ")
            val phonetics = words.map { word ->
                word.map { c ->
                    charToPhonetic[c.uppercaseChar()] ?: c.toString()
                }.joinToString(" ")
            }
            resultStr = phonetics.joinToString("   ") // 3 spaces between distinct words
            wordsCount = words.size
        } else {
            val tokens = raw.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
            val sb = StringBuilder()
            for (t in tokens) {
                val ch = phoneticToChar[t.lowercase(Locale.ROOT)]
                if (ch != null) {
                    sb.append(ch)
                } else if (t.length == 1) {
                    sb.append(t)
                }
            }
            resultStr = sb.toString()
            wordsCount = tokens.size
        }

        val report = buildString {
            appendLine("NATO / ICAO RADIO PHONETIC TRANSLATION")
            appendLine("--------------------------------------------------")
            appendLine("Mode:             ${if (input.mode == NatoMode.TEXT_TO_PHONETIC) "Text → NATO Phonetic" else "NATO Phonetic → Text"}")
            appendLine("Input:            $raw")
            appendLine()
            appendLine("Result:")
            appendLine(resultStr)
        }

        val summary = if (input.mode == NatoMode.TEXT_TO_PHONETIC) {
            "NATO: ${resultStr.take(30)}..."
        } else {
            "Decoded: $resultStr"
        }

        return ToolResult.Success(
            data = NatoOutput(
                result = resultStr,
                mode = input.mode,
                wordCount = wordsCount,
                characterCount = resultStr.length,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
