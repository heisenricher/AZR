package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class SemverRangeInput(
    val rangeExpression: String = "^1.2.0 || >=2.1.0 <3.0.0",
    val candidateVersions: String = "1.1.0, 1.2.0, 1.2.4, 1.3.0, 2.0.0, 2.1.0, 2.4.9, 3.0.0, 3.1.2"
)

data class RangeSemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val raw: String
) : Comparable<RangeSemVer> {
    override fun compareTo(other: RangeSemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"
}

data class VersionMatchResult(
    val version: String,
    val isSatisfied: Boolean,
    val reason: String
)

data class SemverRangeOutput(
    val rangeExpression: String,
    val bestResolvedVersion: String?,
    val lowestResolvedVersion: String?,
    val satisfiedVersions: List<String>,
    val rejectedVersions: List<String>,
    val evaluationResults: List<VersionMatchResult>,
    val formattedReport: String,
    val summary: String
)

class SemverRangeSolverTool : Tool<SemverRangeInput, SemverRangeOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "semver_range_solver_tool",
        name = "SemVer Range & Version Solver",
        description = "Resolve and test semantic version ranges (^, ~, >=, <=, ||, wildcards, hyphen ranges) against candidate release versions to determine highest resolved dependencies.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("semver", "npm", "cargo", "version", "range", "resolver", "package manager", "dependency", "comparator"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "GitBranch"
    )

    private fun parseSemver(vStr: String): RangeSemVer? {
        val clean = vStr.trim().removePrefix("v").removePrefix("V")
        val core = clean.substringBefore("-").substringBefore("+")
        val parts = core.split(".")
        if (parts.isEmpty()) return null
        val maj = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val pat = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return RangeSemVer(maj, min, pat, vStr.trim())
    }

    private fun testSingleComparator(v: RangeSemVer, comp: String): Boolean {
        val trimmed = comp.trim()
        if (trimmed == "*" || trimmed == "x" || trimmed == "X" || trimmed.isEmpty()) return true

        return when {
            trimmed.startsWith("^") -> {
                val base = parseSemver(trimmed.substring(1)) ?: return false
                if (v < base) return false
                if (base.major > 0) {
                    v.major == base.major
                } else if (base.minor > 0) {
                    v.major == 0 && v.minor == base.minor
                } else {
                    v.major == 0 && v.minor == 0 && v.patch == base.patch
                }
            }
            trimmed.startsWith("~") -> {
                val base = parseSemver(trimmed.substring(1)) ?: return false
                if (v < base) return false
                v.major == base.major && v.minor == base.minor
            }
            trimmed.startsWith(">=") -> {
                val base = parseSemver(trimmed.substring(2)) ?: return false
                v >= base
            }
            trimmed.startsWith("<=") -> {
                val base = parseSemver(trimmed.substring(2)) ?: return false
                v <= base
            }
            trimmed.startsWith(">") -> {
                val base = parseSemver(trimmed.substring(1)) ?: return false
                v > base
            }
            trimmed.startsWith("<") -> {
                val base = parseSemver(trimmed.substring(1)) ?: return false
                v < base
            }
            trimmed.startsWith("=") -> {
                val base = parseSemver(trimmed.substring(1)) ?: return false
                v.compareTo(base) == 0
            }
            trimmed.contains(".x") || trimmed.contains(".X") || trimmed.contains(".*") -> {
                val prefix = trimmed.substringBefore(".").toIntOrNull() ?: return false
                v.major == prefix
            }
            else -> {
                val base = parseSemver(trimmed) ?: return false
                v.compareTo(base) == 0
            }
        }
    }

    private fun testConjunctiveClause(v: RangeSemVer, clause: String): Boolean {
        // Hyphen range: "1.2.3 - 2.4.0"
        if (clause.contains(" - ")) {
            val parts = clause.split(" - ")
            val low = parseSemver(parts[0]) ?: return false
            val high = parseSemver(parts[1]) ?: return false
            return v >= low && v <= high
        }

        // Space-separated AND conditions: ">=1.0.0 <2.0.0"
        val comparators = clause.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        for (c in comparators) {
            if (!testSingleComparator(v, c)) return false
        }
        return true
    }

    private fun satisfiesRange(v: RangeSemVer, range: String): Boolean {
        // Disjunctive OR clauses: clause1 || clause2
        val disjunctions = range.split("||")
        for (d in disjunctions) {
            if (testConjunctiveClause(v, d.trim())) return true
        }
        return false
    }

    override suspend fun execute(input: SemverRangeInput): ToolResult<SemverRangeOutput> {
        val startTime = System.currentTimeMillis()
        val range = input.rangeExpression.trim()
        if (range.isBlank()) {
            return ToolResult.Failure("SemVer range expression cannot be empty.")
        }

        val candidatesRaw = input.candidateVersions
            .split(",", "\n", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (candidatesRaw.isEmpty()) {
            return ToolResult.Failure("Candidate versions list cannot be empty.")
        }

        val parsedCandidates = mutableListOf<RangeSemVer>()
        for (raw in candidatesRaw) {
            val parsed = parseSemver(raw)
            if (parsed != null) {
                parsedCandidates.add(parsed)
            }
        }

        if (parsedCandidates.isEmpty()) {
            return ToolResult.Failure("No valid semantic versions found in candidates.")
        }

        parsedCandidates.sort()

        val results = mutableListOf<VersionMatchResult>()
        val satisfied = mutableListOf<String>()
        val rejected = mutableListOf<String>()

        for (cand in parsedCandidates) {
            val ok = satisfiesRange(cand, range)
            if (ok) {
                satisfied.add(cand.raw)
                results.add(VersionMatchResult(cand.raw, true, "Satisfies range '$range'"))
            } else {
                rejected.add(cand.raw)
                results.add(VersionMatchResult(cand.raw, false, "Does not satisfy constraints in '$range'"))
            }
        }

        val best = satisfied.lastOrNull()
        val lowest = satisfied.firstOrNull()

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== SEMVER RANGE SOLVER REPORT ===")
            appendLine("Target Range:     $range")
            appendLine("Total Evaluated:  ${parsedCandidates.size} versions")
            appendLine("Matched Versions: ${satisfied.size}")
            appendLine("----------------------------------------")
            appendLine("HIGHEST RESOLVED: ${best ?: "NONE (No compatible release found)"}")
            appendLine("LOWEST MATCH:     ${lowest ?: "NONE"}")
            appendLine("----------------------------------------")
            appendLine("VERSION MATCH DETAILS:")
            results.forEach { r ->
                val badge = if (r.isSatisfied) "[MATCH]   " else "[REJECTED]"
                appendLine("  $badge %-12s : %s".format(Locale.US, r.version, r.reason))
            }
        }

        return ToolResult.Success(
            data = SemverRangeOutput(
                rangeExpression = range,
                bestResolvedVersion = best,
                lowestResolvedVersion = lowest,
                satisfiedVersions = satisfied,
                rejectedVersions = rejected,
                evaluationResults = results,
                formattedReport = report,
                summary = "Resolved range '$range' -> ${best ?: "No match"} (${satisfied.size}/${parsedCandidates.size} candidates matched)."
            ),
            executionTimeMs = elapsed,
            summary = "Resolved: ${best ?: "No match"}"
        )
    }
}
