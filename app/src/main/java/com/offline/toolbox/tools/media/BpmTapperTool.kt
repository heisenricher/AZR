package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class BpmInput(
    val tapTimestampsMs: List<Long> = emptyList(),
    val directBpm: Double? = 120.0
)

data class BpmOutput(
    val calculatedBpm: Double,
    val tempoMarking: String,
    val quarterNoteMs: Double,
    val eighthNoteMs: Double,
    val sixteenthNoteMs: Double,
    val tripletQuarterMs: Double,
    val tapCount: Int,
    val formattedReport: String,
    val summary: String
)

class BpmTapperTool : Tool<BpmInput, BpmOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "bpm_tapper_tool",
        name = "Metronome & BPM Tempo Tapper",
        description = "Calculate Beats Per Minute (BPM), Italian musical tempo markings, and sub-division note intervals in milliseconds.",
        category = ToolCategory.MEDIA,
        tags = listOf("bpm", "tempo", "metronome", "music", "audio", "tap", "beats", "rhythm", "subdivision"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Speed"
    )

    override suspend fun execute(input: BpmInput): ToolResult<BpmOutput> {
        val startTime = System.currentTimeMillis()
        val taps = input.tapTimestampsMs

        val bpm: Double = if (taps.size >= 2) {
            val intervals = taps.zipWithNext { a, b -> b - a }.filter { it > 50 } // filter debounce < 50ms
            if (intervals.isEmpty()) {
                input.directBpm ?: 120.0
            } else {
                val avgInterval = intervals.average()
                if (avgInterval > 0) 60000.0 / avgInterval else 120.0
            }
        } else {
            input.directBpm ?: 120.0
        }

        if (bpm <= 0.0 || bpm > 1000.0) {
            return ToolResult.Failure("BPM value must be between 1 and 1000 (got $bpm).")
        }

        val tempoName = when {
            bpm < 40 -> "Grave (Extremely Slow, Solemn)"
            bpm in 40.0..59.0 -> "Largo (Broad, Slow)"
            bpm in 60.0..75.0 -> "Adagio (Slow, Expressive)"
            bpm in 76.0..107.0 -> "Andante (Walking Pace)"
            bpm in 108.0..119.0 -> "Moderato (Moderate)"
            bpm in 120.0..155.0 -> "Allegro (Fast, Bright)"
            bpm in 156.0..175.0 -> "Vivace (Lively, Fast)"
            bpm in 176.0..199.0 -> "Presto (Very Fast)"
            else -> "Prestissimo (Extremely Rapid)"
        }

        // Note duration intervals in milliseconds
        // Quarter note = 60,000 / BPM
        val quarter = 60000.0 / bpm
        val eighth = quarter / 2.0
        val sixteenth = quarter / 4.0
        val tripletQuarter = (quarter * 2.0) / 3.0

        val report = buildString {
            appendLine("TEMPO & RHYTHMIC TIMING AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Calculated BPM:   ${String.format(Locale.US, "%.1f", bpm)} BPM")
            appendLine("Tempo Marking:    $tempoName")
            appendLine("Tap Samples:      ${if (taps.size >= 2) "${taps.size} taps recorded" else "Direct specification"}")
            appendLine()
            appendLine("NOTE DURATION INTERVALS (Delay/Reverb Timings)")
            appendLine("• Quarter Note (1/4):    ${String.format(Locale.US, "%.1f", quarter)} ms")
            appendLine("• Eighth Note (1/8):     ${String.format(Locale.US, "%.1f", eighth)} ms")
            appendLine("• Sixteenth Note (1/16): ${String.format(Locale.US, "%.1f", sixteenth)} ms")
            appendLine("• Triplet Quarter (1/4T):${String.format(Locale.US, "%.1f", tripletQuarter)} ms")
        }

        val summary = "${String.format(Locale.US, "%.1f", bpm)} BPM ($tempoName)"

        return ToolResult.Success(
            data = BpmOutput(
                calculatedBpm = bpm,
                tempoMarking = tempoName,
                quarterNoteMs = quarter,
                eighthNoteMs = eighth,
                sixteenthNoteMs = sixteenth,
                tripletQuarterMs = tripletQuarter,
                tapCount = taps.size,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
