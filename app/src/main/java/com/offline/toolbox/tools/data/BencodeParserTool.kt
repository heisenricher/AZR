package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

sealed class BencodeNode {
    data class BInteger(val value: Long) : BencodeNode()
    data class BString(val value: String) : BencodeNode()
    data class BList(val values: List<BencodeNode>) : BencodeNode()
    data class BDict(val entries: Map<String, BencodeNode>) : BencodeNode()

    fun toJson(indent: Int = 0): String {
        val spaces = "  ".repeat(indent)
        return when (this) {
            is BInteger -> value.toString()
            is BString -> "\"${value.replace("\"", "\\\"").replace("\n", "\\n")}\""
            is BList -> {
                if (values.isEmpty()) "[]"
                else buildString {
                    appendLine("[")
                    values.forEachIndexed { idx, item ->
                        append("  ".repeat(indent + 1))
                        append(item.toJson(indent + 1))
                        if (idx < values.size - 1) append(",")
                        appendLine()
                    }
                    append("$spaces]")
                }
            }
            is BDict -> {
                if (entries.isEmpty()) "{}"
                else buildString {
                    appendLine("{")
                    val keys = entries.keys.toList()
                    keys.forEachIndexed { idx, key ->
                        val node = entries[key]!!
                        append("  ".repeat(indent + 1))
                        append("\"$key\": ")
                        append(node.toJson(indent + 1))
                        if (idx < keys.size - 1) append(",")
                        appendLine()
                    }
                    append("$spaces}")
                }
            }
        }
    }

    fun toBencode(): String = when (this) {
        is BInteger -> "i${value}e"
        is BString -> "${value.toByteArray(Charsets.UTF_8).size}:$value"
        is BList -> "l${values.joinToString("") { it.toBencode() }}e"
        is BDict -> "d${entries.toSortedMap().map { (k, v) -> "${k.toByteArray(Charsets.UTF_8).size}:$k${v.toBencode()}" }.joinToString("")}e"
    }

    fun countNodes(): Int = when (this) {
        is BInteger, is BString -> 1
        is BList -> 1 + values.sumOf { it.countNodes() }
        is BDict -> 1 + entries.values.sumOf { it.countNodes() }
    }

    fun maxDepth(): Int = when (this) {
        is BInteger, is BString -> 1
        is BList -> 1 + (values.maxOfOrNull { it.maxDepth() } ?: 0)
        is BDict -> 1 + (entries.values.maxOfOrNull { it.maxDepth() } ?: 0)
    }
}

data class BencodeInput(
    val operation: String = "DECODE", // DECODE or ENCODE_DEMO
    val payload: String = "d4:info4:hash4:name6:ubuntu8:piecessi256ee"
)

data class BencodeOutput(
    val operation: String,
    val canonicalBencode: String,
    val jsonEquivalent: String,
    val nodeCount: Int,
    val treeDepth: Int,
    val formattedReport: String,
    val summary: String
)

