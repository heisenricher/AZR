package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class DnsRecordItem(
    val name: String,
    val ttl: Long,
    val recordClass: String,
    val type: String,
    val rdata: String
)

data class DnsParserInput(
    val zoneContent: String = """
        example.com.        3600    IN  A       93.184.216.34
        example.com.        3600    IN  AAAA    2606:2800:220:1:248:1893:25c8:1946
        example.com.        86400   IN  NS      ns1.example.com.
        mail.example.com.   3600    IN  CNAME   example.com.
        example.com.        3600    IN  MX      10 mail.example.com.
        example.com.        300     IN  TXT     "v=spf1 -all"
    """.trimIndent()
)

data class DnsParserOutput(
    val totalRecords: Int,
    val records: List<DnsRecordItem>,
    val recordTypeCounts: Map<String, Int>,
    val auditIssues: List<String>,
    val formattedReport: String,
    val summary: String
)

class DnsRecordParserTool : Tool<DnsParserInput, DnsParserOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "dns_record_parser_tool",
        name = "DNS Zone File & Resource Record Inspector",
        description = "Parse offline DNS zone files and BIND resource records (A, AAAA, CNAME, MX, TXT, NS, SOA) and audit TTLs.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("dns", "zone", "bind", "record", "mx", "cname", "txt", "ns", "ttl", "networking", "domain"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Dns"
    )

    override suspend fun execute(input: DnsParserInput): ToolResult<DnsParserOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.zoneContent.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("DNS zone text cannot be empty.")
        }

        val lines = raw.lines()
        val records = mutableListOf<DnsRecordItem>()
        val issues = mutableListOf<String>()
        val typeCounts = mutableMapOf<String, Int>()

        var lineNum = 0
        var origin = "@"
        var defaultTtl = 3600L

        for (line in lines) {
            lineNum++
            val stripped = line.substringBefore(";").trim()
            if (stripped.isEmpty()) continue

            if (stripped.startsWith("\$ORIGIN", ignoreCase = true)) {
                origin = stripped.substringAfter(" ").trim()
                continue
            }
            if (stripped.startsWith("\$TTL", ignoreCase = true)) {
                defaultTtl = stripped.substringAfter(" ").trim().toLongOrNull() ?: 3600L
                continue
            }

            val tokens = stripped.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (tokens.size < 4) {
                issues.add("Line $lineNum: Insufficient fields for standard DNS record: '$line'")
                continue
            }

            var idx = 0
            var name = tokens[idx++]
            if (name == "@") name = origin

            var ttl = defaultTtl
            var rClass = "IN"

            // Check if next token is TTL
            if (idx < tokens.size && tokens[idx].toLongOrNull() != null) {
                ttl = tokens[idx++].toLong()
            }

            // Check if next token is Class (IN, CH, HS)
            if (idx < tokens.size && tokens[idx].uppercase() in listOf("IN", "CH", "HS", "CS")) {
                rClass = tokens[idx++].uppercase()
            } else if (idx < tokens.size && tokens[idx].toLongOrNull() != null) {
                ttl = tokens[idx++].toLong()
            }

            if (idx >= tokens.size) {
                issues.add("Line $lineNum: Missing record type.")
                continue
            }

            val type = tokens[idx++].uppercase()
            val rdata = tokens.drop(idx).joinToString(" ")

            if (ttl <= 0) {
                issues.add("Line $lineNum: Negative or zero TTL for $name: $ttl")
            } else if (ttl > 604800) {
                issues.add("Line $lineNum: Very high TTL (> 7 days) for $name: $ttl seconds")
            }

            if (type == "CNAME" && name.equals(origin, ignoreCase = true)) {
                issues.add("Line $lineNum: CNAME at zone apex ($origin) violates RFC 1912")
            }

            typeCounts[type] = (typeCounts[type] ?: 0) + 1
            records.add(DnsRecordItem(name, ttl, rClass, type, rdata))
        }

        val report = buildString {
            appendLine("DNS ZONE RESOURCE RECORD AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Total Records Parsed: ${records.size}")
            appendLine("Record Breakdown:     ${typeCounts.entries.joinToString(", ") { "${it.key}: ${it.value}" }}")
            appendLine("Health & RFC Issues:  ${issues.size}")
            appendLine()
            if (issues.isNotEmpty()) {
                appendLine("POTENTIAL ISSUES DETECTED:")
                issues.forEach { appendLine("• $it") }
                appendLine()
            }
            appendLine("PARSED RESOURCE RECORDS:")
            records.forEach { r ->
                appendLine("• %-24s  TTL: %-6d  %s %-5s  %s".format(r.name, r.ttl, r.recordClass, r.type, r.rdata))
            }
        }

        val summary = "Parsed ${records.size} DNS record(s) (${typeCounts.keys.size} types)"

        return ToolResult.Success(
            data = DnsParserOutput(
                totalRecords = records.size,
                records = records,
                recordTypeCounts = typeCounts,
                auditIssues = issues,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
