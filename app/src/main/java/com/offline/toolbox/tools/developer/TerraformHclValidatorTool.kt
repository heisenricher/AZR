package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class TerraformHclInput(
    val hclContent: String = """
        terraform {
          required_version = ">= 1.5.0"
          required_providers {
            aws = {
              source  = "hashicorp/aws"
              version = "~> 5.0"
            }
          }
        }

        provider "aws" {
          region = var.aws_region
        }

        variable "aws_region" {
          type        = string
          default     = "us-east-1"
          description = "Target deployment region"
        }

        resource "aws_s3_bucket" "secure_storage" {
          bucket = "offline-app-storage-bucket"

          tags = {
            Environment = "Production"
            ManagedBy   = "Terraform"
          }
        }

        output "bucket_arn" {
          value       = aws_s3_bucket.secure_storage.arn
          description = "ARN of the production S3 bucket"
        }
    """.trimIndent()
)

data class HclBlockInfo(
    val blockType: String,
    val primaryLabel: String,
    val secondaryLabel: String,
    val lineNumber: Int
)

data class HclLintFinding(
    val severity: String, // ERROR, WARNING, INFO
    val lineNumber: Int,
    val rule: String,
    val message: String
)

data class TerraformHclOutput(
    val isValidSyntax: Boolean,
    val totalBlocks: Int,
    val resourceCount: Int,
    val variableCount: Int,
    val outputCount: Int,
    val providerCount: Int,
    val detectedBlocks: List<HclBlockInfo>,
    val healthScore: Int,
    val findings: List<HclLintFinding>,
    val formattedReport: String,
    val summary: String
)

