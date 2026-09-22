package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class ScryptCostInput(
    val nCostFactor: Long = 16384L, // N: must be power of 2 (e.g. 16384, 32768, 65536, 1048576)
    val rBlockSize: Int = 8,        // r: default 8
    val pParallelism: Int = 1,      // p: default 1
    val keyLengthBytes: Int = 32
)

data class ScryptCostOutput(
    val n: Long,
    val r: Int,
    val p: Int,
    val keyLength: Int,
    val memorySizeBytes: Long,
    val memorySizeMib: Double,
    val memorySizeGib: Double,
    val isPowerOfTwo: Boolean,
    val securityLevelGrade: String,
    val targetUseProfile: String,
    val formattedReport: String,
    val summary: String
)

class ScryptCostEstimatorTool : Tool<ScryptCostInput, ScryptCostOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "scrypt_cost_estimator_tool",
        name = "scrypt (RFC 7914) Memory Cost & Security Sizing",
        description = "Calculate exact RAM requirements (128 · r · N), CPU iterations, ASIC resistance scores, and mobile compatibility budgets for scrypt key derivation.",
        category = ToolCategory.SECURITY,
        tags = listOf("scrypt", "kdf", "cryptography", "rfc7914", "memory hard", "asic", "password hashing", "security", "argon2"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Key"
    )

    private fun isPowerOfTwo(x: Long): Boolean = x > 1 && (x and (x - 1L)) == 0L

    override suspend fun execute(input: ScryptCostInput): ToolResult<ScryptCostOutput> {
        val startTime = System.currentTimeMillis()
        val n = input.nCostFactor
        val r = input.rBlockSize
        val p = input.pParallelism
        val keyLen = input.keyLengthBytes

        if (n <= 1) {
            return ToolResult.Failure("Cost factor N must be greater than 1.")
        }
        if (r <= 0) {
            return ToolResult.Failure("Block size r must be greater than 0.")
        }
        if (p <= 0) {
            return ToolResult.Failure("Parallelism p must be greater than 0.")
        }

        val powerOf2 = isPowerOfTwo(n)
        if (!powerOf2) {
            return ToolResult.Failure("Cost parameter N ($n) must be an exact power of 2 (e.g. 1024, 2048, 4096, 16384, 32768, 65536, 1048576).")
        }

        // Memory size in bytes = 128 * r * N
        val memBytes = 128L * r.toLong() * n
        val memMib = memBytes.toDouble() / (1024.0 * 1024.0)
        val memGib = memBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

        val profile = when {
            memMib <= 16.0 -> "Interactive Mobile / Low-Memory Clients (≤ 16 MiB)"
            memMib <= 64.0 -> "Standard Web Service / Desktop App (16 - 64 MiB)"
            memMib <= 256.0 -> "High-Security Sensitive Authentication (64 - 256 MiB)"
            else -> "Cold-Storage Master Key / Long-Term Archive File Vault (> 256 MiB)"
        }

        val securityGrade = when {
            n < 16384L -> "Low (Legacy / vulnerable to modern GPU parallel brute-force)"
            n in 16384L..32767L -> "Moderate (Standard interactive authentication)"
            n in 32768L..262143L -> "Strong (High ASIC resistance, recommended for web servers)"
            else -> "Military-Grade / Quantum-Era (Extreme ASIC/FPGA memory cost penalty)"
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== SCRYPT (RFC 7914) PARAMETER SIZING ===")
            appendLine("Parameters:")
            appendLine("  N (CPU/Memory Cost): $n (2^${(kotlin.math.log2(n.toDouble())).toInt()})")
            appendLine("  r (Block Size):      $r")
            appendLine("  p (Parallelization): $p")
            appendLine("  Key Length:          $keyLen bytes (${keyLen * 8} bits)")
            appendLine("----------------------------------------")
            appendLine("MEMORY REQUIREMENTS:")
            appendLine("  Total Bytes:  %,d bytes".format(Locale.US, memBytes))
            appendLine("  Megabytes:    ${String.format(Locale.US, "%.2f", memMib)} MiB")
            appendLine("  Gigabytes:    ${String.format(Locale.US, "%.4f", memGib)} GiB")
            appendLine("----------------------------------------")
            appendLine("SECURITY EVALUATION:")
            appendLine("  Security Rating: $securityGrade")
            appendLine("  Target Profile:  $profile")
            appendLine("  ASIC Resistance: High (Memory-Hard sequential memory lookups)")
            if (memMib > 128.0) {
                appendLine("  [WARNING] High memory consumption (${String.format(Locale.US, "%.1f", memMib)} MiB) may cause Out-Of-Memory (OOM) kills on low-end mobile devices.")
            }
        }

        return ToolResult.Success(
            data = ScryptCostOutput(
                n = n,
                r = r,
                p = p,
                keyLength = keyLen,
                memorySizeBytes = memBytes,
                memorySizeMib = memMib,
                memorySizeGib = memGib,
                isPowerOfTwo = true,
                securityLevelGrade = securityGrade,
                targetUseProfile = profile,
                formattedReport = report,
                summary = "scrypt requires ${String.format(Locale.US, "%.1f", memMib)} MiB RAM (N=$n, r=$r, p=$p) [$securityGrade]."
            ),
            executionTimeMs = elapsed,
            summary = "RAM: ${String.format(Locale.US, "%.1f", memMib)} MiB ($securityGrade)"
        )
    }
}