class BencodeParserTool : Tool<BencodeInput, BencodeOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "bencode_parser_tool",
        name = "BitTorrent Bencode Parser & Serializer",
        description = "Parse, inspect, and serialize BitTorrent Bencode data structures (integers, byte strings, lists, dictionaries) to and from formatted JSON.",
        category = ToolCategory.DATA,
        tags = listOf("bencode", "bittorrent", "torrent", "p2p", "serialization", "decoder", "encoder", "json"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FileCode"
    )

    override suspend fun execute(input: BencodeInput): ToolResult<BencodeOutput> {
        val startTime = System.currentTimeMillis()
        val op = input.operation.trim().uppercase(Locale.US)
        val raw = input.payload.trim()

        if (raw.isBlank()) {
            return ToolResult.Failure("Bencode input payload cannot be empty.")
        }

        return try {
            val rootNode = parseRoot(raw)
            val json = rootNode.toJson(0)
            val canonical = rootNode.toBencode()
            val nodes = rootNode.countNodes()
            val depth = rootNode.maxDepth()

            val report = buildString {
                appendLine("BITTORRENT BENCODE PARSER & INSPECTION REPORT")
                appendLine("--------------------------------------------------")
                appendLine("Operation Mode:    $op")
                appendLine("Total Nodes:       $nodes")
                appendLine("Tree Max Depth:    $depth levels")
                appendLine("Raw Byte Length:   ${raw.toByteArray(Charsets.UTF_8).size} bytes")
                appendLine("Canonical Bencode: ${canonical.toByteArray(Charsets.UTF_8).size} bytes")
                appendLine("--------------------------------------------------")
                appendLine("JSON EQUIVALENT TREE:")
                appendLine(json)
                appendLine("--------------------------------------------------")
                appendLine("CANONICAL BENCODE STREAM:")
                appendLine(canonical)
            }

            ToolResult.Success(
                data = BencodeOutput(
                    operation = op,
                    canonicalBencode = canonical,
                    jsonEquivalent = json,
                    nodeCount = nodes,
                    treeDepth = depth,
                    formattedReport = report,
                    summary = "Bencode ($nodes nodes, depth $depth)"
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "Parsed Bencode ($nodes nodes)"
            )
        } catch (e: Exception) {
            ToolResult.Failure("Failed to parse Bencode payload: ${e.message}")
        }
    }

    private class Parser(val input: String) {
        var pos = 0

        fun hasMore(): Boolean = pos < input.length
        fun peek(): Char = if (hasMore()) input[pos] else throw IllegalStateException("Unexpected end of input at position $pos")
        fun next(): Char = if (hasMore()) input[pos++] else throw IllegalStateException("Unexpected end of input at position $pos")

        fun parse(): BencodeNode {
            return when (val ch = peek()) {
                'i' -> parseInteger()
                'l' -> parseList()
                'd' -> parseDict()
                in '0'..'9' -> parseString()
                else -> throw IllegalArgumentException("Invalid Bencode token '$ch' at position $pos")
            }
        }

        private fun parseInteger(): BencodeNode.BInteger {
            next() // consume 'i'
            val start = pos
            while (hasMore() && peek() != 'e') {
                next()
            }
            if (!hasMore()) throw IllegalArgumentException("Unterminated integer starting at $start")
            next() // consume 'e'
            val numStr = input.substring(start, pos - 1)
            val value = numStr.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid integer format '$numStr'")
            return BencodeNode.BInteger(value)
        }

        private fun parseString(): BencodeNode.BString {
            val start = pos
            while (hasMore() && peek() != ':') {
                val ch = next()
                if (!ch.isDigit()) throw IllegalArgumentException("Invalid character '$ch' in string length prefix at $pos")
            }
            if (!hasMore()) throw IllegalArgumentException("Missing ':' delimiter in string token")
            next() // consume ':'
            val lenStr = input.substring(start, pos - 1)
            val len = lenStr.toIntOrNull() ?: throw IllegalArgumentException("Invalid string length '$lenStr'")
            if (pos + len > input.length) {
                throw IllegalArgumentException("Declared string length $len exceeds input buffer from position $pos")
            }
            val strContent = input.substring(pos, pos + len)
            pos += len
            return BencodeNode.BString(strContent)
        }

        private fun parseList(): BencodeNode.BList {
            next() // consume 'l'
            val list = mutableListOf<BencodeNode>()
            while (hasMore() && peek() != 'e') {
                list.add(parse())
            }
            if (!hasMore()) throw IllegalArgumentException("Unterminated list token 'l'")
            next() // consume 'e'
            return BencodeNode.BList(list)
        }

        private fun parseDict(): BencodeNode.BDict {
            next() // consume 'd'
            val map = LinkedHashMap<String, BencodeNode>()
            while (hasMore() && peek() != 'e') {
                val keyNode = parse()
                if (keyNode !is BencodeNode.BString) {
                    throw IllegalArgumentException("Bencode dictionary keys must be byte strings")
                }
                val valNode = parse()
                map[keyNode.value] = valNode
            }
            if (!hasMore()) throw IllegalArgumentException("Unterminated dictionary token 'd'")
            next() // consume 'e'
            return BencodeNode.BDict(map)
        }
    }

    private fun parseRoot(input: String): BencodeNode {
        val parser = Parser(input)
        val node = parser.parse()
        if (parser.hasMore()) {
            // If trailing whitespace only, tolerate it, else throw
            val trailing = parser.input.substring(parser.pos).trim()
            if (trailing.isNotEmpty()) {
                throw IllegalArgumentException("Extraneous characters after root Bencode object at position ${parser.pos}")
            }
        }
        return node
    }
}
