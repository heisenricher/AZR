package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

data class FrequencyNoteInput(
    val frequencyHz: Double = 440.0,
    val referenceA4Hz: Double = 440.0
)

data class FrequencyNoteOutput(
    val inputFrequencyHz: Double,
    val noteName: String,
    val octave: Int,
    val fullNoteLabel: String,
    val midiNoteNumber: Int,
    val exactNoteFrequencyHz: Double,
    val centsDeviation: Double,
    val tuningStatus: String,
    val formattedReport: String,
    val summary: String
)

class FrequencyToNoteTool : Tool<FrequencyNoteInput, FrequencyNoteOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "frequency_to_note_tool",
        name = "Audio Frequency & Musical Note Tuner",
        description = "Translate acoustic frequency in Hertz (Hz) to musical note names, octaves, MIDI numbers, and pitch cents tuning offsets.",
        category = ToolCategory.MEDIA,
        tags = listOf("audio", "frequency", "hertz", "note", "music", "tuner", "midi", "pitch", "cents", "sound"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "MusicNote"
    )

    private val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    override suspend fun execute(input: FrequencyNoteInput): ToolResult<FrequencyNoteOutput> {
        val startTime = System.currentTimeMillis()
        val freq = input.frequencyHz
        val a4 = if (input.referenceA4Hz in 400.0..480.0) input.referenceA4Hz else 440.0

        if (freq <= 0.0) {
            return ToolResult.Failure("Frequency must be greater than 0 Hz (got $freq Hz).")
        }

        // MIDI formula: n = 69 + 12 * log2(f / 440)
        // log2(x) = ln(x) / ln(2)
        val midiFloat = 69.0 + 12.0 * (ln(freq / a4) / ln(2.0))
        val midiRounded = midiFloat.roundToInt().coerceIn(0, 127)

        val cents = (midiFloat - midiRounded) * 100.0
        val exactFreq = a4 * 2.0.pow((midiRounded - 69) / 12.0)

        // Note index (0 = C, 9 = A)
        val noteIdx = midiRounded % 12
        val octave = (midiRounded / 12) - 1
        val noteName = noteNames[noteIdx]
        val fullNote = "$noteName$octave"

        val tuningStatus = when {
            kotlin.math.abs(cents) < 3.0 -> "In Tune (Perfect Pitch)"
            cents > 0 -> "+${String.format(Locale.US, "%.1f", cents)} cents Sharp (♯)"
            else -> "${String.format(Locale.US, "%.1f", cents)} cents Flat (♭)"
        }

        val report = buildString {
            appendLine("ACOUSTIC FREQUENCY & MUSICAL NOTE REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Input Frequency:  ${String.format(Locale.US, "%.2f", freq)} Hz")
            appendLine("A4 Reference:     ${String.format(Locale.US, "%.1f", a4)} Hz")
            appendLine("Closest Note:     $fullNote ($noteName in Octave $octave)")
            appendLine("MIDI Number:      $midiRounded")
            appendLine("Target Frequency: ${String.format(Locale.US, "%.2f", exactFreq)} Hz")
            appendLine("Cents Offset:     ${String.format(Locale.US, "%+.1f", cents)} cents")
            appendLine("Tuning Status:    $tuningStatus")
        }

        val summary = "$fullNote (${String.format(Locale.US, "%.1f", freq)} Hz, $tuningStatus)"

        return ToolResult.Success(
            data = FrequencyNoteOutput(
                inputFrequencyHz = freq,
                noteName = noteName,
                octave = octave,
                fullNoteLabel = fullNote,
                midiNoteNumber = midiRounded,
                exactNoteFrequencyHz = exactFreq,
                centsDeviation = cents,
                tuningStatus = tuningStatus,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
