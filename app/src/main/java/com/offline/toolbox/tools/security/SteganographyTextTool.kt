package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class StegoMode {
    ENCODE,
    DECODE
}

data class StegoInput(
    val mode: StegoMode = StegoMode.ENCODE,
    val coverText: String = "The weekly team sync will take place in Conference Room B tomorrow at 10 AM.",
    val hiddenMessage: String = "Confidential Project Phoenix Alpha",
    val stegoPayloadToDecode: String = ""
)

data class StegoOutput(
    val mode: StegoMode,
    val resultingText: String,
    val hiddenMessageLength: Int,
    val zeroWidthCharCount: Int,
    val formattedReport: String,
    val summary: String
)

class SteganographyTextTool : Tool<StegoInput, StegoOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "steganography_text_tool",
        name = "Invisible Unicode Text Steganography",
        description = "Hide confidential messages invisibly inside plain text using zero-width Unicode characters, and extract concealed payloads.",
        category = ToolCategory.SECURITY,
        tags = listOf("steganography", "hidden", "invisible", "unicode", "zero-width", "covert", "privacy", "security"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "VisibilityOff"
    )

    companion object {
        private const val ZW_ZERO = '\u200C'  // Zero-width non-joiner -> bit 0
        private const val ZW_ONE = '\u200D'   // Zero-width joiner     -> bit 1
        private const val ZW_START = '\u200B' // Zero-width space      -> delimiter start/end
    }

    override suspend fun execute(input: StegoInput): ToolResult<StegoOutput> {
        val startTime = System.currentTimeMillis()

        if (input.mode == StegoMode.ENCODE) {
            val cover = input.coverText.trim()
            val secret = input.hiddenMessage.trim()

            if (cover.isEmpty()) {
                return ToolResult.Failure("Cover text cannot be empty.")
            }
            if (secret.isEmpty()) {
                return ToolResult.Failure("Hidden message cannot be empty.")
            }

            // Convert secret to 8-bit binary
            val secretBytes = secret.toByteArray(Charsets.UTF_8)
            val zwBuilder = java.lang.StringBuilder()
            zwBuilder.append(ZW_START)

            for (b in secretBytes) {
                val byteVal = b.toInt() and 0xFF
                for (bit in 7 downTo 0) {
                    if ((byteVal and (1 shl bit)) != 0) {
                        zwBuilder.append(ZW_ONE)
                    } else {
                        zwBuilder.append(ZW_ZERO)
                    }
                }
            }
            zwBuilder.append(ZW_START)

            val zwString = zwBuilder.toString()

            // Inject zero-width characters into cover text after first space or end
            val insertPos = cover.indexOf(' ').let { if (it >= 0) it + 1 else cover.length }
            val stegoText = cover.substring(0, insertPos) + zwString + cover.substring(insertPos)

            val report = buildString {
                appendLine("ZERO-WIDTH INVISIBLE STEGANOGRAPHY (ENCODE)")
                appendLine("--------------------------------------------------")
                appendLine("Cover Text Length:   ${cover.length} visible characters")
                appendLine("Hidden Secret:       \"$secret\" (${secretBytes.size} bytes)")
                appendLine("Hidden Bit Length:   ${secretBytes.size * 8} bits (${zwString.length} zero-width chars)")
                appendLine()
                appendLine("Stego Text Preview (Zero-width chars are invisible):")
                appendLine(stegoText)
                appendLine()
                appendLine("NOTE: Copying this text preserves the invisible secret.")
            }

            val summary = "Embedded ${secretBytes.size} bytes secretly into cover text"

            return ToolResult.Success(
                data = StegoOutput(
                    mode = StegoMode.ENCODE,
                    resultingText = stegoText,
                    hiddenMessageLength = secret.length,
                    zeroWidthCharCount = zwString.length,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } else {
            // DECODE MODE
            val target = if (input.stegoPayloadToDecode.isNotBlank()) input.stegoPayloadToDecode else input.coverText

            var inPayload = false
            val bitBuilder = java.lang.StringBuilder()

            for (c in target) {
                if (c == ZW_START) {
                    if (!inPayload) {
                        inPayload = true
                    } else {
                        // End of payload delimiter
                        break
                    }
                } else if (inPayload) {
                    if (c == ZW_ZERO) bitBuilder.append('0')
                    else if (c == ZW_ONE) bitBuilder.append('1')
                }
            }

            // Fallback: If no ZW_START was found, extract any ZW_ZERO or ZW_ONE
            if (bitBuilder.isEmpty()) {
                for (c in target) {
                    if (c == ZW_ZERO) bitBuilder.append('0')
                    else if (c == ZW_ONE) bitBuilder.append('1')
                }
            }

            val bitStr = bitBuilder.toString()
            if (bitStr.length < 8) {
                return ToolResult.Failure("No hidden steganographic payload detected in the input text.")
            }

            val byteCount = bitStr.length / 8
            val recoveredBytes = ByteArray(byteCount)
            for (i in 0 until byteCount) {
                val byteChunk = bitStr.substring(i * 8, i * 8 + 8)
                recoveredBytes[i] = byteChunk.toInt(2).toByte()
            }

            val extractedSecret = String(recoveredBytes, Charsets.UTF_8)

            val report = buildString {
                appendLine("ZERO-WIDTH INVISIBLE STEGANOGRAPHY (DECODE)")
                appendLine("--------------------------------------------------")
                appendLine("Payload Bits Found:  ${bitStr.length} bits")
                appendLine("Extracted Bytes:     $byteCount bytes")
                appendLine()
                appendLine("REVEALED HIDDEN SECRET:")
                appendLine(extractedSecret)
            }

            val summary = "Extracted hidden payload ($byteCount bytes): \"$extractedSecret\""

            return ToolResult.Success(
                data = StegoOutput(
                    mode = StegoMode.DECODE,
                    resultingText = extractedSecret,
                    hiddenMessageLength = extractedSecret.length,
                    zeroWidthCharCount = bitStr.length,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        }
    }
}
