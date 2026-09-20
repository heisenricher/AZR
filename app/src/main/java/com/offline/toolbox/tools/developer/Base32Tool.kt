package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.nio.charset.StandardCharsets
import java.util.Locale

enum class Base32Mode(val label: String) {
    ENCODE("Encode to Base32"),
    DECODE("Decode from Base32")
}

enum class Base32Alphabet(val label: String, val chars: String) {
    RFC4648("Standard RFC 4648 (A-Z, 2-7)", "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"),
    HEX("Base32Hex Extended (0-9, A-V)", "0123456789ABCDEFGHIJKLMNOPQRSTUV")
}

data class Base32Input(
    val text: String = "",
    val mode: Base32Mode = Base32Mode.ENCODE,
    val alphabet: Base32Alphabet = Base32Alphabet.RFC4648,
    val pad: Boolean = true
)

data class Base32Output(
    val result: String,
    val inputByteCount: Int,
    val outputByteCount: Int,
    val formattedReport: String,
    val summary: String
)

class Base32Tool : Tool<Base32Input, Base32Output> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "base32_tool",
        name = "Base32 Encoder & Decoder (RFC 4648)",
        description = "Encode and decode RFC 4648 Base32 and Base32Hex strings used in TOTP 2FA keys and DNSSEC.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("base32", "rfc4648", "totp", "2fa", "encode", "decode", "binary", "hex"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Code"
    )

    override suspend fun execute(input: Base32Input): ToolResult<Base32Output> {
        val startTime = System.currentTimeMillis()

        if (input.text.isEmpty()) {
            return ToolResult.Failure(
                message = "Input string is empty.",
                userGuidance = "Provide text to encode or a Base32 string to decode."
            )
        }

        return try {
            when (input.mode) {
                Base32Mode.ENCODE -> {
                    val rawBytes = input.text.toByteArray(StandardCharsets.UTF_8)
                    val encoded = encode(rawBytes, input.alphabet.chars, input.pad)
                    val summary = "Encoded ${rawBytes.size} bytes -> ${encoded.length} Base32 chars"

                    val report = buildString {
                        appendLine("BASE32 ENCODING SUCCESSFUL")
                        appendLine("Alphabet:      ${input.alphabet.label}")
                        appendLine("Padding '=':   ${if (input.pad) "Included" else "Stripped"}")
                        appendLine("Input Size:    ${rawBytes.size} bytes")
                        appendLine("Output Size:   ${encoded.length} chars")
                        appendLine("--------------------------------")
                        appendLine("Result:")
                        appendLine(encoded)
                    }

                    ToolResult.Success(
                        data = Base32Output(
                            result = encoded,
                            inputByteCount = rawBytes.size,
                            outputByteCount = encoded.length,
                            formattedReport = report,
                            summary = summary
                        ),
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        summary = summary
                    )
                }

                Base32Mode.DECODE -> {
                    val decodedBytes = decode(input.text, input.alphabet.chars)
                    val decodedText = String(decodedBytes, StandardCharsets.UTF_8)
                    val summary = "Decoded ${input.text.length} chars -> ${decodedBytes.size} bytes"

                    val report = buildString {
                        appendLine("BASE32 DECODING SUCCESSFUL")
                        appendLine("Alphabet:      ${input.alphabet.label}")
                        appendLine("Input Length:  ${input.text.length} chars")
                        appendLine("Output Bytes:  ${decodedBytes.size} bytes")
                        appendLine("--------------------------------")
                        appendLine("Decoded String:")
                        appendLine(decodedText)
                    }

                    ToolResult.Success(
                        data = Base32Output(
                            result = decodedText,
                            inputByteCount = input.text.length,
                            outputByteCount = decodedBytes.size,
                            formattedReport = report,
                            summary = summary
                        ),
                        executionTimeMs = System.currentTimeMillis() - startTime,
                        summary = summary
                    )
                }
            }
        } catch (e: Exception) {
            ToolResult.Failure("Base32 operation failed: ${e.message}", cause = e)
        }
    }

    private fun encode(data: ByteArray, alphabet: String, pad: Boolean): String {
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0

        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer ushr (bitsLeft - 5)) and 0x1F
                sb.append(alphabet[index])
                bitsLeft -= 5
            }
        }

        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            sb.append(alphabet[index])
        }

        if (pad) {
            while (sb.length % 8 != 0) {
                sb.append('=')
            }
        }

        return sb.toString()
    }

    private fun decode(base32: String, alphabet: String): ByteArray {
        val clean = base32.trim().uppercase(Locale.US).replace("=", "").replace(" ", "").replace("-", "")
        val charMap = alphabet.mapIndexed { index, c -> c to index }.toMap()

        val output = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (c in clean) {
            val value = charMap[c] ?: throw IllegalArgumentException("Illegal Base32 character '$c' for alphabet")
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                val b = (buffer ushr (bitsLeft - 8)) and 0xFF
                output.add(b.toByte())
                bitsLeft -= 8
            }
        }

        return output.toByteArray()
    }
}
