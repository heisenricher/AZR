package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.abs

enum class BrainwaveBand(
    val displayName: String,
    val rangeHz: String,
    val mentalState: String
) {
    INFRA_LOW("Infra-Low (< 0.5 Hz)", "< 0.5 Hz", "Sub-perceptual grounding, baseline neurological oscillation."),
    DELTA("Delta (0.5 - 4.0 Hz)", "0.5 - 4.0 Hz", "Deep dreamless sleep, somatic cell restoration, immune boost."),
    THETA("Theta (4.0 - 8.0 Hz)", "4.0 - 8.0 Hz", "Deep meditation, REM dreaming, hypnagogia, intuition, memory consolidation."),
    ALPHA("Alpha (8.0 - 13.0 Hz)", "8.0 - 13.0 Hz", "Relaxed alertness, calm focus, flow state, anxiety relief."),
    BETA("Beta (13.0 - 30.0 Hz)", "13.0 - 30.0 Hz", "Active problem solving, conscious analytical reasoning, alertness."),
    GAMMA("Gamma (30.0 - 100.0 Hz)", "30.0 - 100.0 Hz", "Peak cognitive performance, multi-sensory binding, insight, hyper-focus."),
    ACOUSTIC_ROUGHNESS("Audible Flutter / Chord (> 100 Hz)", "> 100 Hz", "Exceeds neuro-entrainment range; perceived as acoustic roughness or musical interval.")
}

data class BeatsBinauralInput(
    val leftChannelHz: Double = 432.0,   // Frequency 1
    val rightChannelHz: Double = 440.0   // Frequency 2 (8 Hz difference = Alpha)
)

data class BeatsBinauralOutput(
    val freq1Hz: Double,
    val freq2Hz: Double,
    val carrierFreqHz: Double,
    val beatFreqHz: Double,
    val beatPeriodMs: Double,
    val brainwaveBand: BrainwaveBand,
    val isTrueBinauralRange: Boolean,
    val formattedReport: String,
    val summary: String
)

class BeatsBinauralAcousticTool : Tool<BeatsBinauralInput, BeatsBinauralOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "beats_binaural_acoustic_tool",
        name = "Acoustic Beat & Binaural Frequency Analyzer",
        description = "Calculate interference beat frequencies (|f1 - f2|), carrier frequency, and classify brainwave entrainment states (Delta, Theta, Alpha, Beta, Gamma).",
        category = ToolCategory.MEDIA,
        tags = listOf("binaural", "beats", "acoustics", "frequency", "brainwave", "sound", "alpha", "theta", "delta", "beta", "gamma", "interference"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Headphones"
    )

    override suspend fun execute(input: BeatsBinauralInput): ToolResult<BeatsBinauralOutput> {
        val startTime = System.currentTimeMillis()
        val f1 = input.leftChannelHz
        val f2 = input.rightChannelHz

        if (f1 <= 0.0 || f2 <= 0.0) {
            return ToolResult.Failure("Both channel frequencies must be strictly greater than 0 Hz.")
        }

        val beat = abs(f1 - f2)
        val carrier = (f1 + f2) / 2.0
        val periodMs = if (beat > 0.0001) (1000.0 / beat) else 0.0

        val band = when {
            beat < 0.5 -> BrainwaveBand.INFRA_LOW
            beat < 4.0 -> BrainwaveBand.DELTA
            beat < 8.0 -> BrainwaveBand.THETA
            beat < 13.0 -> BrainwaveBand.ALPHA
            beat < 30.0 -> BrainwaveBand.BETA
            beat <= 100.0 -> BrainwaveBand.GAMMA
            else -> BrainwaveBand.ACOUSTIC_ROUGHNESS
        }

        // True binaural beat perception in humans occurs when carrier < 1000 Hz and beat < 30 Hz
        val isTrueBinaural = carrier < 1000.0 && beat <= 30.0

        val report = buildString {
            appendLine("ACOUSTIC INTERFERENCE & BINAURAL BEAT REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Left Channel (f1):        ${String.format(Locale.US, "%.2f Hz", f1)}")
            appendLine("Right Channel (f2):       ${String.format(Locale.US, "%.2f Hz", f2)}")
            appendLine("Carrier Frequency (fc):   ${String.format(Locale.US, "%.2f Hz", carrier)}")
            appendLine("Beat Frequency (f_beat):  ${String.format(Locale.US, "%.3f Hz", beat)}")
            appendLine("Modulation Period (T):    ${String.format(Locale.US, "%.1f ms", periodMs)}")
            appendLine("--------------------------------------------------")
            appendLine("BRAINWAVE ENTRAINMENT CLASSIFICATION:")
            appendLine("State / Band:             ${band.displayName}")
            appendLine("Target Mental State:      ${band.mentalState}")
            appendLine("Physiological Mode:       ${if (isTrueBinaural) "Pure Binaural Headphone Entrainment (Optimal)" else "Physical Superposition / Monaural Beat"}")
            appendLine("--------------------------------------------------")
            appendLine("ACOUSTIC SUPERPOSITION FORMULA:")
            appendLine("y(t) = 2 * cos(2π * ${String.format(Locale.US, "%.2f", beat / 2.0)} * t) * sin(2π * ${String.format(Locale.US, "%.2f", carrier)} * t)")
        }

        val output = BeatsBinauralOutput(
            freq1Hz = f1,
            freq2Hz = f2,
            carrierFreqHz = carrier,
            beatFreqHz = beat,
            beatPeriodMs = periodMs,
            brainwaveBand = band,
            isTrueBinauralRange = isTrueBinaural,
            formattedReport = report,
            summary = "Beat: ${String.format(Locale.US, "%.1f", beat)} Hz (${band.displayName.split(" ")[0]}), Carrier: ${String.format(Locale.US, "%.1f", carrier)} Hz"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Computed beat frequency and brainwave state"
        )
    }
}
