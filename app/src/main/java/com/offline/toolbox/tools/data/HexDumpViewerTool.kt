package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.log2

enum class HexDumpInputMode {
    PLAIN_TEXT,
    HEX_STRING
}

data class HexDumpInput(
    val content: String = "AZR Offline Android Utility Toolbox 2026\nSovereign Suite",
    val inputMode: HexDumpInputMode = HexDumpInputMode.PLAIN_TEXT,
    val bytesPerLine: Int = 16
)

data class HexDumpOutput(
    val formattedHexDump: String,
    val totalBytes: Int,
    val totalLines: Int,
    val byteEntropy: Double,
    val formattedReport: String,
    val summary: String
)

class HexDumpViewerTool : Tool<HexDumpInput, HexDumpOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "hex_dump_viewer_tool",
        name = "Hex Dump & Binary Memory Inspector (xxd)",
        description = "Format binary or text data into standard 16-byte hex dump layout with offsets, hex pairs, and printable ASCII representation.",
        category = ToolCategory.DATA,
        tags = listOf("hex", "dump", "binary", "xxd", "hexdump", "bytes", "memory", "inspector", "offset"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Memory"
    )

    override suspend fun execute(input: HexDumpInput): ToolResult<HexDumpOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.content

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input content cannot be empty.")
        }

        val bytes: ByteArray = if (input.inputMode == HexDumpInputMode.PLAIN_TEXT) {
            raw.toByteArray(Charsets.UTF_8)
        } else {
            val cleanHex = raw.replace(Regex("[^0-9a-fA-F]"), "")
            if (cleanHex.length % 2 != 0) {
                return ToolResult.Failure("Hex string must have an even number of hex characters.")
            }
            ByteArray(cleanHex.length / 2) { idx ->
                cleanHex.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
        }

        if (bytes.isEmpty()) {
            return ToolResult.Failure("No bytes to display.")
        }

        val bpl = input.bytesPerLine.coerceIn(8, 32)
        val sb = StringBuilder()
        var offset = 0

        while (offset < bytes.size) {
            val count = minOf(bpl, bytes.size - offset)
            // Offset
            sb.append("%08X  ".format(offset))

            // Hex pairs
            val hexPart = StringBuilder()
            for (i in 0 until bpl) {
                if (i < count) {
                    val b = bytes[offset + i].toInt() and 0xFF
                    hexPart.append("%02X ".format(b))
                } else {
                    hexPart.append("   ")
                }
                if (i == (bpl / 2) - 1) hexPart.append(" ") // extra spacer in middle
            }
            sb.append(hexPart.toString())
            sb.append(" |")

            // ASCII part
            for (i in 0 until count) {
                val b = bytes[offset + i].toInt() and 0xFF
                if (b in 32..126) {
                    sb.append(b.toChar())
                } else {
                    sb.append('.')
                }
            }
            sb.appendLine("|")
            offset += count
        }

        // Byte Shannon Entropy
        val freq = IntArray(256)
        for (b in bytes) freq[b.toInt() and 0xFF]++
        var entropy = 0.0
        val total = bytes.size.toDouble()
        for (f in freq) {
            if (f > 0) {
                val p = f / total
                entropy -= p * log2(p)
            }
        }

        val dumpResult = sb.toString()
        val lineCount = (bytes.size + bpl - 1) / bpl

        val report = buildString {
            appendLine("BINARY HEX DUMP INSPECTION")
            appendLine("--------------------------------------------------")
            appendLine("Total Bytes:   ${bytes.size} bytes ($lineCount lines)")
            appendLine("Bytes / Line:  $bpl")
            appendLine("Byte Entropy:  ${String.format(Locale.US, "%.3f", entropy)} bits/byte (Max: 8.000)")
            appendLine()
            appendLine(dumpResult)
        }

        val summary = "${bytes.size} bytes (${String.format(Locale.US, "%.2f", entropy)} bits entropy)"

        return ToolResult.Success(
            data = HexDumpOutput(
                formattedHexDump = dumpResult,
                totalBytes = bytes.size,
                totalLines = lineCount,
                byteEntropy = entropy,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
