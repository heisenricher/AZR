package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class BcryptInput(
    val hashOrCost: String = "\$2a\$12\$e8kZ1VvI5yJ7wE5O8h.bOed9WJ8a4lK1U2m3N4o5P6q7R8s9T0u1v"
)

data class BcryptOutput(
    val algorithm: String,
    val costFactor: Int,
    val totalRounds: Long,
    val saltPart: String,
    val checksumPart: String,
    val securityLevel: String,
    val estimatedCheckTimeMs: String,
    val formattedReport: String,
    val summary: String
)

class BcryptWorkFactorTool : Tool<BcryptInput, BcryptOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "bcrypt_work_factor_tool",
        name = "Bcrypt Work Factor & Hash Analyzer",
        description = "Decompose bcrypt hash structures, inspect iteration work factors, and evaluate offline cracking resistance.",
        category = ToolCategory.SECURITY,
        tags = listOf("bcrypt", "hash", "cost", "work factor", "password", "security", "argon2", "rounds"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Password"
    )

    override suspend fun execute(input: BcryptInput): ToolResult<BcryptOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.hashOrCost.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input cannot be empty. Enter a bcrypt hash or cost integer.")
        }

        // Check if raw input is just an integer cost (e.g. "12")
        val directCost = raw.toIntOrNull()
        if (directCost != null) {
            if (directCost !in 4..31) {
                return ToolResult.Failure("Bcrypt cost factor must be between 4 and 31 (got $directCost).")
            }
            return evaluateCostOnly(directCost, startTime)
        }

        // Match standard bcrypt regex: $2[abxy]?$[0-9]{2}$[A-Za-z0-9./]{53}
        val bcryptRegex = Regex("^\\$2([abxy]?)\\$(\\d{2})\\$([A-Za-z0-9./]{22})([A-Za-z0-9./]{31})$")
        val match = bcryptRegex.find(raw)

        if (match == null) {
            return ToolResult.Failure(
                message = "Invalid bcrypt hash structure.",
                userGuidance = "Standard bcrypt hashes start with '\$2a\$', '\$2b\$', or '\$2y\$' followed by a 2-digit cost and 53-character salt+hash."
            )
        }

        val (version, costStr, salt, checksum) = match.destructured
        val cost = costStr.toIntOrNull() ?: 10
        val rounds = 1L shl cost

        val versionDisplay = when (version) {
            "a" -> "Bcrypt 2a (OpenBSD / Standard)"
            "b" -> "Bcrypt 2b (Modern OpenBSD fix)"
            "y" -> "Bcrypt 2y (PHP Crypt compatibility)"
            else -> "Bcrypt 2 (Legacy)"
        }

        val (secLevel, estTime) = estimateSecurity(cost)

        val report = buildString {
            appendLine("BCRYPT HASH STRUCTURE DECOMPOSITION")
            appendLine("--------------------------------------------------")
            appendLine("Full Hash:        $raw")
            appendLine("Algorithm:        $versionDisplay")
            appendLine("Work Factor:      $cost (2^$cost = $rounds key expansion rounds)")
            appendLine("Embedded Salt:    $salt (16 bytes base64)")
            appendLine("Ciphertext Hash:  $checksum")
            appendLine()
            appendLine("SECURITY EVALUATION")
            appendLine("--------------------------------------------------")
            appendLine("Security Rating:  $secLevel")
            appendLine("CPU Verify Time:  $estTime")
            appendLine("Hardware Attack:  High ASIC/GPU memory-hardness resistance")
        }

        val summary = "Bcrypt Cost $cost ($rounds rounds) — $secLevel"

        return ToolResult.Success(
            data = BcryptOutput(
                algorithm = versionDisplay,
                costFactor = cost,
                totalRounds = rounds,
                saltPart = salt,
                checksumPart = checksum,
                securityLevel = secLevel,
                estimatedCheckTimeMs = estTime,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun evaluateCostOnly(cost: Int, startTime: Long): ToolResult<BcryptOutput> {
        val rounds = 1L shl cost
        val (secLevel, estTime) = estimateSecurity(cost)

        val report = buildString {
            appendLine("BCRYPT WORK FACTOR PROJECTION")
            appendLine("--------------------------------------------------")
            appendLine("Cost Parameter:   $cost")
            appendLine("Total Iterations: $rounds key expansion rounds (2^$cost)")
            appendLine("Security Level:   $secLevel")
            appendLine("CPU Verify Time:  $estTime")
        }

        val summary = "Cost $cost: $rounds rounds ($secLevel)"

        return ToolResult.Success(
            data = BcryptOutput(
                algorithm = "Bcrypt (Cost Specification)",
                costFactor = cost,
                totalRounds = rounds,
                saltPart = "[Synthetic/Projected]",
                checksumPart = "[Synthetic/Projected]",
                securityLevel = secLevel,
                estimatedCheckTimeMs = estTime,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun estimateSecurity(cost: Int): Pair<String, String> {
        return when {
            cost < 8 -> Pair("WEAK / OBSOLETE", "~2 - 5 ms (Vulnerable to modern GPU cracking)")
            cost in 8..9 -> Pair("MODERATE (Low End)", "~10 - 25 ms (Borderline for interactive web logins)")
            cost in 10..11 -> Pair("GOOD / ACCEPTABLE", "~50 - 150 ms (Common default for web applications)")
            cost in 12..13 -> Pair("STRONG (Recommended 2026)", "~250 - 600 ms (Optimal balance of security & UX)")
            cost in 14..15 -> Pair("VERY STRONG / HIGH SECURITY", "~1,000 - 3,000 ms (Suitable for sensitive vaults)")
            else -> Pair("EXTREME / EXPONENTIAL", "> 5,000 ms (High server load risk)")
        }
    }
}
