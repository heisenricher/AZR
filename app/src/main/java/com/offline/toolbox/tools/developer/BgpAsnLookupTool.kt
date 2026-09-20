package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class BgpAsnInput(
    val asnQuery: String = "AS15169"
)

data class BgpAsnOutput(
    val asNumber: Long,
    val asPlain: String,
    val asDot: String,
    val is4ByteAsn: Boolean,
    val rangeClassification: String,
    val isPrivateOrReserved: Boolean,
    val matchedOrgName: String?,
    val formattedReport: String,
    val summary: String
)

class BgpAsnLookupTool : Tool<BgpAsnInput, BgpAsnOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "bgp_asn_lookup_tool",
        name = "BGP Autonomous System Number (ASN) Directory",
        description = "Lookup offline BGP Autonomous System Numbers, convert between ASPLAIN and ASDOT notations, and classify public/private ranges.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("asn", "bgp", "routing", "autonomous system", "asdot", "asplain", "network", "isp", "transit"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Public"
    )

    private val wellKnownAsns = mapOf(
        15169L to "Google LLC",
        714L to "Apple Inc.",
        32934L to "Meta Platforms (Facebook)",
        16509L to "Amazon.com (AWS)",
        8075L to "Microsoft Corporation",
        13335L to "Cloudflare, Inc.",
        3356L to "Lumen Technologies (Level 3)",
        2914L to "NTT America, Inc.",
        174L to "Cogent Communications",
        7018L to "AT&T Services, Inc.",
        1299L to "Arelion (Telia Carrier)",
        6939L to "Hurricane Electric LLC",
        20940L to "Akamai Technologies",
        1200L to "Amsterdam Internet Exchange (AMS-IX)",
        209L to "CenturyLink (Qwest)"
    )

    override suspend fun execute(input: BgpAsnInput): ToolResult<BgpAsnOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.asnQuery.trim().uppercase().removePrefix("AS")

        if (raw.isEmpty()) {
            return ToolResult.Failure("ASN query cannot be empty.")
        }

        val asNum: Long = if (raw.contains(".")) {
            // ASDOT notation: High.Low -> High * 65536 + Low
            val parts = raw.split(".")
            if (parts.size != 2) return ToolResult.Failure("Invalid ASDOT notation: '$raw'. Expected format e.g. 1.25")
            val hi = parts[0].toLongOrNull() ?: return ToolResult.Failure("Invalid high 16-bit word.")
            val lo = parts[1].toLongOrNull() ?: return ToolResult.Failure("Invalid low 16-bit word.")
            if (hi !in 0..65535 || lo !in 0..65535) {
                return ToolResult.Failure("ASDOT components must be between 0 and 65535.")
            }
            (hi shl 16) or lo
        } else {
            raw.toLongOrNull() ?: return ToolResult.Failure("Invalid ASN number: '$raw'. Must be numeric integer or ASDOT notation.")
        }

        if (asNum < 0 || asNum > 4294967295L) {
            return ToolResult.Failure("ASN must be in 32-bit unsigned range 0 to 4,294,967,295.")
        }

        val asPlain = "AS$asNum"
        val asDot = "${asNum shr 16}.${asNum and 0xFFFFL}"
        val is4Byte = asNum > 65535L

        // Range classification (RFC 6996 / RFC 5398)
        val isPrivate: Boolean
        val classification: String

        when {
            asNum == 0L -> {
                classification = "RFC 7607 Reserved (AS 0)"
                isPrivate = true
            }
            asNum in 1L..64495L -> {
                classification = "16-Bit Public Internet ASN"
                isPrivate = false
            }
            asNum in 64496L..64511L -> {
                classification = "RFC 5398 16-Bit Documentation & Examples"
                isPrivate = true
            }
            asNum in 64512L..65534L -> {
                classification = "RFC 6996 16-Bit Private Use ASN"
                isPrivate = true
            }
            asNum == 65535L -> {
                classification = "RFC 7300 16-Bit Last Reserved ASN"
                isPrivate = true
            }
            asNum in 65536L..65551L -> {
                classification = "RFC 5398 32-Bit Documentation ASN"
                isPrivate = true
            }
            asNum in 4200000000L..4294967294L -> {
                classification = "RFC 6996 32-Bit Private Use ASN"
                isPrivate = true
            }
            asNum == 4294967295L -> {
                classification = "RFC 7300 32-Bit Max Reserved ASN"
                isPrivate = true
            }
            else -> {
                classification = "32-Bit 4-Byte Public Internet ASN"
                isPrivate = false
            }
        }

        val org = wellKnownAsns[asNum]

        val report = buildString {
            appendLine("BGP AUTONOMOUS SYSTEM NUMBER (ASN) AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("AS Number (Dec):     $asNum")
            appendLine("ASPLAIN Notation:    $asPlain")
            appendLine("ASDOT Notation:      $asDot")
            appendLine("ASN Format:          ${if (is4Byte) "4-Byte (32-Bit)" else "2-Byte (16-Bit Legacy)"}")
            appendLine("Classification:      $classification")
            appendLine("Routability:         ${if (isPrivate) "Private / Non-Routable on Global DFZ" else "Globally Routable"}")
            if (org != null) {
                appendLine()
                appendLine("IDENTIFIED NETWORK ENTITY:")
                appendLine("• Organization:      $org")
            }
        }

        val summary = "$asPlain ($asDot) — ${org ?: classification}"

        return ToolResult.Success(
            data = BgpAsnOutput(
                asNumber = asNum,
                asPlain = asPlain,
                asDot = asDot,
                is4ByteAsn = is4Byte,
                rangeClassification = classification,
                isPrivateOrReserved = isPrivate,
                matchedOrgName = org,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
