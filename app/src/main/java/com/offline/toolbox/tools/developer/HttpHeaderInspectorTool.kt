package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class SecurityHeaderAudit(
    val headerName: String,
    val present: Boolean,
    val currentValue: String,
    val status: String,
    val recommendation: String
)

data class HttpHeaderInput(
    val rawHeaders: String = """
        HTTP/1.1 200 OK
        Content-Type: text/html; charset=UTF-8
        Server: nginx
        Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
        X-Frame-Options: SAMEORIGIN
        X-Content-Type-Options: nosniff
        Referrer-Policy: strict-origin-when-cross-origin
    """.trimIndent()
)

data class HttpHeaderOutput(
    val statusCode: Int?,
    val statusMessage: String,
    val totalHeaders: Int,
    val securityScore: Int,
    val securityRating: String,
    val securityAudits: List<SecurityHeaderAudit>,
    val parsedHeaders: Map<String, String>,
    val formattedReport: String,
    val summary: String
)

class HttpHeaderInspectorTool : Tool<HttpHeaderInput, HttpHeaderOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "http_header_inspector_tool",
        name = "HTTP Response Header & Security Auditor",
        description = "Parse raw HTTP headers offline, audit crucial security headers (HSTS, CSP, X-Frame-Options), and calculate security score.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("http", "headers", "security", "csp", "hsts", "xfo", "web", "cors", "audit", "pentest"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Http"
    )

    override suspend fun execute(input: HttpHeaderInput): ToolResult<HttpHeaderOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.rawHeaders.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input HTTP headers cannot be empty.")
        }

        var statusCode: Int? = null
        var statusMsg = ""
        val headers = mutableMapOf<String, String>()

        for (line in raw.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("HTTP/", ignoreCase = true)) {
                val parts = trimmed.split(" ", limit = 3)
                if (parts.size >= 2) {
                    statusCode = parts[1].toIntOrNull()
                    statusMsg = if (parts.size >= 3) parts[2] else ""
                }
                continue
            }

            val colonIdx = trimmed.indexOf(':')
            if (colonIdx > 0) {
                val name = trimmed.substring(0, colonIdx).trim()
                val value = trimmed.substring(colonIdx + 1).trim()
                headers[name] = value
            }
        }

        val caseInsensitiveHeaders = headers.mapKeys { it.key.lowercase(Locale.ROOT) }

        // Audit standard security headers
        val audits = mutableListOf<SecurityHeaderAudit>()
        var score = 0

        // 1. Strict-Transport-Security (HSTS)
        val hsts = caseInsensitiveHeaders["strict-transport-security"]
        if (hsts != null) {
            score += 25
            val hasSubdomains = hsts.contains("includesubdomains", ignoreCase = true)
            audits.add(SecurityHeaderAudit("Strict-Transport-Security", true, hsts, "PASS",
                if (hasSubdomains) "Strong HSTS configuration with subdomains enabled." else "Consider adding 'includeSubDomains'."
            ))
        } else {
            audits.add(SecurityHeaderAudit("Strict-Transport-Security", false, "", "FAIL", "Missing HSTS. Enables potential MITM SSL-stripping."))
        }

        // 2. Content-Security-Policy (CSP)
        val csp = caseInsensitiveHeaders["content-security-policy"]
        if (csp != null) {
            score += 25
            audits.add(SecurityHeaderAudit("Content-Security-Policy", true, csp, "PASS", "Active CSP restricts unauthorized script and resource injection."))
        } else {
            audits.add(SecurityHeaderAudit("Content-Security-Policy", false, "", "WARN", "Missing CSP. Increases vulnerability to Cross-Site Scripting (XSS)."))
        }

        // 3. X-Frame-Options (Clickjacking)
        val xfo = caseInsensitiveHeaders["x-frame-options"]
        if (xfo != null) {
            score += 15
            audits.add(SecurityHeaderAudit("X-Frame-Options", true, xfo, "PASS", "Prevents clickjacking framing attacks."))
        } else {
            audits.add(SecurityHeaderAudit("X-Frame-Options", false, "", "WARN", "Missing X-Frame-Options. Site may be framed in iframes (Clickjacking risk)."))
        }

        // 4. X-Content-Type-Options (MIME sniffing)
        val xcto = caseInsensitiveHeaders["x-content-type-options"]
        if (xcto != null && xcto.contains("nosniff", ignoreCase = true)) {
            score += 15
            audits.add(SecurityHeaderAudit("X-Content-Type-Options", true, xcto, "PASS", "MIME sniffing disabled (nosniff)."))
        } else {
            audits.add(SecurityHeaderAudit("X-Content-Type-Options", false, xcto ?: "", "FAIL", "Set 'X-Content-Type-Options: nosniff' to prevent drive-by attacks."))
        }

        // 5. Referrer-Policy
        val refPol = caseInsensitiveHeaders["referrer-policy"]
        if (refPol != null) {
            score += 10
            audits.add(SecurityHeaderAudit("Referrer-Policy", true, refPol, "PASS", "Referrer information leak protected."))
        } else {
            audits.add(SecurityHeaderAudit("Referrer-Policy", false, "", "INFO", "Consider setting 'Referrer-Policy: strict-origin-when-cross-origin'."))
        }

        // 6. Permissions-Policy
        val permPol = caseInsensitiveHeaders["permissions-policy"]
        if (permPol != null) {
            score += 10
            audits.add(SecurityHeaderAudit("Permissions-Policy", true, permPol, "PASS", "Browser device APIs restricted."))
        } else {
            audits.add(SecurityHeaderAudit("Permissions-Policy", false, "", "INFO", "Consider restricting camera/geolocation/microphone APIs."))
        }

        val rating = when {
            score >= 85 -> "A+ (Excellent Protection)"
            score >= 70 -> "B (Good Security Baseline)"
            score >= 50 -> "C (Moderate Risk)"
            else -> "F (Critical Security Deficiencies)"
        }

        val report = buildString {
            appendLine("HTTP RESPONSE HEADER SECURITY AUDIT")
            appendLine("--------------------------------------------------")
            if (statusCode != null) {
                appendLine("HTTP Status:      $statusCode $statusMsg")
            }
            appendLine("Total Headers:    ${headers.size}")
            appendLine("Security Score:   $score / 100 ($rating)")
            appendLine()
            appendLine("SECURITY HEADERS EVALUATION")
            audits.forEach { a ->
                val icon = when (a.status) {
                    "PASS" -> "[✓]"
                    "WARN" -> "[!]"
                    "FAIL" -> "[✗]"
                    else -> "[i]"
                }
                appendLine("$icon ${a.headerName.padEnd(28)} : ${if (a.present) a.currentValue else "NOT SET"}")
                appendLine("    Recommendation: ${a.recommendation}")
            }
        }

        val summary = "HTTP Security Score: $score/100 ($rating)"

        return ToolResult.Success(
            data = HttpHeaderOutput(
                statusCode = statusCode,
                statusMessage = statusMsg,
                totalHeaders = headers.size,
                securityScore = score,
                securityRating = rating,
                securityAudits = audits,
                parsedHeaders = headers,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
