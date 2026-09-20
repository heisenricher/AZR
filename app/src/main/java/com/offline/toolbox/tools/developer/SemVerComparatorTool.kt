package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String = "",
    val buildMetadata: String = ""
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        if (prerelease.isEmpty() && other.prerelease.isNotEmpty()) return 1
        if (prerelease.isNotEmpty() && other.prerelease.isEmpty()) return -1
        return prerelease.compareTo(other.prerelease)
    }

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        if (prerelease.isNotEmpty()) append("-$prerelease")
        if (buildMetadata.isNotEmpty()) append("+$buildMetadata")
    }
}

data class SemVerInput(
    val versionA: String = "2.4.1",
    val versionB: String = "2.5.0-alpha.1",
    val constraint: String = "^2.4.0"
)

data class SemVerOutput(
    val parsedA: String,
    val parsedB: String,
    val comparisonResult: String,
    val diffType: String,
    val satisfiesConstraint: Boolean,
    val nextPatch: String,
    val nextMinor: String,
    val nextMajor: String,
    val formattedReport: String,
    val summary: String
)

class SemVerComparatorTool : Tool<SemVerInput, SemVerOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "semver_comparator_tool",
        name = "Semantic Versioning (SemVer 2.0) Evaluator",
        description = "Validate SemVer versions, compare precedence, test ranges (^, ~), and project major, minor, and patch bumps.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("semver", "version", "comparator", "range", "npm", "cargo", "bump", "release", "patch", "minor", "major"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Commit"
    )

    override suspend fun execute(input: SemVerInput): ToolResult<SemVerOutput> {
        val startTime = System.currentTimeMillis()
        val rawA = input.versionA.trim()
        val rawB = input.versionB.trim()

        if (rawA.isEmpty()) {
            return ToolResult.Failure("Version A cannot be empty.")
        }

        val parsedA = parseSemVer(rawA)
            ?: return ToolResult.Failure("Invalid SemVer string for Version A: '$rawA'. Must match MAJOR.MINOR.PATCH format.")

        val parsedB = if (rawB.isNotEmpty()) parseSemVer(rawB) else null

        val compResult: String
        val diffType: String
        if (parsedB != null) {
            val cmp = parsedA.compareTo(parsedB)
            compResult = when {
                cmp > 0 -> "Version A ($parsedA) is GREATER than Version B ($parsedB)"
                cmp < 0 -> "Version A ($parsedA) is LESS than Version B ($parsedB)"
                else -> "Version A ($parsedA) is EQUAL to Version B ($parsedB)"
            }
            diffType = when {
                parsedA.major != parsedB.major -> "Major Change (Breaking)"
                parsedA.minor != parsedB.minor -> "Minor Change (Feature)"
                parsedA.patch != parsedB.patch -> "Patch Change (Bugfix)"
                parsedA.prerelease != parsedB.prerelease -> "Prerelease Tag Change"
                else -> "Identical Precedence"
            }
        } else {
            compResult = "Single version evaluated: $parsedA"
            diffType = "N/A"
        }

        // Evaluate constraint (e.g. ^1.2.3 or ~1.2.3)
        val satisfies = checkConstraint(parsedA, input.constraint.trim())

        // Next version bumps for A
        val nextPatch = "${parsedA.major}.${parsedA.minor}.${parsedA.patch + 1}"
        val nextMinor = "${parsedA.major}.${parsedA.minor + 1}.0"
        val nextMajor = "${parsedA.major + 1}.0.0"

        val report = buildString {
            appendLine("SEMVER 2.0.0 SPECIFICATION AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Target Version (A):   $parsedA")
            if (parsedB != null) {
                appendLine("Comparison (B):       $parsedB")
                appendLine("Comparison Result:    $compResult")
                appendLine("Precedence Delta:     $diffType")
            }
            if (input.constraint.isNotBlank()) {
                appendLine()
                appendLine("CONSTRAINT EVALUATION")
                appendLine("Range Constraint:     \"${input.constraint}\"")
                appendLine("Satisfies Range:      ${if (satisfies) "YES (Satisfied)" else "NO (Violation)"}")
            }
            appendLine()
            appendLine("PROJECTED VERSION BUMPS (for $parsedA)")
            appendLine("• Next Patch (Bugfix):    $nextPatch")
            appendLine("• Next Minor (Feature):   $nextMinor")
            appendLine("• Next Major (Breaking):  $nextMajor")
        }

        val summary = if (parsedB != null) {
            "$parsedA vs $parsedB: $diffType"
        } else {
            "SemVer: $parsedA (Next: $nextPatch / $nextMinor / $nextMajor)"
        }

        return ToolResult.Success(
            data = SemVerOutput(
                parsedA = parsedA.toString(),
                parsedB = parsedB?.toString() ?: "",
                comparisonResult = compResult,
                diffType = diffType,
                satisfiesConstraint = satisfies,
                nextPatch = nextPatch,
                nextMinor = nextMinor,
                nextMajor = nextMajor,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun parseSemVer(raw: String): SemVer? {
        val clean = raw.removePrefix("v").removePrefix("V").trim()
        val regex = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?(?:\\+([0-9A-Za-z.-]+))?$")
        val match = regex.find(clean) ?: return null

        val (major, minor, patch, pre, build) = match.destructured
        return SemVer(
            major = major.toIntOrNull() ?: return null,
            minor = minor.toIntOrNull() ?: return null,
            patch = patch.toIntOrNull() ?: return null,
            prerelease = pre,
            buildMetadata = build
        )
    }

    private fun checkConstraint(v: SemVer, constraint: String): Boolean {
        if (constraint.isEmpty()) return true
        val cleanConstraint = constraint.trim()

        if (cleanConstraint.startsWith("^")) {
            val base = parseSemVer(cleanConstraint.removePrefix("^")) ?: return false
            return if (base.major > 0) {
                v.major == base.major && v >= base
            } else if (base.minor > 0) {
                v.major == 0 && v.minor == base.minor && v >= base
            } else {
                v.major == 0 && v.minor == 0 && v.patch == base.patch
            }
        }

        if (cleanConstraint.startsWith("~")) {
            val base = parseSemVer(cleanConstraint.removePrefix("~")) ?: return false
            return v.major == base.major && v.minor == base.minor && v >= base
        }

        if (cleanConstraint.startsWith(">=")) {
            val base = parseSemVer(cleanConstraint.removePrefix(">=").trim()) ?: return false
            return v >= base
        }

        if (cleanConstraint.startsWith("<=")) {
            val base = parseSemVer(cleanConstraint.removePrefix("<=").trim()) ?: return false
            return v <= base
        }

        val direct = parseSemVer(cleanConstraint)
        return direct != null && v == direct
    }
}
