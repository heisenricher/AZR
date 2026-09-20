package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class TextBinaryHexMode {
    TEXT_TO_ALL,
    BINARY_TO_ALL,
    HEX_TO_ALL,
    DECIMAL_TO_ALL
}

data class TextBinaryHexInput(
    val input: String,
    val mode: TextBinaryHexMode = TextBinaryHexMode.TEXT_TO_ALL,
    val hexDelimiter: String = " ",
    val binaryDelimiter: String = " ",
    val decimalDelimiter: String = " "
)

data class TextBinaryHexOutput(
    val text: String,
    val binary: String,
    val hexSpaced: String,
    val hexContinuous: String,
    val hexPrefixed: String,
    val decimal: String,
    val byteCount: Int,
    val charCount: Int
)

class TextBinaryHexTool : Tool<TextBinaryHexInput, TextBinaryHexOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "text_binary_hex",
        name = "Binary & Hex Converter",
        description = "Simultaneous multi-representation stream converter between Text, 8-bit Binary, Hex, and Decimal bytes.",
        category = ToolCategory.TEXT,
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        )
    )

    override suspend fun execute(input: TextBinaryHexInput): ToolResult<TextBinaryHexOutput> {
        val raw = input.input.trim()
        if (raw.isEmpty()) {
            return ToolResult.Success(
                TextBinaryHexOutput(
                    text = "",
                    binary = "",
                    hexSpaced = "",
                    hexContinuous = "",
                    hexPrefixed = "",
                    decimal = "",
                    byteCount = 0,
                    charCount = 0
                )
            )
        }

        val bytes: ByteArray = try {
            when (input.mode) {
                TextBinaryHexMode.TEXT_TO_ALL -> {
                    input.input.toByteArray(Charsets.UTF_8)
                }
                TextBinaryHexMode.BINARY_TO_ALL -> {
                    parseBinaryToBytes(raw)
                }
                TextBinaryHexMode.HEX_TO_ALL -> {
                    parseHexToBytes(raw)
                }
                TextBinaryHexMode.DECIMAL_TO_ALL -> {
                    parseDecimalToBytes(raw)
                }
            }
        } catch (e: Exception) {
            return ToolResult.Failure(e.message ?: "Failed to parse input stream")
        }

        val text = String(bytes, Charsets.UTF_8)
        val binary = bytes.joinToString(input.binaryDelimiter) { b ->
            String.format("%8s", Integer.toBinaryString(b.toInt() and 0xFF)).replace(' ', '0')
        }
        val hexSpaced = bytes.joinToString(input.hexDelimiter) { b ->
            String.format("%02X", b)
        }
        val hexContinuous = bytes.joinToString("") { b ->
            String.format("%02X", b)
        }
        val hexPrefixed = bytes.joinToString(" ") { b ->
            String.format("0x%02X", b)
        }
        val decimal = bytes.joinToString(input.decimalDelimiter) { b ->
            (b.toInt() and 0xFF).toString()
        }

        return ToolResult.Success(
            TextBinaryHexOutput(
                text = text,
                binary = binary,
                hexSpaced = hexSpaced,
                hexContinuous = hexContinuous,
                hexPrefixed = hexPrefixed,
                decimal = decimal,
                byteCount = bytes.size,
                charCount = text.length
            )
        )
    }

    private fun parseBinaryToBytes(binaryStr: String): ByteArray {
        val cleaned = binaryStr.replace(Regex("[^01]"), "")
        if (cleaned.isEmpty()) throw IllegalArgumentException("No binary digits found")
        if (cleaned.length % 8 != 0) {
            throw IllegalArgumentException("Binary bit length must be a multiple of 8 (got ${cleaned.length} bits)")
        }
        val bytes = ByteArray(cleaned.length / 8)
        for (i in bytes.indices) {
            val chunk = cleaned.substring(i * 8, (i + 1) * 8)
            bytes[i] = chunk.toInt(2).toByte()
        }
        return bytes
    }

    private fun parseHexToBytes(hexStr: String): ByteArray {
        val cleaned = hexStr.replace("0x", "", ignoreCase = true)
            .replace(Regex("[^0-9a-fA-F]"), "")
        if (cleaned.isEmpty()) throw IllegalArgumentException("No hex digits found")
        if (cleaned.length % 2 != 0) {
            throw IllegalArgumentException("Hex stream must contain an even number of characters (got ${cleaned.length})")
        }
        val bytes = ByteArray(cleaned.length / 2)
        for (i in bytes.indices) {
            val chunk = cleaned.substring(i * 2, (i + 1) * 2)
            bytes[i] = chunk.toInt(16).toByte()
        }
        return bytes
    }

    private fun parseDecimalToBytes(decStr: String): ByteArray {
        val tokens = decStr.split(Regex("[,;\\s]+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) throw IllegalArgumentException("No decimal byte values found")
        val bytes = ByteArray(tokens.size)
        for (i in tokens.indices) {
            val v = tokens[i].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid decimal byte at token ${i + 1}: '${tokens[i]}'")
            if (v !in 0..255) {
                throw IllegalArgumentException("Byte value out of range (0-255): $v at token ${i + 1}")
            }
            bytes[i] = v.toByte()
        }
        return bytes
    }
}
