package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class LeetLevel {
    BASIC,
    INTERMEDIATE,
    ADVANCED_HACKER
}

enum class LeetMode {
    ENCODE,
    DECODE
}

data class LeetInput(
    val text: String,
    val level: LeetLevel = LeetLevel.BASIC,
    val mode: LeetMode = LeetMode.ENCODE
)

data class LeetOutput(
    val resultText: String,
    val substitutedCount: Int,
    val substitutionPercentage: Double,
    val originalLength: Int,
    val resultLength: Int
)

class LeetspeakTool : Tool<LeetInput, LeetOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "leetspeak",
        name = "Leetspeak Generator & Decoder",
        description = "Transforms text into hacker leetspeak (1337) across Basic, Intermediate, and Advanced symbol levels, with decoding support.",
        category = ToolCategory.TEXT,
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        )
    )

    private val basicMap = mapOf(
        'a' to "4", 'A' to "4",
        'e' to "3", 'E' to "3",
        'i' to "1", 'I' to "1",
        'o' to "0", 'O' to "0",
        's' to "5", 'S' to "5",
        't' to "7", 'T' to "7"
    )

    private val intermediateMap = basicMap + mapOf(
        'b' to "8", 'B' to "8",
        'g' to "9", 'G' to "9",
        'l' to "1", 'L' to "1",
        'z' to "2", 'Z' to "2"
    )

    private val advancedMap = intermediateMap + mapOf(
        'c' to "(", 'C' to "(",
        'd' to "|)", 'D' to "|)",
        'h' to "|-|", 'H' to "|-|",
        'k' to "|<", 'K' to "|<",
        'm' to "/\\/\\", 'M' to "/\\/\\",
        'n' to "|\\|", 'N' to "|\\|",
        'u' to "|_|", 'U' to "|_|",
        'v' to "\\/", 'V' to "\\/",
        'w' to "\\/\\/", 'W' to "\\/\\/"
    )

    override suspend fun execute(input: LeetInput): ToolResult<LeetOutput> {
        val raw = input.text
        if (raw.isEmpty()) {
            return ToolResult.Success(
                LeetOutput(
                    resultText = "",
                    substitutedCount = 0,
                    substitutionPercentage = 0.0,
                    originalLength = 0,
                    resultLength = 0
                )
            )
        }

        return when (input.mode) {
            LeetMode.ENCODE -> encode(raw, input.level)
            LeetMode.DECODE -> decode(raw)
        }
    }

    private fun encode(text: String, level: LeetLevel): ToolResult<LeetOutput> {
        val mapping = when (level) {
            LeetLevel.BASIC -> basicMap
            LeetLevel.INTERMEDIATE -> intermediateMap
            LeetLevel.ADVANCED_HACKER -> advancedMap
        }

        var replacedCount = 0
        val sb = StringBuilder()

        for (char in text) {
            val replacement = mapping[char]
            if (replacement != null) {
                sb.append(replacement)
                replacedCount++
            } else {
                sb.append(char)
            }
        }

        val resultStr = sb.toString()
        val percentage = if (text.isNotEmpty()) {
            (replacedCount.toDouble() / text.length) * 100.0
        } else 0.0

        return ToolResult.Success(
            LeetOutput(
                resultText = resultStr,
                substitutedCount = replacedCount,
                substitutionPercentage = Math.round(percentage * 10.0) / 10.0,
                originalLength = text.length,
                resultLength = resultStr.length
            )
        )
    }

    private fun decode(text: String): ToolResult<LeetOutput> {
        // Decode priority: longer symbols first to avoid prefix clashes
        var current = text
        val reverseMap = listOf(
            "\\/\\/" to "w",
            "/\\/\\" to "m",
            "|-|" to "h",
            "|<" to "k",
            "|\\|" to "n",
            "|_|" to "u",
            "\\/" to "v",
            "|)" to "d",
            "4" to "a",
            "8" to "b",
            "3" to "e",
            "9" to "g",
            "1" to "i",
            "0" to "o",
            "5" to "s",
            "7" to "t",
            "2" to "z"
        )

        var substitutions = 0
        for ((k, v) in reverseMap) {
            val count = (current.length - current.replace(k, "").length) / k.length
            if (count > 0) {
                substitutions += count
                current = current.replace(k, v)
            }
        }

        val percentage = if (text.isNotEmpty()) {
            (substitutions.toDouble() / text.length) * 100.0
        } else 0.0

        return ToolResult.Success(
            LeetOutput(
                resultText = current,
                substitutedCount = substitutions,
                substitutionPercentage = Math.round(percentage * 10.0) / 10.0,
                originalLength = text.length,
                resultLength = current.length
            )
        )
    }
}
