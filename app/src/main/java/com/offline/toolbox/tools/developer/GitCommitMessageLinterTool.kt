package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class GitCommitLinterInput(
    val commitMessage: String = """
        feat(auth)!: add biometric fingerprint authentication

        Implement Android BiometricPrompt API for fast local biometric unlock.
        Replaces legacy pin-only authentication pipeline.

        BREAKING CHANGE: Minimum supported biometric hardware level is BIOMETRIC_STRONG.
        Fixes: #1042
    """.trimIndent(),
    val maxHeaderLength: Int = 72,
    val enforceImperativeMood: Boolean = true
)

data class CommitLintIssue(
    val severity: String, // ERROR, WARNING, INFO
    val rule: String,
    val message: String
)

data class GitCommitLinterOutput(
    val isValid: Boolean,
    val commitType: String?,
    val scope: String?,
    val isBreakingChange: Boolean,
    val subject: String?,
    val body: String?,
    val footers: List<String>,
    val qualityScore: Int,
    val issues: List<CommitLintIssue>,
    val formattedReport: String,
    val summary: String
)

class GitCommitMessageLinterTool : Tool<GitCommitLinterInput, GitCommitLinterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "git_commit_message_linter_tool",
        name = "Conventional Commits 1.0.0 Linter & Validator",
        description = "Validate git commit messages against the Conventional Commits 1.0.0 specification with type checking, header length budgets, imperative mood enforcement, and breaking change detection.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("git", "commit", "conventional commits", "linter", "devops", "vcs", "imperative mood", "semantic release", "changelog"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "GitCommit"
    )

    private val recognizedTypes = setOf(
        "feat", "fix", "docs", "style", "refactor",
        "perf", "test", "build", "ci", "chore", "revert"
    )

    private val nonImperativeVerbs = mapOf(
        "added" to "add", "adds" to "add", "adding" to "add",
        "fixed" to "fix", "fixes" to "fix", "fixing" to "fix",
        "updated" to "update", "updates" to "update", "updating" to "update",
        "removed" to "remove", "removes" to "remove", "removing" to "remove",
        "changed" to "change", "changes" to "change", "changing" to "change",
        "refactored" to "refactor", "refactors" to "refactor", "refactoring" to "refactor",
        "implemented" to "implement", "implements" to "implement", "implementing" to "implement",
        "deleted" to "delete", "deletes" to "delete", "deleting" to "delete",
        "created" to "create", "creates" to "create", "creating" to "create",
        "modified" to "modify", "modifies" to "modify", "modifying" to "modify"
    )

    override suspend fun execute(input: GitCommitLinterInput): ToolResult<GitCommitLinterOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.commitMessage.trim()
        if (raw.isBlank()) {
            return ToolResult.Failure("Commit message cannot be empty.")
        }

        val lines = raw.lines()
        val header = lines.first().trim()
        val issues = mutableListOf<CommitLintIssue>()

        // 1. Header length checks
        if (header.length > input.maxHeaderLength) {
            issues.add(
                CommitLintIssue(
                    severity = "ERROR",
                    rule = "HEADER_MAX_LENGTH",
                    message = "Commit header length (${header.length} chars) exceeds maximum allowable limit of ${input.maxHeaderLength} characters."
                )
            )
        } else if (header.length > 50) {
            issues.add(
                CommitLintIssue(
                    severity = "INFO",
                    rule = "HEADER_RECOMMENDED_LENGTH",
                    message = "Commit header is ${header.length} chars. Best practice recommends 50 characters or fewer for optimal git log readability."
                )
            )
        }

        // 2. Parse conventional commit header regex: ^(?<type>[a-zA-Z]+)(?:\((?<scope>[^)]+)\))?(?<breaking>!)?:\s+(?<subject>.+)$
        val headerRegex = Regex("""^([a-zA-Z]+)(?:\(([^)]+)\))?(!)?:\s+(.+)$""")
        val match = headerRegex.find(header)

        var detectedType: String? = null
        var detectedScope: String? = null
        var isBreaking = false
        var detectedSubject: String? = null

        if (match == null) {
            issues.add(
                CommitLintIssue(
                    severity = "ERROR",
                    rule = "CONVENTIONAL_STRUCTURE",
                    message = "Header does not follow Conventional Commits structure '<type>(<optional scope>): <subject>'."
                )
            )
        } else {
            detectedType = match.groupValues[1].lowercase(Locale.US)
            detectedScope = match.groupValues[2].takeIf { it.isNotEmpty() }
            isBreaking = match.groupValues[3] == "!"
            detectedSubject = match.groupValues[4]

            if (detectedType !in recognizedTypes) {
                issues.add(
                    CommitLintIssue(
                        severity = "WARNING",
                        rule = "UNKNOWN_TYPE",
                        message = "Type '$detectedType' is not a standard Conventional Commit type (${recognizedTypes.joinToString(", ")})."
                    )
                )
            }

            // Subject checks
            if (detectedSubject.endsWith(".")) {
                issues.add(
                    CommitLintIssue(
                        severity = "WARNING",
                        rule = "SUBJECT_TRAILING_PERIOD",
                        message = "Subject should not end with a trailing period."
                    )
                )
            }

            if (detectedSubject.firstOrNull()?.isUpperCase() == true) {
                issues.add(
                    CommitLintIssue(
                        severity = "INFO",
                        rule = "SUBJECT_LOWERCASE",
                        message = "Subject should ideally begin with a lowercase letter."
                    )
                )
            }

            // Imperative mood check on first word of subject
            if (input.enforceImperativeMood) {
                val firstWord = detectedSubject.trim().split(Regex("\\s+")).first().lowercase(Locale.US)
                val suggestedImperative = nonImperativeVerbs[firstWord]
                if (suggestedImperative != null) {
                    issues.add(
                        CommitLintIssue(
                            severity = "WARNING",
                            rule = "IMPERATIVE_MOOD",
                            message = "Use imperative mood in subject: replace '$firstWord' with '$suggestedImperative'."
                        )
                    )
                }
            }
        }

        // 3. Body & Blank line checks
        var detectedBody: String? = null
        val footers = mutableListOf<String>()

        if (lines.size > 1) {
            val secondLine = lines[1].trim()
            if (secondLine.isNotEmpty()) {
                issues.add(
                    CommitLintIssue(
                        severity = "ERROR",
                        rule = "MISSING_BLANK_LINE",
                        message = "There must be a blank line between the header and the body."
                    )
                )
            }

            val bodyLines = mutableListOf<String>()
            var inFooters = false

            for (i in 2 until lines.size) {
                val line = lines[i]
                if (line.startsWith("BREAKING CHANGE:") || line.startsWith("BREAKING-CHANGE:") ||
                    line.matches(Regex("^[a-zA-Z0-9_-]+:\\s+.+$")) || line.matches(Regex("^[a-zA-Z0-9_-]+ #\\d+$"))
                ) {
                    inFooters = true
                    if (line.contains("BREAKING")) isBreaking = true
                    footers.add(line.trim())
                } else if (!inFooters) {
                    if (line.length > 72) {
                        issues.add(
                            CommitLintIssue(
                                severity = "INFO",
                                rule = "BODY_LINE_WRAP",
                                message = "Line ${i + 1} exceeds recommended 72-character body wrap limit (${line.length} chars)."
                            )
                        )
                    }
                    bodyLines.add(line)
                } else {
                    footers.add(line.trim())
                }
            }

            detectedBody = bodyLines.joinToString("\n").trim().takeIf { it.isNotEmpty() }
        }

        val errorCount = issues.count { it.severity == "ERROR" }
        val warningCount = issues.count { it.severity == "WARNING" }
        val isValid = errorCount == 0

        var score = 100 - (errorCount * 30) - (warningCount * 10) - (issues.count { it.severity == "INFO" } * 5)
        score = score.coerceIn(0, 100)

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== CONVENTIONAL COMMITS 1.0.0 LINT REPORT ===")
            appendLine("Validation:       ${if (isValid) "PASSED" else "FAILED"}")
            appendLine("Quality Score:    $score / 100")
            appendLine("Commit Type:      ${detectedType ?: "Unknown"}")
            appendLine("Scope:            ${detectedScope ?: "None"}")
            appendLine("Breaking Change:  $isBreaking")
            appendLine("Subject:          ${detectedSubject ?: "None"}")
            appendLine("----------------------------------------")
            appendLine("HEADER ANALYSIS:")
            appendLine("  Header Text:    $header")
            appendLine("  Header Length:  ${header.length} characters (Max budget: ${input.maxHeaderLength})")
            if (detectedBody != null) {
                appendLine("----------------------------------------")
                appendLine("BODY SUMMARY:")
                appendLine(detectedBody)
            }
            if (footers.isNotEmpty()) {
                appendLine("----------------------------------------")
                appendLine("FOOTERS / REFERENCES:")
                footers.forEach { appendLine("  - $it") }
            }
            if (issues.isNotEmpty()) {
                appendLine("----------------------------------------")
                appendLine("LINT FINDINGS (${issues.size}):")
                issues.forEach { issue ->
                    val badge = when (issue.severity) {
                        "ERROR" -> "[ERROR]  "
                        "WARNING" -> "[WARNING]"
                        else -> "[INFO]   "
                    }
                    appendLine("  $badge [${issue.rule}] ${issue.message}")
                }
            } else {
                appendLine("----------------------------------------")
                appendLine("Perfect commit structure! No issues detected.")
            }
        }

        val summaryText = if (isValid) {
            "Valid Conventional Commit (${detectedType ?: "generic"}${if (isBreaking) "!" else ""}): score $score/100."
        } else {
            "Invalid Commit: $errorCount error(s), $warningCount warning(s)."
        }

        return ToolResult.Success(
            data = GitCommitLinterOutput(
                isValid = isValid,
                commitType = detectedType,
                scope = detectedScope,
                isBreakingChange = isBreaking,
                subject = detectedSubject,
                body = detectedBody,
                footers = footers,
                qualityScore = score,
                issues = issues,
                formattedReport = report,
                summary = summaryText
            ),
            executionTimeMs = elapsed,
            summary = summaryText
        )
    }
}
