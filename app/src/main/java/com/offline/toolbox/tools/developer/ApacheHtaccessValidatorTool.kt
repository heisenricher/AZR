package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class HtaccessDirective(val directive: String, val arguments: String, val lineNumber: Int)

data class ApacheHtaccessInput(
    val htaccessContent: String = """
        Options -Indexes
        ServerSignature Off

        RewriteEngine On
        RewriteBase /

        # Force HTTPS
        RewriteCond %{HTTPS} off
        RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]

        # Front Controller Pattern
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule ^(.*)$ index.php [QSA,L]

        # Security Headers
        Header set X-Content-Type-Options "nosniff"
        Header set X-Frame-Options "SAMEORIGIN"
        ErrorDocument 404 /404.html
    """.trimIndent()
)

data class ApacheHtaccessOutput(
    val isValid: Boolean,
    val totalDirectives: Int,
    val rewriteRuleCount: Int,
    val rewriteCondCount: Int,
    val hasHttpsRedirect: Boolean,
    val hasIndexesDisabled: Boolean,
    val detectedSecurityHeaders: List<String>,
    val warnings: List<String>,
    val formattedReport: String,
    val summary: String
)

class ApacheHtaccessValidatorTool : Tool<ApacheHtaccessInput, ApacheHtaccessOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "apache_htaccess_validator_tool",
        name = "Apache .htaccess Linter & Rewrite Validator",
        description = "Validate Apache .htaccess syntax, RewriteEngine rules, RewriteCond flags ([L], [R=301], [QSA]), directory indexing, and security headers.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("apache", "htaccess", "rewrite", "mod_rewrite", "redirect", "security", "sysadmin", "web server"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FileText"
    )

    override suspend fun execute(input: ApacheHtaccessInput): ToolResult<ApacheHtaccessOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.htaccessContent.trim()

        if (raw.isBlank()) {
            return ToolResult.Failure("Apache .htaccess content cannot be empty.")
        }

        val lines = raw.lines()
        val directives = mutableListOf<HtaccessDirective>()
        val warnings = mutableListOf<String>()
        val securityHeaders = mutableListOf<String>()

        var rewriteEngineActive = false
        var hasIndexesDisabled = false
        var hasHttpsRedirect = false
        var rewriteRuleCount = 0
        var rewriteCondCount = 0

        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = line.trim()

            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val spaceIdx = trimmed.indexOfFirst { it.isWhitespace() }
            val name = if (spaceIdx != -1) trimmed.substring(0, spaceIdx) else trimmed
            val args = if (spaceIdx != -1) trimmed.substring(spaceIdx + 1).trim() else ""

            directives.add(HtaccessDirective(name, args, lineNum))

            when (name.lowercase(Locale.US)) {
                "rewriteengine" -> {
                    rewriteEngineActive = args.equals("on", ignoreCase = true)
                }
                "rewritecond" -> {
                    rewriteCondCount++
                    if (args.contains("%{HTTPS}") && args.contains("off")) {
                        hasHttpsRedirect = true
                    }
                }
                "rewriterule" -> {
                    rewriteRuleCount++
                    if (!rewriteEngineActive) {
                        warnings.add("Line $lineNum: 'RewriteRule' defined without preceding 'RewriteEngine On'.")
                    }
                    // Validate flags e.g. [L,R=301]
                    val flagMatch = Regex("""\[([^\]]+)\]""").find(args)
                    if (flagMatch != null) {
                        val flags = flagMatch.groupValues[1].split(",").map { it.trim().uppercase(Locale.US) }
                        for (flag in flags) {
                            val baseFlag = flag.split("=").first()
                            if (baseFlag !in listOf("L", "R", "QSA", "NC", "F", "G", "C", "PT", "E", "S", "CO", "NS", "OR", "B")) {
                                warnings.add("Line $lineNum: Unknown or invalid RewriteRule flag '[$flag]'.")
                            }
                        }
                    }
                }
                "options" -> {
                    if (args.contains("-Indexes")) hasIndexesDisabled = true
                    if (args.contains("+Indexes")) {
                        warnings.add("Line $lineNum: Security Risk: '+Indexes' explicitly enables directory listing.")
                    }
                }
                "header" -> {
                    if (args.contains("X-Frame-Options") || args.contains("X-Content-Type-Options") || args.contains("Strict-Transport-Security") || args.contains("Content-Security-Policy")) {
                        securityHeaders.add(args.split(Regex("\\s+")).getOrNull(1) ?: args)
                    }
                }
            }
        }

        if (!hasIndexesDisabled) {
            warnings.add("Directory Security: 'Options -Indexes' is not configured (directory listings may be publicly visible).")
        }

        val isValid = warnings.none { it.contains("Unknown or invalid") || it.contains("without preceding") }

        val report = buildString {
            appendLine("APACHE .HTACCESS INSPECTION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Syntax Status:       ${if (isValid) "VALID" else "WARNINGS DETECTED"}")
            appendLine("Total Directives:    ${directives.size}")
            appendLine("Rewrite Rules:       $rewriteRuleCount rule(s)")
            appendLine("Rewrite Conditions:  $rewriteCondCount condition(s)")
            appendLine("RewriteEngine State: ${if (rewriteEngineActive) "ON" else "OFF/NOT_SET"}")
            appendLine("HTTPS Enforcement:   ${if (hasHttpsRedirect) "Active (%{HTTPS} off)" else "Not Detected"}")
            appendLine("Directory Browsing:  ${if (hasIndexesDisabled) "Protected (-Indexes)" else "Vulnerable"}")
            appendLine("Security Headers:    ${if (securityHeaders.isNotEmpty()) securityHeaders.joinToString(", ") else "None"}")
            appendLine("--------------------------------------------------")
            if (warnings.isNotEmpty()) {
                appendLine("WARNINGS & RECOMMENDATIONS:")
                warnings.forEach { appendLine("  ⚠ $it") }
            } else {
                appendLine("AUDIT VERDICT: Clean .htaccess configuration with good security posture.")
            }
        }

        return ToolResult.Success(
            data = ApacheHtaccessOutput(
                isValid = isValid,
                totalDirectives = directives.size,
                rewriteRuleCount = rewriteRuleCount,
                rewriteCondCount = rewriteCondCount,
                hasHttpsRedirect = hasHttpsRedirect,
                hasIndexesDisabled = hasIndexesDisabled,
                detectedSecurityHeaders = securityHeaders,
                warnings = warnings,
                formattedReport = report,
                summary = ".htaccess: ${if (isValid) "Valid" else "Warnings"} ($rewriteRuleCount rules, ${warnings.size} warnings)"
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Validated .htaccess ($rewriteRuleCount rules)"
        )
    }
}
