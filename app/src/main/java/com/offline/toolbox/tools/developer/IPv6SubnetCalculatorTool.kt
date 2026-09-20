package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.math.BigInteger
import java.util.Locale

data class IPv6SubnetInput(
    val ipv6Address: String = "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
    val prefixLength: Int = 64
)

data class IPv6SubnetOutput(
    val expandedAddress: String,
    val compressedAddress: String,
    val prefixLength: Int,
    val networkPrefix: String,
    val firstAddress: String,
    val lastAddress: String,
    val addressType: String,
    val isGlobalUnicast: Boolean,
    val isLinkLocal: Boolean,
    val isUniqueLocal: Boolean,
    val isMulticast: Boolean,
    val totalAddressesDisplay: String,
    val formattedReport: String,
    val summary: String
)

class IPv6SubnetCalculatorTool : Tool<IPv6SubnetInput, IPv6SubnetOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "ipv6_subnet_calculator",
        name = "IPv6 Subnet & Address Analyzer",
        description = "Expand, compress (RFC 5952), subnet, and classify IPv6 addresses with scope and capacity analysis.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("ipv6", "subnet", "cidr", "network", "ip", "address", "rfc5952", "prefix"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "SettingsEthernet"
    )

    override suspend fun execute(input: IPv6SubnetInput): ToolResult<IPv6SubnetOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.ipv6Address.trim()
        val prefix = input.prefixLength.coerceIn(1, 128)

        if (raw.isEmpty()) {
            return ToolResult.Failure("IPv6 address cannot be empty.")
        }

        val bigInt = parseIpv6(raw)
            ?: return ToolResult.Failure("Invalid IPv6 address format: '$raw'")

        val expanded = toExpandedString(bigInt)
        val compressed = toCompressedString(bigInt)

        // Subnet network mask calculation
        val shiftBits = 128 - prefix
        val mask = if (prefix == 128) {
            BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE)
        } else {
            BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE).shiftRight(shiftBits).shiftLeft(shiftBits)
        }

        val networkBigInt = bigInt.and(mask)
        val hostBitsCount = 128 - prefix
        val totalAddressesBigInt = BigInteger.TWO.pow(hostBitsCount)
        val broadcastBigInt = networkBigInt.add(totalAddressesBigInt.subtract(BigInteger.ONE))

        val networkPrefixStr = toCompressedString(networkBigInt) + "/$prefix"
        val firstAddressStr = toCompressedString(networkBigInt)
        val lastAddressStr = toCompressedString(broadcastBigInt)

        // Address classification
        val isGlobalUnicast = (bigInt.shiftRight(125).toInt() == 1) // 2000::/3
        val isLinkLocal = (bigInt.shiftRight(118).toInt() == 0x3FA) // fe80::/10 (fe80 to febf)
        val isUniqueLocal = (bigInt.shiftRight(121).toInt() == 0x7E || bigInt.shiftRight(121).toInt() == 0x7F) // fc00::/7
        val isMulticast = (bigInt.shiftRight(120).toInt() == 0xFF) // ff00::/8
        val isLoopback = (bigInt == BigInteger.ONE)
        val isUnspecified = (bigInt == BigInteger.ZERO)

        val addressType = when {
            isLoopback -> "Loopback (::1)"
            isUnspecified -> "Unspecified (::)"
            isLinkLocal -> "Link-Local Unicast (fe80::/10)"
            isUniqueLocal -> "Unique Local Unicast (fc00::/7)"
            isMulticast -> "Multicast (ff00::/8)"
            isGlobalUnicast -> "Global Unicast (2000::/3)"
            else -> "Reserved / Other Special Purpose"
        }

        val totalAddressesDisplay = when {
            hostBitsCount == 0 -> "1"
            hostBitsCount <= 32 -> totalAddressesBigInt.toString()
            hostBitsCount == 64 -> "18,446,744,073,709,551,616 (2^64 standard subnet)"
            else -> "2^$hostBitsCount (~10^${(hostBitsCount * 0.30103).toInt()})"
        }

        val report = buildString {
            appendLine("IPV6 SUBNET & ADDRESS REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Input Address:      $raw")
            appendLine("CIDR Prefix:        /$prefix")
            appendLine("Expanded (Full):    $expanded")
            appendLine("RFC 5952 Canonical: $compressed")
            appendLine("Address Scope:      $addressType")
            appendLine("Network Prefix:     $networkPrefixStr")
            appendLine("First Address:      $firstAddressStr")
            appendLine("Last Address:       $lastAddressStr")
            appendLine("Host Capacity:      $totalAddressesDisplay")
        }

        val summary = "$compressed/$prefix ($addressType)"

        return ToolResult.Success(
            data = IPv6SubnetOutput(
                expandedAddress = expanded,
                compressedAddress = compressed,
                prefixLength = prefix,
                networkPrefix = networkPrefixStr,
                firstAddress = firstAddressStr,
                lastAddress = lastAddressStr,
                addressType = addressType,
                isGlobalUnicast = isGlobalUnicast,
                isLinkLocal = isLinkLocal,
                isUniqueLocal = isUniqueLocal,
                isMulticast = isMulticast,
                totalAddressesDisplay = totalAddressesDisplay,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun parseIpv6(str: String): BigInteger? {
        try {
            var s = str.lowercase(Locale.ROOT)
            val doubleColonCount = s.windowed(2).count { it == "::" }
            if (doubleColonCount > 1) return null

            val parts: List<String>
            if (s.contains("::")) {
                val halves = s.split("::")
                val left = if (halves[0].isEmpty()) emptyList() else halves[0].split(":")
                val right = if (halves.size > 1 && halves[1].isNotEmpty()) halves[1].split(":") else emptyList()
                val missing = 8 - (left.size + right.size)
                if (missing < 0) return null
                val full = mutableListOf<String>()
                full.addAll(left)
                repeat(missing) { full.add("0") }
                full.addAll(right)
                parts = full
            } else {
                parts = s.split(":")
                if (parts.size != 8) return null
            }

            if (parts.size != 8) return null

            var result = BigInteger.ZERO
            for (p in parts) {
                if (p.length > 4) return null
                val v = p.toIntOrNull(16) ?: return null
                result = result.shiftLeft(16).add(BigInteger.valueOf(v.toLong()))
            }
            return result
        } catch (_: Exception) {
            return null
        }
    }

    private fun toExpandedString(bi: BigInteger): String {
        val hextets = IntArray(8)
        var temp = bi
        for (i in 7 downTo 0) {
            hextets[i] = temp.and(BigInteger.valueOf(0xFFFF)).toInt()
            temp = temp.shiftRight(16)
        }
        return hextets.joinToString(":") { String.format("%04x", it) }
    }

    private fun toCompressedString(bi: BigInteger): String {
        val hextets = IntArray(8)
        var temp = bi
        for (i in 7 downTo 0) {
            hextets[i] = temp.and(BigInteger.valueOf(0xFFFF)).toInt()
            temp = temp.shiftRight(16)
        }

        // Find longest sequence of consecutive zeroes
        var maxZeroStart = -1
        var maxZeroLen = 0
        var curZeroStart = -1
        var curZeroLen = 0

        for (i in 0 until 8) {
            if (hextets[i] == 0) {
                if (curZeroStart == -1) curZeroStart = i
                curZeroLen++
                if (curZeroLen > maxZeroLen) {
                    maxZeroLen = curZeroLen
                    maxZeroStart = curZeroStart
                }
            } else {
                curZeroStart = -1
                curZeroLen = 0
            }
        }

        if (maxZeroLen <= 1) {
            return hextets.joinToString(":") { Integer.toHexString(it) }
        }

        val sb = StringBuilder()
        var i = 0
        while (i < 8) {
            if (i == maxZeroStart) {
                sb.append("::")
                i += maxZeroLen
            } else {
                if (sb.isNotEmpty() && !sb.endsWith("::")) {
                    sb.append(":")
                }
                sb.append(Integer.toHexString(hextets[i]))
                i++
            }
        }
        return sb.toString()
    }
}
