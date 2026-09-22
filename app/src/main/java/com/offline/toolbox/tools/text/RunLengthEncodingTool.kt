package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class RleInput(
    val operation: String = "ENCODE", // ENCODE or DECODE
    val text: String = "WWWWWWWWWWWWBWWWWWWWWWWWWBBBWWWWWWWWWWWWWWWWWWWWWWWWB"
)

data class RleOutput(
    val operation: String,
    val resultText: String,
    val originalLength: Int,
    val resultLength: Int,
    val compressionRatio: Double,
    val spaceSavingsPercent: Double,
    val totalRuns: Int,
    val maxRunLength: Int,
    val formattedReport: String,
    val summary: String
)

class RunLengthEncodingTool : Tool<RleInput, RleOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "run_length_encoding_tool",
        name = "Run-Length Encoding (RLE) Compression Engine",
        description = "Losslessly compress and decompress repetitive text sequences using Run-Length Encoding (RLE) with compression ratio and run statistics.",
        category = ToolCategory.TEXT,
        tags = listOf("rle", "run length encoding", "compression", "lossless", "decompress", "text", "encoding", "algorithm"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FileArchive"
    )

    override suspend fun execute(input: RleInput): ToolResult<RleOutput> {
        val startTime = System.currentTimeMillis()
        val op = input.operation.trim().uppercase(Locale.US)
        val raw = input.text

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        return try {
            if (op == "ENCODE") {
                encodeRle(raw, startTime)
            } else {
                decodeRle(raw, startTime)
            }
        } catch (e: Exception) {
            ToolResult.Failure("RLE processing error: ${e.message}")
        }
    }

    private fun encodeRle(text: String, startTime: Long): ToolResult<RleOutput> {
        val encoded = StringBuilder()
        var runCount = 0
        var maxRun = 0
        var i = 0

        while (i < text.length) {
            val char = text[i]
            var runLen = 1
            while (i + 1 < text.length && text[i + 1] == char) {
                runLen++
                i++
            }
            if (runLen > maxRun) maxRun = runLen
            runCount++
            encoded.append(runLen).append(char)
            i++
        }

        val resultStr = encoded.toString()
        val origLen = text.length
        val resLen = resultStr.length
        val ratio = resLen.toDouble() / origLen
        val savings = (1.0 - ratio) * 100.0

        val report = buildString {
            appendLine("RUN-LENGTH ENCODING (RLE) COMPRESSION")
            appendLine("--------------------------------------------------")
            appendLine("Operation:          ENCODE")
            appendLine("Original Length:    $origLen characters")
            appendLine("Encoded Length:     $resLen characters")
            appendLine(String.format(Locale.US, "Compression Ratio:  %.4f", ratio))
            appendLine(String.format(Locale.US, "Space Savings:      %.2f%% (%s)", savings, if (savings > 0) "Compressed" else "Expanded"))
            appendLine("Distinct Runs:      $runCount")
            appendLine("Longest Run:        $maxRun characters")
            appendLine("Avg Run Length:     ${String.format(Locale.US, "%.2f", origLen.toDouble() / runCount)} chars/run")
            appendLine("--------------------------------------------------")
            appendLine("ENCODED OUTPUT:")
            appendLine(resultStr)
        }

        return ToolResult.Success(
            data = RleOutput(
                operation = "ENCODE",
                resultText = resultStr,
                originalLength = origLen,
                resultLength = resLen,
                compressionRatio = ratio,
                spaceSavingsPercent = savings,
                totalRuns = runCount,
                maxRunLength = maxRun,
                formattedReport = report,
                summary = String.format(Locale.US, "RLE Encoded: %d chars -> %d chars (%.1f%% savings)", origLen, resLen, savings)
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "RLE Encoded $origLen chars"
        )
    }

    private fun decodeRle(text: String, startTime: Long): ToolResult<RleOutput> {
        val decoded = StringBuilder()
        var runCount = 0
        var maxRun = 0
        var i = 0

        while (i < text.length) {
            val numStart = i
            while (i < text.length && text[i].isDigit()) {
                i++
            }
            if (numStart == i) {
                throw IllegalArgumentException("Expected digit count prefix at character index $i ('${text[i]}')")
            }
            val countStr = text.substring(numStart, i)
            val count = countStr.toIntOrNull() ?: throw IllegalArgumentException("Invalid count value '$countStr'")
            if (count <= 0 || count > 100_000) {
                throw IllegalArgumentException("Run length $count is out of realistic bounds (1 to 100,000)")
            }
            if (i >= text.length) {
                throw IllegalArgumentException("Missing character payload after run length prefix '$countStr'")
            }
            val char = text[i]
            i++

            for (k in 0 until count) {
                decoded.append(char)
            }
            if (count > maxRun) maxRun = count
            runCount++
        }

        val resultStr = decoded.toString()
        val origLen = text.length
        val resLen = resultStr.length
        val ratio = origLen.toDouble() / resLen

        val report = buildString {
            appendLine("RUN-LENGTH DECODING (RLE) EXPANSION")
            appendLine("--------------------------------------------------")
            appendLine("Operation:          DECODE")
            appendLine("Encoded Length:     $origLen characters")
            appendLine("Decoded Length:     $resLen characters")
            appendLine(String.format(Locale.US, "Expansion Factor:   %.2fx", resLen.toDouble() / origLen))
            appendLine("Decoded Runs:       $runCount")
            appendLine("Longest Run:        $maxRun characters")
            appendLine("--------------------------------------------------")
            appendLine("DECODED OUTPUT:")
            appendLine(resultStr)
        }

        return ToolResult.Success(
            data = RleOutput(
                operation = "DECODE",
                resultText = resultStr,
                originalLength = origLen,
                resultLength = resLen,
                compressionRatio = ratio,
                spaceSavingsPercent = 0.0,
                totalRuns = runCount,
                maxRunLength = maxRun,
                formattedReport = report,
                summary = "RLE Decoded: $origLen chars -> $resLen chars"
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "RLE Decoded $resLen chars"
        )
    }
}
