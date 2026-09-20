package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.math.BigInteger
import java.util.Locale

enum class NumberBase(val radix: Int, val displayName: String) {
    BINARY(2, "Binary (Base 2)"),
    OCTAL(8, "Octal (Base 8)"),
    DECIMAL(10, "Decimal (Base 10)"),
    HEXADECIMAL(16, "Hexadecimal (Base 16)")
}

data class NumberBaseInput(
    val value: String,
    val sourceBase: NumberBase = NumberBase.DECIMAL
)

data class NumberBaseOutput(
    val binary: String,
    val binaryFormatted: String,
    val octal: String,
    val decimal: String,
    val hexadecimal: String,
    val bitLength: Int,
    val asciiChar: String?,
    val summary: String
)

class NumberBaseConverterTool : Tool<NumberBaseInput, NumberBaseOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "number_base_converter",
        name = "Number Base Converter",
        description = "Convert numbers between Binary, Octal, Decimal, and Hexadecimal with arbitrary precision.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("base", "binary", "hex", "octal", "decimal", "radix", "converter", "bitwise"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Memory"
    )

    override suspend fun execute(input: NumberBaseInput): ToolResult<NumberBaseOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.value.trim().replace(" ", "").replace("_", "")

        if (raw.isEmpty()) {
            return ToolResult.Failure(
                message = "Input value is empty.",
                userGuidance = "Enter a valid number in the chosen source base."
            )
        }

        val cleanRaw = if (input.sourceBase == NumberBase.HEXADECIMAL) {
            raw.removePrefix("0x").removePrefix("0X")
        } else if (input.sourceBase == NumberBase.BINARY) {
            raw.removePrefix("0b").removePrefix("0B")
        } else {
            raw
        }

        return try {
            val bigInt = BigInteger(cleanRaw, input.sourceBase.radix)

            val bin = bigInt.toString(2)
            val oct = bigInt.toString(8)
            val dec = bigInt.toString(10)
            val hex = bigInt.toString(16).uppercase(Locale.US)

            // Format binary in 4-bit nibbles
            val formattedBin = formatBinaryNibbles(bin)

            val ascii = if (bigInt >= BigInteger.valueOf(32) && bigInt <= BigInteger.valueOf(126)) {
                bigInt.toInt().toChar().toString()
            } else null

            val summary = "Dec: $dec • Hex: 0x$hex • Bin: $bin"

            ToolResult.Success(
                data = NumberBaseOutput(
                    binary = bin,
                    binaryFormatted = formattedBin,
                    octal = oct,
                    decimal = dec,
                    hexadecimal = hex,
                    bitLength = bigInt.bitLength(),
                    asciiChar = ascii,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: NumberFormatException) {
            ToolResult.Failure(
                message = "Invalid digits for ${input.sourceBase.displayName}: '$raw'",
                userGuidance = when (input.sourceBase) {
                    NumberBase.BINARY -> "Binary numbers can only contain '0' and '1'."
                    NumberBase.OCTAL -> "Octal numbers can only contain digits 0 through 7."
                    NumberBase.DECIMAL -> "Decimal numbers can only contain digits 0 through 9."
                    NumberBase.HEXADECIMAL -> "Hexadecimal numbers can contain digits 0-9 and letters A-F."
                },
                cause = e
            )
        }
    }

    private fun formatBinaryNibbles(bin: String): String {
        val isNegative = bin.startsWith("-")
        val digits = if (isNegative) bin.substring(1) else bin
        val pad = (4 - (digits.length % 4)) % 4
        val padded = "0".repeat(pad) + digits
        val chunked = padded.chunked(4).joinToString(" ")
        return if (isNegative) "-$chunked" else chunked
    }
}
