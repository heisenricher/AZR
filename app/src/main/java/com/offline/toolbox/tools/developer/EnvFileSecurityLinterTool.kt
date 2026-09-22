package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class EnvFileLinterInput(
    val envContent: String = """
        # Production Environment Configuration
        PORT=8080
        NODE_ENV=production
        DATABASE_URL=postgres://app_user:s3cr3tP@ssw0rd@db.internal:5432/prod_db
        AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
        JWT_SECRET_TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.placeholderSignatureOnly
        DEBUG=false
        PORT=3000
        api_timeout=60
        GREETING_MESSAGE=Hello World from Service
    """.trimIndent()
)

data class EnvFinding(
    val severity: String, // CRITICAL, WARNING, INFO
    val lineNumber: Int,
    val key: String,
    val rule: String,
    val message: String
)

data class EnvEntry(
    val key: String,
    val value: String,
    val lineNumber: Int,
    val isSensitive: Boolean
)

data class EnvFileLinterOutput(
    val totalEntries: Int,
    val securityScore: Int,
    val criticalLeakCount: Int,
    val warningCount: Int,
    val duplicateKeys: List<String>,
    val findings: List<EnvFinding>,
    val sanitizedExampleContent: String,
    val formattedReport: String,
    val summary: String
)

class EnvFileSecurityLinterTool : Tool<EnvFileLinterInput, EnvFileLinterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "env_file_security_linter_tool",
        name = "Dotenv (.env) Security & Syntax Linter",
        description = "Audit .env configuration files for hardcoded secrets (AWS keys, Stripe keys, JWT, passwords), duplicate variables, shell quoting syntax, and generate sanitized .env.example templates.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("env", "dotenv", "security", "secrets", "linter", "aws", "stripe", "passwords", "config", "devops"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FileText"
    )

    private val awsKeyRegex = Regex("AKIA[0-9A-Z]{16}")
    private val stripeKeyRegex = Regex("sk_live_[0-9a-zA-Z]{24,}")
    private val jwtRegex = Regex("eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
    private val sensitiveKeywords = setOf("PASSWORD", "SECRET", "TOKEN", "KEY", "AUTH", "CREDENTIAL", "PRIVATE")

    override suspend fun execute(input: EnvFileLinterInput): ToolResult<EnvFileLinterOutput> {
        val startTime = System.currentTimeMillis()
        val content = input.envContent.trim()
        if (content.isBlank()) {
            return ToolResult.Failure("Dotenv content cannot be empty.")
        }

        val lines = content.lines()
        val findings = mutableListOf<EnvFinding>()
        val entries = mutableListOf<EnvEntry>()
        val keyOccurrences = mutableMapOf<String, Int>()

        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val eqIdx = trimmed.indexOf("=")
            if (eqIdx == -1) {
                findings.add(
                    EnvFinding("WARNING", lineNum, trimmed, "SYNTAX_MISSING_EQUALS", "Line does not contain an assignment '=' operator.")
                )
                continue
            }

            var rawKey = trimmed.substring(0, eqIdx).trim()
            if (rawKey.startsWith("export ")) {
                rawKey = rawKey.removePrefix("export ").trim()
            }
            val rawVal = trimmed.substring(eqIdx + 1).trim()

            // Key naming checks
            if (!rawKey.matches(Regex("^[A-Za-z_][A-Za-z0-9_]*$"))) {
                findings.add(
                    EnvFinding("WARNING", lineNum, rawKey, "INVALID_KEY_CHARACTERS", "Variable name '$rawKey' contains non-standard characters.")
                )
            } else if (rawKey != rawKey.uppercase(Locale.US)) {
                findings.add(
                    EnvFinding("INFO", lineNum, rawKey, "NON_UPPERCASE_KEY", "Variable '$rawKey' is not in UPPER_CASE / CONSTANT_CASE.")
                )
            }

            // Track duplicate keys
            val currentCount = keyOccurrences.getOrDefault(rawKey, 0) + 1
            keyOccurrences[rawKey] = currentCount
            if (currentCount > 1) {
                findings.add(
                    EnvFinding("WARNING", lineNum, rawKey, "DUPLICATE_KEY", "Variable '$rawKey' is defined multiple times (overrides previous value).")
                )
            }

            // Value checks (Unquoted spaces)
            if (rawVal.contains(" ") && !rawVal.startsWith("\"") && !rawVal.startsWith("'")) {
                findings.add(
                    EnvFinding("WARNING", lineNum, rawKey, "UNQUOTED_WHITESPACE", "Value contains unquoted whitespace which can break shell sourcing.")
                )
            }

            // Secret leakage checks
            var isSensitive = false
            when {
                awsKeyRegex.containsMatchIn(rawVal) -> {
                    isSensitive = true
                    findings.add(
                        EnvFinding("CRITICAL", lineNum, rawKey, "AWS_ACCESS_KEY_LEAK", "Live AWS Access Key ID detected in value!")
                    )
                }
                stripeKeyRegex.containsMatchIn(rawVal) -> {
                    isSensitive = true
                    findings.add(
                        EnvFinding("CRITICAL", lineNum, rawKey, "STRIPE_SECRET_KEY_LEAK", "Live Stripe Secret Key detected in value!")
                    )
                }
                jwtRegex.containsMatchIn(rawVal) -> {
                    isSensitive = true
                    findings.add(
                        EnvFinding("CRITICAL", lineNum, rawKey, "JWT_TOKEN_LEAK", "Hardcoded JSON Web Token (JWT) detected in value!")
                    )
                }
                rawVal.contains("-----BEGIN") -> {
                    isSensitive = true
                    findings.add(
                        EnvFinding("CRITICAL", lineNum, rawKey, "PRIVATE_KEY_LEAK", "Private cryptographic key block detected in value!")
                    )
                }
                else -> {
                    val upperKey = rawKey.uppercase(Locale.US)
                    for (kw in sensitiveKeywords) {
                        if (upperKey.contains(kw) && rawVal.length > 3 && !rawVal.startsWith("your_")) {
                            isSensitive = true
                            findings.add(
                                EnvFinding("WARNING", lineNum, rawKey, "PLAINTEXT_SECRET", "Variable '$rawKey' likely contains sensitive credentials in plaintext.")
                            )
                            break
                        }
                    }
                }
            }

            entries.add(EnvEntry(rawKey, rawVal, lineNum, isSensitive))
        }

        val duplicateKeys = keyOccurrences.filter { it.value > 1 }.keys.toList()
        val criticalCount = findings.count { it.severity == "CRITICAL" }
        val warningCount = findings.count { it.severity == "WARNING" }

        var score = 100 - (criticalCount * 30) - (warningCount * 10) - (duplicateKeys.size * 5)
        score = score.coerceIn(0, 100)

        // Generate sanitized .env.example
        val exampleLines = mutableListOf<String>()
        exampleLines.add("# Auto-generated Sanitized .env.example")
        for (e in entries.distinctBy { it.key }) {
            val placeholder = if (e.isSensitive) "your_${e.key.lowercase(Locale.US)}_here" else e.value
            exampleLines.add("${e.key}=$placeholder")
        }
        val sanitizedContent = exampleLines.joinToString("\n")

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== DOTENV (.ENV) SECURITY AUDIT REPORT ===")
            appendLine("Security Score:       $score / 100")
            appendLine("Total Variables:      ${entries.size}")
            appendLine("Critical Secret Leaks: $criticalCount")
            appendLine("Warnings:             $warningCount")
            appendLine("Duplicate Overrides:  ${if (duplicateKeys.isNotEmpty()) duplicateKeys.joinToString(", ") else "None"}")
            appendLine("----------------------------------------")
            appendLine("SECURITY FINDINGS (${findings.size}):")
            if (findings.isEmpty()) {
                appendLine("  No security or syntax issues found.")
            } else {
                findings.forEach { f ->
                    appendLine("  [%-8s] Line %2d [%s]: %s".format(Locale.US, f.severity, f.lineNumber, f.key, f.message))
                }
            }
            appendLine("----------------------------------------")
            appendLine("SANITIZED .env.example TEMPLATE:")
            appendLine(sanitizedContent)
        }

        return ToolResult.Success(
            data = EnvFileLinterOutput(
                totalEntries = entries.size,
                securityScore = score,
                criticalLeakCount = criticalCount,
                warningCount = warningCount,
                duplicateKeys = duplicateKeys,
                findings = findings,
                sanitizedExampleContent = sanitizedContent,
                formattedReport = report,
                summary = "Audited ${entries.size} env vars. Score: $score/100 ($criticalCount critical leaks, $warningCount warnings)."
            ),
            executionTimeMs = elapsed,
            summary = "Score: $score/100 ($criticalCount leaks, $warningCount warnings)"
        )
    }
}
