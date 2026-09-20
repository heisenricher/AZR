package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.log2
import kotlin.math.pow

enum class MusicalInterval(val displayName: String, val semitones: Int, val justRatioNum: Int, val justRatioDen: Int) {
    UNISON("Perfect Unison (P1)", 0, 1, 1),
    MINOR_SECOND("Minor Second (m2)", 1, 16, 15),
    MAJOR_SECOND("Major Second (M2)", 2, 9, 8),
    MINOR_THIRD("Minor Third (m3)", 3, 6, 5),
    MAJOR_THIRD("Major Third (M3)", 4, 5, 4),
    PERFECT_FOURTH("Perfect Fourth (P4)", 5, 4, 3),
    TRITONE("Augmented Fourth / Diminished Fifth (TT)", 6, 45, 32),
    PERFECT_FIFTH("Perfect Fifth (P5)", 7, 3, 2),
    MINOR_SIXTH("Minor Sixth (m6)", 8, 8, 5),
    MAJOR_SIXTH("Major Sixth (M6)", 9, 5, 3),
    MINOR_SEVENTH("Minor Seventh (m7)", 10, 9, 5),
    MAJOR_SEVENTH("Major Seventh (M7)", 11, 15, 8),
    OCTAVE("Perfect Octave (P8)", 12, 2, 1)
}

data class AudioIntervalInput(
    val baseFrequencyHz: Double = 440.0, // A4
    val interval: MusicalInterval = MusicalInterval.PERFECT_FIFTH
)

data class AudioIntervalOutput(
    val baseFrequencyHz: Double,
    val intervalName: String,
    val equalTemperamentHz: Double,
    val justIntonationHz: Double,
    val centsDivergence: Double,
    val justRatio: String,
    val formattedReport: String,
    val summary: String
)

class AudioFrequencyIntervalTool : Tool<AudioIntervalInput, AudioIntervalOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "audio_frequency_interval_tool",
        name = "Musical Interval & Acoustic Harmony Calculator",
        description = "Compute 12-TET vs Just Intonation acoustic frequencies, harmonic ratios, and cents discrepancy.",
        category = ToolCategory.MEDIA,
        tags = listOf("audio", "frequency", "interval", "harmony", "music", "tuning", "cents", "pitch", "temperament"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Audiotrack"
    )

    override suspend fun execute(input: AudioIntervalInput): ToolResult<AudioIntervalOutput> {
        val startTime = System.currentTimeMillis()
        val f0 = input.baseFrequencyHz

        if (f0 <= 0.0 || f0 > 24000.0) {
            return ToolResult.Failure("Base frequency must be between 1 Hz and 24,000 Hz (got $f0 Hz).")
        }

        val inter = input.interval

        // 12-TET frequency: f0 * 2^(semitones / 12)
        val fTet = f0 * 2.0.pow(inter.semitones / 12.0)

        // Just intonation frequency: f0 * (num / den)
        val justRatioVal = inter.justRatioNum.toDouble() / inter.justRatioDen.toDouble()
        val fJust = f0 * justRatioVal

        // Cents divergence: 1200 * log2(fJust / fTet)
        val centsDiv = 1200.0 * log2(fJust / fTet)

        val report = buildString {
            appendLine("ACOUSTIC HARMONY & INTERVAL ANALYSIS")
            appendLine("--------------------------------------------------")
            appendLine("Base Pitch (f0):      ${String.format(Locale.US, "%.2f", f0)} Hz")
            appendLine("Musical Interval:     ${inter.displayName}")
            appendLine("Semitone Distance:    ${inter.semitones} semitones")
            appendLine("Just Harmonic Ratio:  ${inter.justRatioNum}:${inter.justRatioDen}")
            appendLine()
            appendLine("FREQUENCY COMPARISON:")
            appendLine("• Equal Temp (12-TET): ${String.format(Locale.US, "%.2f", fTet)} Hz")
            appendLine("• Just Intonation:     ${String.format(Locale.US, "%.2f", fJust)} Hz")
            appendLine("• Cents Divergence:    ${String.format(Locale.US, "%+.2f", centsDiv)} cents")
            appendLine()
            appendLine("Harmonic Note:")
            if (centsDiv > 0) {
                appendLine("Just Intonation is sharp by ${String.format(Locale.US, "%.1f", centsDiv)} cents compared to modern 12-TET.")
            } else if (centsDiv < 0) {
                appendLine("Just Intonation is flat by ${String.format(Locale.US, "%.1f", -centsDiv)} cents compared to modern 12-TET.")
            } else {
                appendLine("Exact harmonic consensus (0 cents difference).")
            }
        }

        val summary = "${inter.name}: 12-TET ${String.format(Locale.US, "%.1f", fTet)}Hz vs Just ${String.format(Locale.US, "%.1f", fJust)}Hz (${String.format(Locale.US, "%+.1f", centsDiv)}¢)"

        return ToolResult.Success(
            data = AudioIntervalOutput(
                baseFrequencyHz = f0,
                intervalName = inter.displayName,
                equalTemperamentHz = fTet,
                justIntonationHz = fJust,
                centsDivergence = centsDiv,
                justRatio = "${inter.justRatioNum}:${inter.justRatioDen}",
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
