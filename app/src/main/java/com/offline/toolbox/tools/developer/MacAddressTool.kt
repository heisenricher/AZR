package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class MacNotation(val label: String) {
    COLON("Colon (00:1A:2B:3C:4D:5E)"),
    HYPHEN("Hyphen (00-1A-2B-3C-4D-5E)"),
    CISCO_DOT("Cisco Dot (001a.2b3c.4d5e)"),
    RAW_HEX("Raw Hex (001A2B3C4D5E)")
}

data class MacAddressInput(
    val macAddress: String = "00:1A:2B:3C:4D:5E",
    val targetNotation: MacNotation = MacNotation.COLON,
    val uppercase: Boolean = true
)

data class MacAddressOutput(
    val formattedMac: String,
    val colonFormat: String,
    val hyphenFormat: String,
    val ciscoFormat: String,
    val rawHex: String,
    val isMulticast: Boolean,
    val isLocallyAdministered: Boolean,
    val vendorOui: String?,
    val formattedReport: String,
    val summary: String
)

class MacAddressTool : Tool<MacAddressInput, MacAddressOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "mac_address_tool",
        name = "MAC Address Formatter & Inspector",
        description = "Normalize MAC addresses into standard formats, inspect OUI vendor hardware, and check multicast bits.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("mac", "address", "ethernet", "oui", "vendor", "hardware", "network", "cisco"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "SettingsEthernet"
    )

    override suspend fun execute(input: MacAddressInput): ToolResult<MacAddressOutput> {
        val startTime = System.currentTimeMillis()

        val rawClean = input.macAddress.replace(Regex("[^0-9a-fA-F]"), "")
        if (rawClean.length != 12) {
            return ToolResult.Failure(
                message = "Invalid MAC address: '${input.macAddress}'",
                userGuidance = "MAC addresses must contain exactly 12 hexadecimal characters (48 bits)."
            )
        }

        val hex = if (input.uppercase) rawClean.uppercase(Locale.US) else rawClean.lowercase(Locale.US)

        val pairs = (0 until 6).map { hex.substring(it * 2, it * 2 + 2) }
        val colon = pairs.joinToString(":")
        val hyphen = pairs.joinToString("-")
        val cisco = (0 until 3).joinToString(".") { hex.substring(it * 4, it * 4 + 4).lowercase(Locale.US) }

        val firstByte = pairs[0].toInt(16)
        val isMulticast = (firstByte and 0x01) == 1
        val isLocal = (firstByte and 0x02) == 2

        val ouiPrefix = hex.take(6).uppercase(Locale.US)
        val vendor = WELL_KNOWN_OUIS[ouiPrefix] ?: "Unknown Vendor (Private / Custom)"

        val formattedResult = when (input.targetNotation) {
            MacNotation.COLON -> colon
            MacNotation.HYPHEN -> hyphen
            MacNotation.CISCO_DOT -> cisco
            MacNotation.RAW_HEX -> hex
        }

        val report = buildString {
            appendLine("MAC ADDRESS ANALYSIS REPORT")
            appendLine("--------------------------------")
            appendLine("Formatted MAC:     $formattedResult")
            appendLine("Colon Notation:    $colon")
            appendLine("Hyphen Notation:   $hyphen")
            appendLine("Cisco Dot:         $cisco")
            appendLine("Raw Hex (48-bit):  $hex")
            appendLine("--------------------------------")
            appendLine("OUI Prefix:        $ouiPrefix")
            appendLine("Vendor / Device:   $vendor")
            appendLine("Transmission:      ${if (isMulticast) "Multicast / Broadcast (I/G bit = 1)" else "Unicast (I/G bit = 0)"}")
            appendLine("Administration:    ${if (isLocal) "Locally Administered (U/L bit = 1)" else "Universally Administered (IEEE OUI, U/L bit = 0)"}")
        }

        val summary = "$formattedResult ($vendor)"

        return ToolResult.Success(
            data = MacAddressOutput(
                formattedMac = formattedResult,
                colonFormat = colon,
                hyphenFormat = hyphen,
                ciscoFormat = cisco,
                rawHex = hex,
                isMulticast = isMulticast,
                isLocallyAdministered = isLocal,
                vendorOui = vendor,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    companion object {
        private val WELL_KNOWN_OUIS = mapOf(
            "001A2B" to "Ayecom Technology",
            "B827EB" to "Raspberry Pi Foundation",
            "D83ADD" to "Raspberry Pi Trading",
            "E45F01" to "Raspberry Pi Ltd",
            "00000C" to "Cisco Systems",
            "000142" to "Cisco Systems",
            "00180A" to "Cisco Systems",
            "F09FBF" to "Apple Inc.",
            "3C0754" to "Apple Inc.",
            "0017F2" to "Apple Inc.",
            "BC9FEF" to "Apple Inc.",
            "ACBC32" to "Apple Inc.",
            "A4C361" to "Apple Inc.",
            "00155D" to "Microsoft Corporation (Hyper-V)",
            "0050F2" to "Microsoft Corporation",
            "001A11" to "Google LLC",
            "F4F5D8" to "Google LLC",
            "546009" to "Google LLC",
            "00216A" to "Intel Corporate",
            "A44CC8" to "Intel Corporate",
            "001E67" to "Intel Corporate",
            "244BFE" to "Espressif Systems (ESP8266/ESP32)",
            "30AEA4" to "Espressif Systems (ESP32)",
            "A4E57C" to "Espressif Systems (ESP32)",
            "00163E" to "XenSource (Xen VM)",
            "005056" to "VMware Inc.",
            "000C29" to "VMware Inc.",
            "080027" to "Oracle VirtualBox",
            "506583" to "Samsung Electronics",
            "F8042E" to "Sony Corporation",
            "001422" to "Dell Inc."
        )
    }
}
