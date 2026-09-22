package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.nio.ByteBuffer
import java.util.Locale

data class CborInput(
    val hexString: String = "a2646e616d6565416c69636563616765181e"
)

data class CborParsedNode(
    val majorType: Int,
    val majorTypeName: String,
    val byteOffset: Int,
    val byteLength: Int,
    val valueRepr: String,
    val jsonRepr: String,
    val children: List<CborParsedNode> = emptyList()
)

data class CborOutput(
    val originalHex: String,
    val byteCount: Int,
    val jsonRepresentation: String,
    val diagnosticNotation: String,
    val annotatedHexDump: String,
    val formattedReport: String,
    val summary: String
)

class CborHexInspectorTool : Tool<CborInput, CborOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "cbor_hex_inspector_tool",
        name = "CBOR Binary Hex Inspector & JSON Converter",
        description = "Parse and inspect Concise Binary Object Representation (CBOR / RFC 8949) hex strings into structured JSON, annotated hex breakdowns, and RFC diagnostic notation.",
        category = ToolCategory.DATA,
        tags = listOf("cbor", "rfc8949", "binary", "hex", "inspector", "json", "decoder", "serialization", "iot", "cose"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Code"
    )

    private fun cleanHex(hex: String): ByteArray {
        val filtered = hex.filter { it in "0123456789abcdefABCDEF" }
        if (filtered.length % 2 != 0) {
            throw IllegalArgumentException("Hex string length must be even (got ${filtered.length} characters).")
        }
        val bytes = ByteArray(filtered.length / 2)
        for (i in filtered.indices step 2) {
            bytes[i / 2] = filtered.substring(i, i + 2).toInt(16).toByte()
        }
        return bytes
    }

    private class CborReader(val bytes: ByteArray) {
        var pos: Int = 0

        fun hasMore(): Boolean = pos < bytes.size

        fun peek(): Int {
            if (!hasMore()) throw IllegalStateException("Unexpected end of CBOR stream at offset $pos")
            return bytes[pos].toInt() and 0xFF
        }

        fun readByte(): Int {
            if (!hasMore()) throw IllegalStateException("Unexpected end of CBOR stream at offset $pos")
            return bytes[pos++].toInt() and 0xFF
        }

        fun readBytes(count: Int): ByteArray {
            if (pos + count > bytes.size) {
                throw IllegalStateException("Attempted to read $count bytes at offset $pos, but only ${bytes.size - pos} available.")
            }
            val res = bytes.copyOfRange(pos, pos + count)
            pos += count
            return res
        }

        fun readLength(additionalInfo: Int): Long {
            return when (additionalInfo) {
                in 0..23 -> additionalInfo.toLong()
                24 -> readByte().toLong()
                25 -> {
                    val b = readBytes(2)
                    ((b[0].toInt() and 0xFF) shl 8 or (b[1].toInt() and 0xFF)).toLong()
                }
                26 -> {
                    val b = readBytes(4)
                    var res = 0L
                    for (i in 0..3) {
                        res = (res shl 8) or (b[i].toLong() and 0xFF)
                    }
                    res
                }
                27 -> {
                    val b = readBytes(8)
                    var res = 0L
                    for (i in 0..7) {
                        res = (res shl 8) or (b[i].toLong() and 0xFF)
                    }
                    res
                }
                else -> throw IllegalArgumentException("Unsupported additional information code $additionalInfo at offset $pos")
            }
        }

        fun parseNode(): CborParsedNode {
            val startPos = pos
            val initial = readByte()
            val major = (initial ushr 5) and 0x07
            val addInfo = initial and 0x1F

            val typeName = when (major) {
                0 -> "Unsigned Integer"
                1 -> "Negative Integer"
                2 -> "Byte String"
                3 -> "Text String"
                4 -> "Array"
                5 -> "Map"
                6 -> "Tagged Item"
                7 -> "Simple / Float"
                else -> "Unknown"
            }

            when (major) {
                0 -> {
                    val value = readLength(addInfo)
                    val len = pos - startPos
                    return CborParsedNode(major, typeName, startPos, len, value.toString(), value.toString())
                }
                1 -> {
                    val unsignedVal = readLength(addInfo)
                    val value = -1 - unsignedVal
                    val len = pos - startPos
                    return CborParsedNode(major, typeName, startPos, len, value.toString(), value.toString())
                }
                2 -> {
                    val count = readLength(addInfo).toInt()
                    val b = readBytes(count)
                    val hexRepr = b.joinToString("") { "%02x".format(it) }
                    val len = pos - startPos
                    return CborParsedNode(major, typeName, startPos, len, "h'$hexRepr'", "\"h'$hexRepr'\"")
                }
                3 -> {
                    val count = readLength(addInfo).toInt()
                    val b = readBytes(count)
                    val strVal = String(b, Charsets.UTF_8)
                    val len = pos - startPos
                    val escaped = strVal.replace("\"", "\\\"").replace("\n", "\\n")
                    return CborParsedNode(major, typeName, startPos, len, "\"$strVal\"", "\"$escaped\"")
                }
                4 -> {
                    val count = readLength(addInfo).toInt()
                    val children = mutableListOf<CborParsedNode>()
                    for (i in 0 until count) {
                        children.add(parseNode())
                    }
                    val len = pos - startPos
                    val jsonItems = children.joinToString(", ") { it.jsonRepr }
                    return CborParsedNode(major, "$typeName ($count items)", startPos, len, "[$count items]", "[$jsonItems]", children)
                }
                5 -> {
                    val pairCount = readLength(addInfo).toInt()
                    val children = mutableListOf<CborParsedNode>()
                    val jsonPairs = mutableListOf<String>()
                    for (i in 0 until pairCount) {
                        val key = parseNode()
                        val value = parseNode()
                        children.add(key)
                        children.add(value)
                        val keyStr = if (key.jsonRepr.startsWith("\"")) key.jsonRepr else "\"${key.valueRepr}\""
                        jsonPairs.add("$keyStr: ${value.jsonRepr}")
                    }
                    val len = pos - startPos
                    return CborParsedNode(major, "$typeName ($pairCount pairs)", startPos, len, "{$pairCount pairs}", "{${jsonPairs.joinToString(", ")}}", children)
                }
                6 -> {
                    val tagNum = readLength(addInfo)
                    val child = parseNode()
                    val len = pos - startPos
                    return CborParsedNode(major, "Tag #$tagNum", startPos, len, "$tagNum(${child.valueRepr})", child.jsonRepr, listOf(child))
                }
                7 -> {
                    val len: Int
                    val (valStr, jsonStr) = when (addInfo) {
                        20 -> "false" to "false"
                        21 -> "true" to "true"
                        22 -> "null" to "null"
                        23 -> "undefined" to "null"
                        25 -> {
                            val b = readBytes(2)
                            val bits = ((b[0].toInt() and 0xFF) shl 8) or (b[1].toInt() and 0xFF)
                            val f = halfFloatToFloat(bits)
                            f.toString() to f.toString()
                        }
                        26 -> {
                            val b = readBytes(4)
                            val f = ByteBuffer.wrap(b).float
                            f.toString() to f.toString()
                        }
                        27 -> {
                            val b = readBytes(8)
                            val d = ByteBuffer.wrap(b).double
                            d.toString() to d.toString()
                        }
                        else -> "simple($addInfo)" to "\"simple($addInfo)\""
                    }
                    len = pos - startPos
                    return CborParsedNode(major, typeName, startPos, len, valStr, jsonStr)
                }
                else -> {
                    val len = pos - startPos
                    return CborParsedNode(major, "Unknown", startPos, len, "unknown", "null")
                }
            }
        }

        private fun halfFloatToFloat(hbits: Int): Float {
            var mant = hbits and 0x03ff
            var exp = hbits and 0x7c00
            if (exp == 0x7c00) {
                exp = 0x3fc00
            } else if (exp != 0) {
                exp += 0x1c000
                if (mant == 0 && exp == 0x1c000) {
                    return Float.fromBits((hbits and 0x8000) shl 16)
                }
            } else if (mant != 0) {
                exp = 0x1c400
                do {
                    mant = mant shl 1
                    exp -= 0x400
                } while ((mant and 0x400) == 0)
                mant = mant and 0x3ff
            }
            return Float.fromBits(((hbits and 0x8000) shl 16) or ((exp or mant) shl 13))
        }
    }

    private fun formatAnnotatedTree(node: CborParsedNode, bytes: ByteArray, indent: Int = 0): String {
        val sb = StringBuilder()
        val pad = "  ".repeat(indent)
        val sliceHex = bytes.sliceArray(node.byteOffset until (node.byteOffset + node.byteLength).coerceAtMost(bytes.size))
            .joinToString("") { "%02x".format(it) }
        val shortHex = if (sliceHex.length > 24) sliceHex.take(20) + "..." else sliceHex
        sb.appendLine("%s[0x%02X] %-18s : %s  (bytes: %s)".format(Locale.US, pad, node.byteOffset, node.majorTypeName, node.valueRepr, shortHex))
        for (child in node.children) {
            sb.append(formatAnnotatedTree(child, bytes, indent + 1))
        }
        return sb.toString()
    }

    private fun prettyPrintJson(json: String): String {
        val sb = StringBuilder()
        var indent = 0
        var inQuote = false
        for (i in json.indices) {
            val c = json[i]
            when {
                c == '"' && (i == 0 || json[i - 1] != '\\') -> {
                    inQuote = !inQuote
                    sb.append(c)
                }
                inQuote -> sb.append(c)
                c == '{' || c == '[' -> {
                    sb.append(c).append("\n").append("  ".repeat(++indent))
                }
                c == '}' || c == ']' -> {
                    sb.append("\n").append("  ".repeat(--indent)).append(c)
                }
                c == ',' -> {
                    sb.append(c).append("\n").append("  ".repeat(indent))
                }
                c == ':' -> sb.append(": ")
                c.isWhitespace() -> {}
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    override suspend fun execute(input: CborInput): ToolResult<CborOutput> {
        val startTime = System.currentTimeMillis()
        val rawHex = input.hexString.trim()
        if (rawHex.isBlank()) {
            return ToolResult.Failure("CBOR hex string cannot be empty.")
        }

        val bytes: ByteArray
        try {
            bytes = cleanHex(rawHex)
        } catch (e: Exception) {
            return ToolResult.Failure("Invalid hex input: ${e.message}")
        }

        if (bytes.isEmpty()) {
            return ToolResult.Failure("No valid hex bytes found in input.")
        }

        try {
            val reader = CborReader(bytes)
            val rootNodes = mutableListOf<CborParsedNode>()
            while (reader.hasMore()) {
                rootNodes.add(reader.parseNode())
            }

            val rawJson = if (rootNodes.size == 1) rootNodes[0].jsonRepr else "[" + rootNodes.joinToString(", ") { it.jsonRepr } + "]"
            val prettyJson = prettyPrintJson(rawJson)

            val hexDump = buildString {
                rootNodes.forEach { node ->
                    append(formatAnnotatedTree(node, bytes))
                }
            }

            val diagnostic = if (rootNodes.size == 1) rootNodes[0].valueRepr else rootNodes.joinToString(", ") { it.valueRepr }

            val elapsed = System.currentTimeMillis() - startTime
            val report = buildString {
                appendLine("=== CBOR (RFC 8949) HEX INSPECTION ===")
                appendLine("Total Payload Size: ${bytes.size} bytes (${rawHex.length} hex chars)")
                appendLine("Top-Level Elements: ${rootNodes.size}")
                appendLine("----------------------------------------")
                appendLine("Diagnostic Notation:")
                appendLine(diagnostic)
                appendLine("----------------------------------------")
                appendLine("Structured JSON Output:")
                appendLine(prettyJson)
                appendLine("----------------------------------------")
                appendLine("Annotated Byte Breakdown:")
                append(hexDump)
            }

            return ToolResult.Success(
                data = CborOutput(
                    originalHex = rawHex,
                    byteCount = bytes.size,
                    jsonRepresentation = prettyJson,
                    diagnosticNotation = diagnostic,
                    annotatedHexDump = hexDump,
                    formattedReport = report,
                    summary = "Parsed ${bytes.size} CBOR bytes into structured JSON (${rootNodes.size} top-level items)."
                ),
                executionTimeMs = elapsed,
                summary = "Parsed ${bytes.size} CBOR bytes to JSON"
            )
        } catch (e: Exception) {
            return ToolResult.Failure("CBOR parsing failed: ${e.message}")
        }
    }
}
