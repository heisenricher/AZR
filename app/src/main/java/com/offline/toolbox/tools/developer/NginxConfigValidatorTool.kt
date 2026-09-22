package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class NginxValidatorInput(
    val configText: String = """
        server {
            listen 80;
            listen 443 ssl http2;
            server_name api.offline.internal;

            ssl_certificate /etc/ssl/certs/bundle.crt;
            ssl_certificate_key /etc/ssl/private/server.key;
            ssl_protocols TLSv1.2 TLSv1.3;

            add_header X-Frame-Options "DENY";
            add_header X-Content-Type-Options "nosniff";
            add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";

            location /api/v1/ {
                proxy_pass http://backend_upstream;
                proxy_set_header Host ${'$'}host;
                proxy_set_header X-Real-IP ${'$'}remote_addr;
                proxy_set_header X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto ${'$'}scheme;
            }

            location /static/ {
                alias /var/www/static/;
                expires 30d;
                access_log off;
            }
        }
    """.trimIndent()
)

data class NginxValidatorOutput(
    val isValid: Boolean,
    val totalDirectives: Int,
    val serverBlocksCount: Int,
    val locationBlocksCount: Int,
    val upstreamBlocksCount: Int,
    val detectedListenPorts: List<String>,
    val securityAuditWarnings: List<String>,
    val syntaxErrors: List<String>,
    val formattedReport: String,
    val summary: String
)

class NginxConfigValidatorTool : Tool<NginxValidatorInput, NginxValidatorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "nginx_config_validator_tool",
        name = "Nginx Configuration & Reverse Proxy Validator",
        description = "Validate Nginx server blocks, proxy_pass reverse proxy routes, detect unbalanced braces, missing semicolons, and audit security headers offline.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("nginx", "reverse proxy", "vhost", "devops", "web server", "ssl", "proxy_pass", "configuration", "linter"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Server"
    )

    override suspend fun execute(input: NginxValidatorInput): ToolResult<NginxValidatorOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.configText.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Nginx configuration content cannot be empty.")
        }

        val lines = raw.lines()
        val syntaxErrors = mutableListOf<String>()
        val securityWarnings = mutableListOf<String>()

        var braceDepth = 0
        var serverCount = 0
        var locationCount = 0
        var upstreamCount = 0
        var directiveCount = 0

        val listenPorts = mutableListOf<String>()
        val directivesFound = mutableSetOf<String>()
        var hasSslListen = false
        var hasSslCert = false

        for ((idx, rawLine) in lines.withIndex()) {
            val lineNum = idx + 1
            // Strip comments
            val commentIdx = rawLine.indexOf('#')
            val line = (if (commentIdx != -1) rawLine.substring(0, commentIdx) else rawLine).trim()

            if (line.isEmpty()) continue

            // Count opening and closing braces
            for (ch in line) {
                if (ch == '{') braceDepth++
                else if (ch == '}') braceDepth--
            }

            if (braceDepth < 0) {
                syntaxErrors.add("Line $lineNum: Unexpected closing brace '}' without matching opening block.")
                braceDepth = 0
            }

            // Identify block headers
            if (line.startsWith("server") && line.contains("{")) {
                serverCount++
            } else if (line.startsWith("location") && line.contains("{")) {
                locationCount++
            } else if (line.startsWith("upstream") && line.contains("{")) {
                upstreamCount++
            } else if (!line.startsWith("}") && !line.endsWith("{")) {
                // Must be a directive ending with semicolon ';'
                directiveCount++
                if (!line.endsWith(";")) {
                    syntaxErrors.add("Line $lineNum: Missing semicolon ';' at end of directive: \"$line\"")
                } else {
                    val cleanDirective = line.removeSuffix(";").trim()
                    val tokens = cleanDirective.split(Regex("\\s+"), 2)
                    val dirName = tokens[0].lowercase(Locale.US)
                    directivesFound.add(dirName)

                    when (dirName) {
                        "listen" -> {
                            val portArg = if (tokens.size > 1) tokens[1] else ""
                            listenPorts.add(portArg)
                            if (portArg.contains("ssl")) hasSslListen = true
                        }
                        "ssl_certificate" -> hasSslCert = true
                        "ssl_protocols" -> {
                            val protoArg = if (tokens.size > 1) tokens[1] else ""
                            if (protoArg.contains("TLSv1.0") || protoArg.contains("TLSv1.1") || protoArg.contains("SSLv3")) {
                                securityWarnings.add("Line $lineNum: Insecure protocol detected ($protoArg). Modern Nginx should only permit TLSv1.2 and TLSv1.3.")
                            }
                        }
                    }
                }
            }
        }

        if (braceDepth > 0) {
            syntaxErrors.add("End of File: $braceDepth unclosed opening brace(s) '{' detected.")
        }

        // Security audits
        if (hasSslListen && !hasSslCert) {
            securityWarnings.add("Server listens on SSL, but no 'ssl_certificate' directive was specified.")
        }
        if (serverCount > 0 && !directivesFound.contains("add_header")) {
            securityWarnings.add("No security response headers ('add_header') found. Recommended: Strict-Transport-Security, X-Frame-Options, X-Content-Type-Options.")
        }

        val isValid = syntaxErrors.isEmpty()

        val report = buildString {
            appendLine("NGINX VIRTUAL HOST & PROXY CONFIGURATION AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Syntax Status:        ${if (isValid) "SYNTAX VALID" else "SYNTAX ERRORS DETECTED"}")
            appendLine("Server Blocks:        $serverCount")
            appendLine("Location Blocks:      $locationCount")
            appendLine("Upstream Blocks:      $upstreamCount")
            appendLine("Total Directives:     $directiveCount")
            appendLine("Listening Ports:      ${if (listenPorts.isNotEmpty()) listenPorts.joinToString(", ") else "None specified"}")
            appendLine("--------------------------------------------------")
            if (syntaxErrors.isNotEmpty()) {
                appendLine("SYNTAX ERRORS:")
                syntaxErrors.forEach { appendLine(" [!] $it") }
                appendLine("--------------------------------------------------")
            }
            if (securityWarnings.isNotEmpty()) {
                appendLine("SECURITY HYGIENE AUDIT:")
                securityWarnings.forEach { appendLine(" [*] $it") }
            } else {
                appendLine("SECURITY HYGIENE AUDIT: Passed (Standard SSL and security directives identified).")
            }
        }

        val output = NginxValidatorOutput(
            isValid = isValid,
            totalDirectives = directiveCount,
            serverBlocksCount = serverCount,
            locationBlocksCount = locationCount,
            upstreamBlocksCount = upstreamCount,
            detectedListenPorts = listenPorts,
            securityAuditWarnings = securityWarnings,
            syntaxErrors = syntaxErrors,
            formattedReport = report,
            summary = "Nginx: ${if (isValid) "Valid" else "Errors found"} ($serverCount servers, $locationCount locations, $directiveCount directives)"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Validated Nginx configuration (${if (isValid) "Valid" else "Errors found"})"
        )
    }
}
