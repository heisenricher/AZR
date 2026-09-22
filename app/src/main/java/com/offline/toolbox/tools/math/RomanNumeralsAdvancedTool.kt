package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class RomanConversionMode(val displayName: String) {
    INTEGER_TO_ROMAN("Integer → Vinculum Roman"),
    ROMAN_TO_INTEGER("Vinculum Roman → Integer")
}

data class RomanNumeralsInput(
    val value: String = "2542456",
    val mode: RomanConversionMode = RomanConversionMode.INTEGER_TO_ROMAN
)

data class RomanNumeralsOutput(
    val inputString: String,
    val result: String,
    val integerVal: Long,
    val vinculumRepresentation: String,
    val bracketRepresentation: String,
    val breakdownComponents: List<String>,
    val formattedReport: String,
    val summary: String
)

class RomanNumeralsAdvancedTool : Tool<RomanNumeralsInput, RomanNumeralsOutput> {
    companion object {
        // Combining overline/macron
        private const val OVERLINE = "\u0305"

        private val ROMAN_PAIRS = listOf(
            Pair(1_000_000L, "M$OVERLINE" to "[M]"),
            Pair(900_000L, "C${OVERLINE}M$OVERLINE" to "[CM]"),
            Pair(500_000L, "D$OVERLINE" to "[D]"),
            Pair(400_000L, "C${OVERLINE}D$OVERLINE" to "[CD]"),
            Pair(100_000L, "C$OVERLINE" to "[C]"),
            Pair(90_000L, "X${OVERLINE}C$OVERLINE" to "[XC]"),
            Pair(50_000L, "L$OVERLINE" to "[L]"),
            Pair(40_000L, "X${OVERLINE}L$OVERLINE" to "[XL]"),
            Pair(10_000L, "X$OVERLINE" to "[X]"),
            Pair(9_000L, "I${OVERLINE}X$OVERLINE" to "[IX]"),
            Pair(5_000L, "V$OVERLINE" to "[V]"),
            Pair(4_000L, "I${OVERLINE}V$OVERLINE" to "[IV]"),
            Pair(1_000L, "M" to "M"),
            Pair(900L, "CM" to "CM"),
            Pair(500L, "D" to "D"),
            Pair(400L, "CD" to "CD"),
            Pair(100L, "C" to "C"),
            Pair(90L, "XC" to "XC"),
            Pair(50L, "L" to "L"),
            Pair(40L, "XL" to "XL"),
            Pair(10L, "X" to "X"),
            Pair(9L, "IX" to "IX"),
            Pair(5L, "V" to "V"),
            Pair(4L, "IV" to "IV"),
            Pair(1L, "I" to "I")
        )
    }

    override val metadata: ToolMetadata = ToolMetadata(
        id = "roman_numerals_advanced_tool",
        name = "Extended Roman Numerals (Vinculum Notation) Converter",
        description = "Convert integers up to 3,999,999 to and from Extended Roman numerals using the Vinculum overline notation (V̄, X̄, L̄, C̄, D̄, M̄).",
        category = ToolCategory.MATH,
        tags = listOf("roman numerals", "vinculum", "ancient math", "numbers", "converter", "latin", "overline", "advanced roman"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Hash"
    )

    override suspend fun execute(input: RomanNumeralsInput): ToolResult<RomanNumeralsOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.value.trim()

        if (text.isEmpty()) {
            return ToolResult.Failure("Input cannot be empty.")
        }

        return if (input.mode == RomanConversionMode.INTEGER_TO_ROMAN) {
            val num = text.toLongOrNull()
                ?: return ToolResult.Failure("Invalid integer: '$text'. Expected a number between 1 and 3,999,999.")

            if (num !in 1..3_999_999) {
                return ToolResult.Failure("Vinculum Roman numerals support range 1 to 3,999,999. Given: $num.")
            }

            var remainder = num
            val vincBuilder = StringBuilder()
            val bracketBuilder = StringBuilder()
            val breakdown = mutableListOf<String>()

            for ((value, symbols) in ROMAN_PAIRS) {
                while (remainder >= value) {
                    vincBuilder.append(symbols.first)
                    bracketBuilder.append(symbols.second)
                    breakdown.add("${symbols.first} = $value")
                    remainder -= value
                }
            }

            val vincStr = vincBuilder.toString()
            val bracketStr = bracketBuilder.toString()

            val report = buildString {
                appendLine("EXTENDED ROMAN NUMERAL CONVERSION REPORT")
                appendLine("--------------------------------------------------")
                appendLine("Decimal Value:               $num")
                appendLine("Vinculum Notation (Unicode): $vincStr")
                appendLine("ASCII Bracket Notation:      $bracketStr")
                appendLine("--------------------------------------------------")
                appendLine("COMPONENT BREAKDOWN:")
                breakdown.forEach { appendLine(" * $it") }
            }

            val output = RomanNumeralsOutput(
                inputString = text,
                result = vincStr,
                integerVal = num,
                vinculumRepresentation = vincStr,
                bracketRepresentation = bracketStr,
                breakdownComponents = breakdown,
                formattedReport = report,
                summary = "$num → $vincStr ($bracketStr)"
            )

            ToolResult.Success(output, System.currentTimeMillis() - startTime, "Converted integer to Vinculum Roman")
        } else {
            // ROMAN_TO_INTEGER
            // Standardize brackets like [X] into X̄
            var s = text.uppercase()
                .replace("[M]", "M$OVERLINE")
                .replace("[CM]", "C${OVERLINE}M$OVERLINE")
                .replace("[D]", "D$OVERLINE")
                .replace("[CD]", "C${OVERLINE}D$OVERLINE")
                .replace("[C]", "C$OVERLINE")
                .replace("[XC]", "X${OVERLINE}C$OVERLINE")
                .replace("[L]", "L$OVERLINE")
                .replace("[XL]", "X${OVERLINE}L$OVERLINE")
                .replace("[X]", "X$OVERLINE")
                .replace("[IX]", "I${OVERLINE}X$OVERLINE")
                .replace("[V]", "V$OVERLINE")
                .replace("[IV]", "I${OVERLINE}V$OVERLINE")

            var sum = 0L
            val breakdown = mutableListOf<String>()
            var idx = 0

            while (idx < s.length) {
                var matched = false
                for ((value, symbols) in ROMAN_PAIRS) {
                    val symbol = symbols.first
                    if (s.startsWith(symbol, idx)) {
                        sum += value
                        breakdown.add("$symbol = $value")
                        idx += symbol.length
                        matched = true
                        break
                    }
                }
                if (!matched) {
                    // Try single char
                    val charAt = s[idx]
                    return ToolResult.Failure("Unrecognized Roman or Vinculum character '$charAt' at index $idx.")
                }
            }

            val report = buildString {
                appendLine("EXTENDED ROMAN NUMERAL DECODING REPORT")
                appendLine("--------------------------------------------------")
                appendLine("Input Roman:                 $text")
                appendLine("Decoded Decimal Value:       $sum")
                appendLine("--------------------------------------------------")
                appendLine("ADDITIVE BREAKDOWN:")
                breakdown.forEach { appendLine(" * $it") }
            }

            val output = RomanNumeralsOutput(
                inputString = text,
                result = sum.toString(),
                integerVal = sum,
                vinculumRepresentation = text,
                bracketRepresentation = text,
                breakdownComponents = breakdown,
                formattedReport = report,
                summary = "$text → $sum"
            )

            ToolResult.Success(output, System.currentTimeMillis() - startTime, "Decoded Vinculum Roman to integer")
        }
    }
}
