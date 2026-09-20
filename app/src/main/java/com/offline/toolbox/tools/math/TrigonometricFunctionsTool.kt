package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan
import kotlin.math.tanh

enum class AngleUnit {
    DEGREES,
    RADIANS,
    GRADIANS
}

data class TrigInput(
    val angleValue: Double = 45.0,
    val unit: AngleUnit = AngleUnit.DEGREES
)

data class TrigOutput(
    val inputAngle: Double,
    val unit: AngleUnit,
    val radians: Double,
    val degrees: Double,
    val gradians: Double,
    val sinVal: Double,
    val cosVal: Double,
    val tanVal: Double?,
    val secVal: Double?,
    val cscVal: Double?,
    val cotVal: Double?,
    val sinhVal: Double,
    val coshVal: Double,
    val tanhVal: Double,
    val formattedReport: String,
    val summary: String
)

class TrigonometricFunctionsTool : Tool<TrigInput, TrigOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "trigonometric_functions_tool",
        name = "Exact Trigonometric & Hyperbolic Calculator",
        description = "Calculate circular (sin, cos, tan, sec, csc, cot) and hyperbolic (sinh, cosh, tanh) functions with radians/degrees/gradians conversions.",
        category = ToolCategory.MATH,
        tags = listOf("trigonometry", "sin", "cos", "tan", "sec", "csc", "cot", "sinh", "cosh", "tanh", "radians", "degrees", "angle"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Architecture"
    )

    override suspend fun execute(input: TrigInput): ToolResult<TrigOutput> {
        val startTime = System.currentTimeMillis()
        val rawAngle = input.angleValue

        // Normalize to radians
        val rad: Double = when (input.unit) {
            AngleUnit.RADIANS -> rawAngle
            AngleUnit.DEGREES -> rawAngle * (PI / 180.0)
            AngleUnit.GRADIANS -> rawAngle * (PI / 200.0)
        }

        val deg = rad * (180.0 / PI)
        val grad = rad * (200.0 / PI)

        // Circular functions with zero-clamping
        var s = sin(rad)
        var c = cos(rad)
        if (abs(s) < 1e-12) s = 0.0
        if (abs(c) < 1e-12) c = 0.0

        val t: Double? = if (abs(c) < 1e-12) null else s / c
        val sec: Double? = if (abs(c) < 1e-12) null else 1.0 / c
        val csc: Double? = if (abs(s) < 1e-12) null else 1.0 / s
        val cot: Double? = if (abs(s) < 1e-12) null else c / s

        // Hyperbolic
        val sh = sinh(rad)
        val ch = cosh(rad)
        val th = tanh(rad)

        fun fmt(v: Double?): String = v?.let { String.format(Locale.US, "%.6f", it) } ?: "Undefined (Asymptote)"

        val report = buildString {
            appendLine("TRIGONOMETRIC & HYPERBOLIC REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Input Angle:         ${String.format(Locale.US, "%.4f", rawAngle)} ${input.unit.name.lowercase()}")
            appendLine("Normalized Radians:  ${String.format(Locale.US, "%.6f", rad)} rad (${String.format(Locale.US, "%.4f", rad / PI)}π)")
            appendLine("Degrees Equivalent:  ${String.format(Locale.US, "%.4f", deg)}°")
            appendLine("Gradians Equivalent: ${String.format(Locale.US, "%.4f", grad)} grad")
            appendLine()
            appendLine("PRIMARY CIRCULAR FUNCTIONS:")
            appendLine("• sin(θ): ${fmt(s)}")
            appendLine("• cos(θ): ${fmt(c)}")
            appendLine("• tan(θ): ${fmt(t)}")
            appendLine()
            appendLine("RECIPROCAL CIRCULAR FUNCTIONS:")
            appendLine("• csc(θ): ${fmt(csc)}")
            appendLine("• sec(θ): ${fmt(sec)}")
            appendLine("• cot(θ): ${fmt(cot)}")
            appendLine()
            appendLine("HYPERBOLIC FUNCTIONS:")
            appendLine("• sinh(θ): ${fmt(sh)}")
            appendLine("• cosh(θ): ${fmt(ch)}")
            appendLine("• tanh(θ): ${fmt(th)}")
        }

        val summary = "sin: ${fmt(s)}, cos: ${fmt(c)}, tan: ${fmt(t)}"

        return ToolResult.Success(
            data = TrigOutput(
                inputAngle = rawAngle,
                unit = input.unit,
                radians = rad,
                degrees = deg,
                gradians = grad,
                sinVal = s,
                cosVal = c,
                tanVal = t,
                secVal = sec,
                cscVal = csc,
                cotVal = cot,
                sinhVal = sh,
                coshVal = ch,
                tanhVal = th,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
