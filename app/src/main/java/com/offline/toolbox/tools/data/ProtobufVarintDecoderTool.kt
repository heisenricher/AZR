package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class VarintOperationMode(val displayName: String) {
    DECODE_HEX_BYTES("Decode Hex Bytes → Integers"),
    ENCODE_INTEGER("Encode Integer → Varint & ZigZag Hex")
}

data class ProtobufVarintInput(
    val inputString: String = "96 01", // Standard Protobuf example 150
    val mode: VarintOperationMode = VarintOperationMode.DECODE_HEX_BYTES
)

data class VarintByteStep(
    val byteIndex: Int,
    val hexValue: String,
    val binaryByte: String,
    val continuationBit: Boolean,
    val payload7Bits: String,
    val shiftBits: Int
)

data class ProtobufVarintOutput(
    val unsignedValue: ULong,
    val signedZigZag32: Int,
    val signedZigZag64: Long,
    val totalBytesConsumed: Int,
    val encodedHex: String,
    val steps: List<VarintByteStep>,
    val formattedReport: String,
    val summary: String
)

class ProtobufVarintDecoderTool : Tool<ProtobufVarintInput, ProtobufVarintOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "protobuf_varint_decoder_tool",
        name = "Protobuf Varint & ZigZag Decoder",
        description = "Decode Protocol Buffers LEB128 Varints, uint32/uint64, and sint32/sint64 ZigZag encoded integers with bitwise analysis.",
        category = ToolCategory.DATA,
        tags = listOf("protobuf", "varint", "leb128", "zigzag", "binary", "hex", "decoder", "serialization", "bytes"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Code"
    )

    override suspend fun execute(input: ProtobufVarintInput): ToolResult<ProtobufVarintOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.inputString.trim()

        if (text.isEmpty()) {
            return ToolResult.Failure("Input string cannot be empty.")
        }

        return try {
            if (input.mode == VarintOperationMode.DECODE_HEX_BYTES) {
                decodeHex(text, startTime)
            } else {
                encodeInteger(text, startTime)
            }
        } catch (e: Exception) {
            ToolResult.Failure("Protobuf Varint processing error: ${e.message}")
        }
    }

    private fun decodeHex(hexStr: String, startTime: Long): ToolResult<ProtobufVarintOutput> {
        val cleanHex = hexStr.replace("0x", "", ignoreCase = true)
            .replace(",", " ")
            .replace(";", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (cleanHex.isEmpty()) {
            return ToolResult.Failure("No valid hexadecimal bytes provided.")
        }

        val bytes = cleanHex.map { it.toInt(16) }
        val steps = mutableListOf<VarintByteStep>()
        var unsignedVal = 0UL
        var shift = 0
        var bytesUsed = 0

        for (b in bytes) {
            bytesUsed++
            val isContinuation = (b and 0x80) != 0
            val payload = b and 0x7F
            val binStr = String.format(Locale.US, "%8s", Integer.toBinaryString(b)).replace(' ', '0')
            val payload7Bin = String.format(Locale.US, "%7s", Integer.toBinaryString(payload)).replace(' ', '0')

            steps.add(
                VarintByteStep(
                    byteIndex = bytesUsed,
                    hexValue = String.format(Locale.US, "0x%02X", b),
                    binaryByte = binStr,
                    continuationBit = isContinuation,
                    payload7Bits = payload7Bin,
                    shiftBits = shift
                )
            )

            unsignedVal = unsignedVal or (payload.toULong() shl shift)
            shift += 7

            if (!isContinuation) {
                break
            }
            if (bytesUsed > 10) {
                return ToolResult.Failure("Varint exceeds 10 bytes (standard 64-bit integer limit).")
            }
        }

        // ZigZag decoding:
        // sint32: (n >>> 1) ^ -(n & 1)
        // sint64: (n >>> 1) ^ -(n & 1)
        val rawLong = unsignedVal.toLong()
        val zz64 = (rawLong ushr 1) xor -(rawLong and 1L)
        val rawInt = unsignedVal.toInt()
        val zz32 = (rawInt ushr 1) xor -(rawInt and 1)

        val hexResult = cleanHex.take(bytesUsed).joinToString(" ") { it.uppercase(Locale.US) }

        val report = buildString {
            appendLine("PROTOBUF VARINT & ZIGZAG DECODING REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Input Hex Stream:      $hexResult")
            appendLine("Bytes Consumed:        $bytesUsed byte(s)")
            appendLine("--------------------------------------------------")
            appendLine("DECODED INTEGER INTERPRETATIONS:")
            appendLine(" * uint64 / int64:     $unsignedVal ($rawLong)")
            appendLine(" * sint32 (ZigZag):    $zz32")
            appendLine(" * sint64 (ZigZag):    $zz64")
            appendLine("--------------------------------------------------")
            appendLine("LEB128 BITWISE PIPELINE:")
            steps.forEach { s ->
                val cont = if (s.continuationBit) "CONTINUE [1]" else "TERMINAL [0]"
                appendLine(" Byte ${s.byteIndex}: ${s.hexValue} | ${s.binaryByte} | 7-bit: ${s.payload7Bits} | shift: ${s.shiftBits} | $cont")
            }
        }

        val output = ProtobufVarintOutput(
            unsignedValue = unsignedVal,
            signedZigZag32 = zz32,
            signedZigZag64 = zz64,
            totalBytesConsumed = bytesUsed,
            encodedHex = hexResult,
            steps = steps,
            formattedReport = report,
            summary = "Varint Decoded: uint=$unsignedVal, sint32(zigzag)=$zz32, sint64(zigzag)=$zz64"
        )

        return ToolResult.Success(output, System.currentTimeMillis() - startTime, "Decoded Protobuf varint")
    }

    private fun encodeInteger(inputNumStr: String, startTime: Long): ToolResult<ProtobufVarintOutput> {
        val num = inputNumStr.toLongOrNull()
            ?: return ToolResult.Failure("Invalid integer format: '$inputNumStr'. Expected a 64-bit integer.")

        // Unsigned varint encoding
        var v = num.toULong()
        val encodedBytes = mutableListOf<Int>()
        val steps = mutableListOf<VarintByteStep>()
        var shift = 0

        while (true) {
            val payload = (v and 0x7FUL).toInt()
            v = v shr 7
            if (v == 0UL) {
                // Last byte
                encodedBytes.add(payload)
                val bin = String.format(Locale.US, "%8s", Integer.toBinaryString(payload)).replace(' ', '0')
                steps.add(
                    VarintByteStep(
                        byteIndex = encodedBytes.size,
                        hexValue = String.format(Locale.US, "0x%02X", payload),
                        binaryByte = bin,
                        continuationBit = false,
                        payload7Bits = String.format(Locale.US, "%7s", Integer.toBinaryString(payload)).replace(' ', '0'),
                        shiftBits = shift
                    )
                )
                break
            } else {
                val b = payload or 0x80
                encodedBytes.add(b)
                val bin = String.format(Locale.US, "%8s", Integer.toBinaryString(b)).replace(' ', '0')
                steps.add(
                    VarintByteStep(
                        byteIndex = encodedBytes.size,
                        hexValue = String.format(Locale.US, "0x%02X", b),
                        binaryByte = bin,
                        continuationBit = true,
                        payload7Bits = String.format(Locale.US, "%7s", Integer.toBinaryString(payload)).replace(' ', '0'),
                        shiftBits = shift
                    )
                )
                shift += 7
            }
        }

        // ZigZag encoding of num: (num shl 1) xor (num shr 63)
        val zz64 = (num shl 1) xor (num shr 63)
        var zzV = zz64.toULong()
        val zzBytes = mutableListOf<Int>()
        while (true) {
            val p = (zzV and 0x7FUL).toInt()
            zzV = zzV shr 7
            if (zzV == 0UL) {
                zzBytes.add(p)
                break
            } else {
                zzBytes.add(p or 0x80)
            }
        }

        val hexStandard = encodedBytes.joinToString(" ") { String.format(Locale.US, "%02X", it) }
        val hexZigzag = zzBytes.joinToString(" ") { String.format(Locale.US, "%02X", it) }

        val report = buildString {
            appendLine("PROTOBUF VARINT & ZIGZAG ENCODING REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Input Integer:         $num")
            appendLine("Standard Varint Hex:   $hexStandard (${encodedBytes.size} byte(s))")
            appendLine("ZigZag Encoded Hex:    $hexZigzag (${zzBytes.size} byte(s))")
            appendLine("ZigZag 64-bit Value:   $zz64 (0x${java.lang.Long.toHexString(zz64)})")
            appendLine("--------------------------------------------------")
            appendLine("STANDARD VARINT STEP-BY-STEP:")
            steps.forEach { s ->
                val cont = if (s.continuationBit) "CONTINUE [1]" else "TERMINAL [0]"
                appendLine(" Byte ${s.byteIndex}: ${s.hexValue} | ${s.binaryByte} | 7-bit: ${s.payload7Bits} | $cont")
            }
        }

        val output = ProtobufVarintOutput(
            unsignedValue = num.toULong(),
            signedZigZag32 = if (num in Int.MIN_VALUE..Int.MAX_VALUE) ((num.toInt() shl 1) xor (num.toInt() shr 31)) else 0,
            signedZigZag64 = zz64,
            totalBytesConsumed = encodedBytes.size,
            encodedHex = hexStandard,
            steps = steps,
            formattedReport = report,
            summary = "Varint Encoded $num → Standard: [$hexStandard], ZigZag: [$hexZigzag]"
        )

        return ToolResult.Success(output, System.currentTimeMillis() - startTime, "Encoded integer into Protobuf Varint")
    }
}
