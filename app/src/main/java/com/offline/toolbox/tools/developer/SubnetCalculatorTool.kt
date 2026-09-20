package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class SubnetInput(
    val ipAddress: String = "192.168.1.1",
    val cidrPrefix: Int = 24
)

data class SubnetOutput(
    val networkAddress: String,
    val broadcastAddress: String,
    val subnetMask: String,
    val wildcardMask: String,
    val firstUsableIp: String,
    val lastUsableIp: String,
    val totalHosts: Long,
    val usableHosts: Long,
    val ipClass: String,
    val isPrivateIp: Boolean,
    val binarySubnetMask: String,
    val formattedReport: String,
    val summary: String
)

class SubnetCalculatorTool : Tool<SubnetInput, SubnetOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "subnet_calculator_tool",
        name = "IPv4 Subnet & CIDR Calculator",
        description = "Calculate network ranges, broadcast IPs, usable host capacity, wildcard masks, and CIDR subnets.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("subnet", "cidr", "ip", "ipv4", "network", "mask", "broadcast", "hosts"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "SettingsEthernet"
    )

    override suspend fun execute(input: SubnetInput): ToolResult<SubnetOutput> {
        val startTime = System.currentTimeMillis()

        val cleanIp = input.ipAddress.trim()
        val cidr = input.cidrPrefix.coerceIn(0, 32)

        val octets = cleanIp.split('.').mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) {
            return ToolResult.Failure(
                message = "Invalid IPv4 address: '$cleanIp'",
                userGuidance = "Enter a valid IPv4 address in dot-decimal format (e.g. 192.168.1.1)."
            )
        }

        val ipLong = (octets[0].toLong() shl 24) or
                     (octets[1].toLong() shl 16) or
                     (octets[2].toLong() shl 8) or
                     octets[3].toLong()

        val maskLong = if (cidr == 0) 0L else (0xFFFFFFFFL shl (32 - cidr)) and 0xFFFFFFFFL
        val wildcardLong = maskLong.inv() and 0xFFFFFFFFL

        val networkLong = ipLong and maskLong
        val broadcastLong = networkLong or wildcardLong

        val totalHosts = 1L shl (32 - cidr)
        val usableHosts = when (cidr) {
            32 -> 1L
            31 -> 2L
            else -> (totalHosts - 2).coerceAtLeast(0)
        }

        val firstUsableLong = if (cidr >= 31) networkLong else networkLong + 1
        val lastUsableLong = if (cidr >= 31) broadcastLong else broadcastLong - 1

        val firstOctet = octets[0]
        val ipClass = when (firstOctet) {
            in 1..126 -> "Class A"
            127 -> "Class A (Loopback)"
            in 128..191 -> "Class B"
            in 192..223 -> "Class C"
            in 224..239 -> "Class D (Multicast)"
            in 240..255 -> "Class E (Experimental)"
            else -> "Unknown"
        }

        val isPrivate = (firstOctet == 10) ||
                        (firstOctet == 172 && octets[1] in 16..31) ||
                        (firstOctet == 192 && octets[1] == 168)

        val netStr = longToIp(networkLong)
        val bcastStr = longToIp(broadcastLong)
        val maskStr = longToIp(maskLong)
        val wildStr = longToIp(wildcardLong)
        val firstStr = longToIp(firstUsableLong)
        val lastStr = longToIp(lastUsableLong)
        val binaryMask = formatBinary(maskLong)

        val report = buildString {
            appendLine("SUBNET / CIDR NETWORK REPORT")
            appendLine("--------------------------------")
            appendLine("IP / Prefix:       $cleanIp / $cidr")
            appendLine("Network Address:   $netStr")
            appendLine("Broadcast Address: $bcastStr")
            appendLine("Subnet Mask:       $maskStr")
            appendLine("Wildcard Mask:     $wildStr")
            appendLine("Usable Host Range: $firstStr - $lastStr")
            appendLine("Total Addresses:   $totalHosts")
            appendLine("Usable Host Count: $usableHosts")
            appendLine("Address Class:     $ipClass (${if (isPrivate) "RFC 1918 Private" else "Public"})")
            appendLine("Binary Mask:       $binaryMask")
        }

        val summary = "$netStr/$cidr ($usableHosts usable hosts)"

        return ToolResult.Success(
            data = SubnetOutput(
                networkAddress = netStr,
                broadcastAddress = bcastStr,
                subnetMask = maskStr,
                wildcardMask = wildStr,
                firstUsableIp = firstStr,
                lastUsableIp = lastStr,
                totalHosts = totalHosts,
                usableHosts = usableHosts,
                ipClass = ipClass,
                isPrivateIp = isPrivate,
                binarySubnetMask = binaryMask,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun longToIp(v: Long): String {
        return "${(v ushr 24) and 0xFF}.${(v ushr 16) and 0xFF}.${(v ushr 8) and 0xFF}.${v and 0xFF}"
    }

    private fun formatBinary(v: Long): String {
        val s = (0..3).map { i ->
            val byteVal = ((v ushr (24 - i * 8)) and 0xFF).toInt()
            "%8s".format(Locale.US, Integer.toBinaryString(byteVal)).replace(' ', '0')
        }
        return s.joinToString(".")
    }
}