class TerraformHclValidatorTool : Tool<TerraformHclInput, TerraformHclOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "terraform_hcl_validator_tool",
        name = "Terraform HCL Syntax & Security Linter",
        description = "Validate HashiCorp HCL syntax, audit Terraform resource blocks, check unpinned provider versions, detect hardcoded plaintext credentials, and score security posture.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("terraform", "hcl", "iac", "devops", "cloud", "aws", "security", "linter", "syntax", "infrastructure"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Server"
    )

    private val recognizedBlocks = setOf("terraform", "provider", "variable", "resource", "data", "output", "locals", "module")
    private val sensitivePatterns = listOf("password", "secret", "token", "private_key", "api_key", "access_key")

    override suspend fun execute(input: TerraformHclInput): ToolResult<TerraformHclOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.hclContent.trim()
        if (text.isBlank()) {
            return ToolResult.Failure("Terraform HCL content cannot be empty.")
        }

        val lines = text.lines()
        val findings = mutableListOf<HclLintFinding>()
        val blocks = mutableListOf<HclBlockInfo>()

        var braceCount = 0
        var bracketCount = 0
        var inQuotes = false

        // Brace and bracket balancing
        for ((lineIdx, line) in lines.withIndex()) {
            val lineNum = lineIdx + 1
            val trimmed = line.trim()

            // Skip comment lines
            if (trimmed.startsWith("#") || trimmed.startsWith("//")) continue

            for (ch in line) {
                if (ch == '"' && !inQuotes) inQuotes = true
                else if (ch == '"' && inQuotes) inQuotes = false

                if (!inQuotes) {
                    when (ch) {
                        '{' -> braceCount++
                        '}' -> braceCount--
                        '[' -> bracketCount++
                        ']' -> bracketCount--
                    }
                }
            }

            if (braceCount < 0) {
                findings.add(
                    HclLintFinding("ERROR", lineNum, "UNBALANCED_BRACE", "Unmatched closing curly brace '}' found.")
                )
                braceCount = 0
            }
            if (bracketCount < 0) {
                findings.add(
                    HclLintFinding("ERROR", lineNum, "UNBALANCED_BRACKET", "Unmatched closing bracket ']' found.")
                )
                bracketCount = 0
            }

            // Detect Block Headers: resource "type" "name" { or variable "name" {
            val blockMatch = Regex("^([a-z_]+)\\s*(?:\"([^\"]+)\")?\\s*(?:\"([^\"]+)\")?\\s*\\{").find(trimmed)
            if (blockMatch != null) {
                val bType = blockMatch.groupValues[1]
                val bLabel1 = blockMatch.groupValues.getOrNull(2) ?: ""
                val bLabel2 = blockMatch.groupValues.getOrNull(3) ?: ""

                if (bType in recognizedBlocks) {
                    blocks.add(HclBlockInfo(bType, bLabel1, bLabel2, lineNum))
                }
            }

            // Audit hardcoded credentials
            for (kw in sensitivePatterns) {
                if (Regex("\\b$kw\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)) {
                    // Check if value is variable or local
                    val valMatch = Regex("\\b$kw\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(trimmed)
                    val secretVal = valMatch?.groupValues?.get(1) ?: ""
                    if (!secretVal.startsWith("var.") && !secretVal.startsWith("local.") && !secretVal.startsWith("\${")) {
                        findings.add(
                            HclLintFinding("WARNING", lineNum, "HARDCODED_SECRET", "Potential hardcoded plaintext credential for attribute '$kw'. Use variables, AWS SSM, or Vault.")
                        )
                    }
                }
            }

            // Check deprecated interpolation syntax: "${var.foo}"
            if (trimmed.contains("\${var.") || trimmed.contains("\${local.")) {
                findings.add(
                    HclLintFinding("INFO", lineNum, "DEPRECATED_INTERPOLATION", "Deprecated string interpolation '\${...}' used on standalone variable. Use direct reference 'var.name'.")
                )
            }
        }

        if (braceCount != 0) {
            findings.add(
                HclLintFinding("ERROR", lines.size, "UNBALANCED_BRACE", "Unbalanced braces at end of file (unclosed braces: $braceCount).")
            )
        }
        if (bracketCount != 0) {
            findings.add(
                HclLintFinding("ERROR", lines.size, "UNBALANCED_BRACKET", "Unbalanced brackets at end of file (unclosed brackets: $bracketCount).")
            )
        }

        // Provider version pin check
        val hasTerraformBlock = blocks.any { it.blockType == "terraform" }
        val providers = blocks.filter { it.blockType == "provider" }
        if (providers.isNotEmpty() && !hasTerraformBlock) {
            findings.add(
                HclLintFinding("WARNING", 1, "UNPINNED_PROVIDER", "Provider block declared without a 'terraform { required_providers { ... } }' version lock.")
            )
        }

        val hasErrors = findings.any { it.severity == "ERROR" }
        val errorCount = findings.count { it.severity == "ERROR" }
        val warningCount = findings.count { it.severity == "WARNING" }

        var score = 100 - (errorCount * 30) - (warningCount * 10)
        score = score.coerceIn(0, 100)

        val resCount = blocks.count { it.blockType == "resource" }
        val varCount = blocks.count { it.blockType == "variable" }
        val outCount = blocks.count { it.blockType == "output" }
        val provCount = blocks.count { it.blockType == "provider" }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== TERRAFORM HCL LINTER & SECURITY REPORT ===")
            appendLine("Syntax Validity: ${if (!hasErrors) "VALID" else "INVALID (Syntax Errors Detected)"}")
            appendLine("Quality Score:   $score / 100")
            appendLine("Total Blocks:    ${blocks.size}")
            appendLine("  Resources:     $resCount")
            appendLine("  Variables:     $varCount")
            appendLine("  Outputs:       $outCount")
            appendLine("  Providers:     $provCount")
            appendLine("----------------------------------------")
            appendLine("BLOCK INVENTORY:")
            blocks.forEach { b ->
                val labels = listOf(b.primaryLabel, b.secondaryLabel).filter { it.isNotBlank() }.joinToString(" ") { "\"$it\"" }
                appendLine("  [Line %3d] %-10s %s".format(Locale.US, b.lineNumber, b.blockType, labels))
            }
            appendLine("----------------------------------------")
            appendLine("FINDINGS (${findings.size} issues):")
            if (findings.isEmpty()) {
                appendLine("  No syntax or security issues detected.")
            } else {
                findings.forEach { f ->
                    appendLine("  [%-7s] Line %3d [%s]: %s".format(Locale.US, f.severity, f.lineNumber, f.rule, f.message))
                }
            }
        }

        return ToolResult.Success(
            data = TerraformHclOutput(
                isValidSyntax = !hasErrors,
                totalBlocks = blocks.size,
                resourceCount = resCount,
                variableCount = varCount,
                outputCount = outCount,
                providerCount = provCount,
                detectedBlocks = blocks,
                healthScore = score,
                findings = findings,
                formattedReport = report,
                summary = "HCL ${if (!hasErrors) "Valid" else "Invalid"} (Score: $score/100, ${blocks.size} blocks, ${findings.size} findings)."
            ),
            executionTimeMs = elapsed,
            summary = "HCL ${if (!hasErrors) "Valid" else "Invalid"} ($score/100)"
        )
    }
}
