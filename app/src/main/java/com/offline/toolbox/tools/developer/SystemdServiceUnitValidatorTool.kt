package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class SystemdDirective(val section: String, val key: String, val value: String, val lineNumber: Int)

data class SystemdUnitInput(
    val unitContent: String = """
        [Unit]
        Description=Offline Internal Microservice
        After=network.target local-fs.target
        Documentation=https://docs.offline.internal

        [Service]
        Type=notify
        ExecStart=/usr/bin/offline-service --config /etc/offline.conf
        ExecReload=/bin/kill -HUP ${'$'}MAINPID
        Restart=always
        RestartSec=5s
        User=offlineapp
        Group=offlineapp
        WorkingDirectory=/var/lib/offline
        NoNewPrivileges=true
        ProtectSystem=strict
        ProtectHome=yes
        PrivateTmp=yes

        [Install]
        WantedBy=multi-user.target
    """.trimIndent()
)

data class SystemdUnitOutput(
    val isValid: Boolean,
    val totalDirectives: Int,
    val sectionsFound: List<String>,
    val serviceType: String,
    val restartPolicy: String,
    val runsAsRoot: Boolean,
    val securityScore: Int,
    val securityFeatures: List<String>,
    val issues: List<String>,
    val formattedReport: String,
    val summary: String
)

class SystemdServiceUnitValidatorTool : Tool<SystemdUnitInput, SystemdUnitOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "systemd_service_unit_validator_tool",
        name = "Systemd Service Unit Validator & Security Auditor",
        description = "Validate Linux systemd unit file syntax ([Unit], [Service], [Install]), ExecStart commands, restart policies, and audit process sandboxing directives.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("systemd", "linux", "service", "sysadmin", "daemon", "security", "sandboxing", "devops", "unit"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Terminal"
    )

    override suspend fun execute(input: SystemdUnitInput): ToolResult<SystemdUnitOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.unitContent.trim()

        if (raw.isBlank()) {
            return ToolResult.Failure("Systemd unit configuration cannot be empty.")
        }

        val lines = raw.lines()
        val directives = mutableListOf<SystemdDirective>()
        val sections = mutableSetOf<String>()
        val issues = mutableListOf<String>()
        val securityFeatures = mutableListOf<String>()

        var currentSection: String? = null

        for ((idx, rawLine) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = rawLine.trim()

            // Skip comments and blank lines
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue
            }

            // Section header: [SectionName]
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                val secName = trimmed.substring(1, trimmed.length - 1).trim()
                if (secName.isEmpty()) {
                    issues.add("Line $lineNum: Empty section header '[]'")
                } else {
                    currentSection = secName
                    sections.add(secName)
                }
                continue
            }

            if (currentSection == null) {
                issues.add("Line $lineNum: Directive '$trimmed' found outside of any [Section] header.")
                continue
            }

            val eqIdx = trimmed.indexOf('=')
            if (eqIdx == -1) {
                issues.add("Line $lineNum: Syntax error. Expected 'Key=Value' directive, got '$trimmed'.")
                continue
            }

            val key = trimmed.substring(0, eqIdx).trim()
            val value = trimmed.substring(eqIdx + 1).trim()
            directives.add(SystemdDirective(currentSection, key, value, lineNum))
        }

        // Validate structure
        if (!sections.contains("Service")) {
            issues.add("Missing required [Service] section in service unit file.")
        }

        val execStart = directives.firstOrNull { it.section == "Service" && it.key == "ExecStart" }
        if (execStart == null && sections.contains("Service")) {
            issues.add("Missing mandatory 'ExecStart=' directive in [Service] section.")
        } else if (execStart != null) {
            val execCmd = execStart.value.split(Regex("\\s+")).firstOrNull() ?: ""
            if (!execCmd.startsWith("/") && !execCmd.startsWith("@") && !execCmd.startsWith("-")) {
                issues.add("ExecStart binary path ('$execCmd') should be an absolute path starting with '/'.")
            }
        }

        val serviceType = directives.firstOrNull { it.section == "Service" && it.key == "Type" }?.value ?: "simple"
        val restartPolicy = directives.firstOrNull { it.section == "Service" && it.key == "Restart" }?.value ?: "no"
        val user = directives.firstOrNull { it.section == "Service" && it.key == "User" }?.value
        val runsAsRoot = user == null || user.equals("root", ignoreCase = true) || user == "0"

        var securityScore = 40 // base score

        if (!runsAsRoot) {
            securityScore += 20
            securityFeatures.add("Runs as unprivileged user: '$user'")
        } else {
            issues.add("Security Warning: Service runs as 'root' or User= is omitted.")
        }

        // Sandboxing checks
        fun checkDirective(key: String, expectedVal: String, desc: String, points: Int) {
            val d = directives.firstOrNull { it.section == "Service" && it.key.equals(key, ignoreCase = true) }
            if (d != null && d.value.equals(expectedVal, ignoreCase = true)) {
                securityScore += points
                securityFeatures.add(desc)
            }
        }

        checkDirective("NoNewPrivileges", "true", "NoNewPrivileges=true (prevents SUID privilege escalation)", 10)
        checkDirective("ProtectSystem", "strict", "ProtectSystem=strict (read-only /usr, /boot, /etc)", 10)
        checkDirective("ProtectHome", "yes", "ProtectHome=yes (inaccessible /home, /root, /run/user)", 10)
        checkDirective("PrivateTmp", "yes", "PrivateTmp=yes (isolated /tmp and /var/tmp namespace)", 10)

        securityScore = securityScore.coerceIn(0, 100)
        val isValid = issues.none { it.contains("Syntax error") || it.contains("Missing mandatory") }

        val report = buildString {
            appendLine("SYSTEMD SERVICE UNIT INSPECTION & SECURITY AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Validation Status:   ${if (isValid) "VALID UNIT FILE" else "ERRORS DETECTED"}")
            appendLine("Total Directives:    ${directives.size}")
            appendLine("Sections Identified: ${sections.joinToString(", ") { "[$it]" }}")
            appendLine("Service Type:        $serviceType")
            appendLine("Restart Policy:      $restartPolicy")
            appendLine("Execution User:      ${user ?: "root (default)"}")
            appendLine("Security Score:      $securityScore / 100")
            appendLine("--------------------------------------------------")
            if (securityFeatures.isNotEmpty()) {
                appendLine("SECURITY SANDBOXING DIRECTIVES FOUND:")
                securityFeatures.forEach { appendLine("  ✓ $it") }
                appendLine("--------------------------------------------------")
            }
            if (issues.isNotEmpty()) {
                appendLine("WARNINGS & ISSUES DETECTED:")
                issues.forEach { appendLine("  ⚠ $it") }
                appendLine("--------------------------------------------------")
            }
            appendLine("SYSTEMD CONFIGURATION DIRECTIVES:")
            sections.forEach { sec ->
                appendLine("[$sec]")
                directives.filter { it.section == sec }.forEach { d ->
                    appendLine("  ${d.key}=${d.value}")
                }
            }
        }

        return ToolResult.Success(
            data = SystemdUnitOutput(
                isValid = isValid,
                totalDirectives = directives.size,
                sectionsFound = sections.toList(),
                serviceType = serviceType,
                restartPolicy = restartPolicy,
                runsAsRoot = runsAsRoot,
                securityScore = securityScore,
                securityFeatures = securityFeatures,
                issues = issues,
                formattedReport = report,
                summary = "Systemd: ${if (isValid) "Valid" else "Issues found"} (Score: $securityScore/100, ${sections.size} sections)"
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Validated systemd service (${directives.size} directives)"
        )
    }
}
