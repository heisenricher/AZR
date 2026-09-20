package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.log10

enum class DecibelCalculationType {
    SOUND_PRESSURE_LEVEL,  // dB SPL (re 20 µPa)
    VOLTAGE_DBU,           // dBu (re 0.775 V)
    VOLTAGE_DBV,           // dBV (re 1.0 V)
    POWER_DBM,             // dBm (re 1 mW)
    DISTANCE_ATTENUATION   // Inverse square law
}

data class DecibelInput(
    val calculationType: DecibelCalculationType = DecibelCalculationType.SOUND_PRESSURE_LEVEL,
    val value: Double = 1.0,         // Pa for SPL, Volts for dBu/dBV, mW for dBm, dB SPL for distance
    val initialDistanceMeters: Double = 1.0,
    val targetDistanceMeters: Double = 4.0
)

data class DecibelOutput(
    val calculationType: DecibelCalculationType,
    val decibels: Double,
    val acousticClassification: String,
    val referenceBasis: String,
    val formattedReport: String,
    val summary: String
)

class AudioDecibelCalculatorTool : Tool<DecibelInput, DecibelOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "audio_decibel_calculator_tool",
        name = "Acoustic Decibel & Signal Gain Engine",
        description = "Calculate Sound Pressure Level (dB SPL), audio signal voltage gain (dBu, dBV, dBm), and inverse-square distance loss.",
        category = ToolCategory.MEDIA,
        tags = listOf("decibel", "db", "audio", "sound", "spl", "dbu", "dbv", "dbm", "acoustics", "signal", "loudness"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "VolumeUp"
    )

    override suspend fun execute(input: DecibelInput): ToolResult<DecibelOutput> {
        val startTime = System.currentTimeMillis()
        val v = input.value

        if (v <= 0.0) {
            return ToolResult.Failure("Input physical value must be strictly positive.")
        }

        val db: Double
        val basis: String
        var classification = "Standard Signal / Acoustic Level"

        when (input.calculationType) {
            DecibelCalculationType.SOUND_PRESSURE_LEVEL -> {
                // dB SPL = 20 * log10(P / 20µPa)
                val p0 = 2e-5 // 20 micropascals
                db = 20.0 * log10(v / p0)
                basis = "Sound Pressure Level (re 20 µPa in air)"

                classification = when {
                    db < 20 -> "Faint (Rustling leaves, threshold of hearing)"
                    db in 20.0..39.9 -> "Quiet (Whisper, quiet library at 30 dB)"
                    db in 40.0..59.9 -> "Moderate (Quiet suburban home, light rainfall)"
                    db in 60.0..74.9 -> "Standard (Normal conversation at 60 dB, office)"
                    db in 75.0..84.9 -> "Loud (Busy city traffic, vacuum cleaner)"
                    db in 85.0..99.9 -> "Hazardous (OSHA 8hr Action Level at 85 dB, lawnmower)"
                    db in 100.0..119.9 -> "Extremely Loud (Live concert, chainsaw)"
                    else -> "Pain Threshold (Jet takeoff at 130 dB, hearing damage imminent)"
                }
            }
            DecibelCalculationType.VOLTAGE_DBU -> {
                // dBu = 20 * log10(V / 0.775V)
                val v0 = 0.775
                db = 20.0 * log10(v / v0)
                basis = "Professional Audio Voltage (re 0.775 V into 600 Ω)"
                classification = if (db >= 4.0) "Pro Audio (+4 dBu standard line level)" else "Consumer / Mic Audio Level"
            }
            DecibelCalculationType.VOLTAGE_DBV -> {
                // dBV = 20 * log10(V / 1.0V)
                val v0 = 1.0
                db = 20.0 * log10(v / v0)
                basis = "Consumer Audio Voltage (re 1.0 V RMS)"
                classification = if (db in -12.0..-8.0) "Consumer Line Level (-10 dBV standard)" else "General Voltage Gain"
            }
            DecibelCalculationType.POWER_DBM -> {
                // dBm = 10 * log10(P / 1mW)
                val p0 = 1.0 // 1 milliwatt
                db = 10.0 * log10(v / p0)
                basis = "RF and Audio Signal Power (re 1.0 mW)"
                classification = "Signal Power ($v mW)"
            }
            DecibelCalculationType.DISTANCE_ATTENUATION -> {
                // SPL2 = SPL1 - 20 * log10(d2 / d1)
                val d1 = input.initialDistanceMeters
                val d2 = input.targetDistanceMeters
                if (d1 <= 0.0 || d2 <= 0.0) {
                    return ToolResult.Failure("Distances must be greater than zero.")
                }
                val loss = 20.0 * log10(d2 / d1)
                db = v - loss
                basis = "Distance Inverse-Square Law (-6 dB per distance doubling)"
                classification = "Attenuation: ${String.format(Locale.US, "%.1f", loss)} dB loss from ${d1}m to ${d2}m"
            }
        }

        val report = buildString {
            appendLine("ACOUSTIC DECIBEL & SIGNAL GAIN REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Calculation Mode:    ${input.calculationType.name}")
            appendLine("Reference Basis:     $basis")
            appendLine("Primary Metric:      ${String.format(Locale.US, "%.2f", db)} dB")
            appendLine("Classification:      $classification")
            appendLine()
            appendLine("ACOUSTIC / SIGNAL DETAILS:")
            if (input.calculationType == DecibelCalculationType.DISTANCE_ATTENUATION) {
                appendLine("• Initial Level:     ${String.format(Locale.US, "%.1f", v)} dB at ${input.initialDistanceMeters}m")
                appendLine("• Resulting Level:   ${String.format(Locale.US, "%.1f", db)} dB at ${input.targetDistanceMeters}m")
            } else {
                appendLine("• Input Value:       $v (${if (input.calculationType == DecibelCalculationType.SOUND_PRESSURE_LEVEL) "Pa" else if (input.calculationType == DecibelCalculationType.POWER_DBM) "mW" else "Volts"})")
            }
        }

        val summary = "${String.format(Locale.US, "%.1f", db)} dB (${input.calculationType.name})"

        return ToolResult.Success(
            data = DecibelOutput(
                calculationType = input.calculationType,
                decibels = db,
                acousticClassification = classification,
                referenceBasis = basis,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
