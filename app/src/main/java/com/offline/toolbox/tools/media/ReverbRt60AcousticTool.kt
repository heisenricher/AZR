package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt

data class ReverbRt60Input(
    val lengthMeters: Double = 8.0,
    val widthMeters: Double = 6.0,
    val heightMeters: Double = 3.0,
    val averageAbsorptionCoeff: Double = 0.15, // 0.01 to 0.99
    val roomTypePreset: String = "CUSTOM" // RECORDING_STUDIO, CLASSROOM, CONCERT_HALL, CATHEDRAL, CUSTOM
)

data class ReverbRt60Output(
    val roomVolumeM3: Double,
    val totalSurfaceAreaM2: Double,
    val totalAbsorptionSabins: Double,
    val sabineRt60Seconds: Double,
    val eyringRt60Seconds: Double,
    val criticalDistanceMeters: Double,
    val acousticVerdict: String,
    val formattedReport: String,
    val summary: String
)

class ReverbRt60AcousticTool : Tool<ReverbRt60Input, ReverbRt60Output> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "reverb_rt60_acoustic_tool",
        name = "Acoustic Reverberation Time (RT60) Calculator",
        description = "Calculate architectural Sabine and Norris-Eyring reverberation times (RT60), room mode absorption, and critical listening distance.",
        category = ToolCategory.MEDIA,
        tags = listOf("reverb", "rt60", "acoustics", "audio", "studio", "sabine", "eyring", "sound", "room acoustics"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Volume2"
    )

    override suspend fun execute(input: ReverbRt60Input): ToolResult<ReverbRt60Output> {
        val startTime = System.currentTimeMillis()

        if (input.lengthMeters <= 0.0 || input.widthMeters <= 0.0 || input.heightMeters <= 0.0) {
            return ToolResult.Failure("Room dimensions (length, width, height) must be positive values.")
        }

        val alpha = when (input.roomTypePreset.uppercase(Locale.US)) {
            "RECORDING_STUDIO" -> 0.45
            "CLASSROOM" -> 0.25
            "CONCERT_HALL" -> 0.18
            "CATHEDRAL" -> 0.05
            else -> input.averageAbsorptionCoeff
        }

        if (alpha <= 0.0 || alpha >= 1.0) {
            return ToolResult.Failure("Average sound absorption coefficient (alpha) must be strictly between 0.0 and 1.0.")
        }

        val l = input.lengthMeters
        val w = input.widthMeters
        val h = input.heightMeters

        val volume = l * w * h
        val surfaceArea = 2.0 * (l * w + l * h + w * h)
        val totalAbsorption = surfaceArea * alpha

        // Sabine Formula: RT60 = 0.161 * V / A
        val sabineRt60 = (0.161 * volume) / totalAbsorption

        // Norris-Eyring Formula: RT60 = 0.161 * V / (-S * ln(1 - alpha))
        val eyringRt60 = (0.161 * volume) / (-surfaceArea * ln(1.0 - alpha))

        // Critical distance (distance where direct sound equals reverberant sound, assuming directional factor Q = 2)
        val criticalDistance = 0.057 * sqrt((2.0 * totalAbsorption) / Math.PI)

        val verdict = when {
            eyringRt60 < 0.3 -> "Dry / Dead (Ideal for Voiceover & Vocal Booths)"
            eyringRt60 in 0.3..0.6 -> "Controlled / Semi-Dry (Ideal for Recording Studios & Control Rooms)"
            eyringRt60 in 0.6..1.0 -> "Intelligible Speech (Ideal for Classrooms, Lecture Halls, Conference Rooms)"
            eyringRt60 in 1.0..1.6 -> "Warm Musical Acoustics (Ideal for Chamber Music & Orchestral Halls)"
            eyringRt60 in 1.6..2.4 -> "Enriched Reverberation (Choral & Symphonic Performance Spaces)"
            else -> "Excessively Live / Cavernous (Cathedral acoustics, poor speech clarity)"
        }

        val report = buildString {
            appendLine("ARCHITECTURAL ACOUSTICS & RT60 REVERBERATION REPORT")
            appendLine("--------------------------------------------------")
            appendLine(String.format(Locale.US, "Room Dimensions:       %.2f m (L) × %.2f m (W) × %.2f m (H)", l, w, h))
            appendLine(String.format(Locale.US, "Enclosed Volume:       %.2f m³", volume))
            appendLine(String.format(Locale.US, "Total Surface Area:    %.2f m²", surfaceArea))
            appendLine(String.format(Locale.US, "Absorption Coeff (ᾱ):  %.3f (%s)", alpha, input.roomTypePreset))
            appendLine(String.format(Locale.US, "Total Absorption:      %.2f metric Sabins (m²)", totalAbsorption))
            appendLine("--------------------------------------------------")
            appendLine(String.format(Locale.US, "Sabine RT60:           %.3f seconds", sabineRt60))
            appendLine(String.format(Locale.US, "Norris-Eyring RT60:    %.3f seconds (recommended)", eyringRt60))
            appendLine(String.format(Locale.US, "Critical Distance:     %.2f meters", criticalDistance))
            appendLine("--------------------------------------------------")
            appendLine("ACOUSTIC EVALUATION:")
            appendLine("Verdict: $verdict")
        }

        return ToolResult.Success(
            data = ReverbRt60Output(
                roomVolumeM3 = volume,
                totalSurfaceAreaM2 = surfaceArea,
                totalAbsorptionSabins = totalAbsorption,
                sabineRt60Seconds = sabineRt60,
                eyringRt60Seconds = eyringRt60,
                criticalDistanceMeters = criticalDistance,
                acousticVerdict = verdict,
                formattedReport = report,
                summary = String.format(Locale.US, "RT60: %.2fs (Sabine: %.2fs)", eyringRt60, sabineRt60)
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = String.format(Locale.US, "RT60: %.2f seconds", eyringRt60)
        )
    }
}
