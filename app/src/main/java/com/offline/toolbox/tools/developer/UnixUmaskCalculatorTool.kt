package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class UnixUmaskInput(
    val umaskOctalString: String = "0022"
)

data class UnixUmaskOutput(
    val umaskOctal: String,
    val umaskSymbolic: String,
    val fileOctal: String,
    val fileSymbolic: String,
    val directoryOctal: String,
    val directorySymbolic: String,
    val securityLevel: String,
    val securityAuditNotes: List<String>,
    val formattedReport: String,
    val summary: String
)

class UnixUmaskCalculatorTool : Tool<UnixUmaskInput, UnixUmaskOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "unix_umask_calculator_tool",
        name = "POSIX Umask & File Permissions Calculator",
        description = "Calculate effective file and directory permissions from POSIX octal umasks with security exposure audits.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("umask", "chmod", "permissions", "posix", "unix", "linux", "security", "octal", "sysadmin"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Shield"
    )

    override suspend fun execute(input: UnixUmaskInput): ToolResult<UnixUmaskOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.umaskOctalString.trim().removePrefix("0o")

        val clean = if (raw.length > 3) raw.takeLast(3) else raw
        val umaskInt = clean.toIntOrNull(8)

        if (umaskInt == null || umaskInt !in 0..0x1FF) {
            return ToolResult.Failure("Invalid octal umask '$raw'. Expected 3 or 4 octal digits (e.g. '022', '0022', '077').")
        }

        // Base file: 0666 (rw-rw-rw-)
        // Base dir:  0777 (rwxrwxrwx)
        val fileEffective = 0x1B6 and umaskInt.inv() // 0666 in octal is 0x1B6 (438 decimal)
        val dirEffective = 0x1FF and umaskInt.inv()  // 0777 in octal is 0x1FF (511 decimal)

        val umaskOctalStr = String.format(Locale.US, "0%03o", umaskInt)
        val fileOctalStr = String.format(Locale.US, "0%03o", fileEffective)
        val dirOctalStr = String.format(Locale.US, "0%03o", dirEffective)

        val fileSym = toSymbolic(fileEffective)
        val dirSym = toSymbolic(dirEffective)
        val umaskSym = toSymbolic(umaskInt)

        val notes = mutableListOf<String>()
        var secLevel = "Standard User Default"

        // Check world permissions (other)
        val otherWrite = (fileEffective and 0x2) != 0
        val otherRead = (fileEffective and 0x4) != 0

        if (otherWrite) {
            secLevel = "CRITICAL RISK (World-Writable)"
            notes.add("Files created with this umask can be modified or overwritten by any local user.")
        } else if (otherRead) {
            secLevel = "Standard (World-Readable)"
            notes.add("Files created are readable by all users on the operating system.")
        } else {
            val groupRead = (fileEffective and 0x20) != 0
            if (groupRead) {
                secLevel = "Hardened Server (Group Only)"
                notes.add("Others have zero permissions; only owner and group can read/access.")
            } else {
                secLevel = "Maximum Privacy (Owner Only)"
                notes.add("Strict isolation: group and other users are denied all permissions.")
            }
        }

        val report = buildString {
            appendLine("POSIX UMASK & FILE PERMISSIONS REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Umask Value:          $umaskOctalStr (Mask: $umaskSym)")
            appendLine("--------------------------------------------------")
            appendLine("EFFECTIVE FILE PERMISSIONS (Base 0666 & ~umask):")
            appendLine(" • Octal:             $fileOctalStr")
            appendLine(" • Symbolic:          $fileSym")
            appendLine("--------------------------------------------------")
            appendLine("EFFECTIVE DIRECTORY PERMISSIONS (Base 0777 & ~umask):")
            appendLine(" • Octal:             $dirOctalStr")
            appendLine(" • Symbolic:          $dirSym")
            appendLine("--------------------------------------------------")
            appendLine("SECURITY EXPOSURE AUDIT:")
            appendLine(" • Posture:           $secLevel")
            notes.forEach { appendLine(" • $it") }
        }

        val output = UnixUmaskOutput(
            umaskOctal = umaskOctalStr,
            umaskSymbolic = umaskSym,
            fileOctal = fileOctalStr,
            fileSymbolic = fileSym,
            directoryOctal = dirOctalStr,
            directorySymbolic = dirSym,
            securityLevel = secLevel,
            securityAuditNotes = notes,
            formattedReport = report,
            summary = "Umask $umaskOctalStr → File: $fileOctalStr ($fileSym), Dir: $dirOctalStr ($dirSym)"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Calculated effective permissions for umask $umaskOctalStr"
        )
    }

    private fun toSymbolic(perm: Int): String {
        val r = if ((perm and 0x4) != 0) "r" else "-"
        val w = if ((perm and 0x2) != 0) "w" else "-"
        val x = if ((perm and 0x1) != 0) "x" else "-"

        val gr = if ((perm and 0x20) != 0) "r" else "-"
        val gw = if ((perm and 0x10) != 0) "w" else "-"
        val gx = if ((perm and 0x8) != 0) "x" else "-"

        val ur = if ((perm and 0x100) != 0) "r" else "-"
        val uw = if ((perm and 0x80) != 0) "w" else "-"
        val ux = if ((perm and 0x40) != 0) "x" else "-"

        return "$ur$uw$ux$gr$gw$gx$r$w$x"
    }
}
