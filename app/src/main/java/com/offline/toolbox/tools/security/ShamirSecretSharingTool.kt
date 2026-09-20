package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.SecureRandom

enum class ShamirMode {
    SPLIT,
    COMBINE
}

data class ShamirInput(
    val mode: ShamirMode = ShamirMode.SPLIT,
    val secretText: String = "Master Recovery Passphrase 2026",
    val thresholdK: Int = 3,
    val totalSharesN: Int = 5,
    val sharesToCombine: List<String> = emptyList()
)

data class ShamirOutput(
    val mode: ShamirMode,
    val thresholdK: Int,
    val totalSharesN: Int,
    val generatedShares: List<String>,
    val reconstructedSecret: String?,
    val formattedReport: String,
    val summary: String
)

class ShamirSecretSharingTool : Tool<ShamirInput, ShamirOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "shamir_secret_sharing_tool",
        name = "Shamir's Secret Sharing Threshold Cryptography",
        description = "Information-theoretically secure (k, n) threshold secret splitting and reconstruction over Galois Field GF(256).",
        category = ToolCategory.SECURITY,
        tags = listOf("shamir", "secret", "sharing", "threshold", "cryptography", "split", "recovery", "key", "shares"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Security"
    )

    private fun gfAdd(a: Int, b: Int): Int = a xor b
    private fun gfSub(a: Int, b: Int): Int = a xor b

    private fun gfMul(a: Int, b: Int): Int {
        var p = 0
        var x = a and 0xFF
        var y = b and 0xFF
        while (y > 0) {
            if ((y and 1) != 0) p = p xor x
            val hi = x and 0x80
            x = (x shl 1) and 0xFF
            if (hi != 0) x = x xor 0x1B // Irreducible poly 0x11B (AES standard)
            y = y shr 1
        }
        return p
    }

    private fun gfInv(b: Int): Int {
        if (b == 0) throw ArithmeticException("GF(256) division by zero")
        var res = 1
        var base = b and 0xFF
        var exp = 254 // b^254 is inverse by Fermat's Little Theorem in GF(2^8)
        while (exp > 0) {
            if ((exp and 1) != 0) res = gfMul(res, base)
            base = gfMul(base, base)
            exp = exp shr 1
        }
        return res
    }

    private fun gfDiv(a: Int, b: Int): Int {
        return gfMul(a, gfInv(b))
    }

    override suspend fun execute(input: ShamirInput): ToolResult<ShamirOutput> {
        val startTime = System.currentTimeMillis()
        val rng = SecureRandom()

        if (input.mode == ShamirMode.SPLIT) {
            val secretBytes = input.secretText.toByteArray(Charsets.UTF_8)
            if (secretBytes.isEmpty()) {
                return ToolResult.Failure("Secret text cannot be empty.")
            }

            val k = input.thresholdK.coerceIn(2, 10)
            val n = input.totalSharesN.coerceIn(k, 15)

            val shareBuffers = Array(n) { ByteArray(secretBytes.size) }

            for (byteIdx in secretBytes.indices) {
                val secretByte = secretBytes[byteIdx].toInt() and 0xFF
                // Random polynomial coefficients: a0 = secret, a1...ak-1 random
                val coeffs = IntArray(k)
                coeffs[0] = secretByte
                for (c in 1 until k) {
                    coeffs[c] = rng.nextInt(256)
                }

                // Evaluate f(x) for x = 1..n
                for (shareIdx in 0 until n) {
                    val x = shareIdx + 1
                    var y = 0
                    for (pow in k - 1 downTo 0) {
                        y = gfAdd(gfMul(y, x), coeffs[pow])
                    }
                    shareBuffers[shareIdx][byteIdx] = y.toByte()
                }
            }

            val sharesList = mutableListOf<String>()
            for (i in 0 until n) {
                val hexPayload = shareBuffers[i].joinToString("") { "%02x".format(it) }
                sharesList.add("${i + 1}-$hexPayload")
            }

            val report = buildString {
                appendLine("SHAMIR'S SECRET SHARING (SPLIT)")
                appendLine("--------------------------------------------------")
                appendLine("Threshold (k):       $k shares required to reconstruct")
                appendLine("Total Shares (n):    $n shares created")
                appendLine("Secret Byte Length:  ${secretBytes.size} bytes")
                appendLine()
                appendLine("GENERATED CRYPTOGRAPHIC SHARES:")
                sharesList.forEach { appendLine("• $it") }
                appendLine()
                appendLine("Security Guarantee:")
                appendLine("Any $k shares will reconstruct the secret. Fewer than $k shares reveal 0 bits of information.")
            }

            val summary = "Generated $n shares ($k-of-$n threshold)"

            return ToolResult.Success(
                data = ShamirOutput(
                    mode = ShamirMode.SPLIT,
                    thresholdK = k,
                    totalSharesN = n,
                    generatedShares = sharesList,
                    reconstructedSecret = null,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } else {
            // COMBINE MODE
            val sharesInput = if (input.sharesToCombine.isNotEmpty()) {
                input.sharesToCombine
            } else {
                input.secretText.lines().map { it.trim() }.filter { it.contains("-") }
            }

            if (sharesInput.isEmpty()) {
                return ToolResult.Failure("No valid shares provided for combination. Expected format e.g. '1-6a7f80...'")
            }

            val parsedShares = mutableListOf<Pair<Int, ByteArray>>()
            for (s in sharesInput) {
                val parts = s.split("-", limit = 2)
                if (parts.size != 2) continue
                val x = parts[0].toIntOrNull() ?: continue
                val hex = parts[1].trim()
                if (hex.length % 2 != 0) continue
                val bytes = ByteArray(hex.length / 2)
                for (i in bytes.indices) {
                    bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
                parsedShares.add(Pair(x, bytes))
            }

            if (parsedShares.size < 2) {
                return ToolResult.Failure("At least 2 shares required to attempt secret reconstruction.")
            }

            val len = parsedShares[0].second.size
            if (parsedShares.any { it.second.size != len }) {
                return ToolResult.Failure("All shares must have identical payload length.")
            }

            // Lagrange interpolation at x = 0 over GF(256)
            val recovered = ByteArray(len)
            for (byteIdx in 0 until len) {
                var secretByte = 0
                for (i in parsedShares.indices) {
                    val xi = parsedShares[i].first
                    val yi = parsedShares[i].second[byteIdx].toInt() and 0xFF

                    var basis = 1
                    for (j in parsedShares.indices) {
                        if (i == j) continue
                        val xj = parsedShares[j].first
                        // basis *= xj / (xj ^ xi)
                        val num = xj
                        val den = gfSub(xj, xi)
                        basis = gfMul(basis, gfDiv(num, den))
                    }
                    secretByte = gfAdd(secretByte, gfMul(yi, basis))
                }
                recovered[byteIdx] = secretByte.toByte()
            }

            val secretString = String(recovered, Charsets.UTF_8)

            val report = buildString {
                appendLine("SHAMIR'S SECRET SHARING (COMBINE)")
                appendLine("--------------------------------------------------")
                appendLine("Shares Combined:     ${parsedShares.size} shares")
                appendLine("Participating X-IDs: ${parsedShares.map { it.first }.joinToString(", ")}")
                appendLine("Reconstructed Size:  ${recovered.size} bytes")
                appendLine()
                appendLine("RECONSTRUCTED SECRET:")
                appendLine(secretString)
            }

            val summary = "Reconstructed secret (${recovered.size} bytes) from ${parsedShares.size} shares"

            return ToolResult.Success(
                data = ShamirOutput(
                    mode = ShamirMode.COMBINE,
                    thresholdK = parsedShares.size,
                    totalSharesN = parsedShares.size,
                    generatedShares = emptyList(),
                    reconstructedSecret = secretString,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        }
    }
}
