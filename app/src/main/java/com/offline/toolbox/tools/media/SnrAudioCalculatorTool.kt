package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt

data class SnrAudioInput(
    val signalRmsVolts: Double = 1.0,
    val noiseRmsVolts: Double = 0.0001, // 100 uV -> ~80 dB SNR
    val bitDepth: Int = 16,
    val harmonicVolts: List<Double> = listOf(0.0005, 0.0002, 0.0001) // Harmonics V2, V3, V4
)

data class SnrAudioOutput(
    val snrDb: Double,
    val theoreticalAdcSnrDb: Double,
    val dynamicRangeDb: Double,
    val thdPercent: Double,
    val thdDb: Double,
    val sinadDb: Double,
    val enobBits: Double,
    val audioQualityGrade: String,
    val formattedReport: String,
    val summary: String
)

class SnrAudioCalculatorTool : Tool<SnrAudioInput, SnrAudioOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "snr_audio_calculator_tool",
        name = "Audio SNR, THD & ENOB Quality Calculator",
        description = "Calculate audio Signal-to-Noise Ratio (SNR), Total Harmonic Distortion (THD), Effective Number of Bits (ENOB), and theoretical quantization limits.",
        category = ToolCategory.MEDIA,
        tags = listOf("snr", "thd", "audio", "enob", "sinad", "sound", "acoustics", "hi-fi", "dac", "adc", "decibels"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Volume2"
    )

    override suspend fun execute(input: SnrAudioInput): ToolResult<SnrAudioOutput> {
        val startTime = System.currentTimeMillis()
        val sig = input.signalRmsVolts
        val noise = input.noiseRmsVolts
        val bits = input.bitDepth.coerceIn(1, 64)

        if (sig <= 0.0) {
            return ToolResult.Failure("Signal RMS voltage must be greater than zero.")
        }
        if (noise <= 0.0) {
            return ToolResult.Failure("Noise RMS voltage must be greater than zero.")
        }

        // SNR in dB = 20 * log10(Vsig / Vnoise)
        val snrDb = 20.0 * log10(sig / noise)

        // Theoretical ADC quantization SNR = 6.02 * N + 1.76 dB
        val theoSnr = 6.02 * bits + 1.76

        // THD calculation
        var sumHarmonicsSq = 0.0
        for (h in input.harmonicVolts) {
            sumHarmonicsSq += h * h
        }
        val vHarmonics = sqrt(sumHarmonicsSq)
        val thdRatio = if (sig > 0) vHarmonics / sig else 0.0
        val thdPercent = thdRatio * 100.0
        val thdDb = if (thdRatio > 0) 20.0 * log10(thdRatio) else -120.0

        // SINAD = (Signal + Noise + Distortion) / (Noise + Distortion)
        val vNoiseAndDist = sqrt(noise * noise + sumHarmonicsSq)
        val sinadDb = if (vNoiseAndDist > 0) 20.0 * log10(sig / vNoiseAndDist) else snrDb

        // ENOB = (SINAD - 1.76) / 6.02
        val enob = (sinadDb - 1.76) / 6.02

        val qualityGrade = when {
            snrDb >= 110.0 -> "Audiophile / Studio Mastering (>110 dB)"
            snrDb >= 90.0 -> "Professional High-Fidelity Audio (90-110 dB)"
            snrDb >= 70.0 -> "Consumer Standard Hi-Fi (70-90 dB)"
            snrDb >= 50.0 -> "Broadcast / Analog FM Quality (50-70 dB)"
            else -> "Voice Band / Low Resolution (<50 dB)"
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== AUDIO FIDELITY METRICS (SNR / THD / ENOB) ===")
            appendLine("Signal RMS:          ${String.format(Locale.US, "%.4f", sig)} V")
            appendLine("Noise Floor RMS:     ${String.format(Locale.US, "%.6f", noise)} V")
            appendLine("Bit Depth Target:    $bits-bit")
            appendLine("----------------------------------------")
            appendLine("Signal-to-Noise Ratio (SNR):     ${String.format(Locale.US, "%.2f", snrDb)} dB")
            appendLine("Theoretical Max ($bits-bit ADC):    ${String.format(Locale.US, "%.2f", theoSnr)} dB")
            appendLine("Total Harmonic Distortion (THD): ${String.format(Locale.US, "%.4f", thdPercent)}% (${String.format(Locale.US, "%.2f", thdDb)} dB)")
            appendLine("SINAD (SNR + Distortion):        ${String.format(Locale.US, "%.2f", sinadDb)} dB")
            appendLine("Effective Number of Bits (ENOB): ${String.format(Locale.US, "%.2f", enob)} bits (of $bits bits)")
            appendLine("Dynamic Range:                   ${String.format(Locale.US, "%.2f", snrDb)} dB")
            appendLine("----------------------------------------")
            appendLine("Acoustic Rating: $qualityGrade")
        }

        return ToolResult.Success(
            data = SnrAudioOutput(
                snrDb = snrDb,
                theoreticalAdcSnrDb = theoSnr,
                dynamicRangeDb = snrDb,
                thdPercent = thdPercent,
                thdDb = thdDb,
                sinadDb = sinadDb,
                enobBits = enob,
                audioQualityGrade = qualityGrade,
                formattedReport = report,
                summary = "Audio SNR: ${String.format(Locale.US, "%.1f", snrDb)} dB | THD: ${String.format(Locale.US, "%.3f", thdPercent)}% | ENOB: ${String.format(Locale.US, "%.1f", enob)} bits."
            ),
            executionTimeMs = elapsed,
            summary = "SNR: ${String.format(Locale.US, "%.1f", snrDb)} dB (ENOB: ${String.format(Locale.US, "%.1f", enob)} bits)"
        )
    }
}
