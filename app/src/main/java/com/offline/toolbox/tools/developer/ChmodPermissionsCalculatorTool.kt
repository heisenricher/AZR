package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class ChmodInput(
    val octalOrSymbolic: String = "755"
)

data class ChmodOutput(
    val octal3Digit: String,
    val octal4Digit: String,
    val symbolicNotation: String,
    val ownerRead: Boolean,
    val ownerWrite: Boolean,
    val ownerExecute: Boolean,
    val groupRead: Boolean,
    val groupWrite: Boolean,
    val groupExecute: Boolean,
    val othersRead: Boolean,
    val othersWrite: Boolean,
    val othersExecute: Boolean,
    val suid: Boolean,
    val sgid: Boolean,
    val stickyBit: Boolean,
    val commandExamples: List<String>,
    val formattedReport: String,
    val summary: String
)

class ChmodPermissionsCalculatorTool : Tool<ChmodInput, ChmodOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "chmod_permissions_calculator_tool",
        name = "Chmod Unix Permissions Calculator",
        description = "Bi-directional conversion between octal permissions (755, 644) and symbolic notation (-rwxr-xr-x) with SUID/SGID/Sticky bits.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("chmod", "permissions", "unix", "linux", "octal", "symbolic", "file", "security", "rwx", "suid"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Security"
    )

    override suspend fun execute(input: ChmodInput): ToolResult<ChmodOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.octalOrSymbolic.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input cannot be empty. Enter octal (e.g. 755) or symbolic notation (e.g. rwxr-xr-x).")
        }

        val (special, u, g, o) = parsePermissions(raw)
            ?: return ToolResult.Failure("Invalid chmod format '$raw'. Enter 3-4 octal digits (e.g. 0755, 644) or 9-10 symbolic characters (e.g. -rwxr-xr-x).")

        val uR = (u and 4) != 0
        val uW = (u and 2) != 0
        val uX = (u and 1) != 0

        val gR = (g and 4) != 0
        val gW = (g and 2) != 0
        val gX = (g and 1) != 0

        val oR = (o and 4) != 0
        val oW = (o and 2) != 0
        val oX = (o and 1) != 0

        val suid = (special and 4) != 0
        val sgid = (special and 2) != 0
        val sticky = (special and 1) != 0

        val symU = buildTriad(uR, uW, uX, suid, 's', 'S')
        val symG = buildTriad(gR, gW, gX, sgid, 's', 'S')
        val symO = buildTriad(oR, oW, oX, sticky, 't', 'T')
        val symbolic = "-$symU$symG$symO"

        val octal3 = "$u$g$o"
        val octal4 = "$special$u$g$o"

        val cmds = listOf(
            "chmod $octal3 filename",
            "chmod u=${symU.trim('-')},g=${symG.trim('-')},o=${symO.trim('-')} filename",
            "chmod -R $octal3 directory/"
        )

        val report = buildString {
            appendLine("UNIX FILE PERMISSIONS DECOMPOSITION")
            appendLine("--------------------------------------------------")
            appendLine("Octal Formats:    $octal3 (Standard) | $octal4 (Full 4-digit)")
            appendLine("Symbolic String:  $symbolic")
            appendLine()
            appendLine("PERMISSIONS MATRIX")
            appendLine("• User (Owner):   r: $uR | w: $uW | x: $uX  (Digit: $u)")
            appendLine("• Group:          r: $gR | w: $gW | x: $gX  (Digit: $g)")
            appendLine("• Others (World): r: $oR | w: $oW | x: $oX  (Digit: $o)")
            if (special > 0) {
                appendLine()
                appendLine("SPECIAL BITS:")
                appendLine("• SUID:   ${if (suid) "Enabled (4000)" else "No"}")
                appendLine("• SGID:   ${if (sgid) "Enabled (2000)" else "No"}")
                appendLine("• Sticky: ${if (sticky) "Enabled (1000)" else "No"}")
            }
            appendLine()
            appendLine("SHELL EXAMPLES")
            cmds.forEach { appendLine("  $ $it") }
        }

        val summary = "Chmod $octal3 ($symbolic)"

        return ToolResult.Success(
            data = ChmodOutput(
                octal3Digit = octal3,
                octal4Digit = octal4,
                symbolicNotation = symbolic,
                ownerRead = uR,
                ownerWrite = uW,
                ownerExecute = uX,
                groupRead = gR,
                groupWrite = gW,
                groupExecute = gX,
                othersRead = oR,
                othersWrite = oW,
                othersExecute = oX,
                suid = suid,
                sgid = sgid,
                stickyBit = sticky,
                commandExamples = cmds,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun buildTriad(r: Boolean, w: Boolean, x: Boolean, special: Boolean, specCharOn: Char, specCharOff: Char): String {
        val sb = StringBuilder()
        sb.append(if (r) 'r' else '-')
        sb.append(if (w) 'w' else '-')
        sb.append(
            if (special) {
                if (x) specCharOn else specCharOff
            } else {
                if (x) 'x' else '-'
            }
        )
        return sb.toString()
    }

    private fun parsePermissions(raw: String): Quadruple<Int, Int, Int, Int>? {
        // Octal check
        if (raw.all { it in '0'..'7' }) {
            return when (raw.length) {
                3 -> Quadruple(0, raw[0] - '0', raw[1] - '0', raw[2] - '0')
                4 -> Quadruple(raw[0] - '0', raw[1] - '0', raw[2] - '0', raw[3] - '0')
                else -> null
            }
        }

        // Symbolic check (e.g. rwxr-xr-x or -rwxr-xr-x)
        val s = if (raw.length == 10 && (raw[0] == '-' || raw[0] == 'd' || raw[0] == 'l')) {
            raw.substring(1)
        } else raw

        if (s.length != 9) return null

        fun parseTriad(triad: String): Pair<Int, Int>? {
            if (triad.length != 3) return null
            var perm = 0
            var spec = 0
            if (triad[0] == 'r') perm += 4 else if (triad[0] != '-') return null
            if (triad[1] == 'w') perm += 2 else if (triad[1] != '-') return null
            when (triad[2]) {
                'x' -> perm += 1
                's' -> { perm += 1; spec = 1 }
                'S' -> { spec = 1 }
                't' -> { perm += 1; spec = 1 }
                'T' -> { spec = 1 }
                '-' -> {}
                else -> return null
            }
            return Pair(perm, spec)
        }

        val uPair = parseTriad(s.substring(0, 3)) ?: return null
        val gPair = parseTriad(s.substring(3, 6)) ?: return null
        val oPair = parseTriad(s.substring(6, 9)) ?: return null

        val special = (uPair.second * 4) + (gPair.second * 2) + oPair.second
        return Quadruple(special, uPair.first, gPair.first, oPair.first)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
