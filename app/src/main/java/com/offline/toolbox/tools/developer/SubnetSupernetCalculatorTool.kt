package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.min

data class SubnetSupernetInput(
    val subnetList: String = """
        192.168.0.0/24
        192.168.1.0/24
        192.168.2.0/24
        192.168.3.0/24
    """.trimIndent()
)

data class SubnetSupernetOutput(
    val summarizedCidrs: List<String>,
    val totalSubnetsInput: Int,
    val inputTotalAddresses: Long,
    val summarizedTotalAddresses: Long,
    val utilizationPercentage: Double,
    val formattedReport: String,
    val summary: String
)

class SubnetSupernetCalculatorTool : Tool<SubnetSupernetInput, SubnetSupernetOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "subnet_supernet_calculator_tool",
        name = "CIDR Route Aggregation & Supernet Calculator",
        description = "Summarize multiple IPv4 subnets into the most compact aggregated CIDR blocks and analyze route table economy.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("cidr", "supernet", "subnet", "route", "aggregation", "ipv4", "network", "bgp", "ip"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Share"
    )

    override suspend fun execute(input: SubnetSupernetInput): ToolResult<SubnetSupernetOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.subnetList.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Subnet list cannot be empty.")
        }

        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val ranges = mutableListOf<IpRange>()

        for (line in lines) {
            val parsed = parseCidr(line) ?: return ToolResult.Failure("Invalid CIDR notation: '$line'. Expected format e.g. 192.168.1.0/24")
            ranges.add(parsed)
        }

        if (ranges.isEmpty()) {
            return ToolResult.Failure("No valid subnets found to aggregate.")
        }

        val inputTotalIps = ranges.sumOf { it.size }

        // Sort ranges and merge overlapping or contiguous
        ranges.sortBy { it.start }
        val merged = mergeRanges(ranges)

        // Decompose each merged range into optimal CIDR blocks
        val aggregatedCidrs = mutableListOf<String>()
        var supernetTotalIps = 0L

        for (range in merged) {
            val cidrs = rangeToCidrs(range.start, range.end)
            aggregatedCidrs.addAll(cidrs)
        }

        for (c in aggregatedCidrs) {
            val prefix = c.substringAfter("/").toInt()
            supernetTotalIps += (1L shl (32 - prefix))
        }

        val utilization = if (supernetTotalIps > 0) {
            (inputTotalIps.toDouble() / supernetTotalIps.toDouble()) * 100.0
        } else 100.0

        val report = buildString {
            appendLine("CIDR ROUTE AGGREGATION & SUPERNET REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Input Subnets:         ${lines.size}")
            appendLine("Total Input Hosts:     $inputTotalIps addresses")
            appendLine("Aggregated Blocks:     ${aggregatedCidrs.size}")
            appendLine("Supernet Capacity:     $supernetTotalIps addresses")
            appendLine("Capacity Efficiency:   ${String.format(Locale.US, "%.2f", utilization)}%")
            appendLine()
            appendLine("OPTIMAL SUMMARIZED CIDR ROUTE(S):")
            aggregatedCidrs.forEachIndexed { index, cidr ->
                val prefix = cidr.substringAfter("/").toInt()
                val ips = 1L shl (32 - prefix)
                appendLine(" [${index + 1}] $cidr ($ips addresses)")
            }
        }

        val summary = "Aggregated ${lines.size} subnets into ${aggregatedCidrs.size} block(s): ${aggregatedCidrs.joinToString(", ")}"

        return ToolResult.Success(
            data = SubnetSupernetOutput(
                summarizedCidrs = aggregatedCidrs,
                totalSubnetsInput = lines.size,
                inputTotalAddresses = inputTotalIps,
                summarizedTotalAddresses = supernetTotalIps,
                utilizationPercentage = utilization,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private data class IpRange(val start: Long, val end: Long) {
        val size: Long get() = end - start + 1
    }

    private fun parseCidr(cidr: String): IpRange? {
        val parts = cidr.split("/")
        if (parts.size != 2) return null
        val ipStr = parts[0].trim()
        val prefix = parts[1].trim().toIntOrNull() ?: return null
        if (prefix !in 0..32) return null

        val ipNum = parseIpv4(ipStr) ?: return null
        val mask = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val netStart = ipNum and mask
        val netEnd = netStart or ((1L shl (32 - prefix)) - 1L)
        return IpRange(netStart, netEnd)
    }

    private fun parseIpv4(ip: String): Long? {
        val octets = ip.split(".")
        if (octets.size != 4) return null
        var result = 0L
        for (o in octets) {
            val num = o.toIntOrNull() ?: return null
            if (num !in 0..255) return null
            result = (result shl 8) or num.toLong()
        }
        return result
    }

    private fun formatIpv4(ip: Long): String {
        return "${(ip shr 24) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 8) and 0xFF}.${ip and 0xFF}"
    }

    private fun mergeRanges(ranges: List<IpRange>): List<IpRange> {
        val merged = mutableListOf<IpRange>()
        var current = ranges[0]

        for (i in 1 until ranges.size) {
            val next = ranges[i]
            if (next.start <= current.end + 1) {
                current = IpRange(current.start, maxOf(current.end, next.end))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun rangeToCidrs(start: Long, end: Long): List<String> {
        val result = mutableListOf<String>()
        var cur = start
        while (cur <= end) {
            // Find max power of 2 aligned with cur
            val trailingZeros = if (cur == 0L) 32 else cur.countTrailingZeroBits()
            var maxBlockSize = 1L shl min(trailingZeros, 32)

            while (cur + maxBlockSize - 1 > end) {
                maxBlockSize = maxBlockSize shr 1
            }

            val prefix = 32 - maxBlockSize.countTrailingZeroBits()
            result.add("${formatIpv4(cur)}/$prefix")
            cur += maxBlockSize
        }
        return result
    }
}
