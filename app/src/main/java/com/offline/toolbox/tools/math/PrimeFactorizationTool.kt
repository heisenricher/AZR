package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import kotlin.math.sqrt

data class PrimeFactor(
    val prime: Long,
    val exponent: Int
)

data class PrimeFactorInput(
    val number: Long = 360L
)

data class PrimeFactorOutput(
    val number: Long,
    val isPrime: Boolean,
    val primeFactors: List<PrimeFactor>,
    val canonicalRepresentation: String,
    val allDivisors: List<Long>,
    val divisorCount: Int,
    val eulerTotient: Long,
    val nextPrime: Long,
    val previousPrime: Long?,
    val formattedReport: String,
    val summary: String
)

class PrimeFactorizationTool : Tool<PrimeFactorInput, PrimeFactorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "prime_factorization_tool",
        name = "Prime Factorization & Divisors",
        description = "Decompose numbers into prime factor powers, test primality, list all divisors, and compute Euler's totient.",
        category = ToolCategory.MATH,
        tags = listOf("prime", "factor", "factorization", "divisors", "totient", "math", "euler", "number theory"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Functions"
    )

    override suspend fun execute(input: PrimeFactorInput): ToolResult<PrimeFactorOutput> {
        val startTime = System.currentTimeMillis()
        val n = input.number

        if (n <= 1) {
            return ToolResult.Failure("Prime factorization requires an integer greater than 1 (got $n).")
        }

        val factors = factorize(n)
        val isPrime = (factors.size == 1 && factors[0].exponent == 1)

        val canonical = factors.joinToString(" × ") {
            if (it.exponent == 1) "${it.prime}" else "${it.prime}^${it.exponent}"
        }

        val divisors = findDivisors(n)
        val totient = computeTotient(n, factors)
        val nextP = findNextPrime(n)
        val prevP = findPrevPrime(n)

        val report = buildString {
            appendLine("PRIME FACTORIZATION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Number:             $n")
            appendLine("Classification:     ${if (isPrime) "PRIME NUMBER" else "COMPOSITE NUMBER"}")
            appendLine("Prime Factors:      $canonical")
            appendLine("Euler's Totient φ:  $totient")
            appendLine("Total Divisors:     ${divisors.size}")
            appendLine("All Divisors:       ${divisors.joinToString(", ")}")
            appendLine("Next Prime:         $nextP")
            if (prevP != null) {
                appendLine("Previous Prime:     $prevP")
            }
        }

        val summary = if (isPrime) "$n is Prime" else "$n = $canonical"

        return ToolResult.Success(
            data = PrimeFactorOutput(
                number = n,
                isPrime = isPrime,
                primeFactors = factors,
                canonicalRepresentation = canonical,
                allDivisors = divisors,
                divisorCount = divisors.size,
                eulerTotient = totient,
                nextPrime = nextP,
                previousPrime = prevP,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun factorize(num: Long): List<PrimeFactor> {
        var n = num
        val result = mutableListOf<PrimeFactor>()

        // Check 2
        var count2 = 0
        while (n % 2L == 0L) {
            count2++
            n /= 2L
        }
        if (count2 > 0) result.add(PrimeFactor(2L, count2))

        // Check odd numbers up to sqrt(n)
        var d = 3L
        while (d * d <= n) {
            var count = 0
            while (n % d == 0L) {
                count++
                n /= d
            }
            if (count > 0) result.add(PrimeFactor(d, count))
            d += 2L
        }

        if (n > 1L) {
            result.add(PrimeFactor(n, 1))
        }

        return result
    }

    private fun findDivisors(n: Long): List<Long> {
        val divs = mutableListOf<Long>()
        val limit = sqrt(n.toDouble()).toLong()
        for (i in 1L..limit) {
            if (n % i == 0L) {
                divs.add(i)
                if (i * i != n) {
                    divs.add(n / i)
                }
            }
        }
        return divs.sorted()
    }

    private fun computeTotient(n: Long, factors: List<PrimeFactor>): Long {
        var res = n
        for (f in factors) {
            res = res / f.prime * (f.prime - 1L)
        }
        return res
    }

    private fun isPrimeNumber(n: Long): Boolean {
        if (n < 2) return false
        if (n == 2L || n == 3L) return true
        if (n % 2L == 0L || n % 3L == 0L) return false
        var i = 5L
        while (i * i <= n) {
            if (n % i == 0L || n % (i + 2L) == 0L) return false
            i += 6L
        }
        return true
    }

    private fun findNextPrime(n: Long): Long {
        var cur = n + 1
        while (!isPrimeNumber(cur)) {
            cur++
        }
        return cur
    }

    private fun findPrevPrime(n: Long): Long? {
        var cur = n - 1
        while (cur >= 2) {
            if (isPrimeNumber(cur)) return cur
            cur--
        }
        return null
    }
}
