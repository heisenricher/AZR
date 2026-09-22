package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class Argon2Variant(val displayName: String, val description: String) {
    ARGON2ID("Argon2id (Recommended)", "Hybrid variant; resistant to both side-channel and GPU/ASIC attacks. RFC 9106 primary recommendation."),
    ARGON2I("Argon2i (Data-Independent)", "Optimized for password hashing where side-channel timing attacks are the primary threat."),
    ARGON2D("Argon2d (Data-Dependent)", "Maximum resistance against GPU/ASIC cracking. Suitable for cryptocurrencies and non-secret data.")
}

enum class Argon2Profile(val displayName: String) {
    RFC_RECOMMENDED_FIRST("RFC 9106 Default (64 MiB, t=3, p=4)"),
    MOBILE_OPTIMIZED("Mobile / Battery Constrained (19 MiB, t=2, p=2)"),
    HIGH_SECURITY_SERVER("Server Backend (128 MiB, t=4, p=4)"),
    PARANOID_COLD_STORAGE("Paranoid Cold Storage (1024 MiB, t=5, p=8)"),
    CUSTOM("Custom Sizing")
}

data class Argon2CalculatorInput(
    val variant: Argon2Variant = Argon2Variant.ARGON2ID,
    val profile: Argon2Profile = Argon2Profile.RFC_RECOMMENDED_FIRST,
    val customMemoryKiB: Int = 65536,
    val customIterations: Int = 3,
    val customParallelism: Int = 4,
    val saltLengthBytes: Int = 16,
    val hashLengthBytes: Int = 32
)

data class Argon2CalculatorOutput(
    val variant: String,
    val memoryKiB: Int,
    val memoryMiB: Double,
    val iterations: Int,
    val parallelism: Int,
    val memoryBandwidthUsedMB: Double,
    val estimatedExecutionTimeMs: String,
    val asicGpuResistanceScore: String,
    val sideChannelImmunityScore: String,
    val rfcComplianceStatus: String,
    val formattedReport: String,
    val summary: String
)

class Argon2ParameterCalculatorTool : Tool<Argon2CalculatorInput, Argon2CalculatorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "argon2_parameter_calculator_tool",
        name = "Argon2 Parameter Cost & Security Calculator",
        description = "Calculate RFC 9106 Argon2 (id/i/d) memory cost (m), time iterations (t), and parallelism (p) sizing recommendations.",
        category = ToolCategory.SECURITY,
        tags = listOf("argon2", "argon2id", "argon2i", "argon2d", "rfc9106", "password hashing", "kdf", "cryptography", "security"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Security"
    )

    override suspend fun execute(input: Argon2CalculatorInput): ToolResult<Argon2CalculatorOutput> {
        val startTime = System.currentTimeMillis()

        val (mKiB, t, p) = when (input.profile) {
            Argon2Profile.RFC_RECOMMENDED_FIRST -> Triple(65536, 3, 4) // 64 MiB, 3 passes, 4 lanes
            Argon2Profile.MOBILE_OPTIMIZED -> Triple(19456, 2, 2)     // 19 MiB, 2 passes, 2 lanes
            Argon2Profile.HIGH_SECURITY_SERVER -> Triple(131072, 4, 4) // 128 MiB, 4 passes, 4 lanes
            Argon2Profile.PARANOID_COLD_STORAGE -> Triple(1048576, 5, 8) // 1 GiB, 5 passes, 8 lanes
            Argon2Profile.CUSTOM -> {
                val mem = input.customMemoryKiB.coerceAtLeast(8)
                val iter = input.customIterations.coerceAtLeast(1)
                val par = input.customParallelism.coerceIn(1, 64)
                Triple(mem, iter, par)
            }
        }

        val mMiB = mKiB / 1024.0
        val totalBandwidthMB = (mKiB.toLong() * t) / 1024.0

        // Estimated execution time on standard smartphone / modern PC CPU
        // Roughly ~1.5 - 2.5 ms per MiB per iteration across multi-threads
        val estMinMs = ((mMiB * t * 1.5) / (p.coerceAtMost(4) * 0.7)).toInt().coerceAtLeast(2)
        val estMaxMs = ((mMiB * t * 2.8) / (p.coerceAtMost(4) * 0.7)).toInt().coerceAtLeast(estMinMs + 5)
        val timeEstimate = "$estMinMs - $estMaxMs ms"

        val sideChannelImmunity = when (input.variant) {
            Argon2Variant.ARGON2ID -> "High (Hybrid: First half data-independent)"
            Argon2Variant.ARGON2I -> "Maximum (Pure data-independent memory access)"
            Argon2Variant.ARGON2D -> "Low (Data-dependent memory access; susceptible to cache-timing)"
        }

        val asicGpuResistance = when (input.variant) {
            Argon2Variant.ARGON2ID -> "Very High (${String.format(Locale.US, "%.1f", mMiB)} MiB memory lockouts)"
            Argon2Variant.ARGON2I -> "Moderate (Time-memory trade-off attacks possible)"
            Argon2Variant.ARGON2D -> "Maximum (${String.format(Locale.US, "%.1f", mMiB)} MiB memory-hard lockout)"
        }

        val isRfcCompliant = mKiB >= 19456 && t >= 1 && p >= 1 && input.saltLengthBytes >= 16

        val rfcStatus = if (isRfcCompliant) {
            "RFC 9106 Compliant (m >= 19 MiB, salt >= 16 bytes)"
        } else {
            "Sub-optimal (RFC 9106 recommends m >= 19,456 KiB and salt >= 16 bytes)"
        }

        val report = buildString {
            appendLine("ARGON2 PASSWORD HASHING COST ANALYSIS (RFC 9106)")
            appendLine("--------------------------------------------------")
            appendLine("Variant:                ${input.variant.displayName}")
            appendLine("Profile:                ${input.profile.displayName}")
            appendLine("Memory Size (m):        $mKiB KiB (${String.format(Locale.US, "%.2f", mMiB)} MiB)")
            appendLine("Time Cost / Passes (t): $t iteration(s)")
            appendLine("Parallelism Lanes (p):  $p thread(s)")
            appendLine("Salt Length:            ${input.saltLengthBytes} bytes (min 16)")
            appendLine("Output Hash Tag Length: ${input.hashLengthBytes} bytes")
            appendLine("--------------------------------------------------")
            appendLine("ESTIMATED METRICS & SECURITY POSTURE:")
            appendLine("Total Memory Traversed: ${String.format(Locale.US, "%.2f", totalBandwidthMB)} MB")
            appendLine("Est. Processing Time:   $timeEstimate")
            appendLine("Side-Channel Defense:   $sideChannelImmunity")
            appendLine("GPU/ASIC Cracking Cost: $asicGpuResistance")
            appendLine("RFC Status:             $rfcStatus")
            appendLine("--------------------------------------------------")
            appendLine("RECOMMENDED USAGE:")
            appendLine(input.variant.description)
        }

        val output = Argon2CalculatorOutput(
            variant = input.variant.name,
            memoryKiB = mKiB,
            memoryMiB = mMiB,
            iterations = t,
            parallelism = p,
            memoryBandwidthUsedMB = totalBandwidthMB,
            estimatedExecutionTimeMs = timeEstimate,
            asicGpuResistanceScore = asicGpuResistance,
            sideChannelImmunityScore = sideChannelImmunity,
            rfcComplianceStatus = rfcStatus,
            formattedReport = report,
            summary = "Argon2 ($mKiB KiB, t=$t, p=$p): $timeEstimate est. duration"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Calculated Argon2 cost sizing parameters"
        )
    }
}
