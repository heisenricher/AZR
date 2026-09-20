package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import kotlin.math.log2
import kotlin.math.pow

data class PasswordStrengthOutput(
    val score: Int,                 // 0 to 4
    val strengthLevel: String,      // Very Weak, Weak, Fair, Strong, Excellent
    val entropyBits: Double,
    val length: Int,
    val hasUpper: Boolean,
    val hasLower: Boolean,
    val hasNumber: Boolean,
    val hasSymbol: Boolean,
    val estimatedCrackTime: String,
    val suggestions: List<String>,
    val summary: String
)

class PasswordStrengthTool : Tool<String, PasswordStrengthOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "password_strength",
        name = "Password Strength Evaluator",
        description = "Evaluate password entropy, complexity, character classes, and estimated crack time.",
        category = ToolCategory.SECURITY,
        tags = listOf("password", "strength", "checker", "entropy", "security", "crack time", "audit"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Security"
    )

    override suspend fun execute(input: String): ToolResult<PasswordStrengthOutput> {
        val startTime = System.currentTimeMillis()
        if (input.isEmpty()) {
            return ToolResult.Success(
                data = PasswordStrengthOutput(
                    score = 0,
                    strengthLevel = "Empty",
                    entropyBits = 0.0,
                    length = 0,
                    hasUpper = false,
                    hasLower = false,
                    hasNumber = false,
                    hasSymbol = false,
                    estimatedCrackTime = "Instant",
                    suggestions = listOf("Enter a password to evaluate."),
                    summary = "Empty password"
                ),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        var poolSize = 0
        val hasLower = input.any { it.isLowerCase() }
        val hasUpper = input.any { it.isUpperCase() }
        val hasDigit = input.any { it.isDigit() }
        val hasSymbol = input.any { !it.isLetterOrDigit() }

        if (hasLower) poolSize += 26
        if (hasUpper) poolSize += 26
        if (hasDigit) poolSize += 10
        if (hasSymbol) poolSize += 32

        val entropy = if (poolSize > 0) input.length * log2(poolSize.toDouble()) else 0.0
        val suggestions = mutableListOf<String>()

        if (input.length < 12) suggestions.add("Increase length to at least 12-16 characters.")
        if (!hasUpper) suggestions.add("Add uppercase letters (A-Z).")
        if (!hasLower) suggestions.add("Add lowercase letters (a-z).")
        if (!hasDigit) suggestions.add("Add numbers (0-9).")
        if (!hasSymbol) suggestions.add("Add special symbols (!@#\$%^&*).")

        // Check common sequences
        val lower = input.lowercase()
        if (lower.contains("1234") || lower.contains("qwerty") || lower.contains("password") || lower.contains("admin")) {
            suggestions.add("Avoid common dictionary words and predictable keyboard sequences like '1234' or 'qwerty'.")
        }

        val (score, level) = when {
            entropy < 28.0 || input.length < 6 -> Pair(0, "Very Weak")
            entropy < 40.0 || input.length < 8 -> Pair(1, "Weak")
            entropy < 60.0 || input.length < 11 -> Pair(2, "Fair")
            entropy < 80.0 -> Pair(3, "Strong")
            else -> Pair(4, "Excellent")
        }

        val crackTime = estimateCrackTime(entropy)
        val summary = "$level (${Math.round(entropy * 10) / 10.0} bits) • Crack Time: $crackTime"

        return ToolResult.Success(
            data = PasswordStrengthOutput(
                score = score,
                strengthLevel = level,
                entropyBits = Math.round(entropy * 10.0) / 10.0,
                length = input.length,
                hasUpper = hasUpper,
                hasLower = hasLower,
                hasNumber = hasDigit,
                hasSymbol = hasSymbol,
                estimatedCrackTime = crackTime,
                suggestions = suggestions,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun estimateCrackTime(entropyBits: Double): String {
        // Assume 10 billion guesses per second (10^10)
        val combinations = 2.0.pow(entropyBits)
        val seconds = combinations / 10_000_000_000.0

        return when {
            seconds < 1.0 -> "Instant"
            seconds < 60.0 -> "${seconds.toInt()} seconds"
            seconds < 3600.0 -> "${(seconds / 60).toInt()} minutes"
            seconds < 86400.0 -> "${(seconds / 3600).toInt()} hours"
            seconds < 31536000.0 -> "${(seconds / 86400).toInt()} days"
            seconds < 3153600000.0 -> "${(seconds / 31536000).toInt()} years"
            seconds < 3153600000000.0 -> "${(seconds / 31536000000.0).toInt()} thousand years"
            else -> "Millions of years"
        }
    }
}
