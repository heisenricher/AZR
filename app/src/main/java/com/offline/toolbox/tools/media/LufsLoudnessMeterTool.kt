package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class LufsLoudnessInput(
    val integratedLufs: Double = -11.5,
    val truePeakDbtp: Double = -0.3,
    val loudnessRangeLu: Double = 6.5,
    val platformTarget: String = "SPOTIFY" // SPOTIFY, APPLE_MUSIC, YOUTUBE, EBU_R128, ATSC_A85
)

data class PlatformCompliance(
    val platformName: String,
    val targetLufs: Double,
    val maxPeakDbtp: Double,
    val gainAdjustmentDb: Double,
    val peakPenaltyWarning: Boolean,
    val isCompliant: Boolean
)

data class LufsLoudnessOutput(
    val integratedLufs: Double,
    val truePeakDbtp: Double,
    val loudnessRangeLu: Double,
    val selectedPlatform: String,
    val gainAdjustmentDb: Double,
    val dynamicRating: String,
    val intersampleClipRisk: Boolean,
    val platformEvaluations: List<PlatformCompliance>,
    val formattedReport: String,
    val summary: String
)

class LufsLoudnessMeterTool : Tool<LufsLoudnessInput, LufsLoudnessOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "lufs_loudness_meter_tool",
        name = "Broadcast Audio LUFS & True Peak Normalizer",
        description = "Evaluate audio loudness (ITU-R BS.1770 / EBU R128) across streaming platforms (Spotify, Apple Music, YouTube) to predict normalization gain penalties and inter-sample clipping.",
        category = ToolCategory.MEDIA,
        tags = listOf("lufs", "ebu r128", "audio", "loudness", "mastering", "spotify", "true peak", "acoustics", "broadcast", "music production"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Volume2"
    )

    private data class TargetStandard(val name: String, val targetLufs: Double, val maxPeak: Double)

    private val platformStandards = listOf(
        TargetStandard("Spotify (Standard)", -14.0, -1.0),
        TargetStandard("Apple Music (Sound Check)", -16.0, -1.0),
        TargetStandard("YouTube", -14.0, -1.0),
        TargetStandard("Tidal", -14.0, -1.0),
        TargetStandard("Amazon Music", -14.0, -1.0),
        TargetStandard("EBU R128 (European Broadcast)", -23.0, -1.0),
        TargetStandard("ATSC A/85 (US Broadcast)", -24.0, -2.0)
    )

    override suspend fun execute(input: LufsLoudnessInput): ToolResult<LufsLoudnessOutput> {
        val startTime = System.currentTimeMillis()
        val lufs = input.integratedLufs
        val tp = input.truePeakDbtp
        val lra = input.loudnessRangeLu.coerceAtLeast(0.0)

        val clipRisk = tp > -1.0

        val evals = platformStandards.map { std ->
            val gainDiff = std.targetLufs - lufs
            val exceedsPeak = tp > std.maxPeak
            val compliant = kotlin.math.abs(gainDiff) <= 0.5 && !exceedsPeak
            PlatformCompliance(
                platformName = std.name,
                targetLufs = std.targetLufs,
                maxPeakDbtp = std.maxPeak,
                gainAdjustmentDb = gainDiff,
                peakPenaltyWarning = exceedsPeak,
                isCompliant = compliant
            )
        }

        val selectedKey = input.platformTarget.trim().uppercase(Locale.US)
        val selectedEval = evals.find {
            val norm = it.platformName.uppercase(Locale.US)
            norm.contains(selectedKey) || norm.replace(" ", "_").contains(selectedKey) || norm.contains(selectedKey.replace("_", " "))
        } ?: evals.first()

        val dynamicGrade = when {
            lra < 4.0 -> "Hyper-Compressed / Severe Loudness War (LRA < 4 LU)"
            lra in 4.0..7.9 -> "Commercial Pop / Electronic Master (LRA 4-8 LU)"
            lra in 8.0..12.0 -> "Dynamic Rock / Acoustic Master (LRA 8-12 LU)"
            else -> "Wide Dynamic Range / Classical / Cinematic (LRA > 12 LU)"
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== AUDIO LOUDNESS & STREAMING COMPLIANCE (ITU-R BS.1770) ===")
            appendLine("Integrated Loudness: ${String.format(Locale.US, "%.1f", lufs)} LUFS")
            appendLine("Max True Peak:       ${String.format(Locale.US, "%.1f", tp)} dBTP")
            appendLine("Loudness Range (LRA): ${String.format(Locale.US, "%.1f", lra)} LU")
            appendLine("Dynamic Profile:     $dynamicGrade")
            if (clipRisk) {
                appendLine("[WARNING] True peak (${String.format(Locale.US, "%.1f", tp)} dBTP) exceeds -1.0 dBTP ceiling. High risk of lossy inter-sample clipping on MP3/AAC encoders!")
            }
            appendLine("----------------------------------------")
            appendLine("TARGET PLATFORM: ${selectedEval.platformName}")
            val adjStr = if (selectedEval.gainAdjustmentDb < 0) {
                "${String.format(Locale.US, "%.1f", selectedEval.gainAdjustmentDb)} dB (Turned DOWN)"
            } else if (selectedEval.gainAdjustmentDb > 0) {
                "+${String.format(Locale.US, "%.1f", selectedEval.gainAdjustmentDb)} dB (Turned UP / Limiter may engage)"
            } else "0.0 dB (Matched)"
            appendLine("Predicted Platform Gain: $adjStr")
            appendLine("----------------------------------------")
            appendLine("STREAMING & BROADCAST COMPARISON MATRIX:")
            appendLine("%-28s | %-10s | %-12s | %s".format(Locale.US, "Platform", "Target", "Gain Change", "Status"))
            evals.forEach { e ->
                val status = if (e.gainAdjustmentDb < 0) "ATTENUATED" else if (e.gainAdjustmentDb > 0) "BOOSTED" else "OK"
                val clipNote = if (e.peakPenaltyWarning) " [CLIP RISK]" else ""
                appendLine("%-28s | %-6.1f LUFS | %-+6.1f dB   | %s%s".format(Locale.US, e.platformName, e.targetLufs, e.gainAdjustmentDb, status, clipNote))
            }
        }

        return ToolResult.Success(
            data = LufsLoudnessOutput(
                integratedLufs = lufs,
                truePeakDbtp = tp,
                loudnessRangeLu = lra,
                selectedPlatform = selectedEval.platformName,
                gainAdjustmentDb = selectedEval.gainAdjustmentDb,
                dynamicRating = dynamicGrade,
                intersampleClipRisk = clipRisk,
                platformEvaluations = evals,
                formattedReport = report,
                summary = "${String.format(Locale.US, "%.1f", lufs)} LUFS -> ${selectedEval.platformName} adjustment: ${String.format(Locale.US, "%+.1f", selectedEval.gainAdjustmentDb)} dB."
            ),
            executionTimeMs = elapsed,
            summary = "${String.format(Locale.US, "%.1f", lufs)} LUFS (${selectedEval.platformName}: ${String.format(Locale.US, "%+.1f", selectedEval.gainAdjustmentDb)} dB)"
        )
    }
}
