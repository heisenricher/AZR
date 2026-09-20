package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class MatrixInput(
    val matrixA: String = "1, 2, 3\n0, 1, 4\n5, 6, 0",
    val scalarMultiplier: Double = 2.0
)

data class MatrixOutput(
    val dimension: String,
    val determinant: Double,
    val trace: Double,
    val isInvertible: Boolean,
    val transposeMatrix: String,
    val inverseMatrix: String,
    val scalarMultipliedMatrix: String,
    val formattedReport: String,
    val summary: String
)

class MatrixCalculatorTool : Tool<MatrixInput, MatrixOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "matrix_calculator_tool",
        name = "Matrix Arithmetic & Determinant Calculator",
        description = "Calculate determinants, inverses, transposes, traces, and scalar multiplications for 2x2 and 3x3 matrices.",
        category = ToolCategory.MATH,
        tags = listOf("matrix", "determinant", "inverse", "transpose", "linear algebra", "math", "trace", "vector"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "GridOn"
    )

    override suspend fun execute(input: MatrixInput): ToolResult<MatrixOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.matrixA.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input matrix cannot be empty.")
        }

        val rows = raw.lines().map { line ->
            line.split(",", " ").filter { it.isNotBlank() }.mapNotNull { it.toDoubleOrNull() }
        }.filter { it.isNotEmpty() }

        val rowCount = rows.size
        if (rowCount !in 2..3 || rows.any { it.size != rowCount }) {
            return ToolResult.Failure("Matrix must be a square 2x2 or 3x3 matrix (got ${rowCount}x${rows.firstOrNull()?.size ?: 0}).")
        }

        val n = rowCount
        val m = Array(n) { i -> DoubleArray(n) { j -> rows[i][j] } }

        // Trace (sum of main diagonal)
        var trace = 0.0
        for (i in 0 until n) trace += m[i][i]

        // Transpose
        val trans = Array(n) { i -> DoubleArray(n) { j -> m[j][i] } }

        // Determinant & Inverse
        val det: Double
        val inv: Array<DoubleArray>?

        if (n == 2) {
            det = m[0][0] * m[1][1] - m[0][1] * m[1][0]
            inv = if (det != 0.0) {
                arrayOf(
                    doubleArrayOf(m[1][1] / det, -m[0][1] / det),
                    doubleArrayOf(-m[1][0] / det, m[0][0] / det)
                )
            } else null
        } else {
            // 3x3
            val a = m[0][0]; val b = m[0][1]; val c = m[0][2]
            val d = m[1][0]; val e = m[1][1]; val f = m[1][2]
            val g = m[2][0]; val h = m[2][1]; val k = m[2][2]

            det = a * (e * k - f * h) - b * (d * k - f * g) + c * (d * h - e * g)

            inv = if (det != 0.0) {
                arrayOf(
                    doubleArrayOf((e * k - f * h) / det, (c * h - b * k) / det, (b * f - c * e) / det),
                    doubleArrayOf((f * g - d * k) / det, (a * k - c * g) / det, (c * d - a * f) / det),
                    doubleArrayOf((d * h - e * g) / det, (b * g - a * h) / det, (a * e - b * d) / det)
                )
            } else null
        }

        // Scalar Multiplication
        val scalar = input.scalarMultiplier
        val scaled = Array(n) { i -> DoubleArray(n) { j -> m[i][j] * scalar } }

        fun formatMatrix(mat: Array<DoubleArray>): String {
            return mat.joinToString("\n") { row ->
                "[ " + row.joinToString("\t") { String.format(Locale.US, "%7.2f", it) } + " ]"
            }
        }

        val transStr = formatMatrix(trans)
        val invStr = inv?.let { formatMatrix(it) } ?: "Non-invertible (Determinant is 0)"
        val scaledStr = formatMatrix(scaled)

        val report = buildString {
            appendLine("MATRIX ANALYSIS REPORT (${n}x$n)")
            appendLine("--------------------------------------------------")
            appendLine("Original Matrix:")
            appendLine(formatMatrix(m))
            appendLine()
            appendLine("Determinant:       ${String.format(Locale.US, "%.4f", det)}")
            appendLine("Trace (Tr):        ${String.format(Locale.US, "%.4f", trace)}")
            appendLine("Invertible:        ${if (det != 0.0) "Yes" else "No (Singular Matrix)"}")
            appendLine()
            appendLine("Inverse Matrix (A^-1):")
            appendLine(invStr)
            appendLine()
            appendLine("Transpose Matrix (A^T):")
            appendLine(transStr)
            appendLine()
            appendLine("Scalar Multiplied (${scalar}x):")
            appendLine(scaledStr)
        }

        val summary = "${n}x$n Matrix (Det = ${String.format(Locale.US, "%.2f", det)}, Tr = ${String.format(Locale.US, "%.2f", trace)})"

        return ToolResult.Success(
            data = MatrixOutput(
                dimension = "${n}x$n",
                determinant = det,
                trace = trace,
                isInvertible = det != 0.0,
                transposeMatrix = transStr,
                inverseMatrix = invStr,
                scalarMultipliedMatrix = scaledStr,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
