package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class RomanMode(val label: String) {
    AUTO_DETECT("Auto-Detect Input"),
    NUMBER_TO_ROMAN("Number → Roman"),
    ROMAN_TO_NUMBER("Roman → Number")
}

data class RomanNumeralInput(
    val input: String,
    val mode: RomanMode = RomanMode.AUTO_DETECT
)

class RomanNumeralConverterTool : Tool<RomanNumeralInput, String> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "roman_numeral_converter",
        name = "Roman Numeral Converter",
        description = "Convert between decimal Arabic numbers (1-3999) and Roman numerals with validation.",
        category = ToolCategory.CONVERTER,
        tags = listOf("roman", "numerals", "arabic", "converter", "history", "numbers", "math"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "LooksOne"
    )

    private val romanMap = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )

    private val charValues = mapOf(
        'I' to 1, 'V' to 5, 'X' to 10, 'L' to 50,
        'C' to 100, 'D' to 500, 'M' to 1000
    )

    override suspend fun execute(input: RomanNumeralInput): ToolResult<String> {
        val startTime = System.currentTimeMillis()
        val text = input.input.trim()

        if (text.isEmpty()) {
            return ToolResult.Failure(
                message = "Input is empty.",
                userGuidance = "Enter an integer from 1 to 3999, or a Roman numeral string (e.g. MMXXIV)."
            )
        }

        val isNumeric = text.all { it.isDigit() }
        val mode = when (input.mode) {
            RomanMode.AUTO_DETECT -> if (isNumeric) RomanMode.NUMBER_TO_ROMAN else RomanMode.ROMAN_TO_NUMBER
            else -> input.mode
        }

        return try {
            val result = if (mode == RomanMode.NUMBER_TO_ROMAN) {
                val num = text.toIntOrNull()
                    ?: return ToolResult.Failure("Invalid integer: '$text'", "Value must be a whole number between 1 and 3999.")
                if (num < 1 || num > 3999) {
                    return ToolResult.Failure(
                        message = "Number $num out of range.",
                        userGuidance = "Standard Roman numerals only support integers from 1 to 3999."
                    )
                }
                numberToRoman(num)
            } else {
                val upper = text.uppercase(Locale.US)
                val num = romanToNumber(upper)
                // Verify canonical representation
                val canonical = numberToRoman(num)
                if (canonical != upper) {
                    return ToolResult.Failure(
                        message = "Malformed Roman numeral: '$text'",
                        userGuidance = "The standard canonical representation for $num is '$canonical'."
                    )
                }
                num.toString()
            }

            ToolResult.Success(
                data = result,
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "$text → $result"
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Conversion failed: ${e.message}",
                cause = e
            )
        }
    }

    private fun numberToRoman(num: Int): String {
        var current = num
        val sb = StringBuilder()
        for ((value, roman) in romanMap) {
            while (current >= value) {
                sb.append(roman)
                current -= value
            }
        }
        return sb.toString()
    }

    private fun romanToNumber(roman: String): Int {
        for (c in roman) {
            if (c !in charValues) {
                throw IllegalArgumentException("Invalid Roman character '$c'. Allowed: I, V, X, L, C, D, M.")
            }
        }

        var total = 0
        var prev = 0
        for (i in roman.length - 1 downTo 0) {
            val curr = charValues[roman[i]] ?: 0
            if (curr < prev) {
                total -= curr
            } else {
                total += curr
            }
            prev = curr
        }
        return total
    }
}
