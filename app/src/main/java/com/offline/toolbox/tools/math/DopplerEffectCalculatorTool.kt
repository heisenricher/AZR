package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

enum class DopplerDomain(val displayName: String) {
    ACOUSTIC_SOUND("Acoustic / Sound Waves (Air Medium)"),
    OPTICAL_RELATIVISTIC("Relativistic / Electromagnetic Light (Vacuum)")
}

data class DopplerInput(
    val domain: DopplerDomain = DopplerDomain.ACOUSTIC_SOUND,
    val sourceFrequencyHz: Double = 1000.0, // e.g. 1000 Hz tone or optical Hz
    // Acoustic parameters:
    val sourceVelocityMs: Double = 30.0,    // + towards observer, - away
    val observerVelocityMs: Double = 0.0,  // + towards source, - away
    val airTemperatureCelsius: Double = 20.0,
    // Relativistic parameter:
    val relativeVelocityKmS: Double = 30000.0 // + approaching, - receding (km/s)
)

data class DopplerOutput(
    val domain: String,
    val sourceFrequencyHz: Double,
    val observedFrequencyHz: Double,
    val frequencyShiftHz: Double,
    val percentShift: Double,
    val mediumSpeedMs: Double,
    val machNumberOrBeta: Double,
    val redshiftZ: Double?,
    val classification: String,
    val formattedReport: String,
    val summary: String
)

class DopplerEffectCalculatorTool : Tool<DopplerInput, DopplerOutput> {
    companion object {
        const val SPEED_OF_LIGHT_MS = 299_792_458.0
    }

    override val metadata: ToolMetadata = ToolMetadata(
        id = "doppler_effect_calculator_tool",
        name = "Doppler Effect (Acoustic & Relativistic) Calculator",
        description = "Calculate acoustic Doppler frequency shifts with temperature-dependent sound speed, and relativistic optical Doppler shifts.",
        category = ToolCategory.MATH,
        tags = listOf("doppler", "frequency", "sound", "acoustics", "physics", "relativistic", "optics", "redshift", "blueshift", "speed of sound"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Activity"
    )

    override suspend fun execute(input: DopplerInput): ToolResult<DopplerOutput> {
        val startTime = System.currentTimeMillis()
        val fs = input.sourceFrequencyHz

        if (fs <= 0.0) {
            return ToolResult.Failure("Source frequency must be strictly greater than 0 Hz.")
        }

        return if (input.domain == DopplerDomain.ACOUSTIC_SOUND) {
            calculateAcoustic(input, fs, startTime)
        } else {
            calculateRelativistic(input, fs, startTime)
        }
    }

    private fun calculateAcoustic(input: DopplerInput, fs: Double, startTime: Long): ToolResult<DopplerOutput> {
        val tempC = input.airTemperatureCelsius
        if (tempC < -273.15) {
            return ToolResult.Failure("Temperature cannot be below absolute zero (-273.15 °C).")
        }

        // Speed of sound in ideal air: c = 331.3 * sqrt(1 + T / 273.15)
        val speedOfSound = 331.3 * sqrt(1.0 + (tempC / 273.15))

        val vs = input.sourceVelocityMs // positive towards observer
        val vo = input.observerVelocityMs // positive towards source

        if (vs >= speedOfSound) {
            return ToolResult.Failure("Source speed ($vs m/s) is equal to or exceeds sound speed (${String.format(Locale.US, "%.1f", speedOfSound)} m/s). Sonic shockwave / sonic boom occurs; classical linear Doppler formula diverges.")
        }

        // fo = fs * ((c + vo) / (c - vs))
        val fo = fs * ((speedOfSound + vo) / (speedOfSound - vs))
        val deltaF = fo - fs
        val pctShift = (deltaF / fs) * 100.0
        val mach = abs(vs) / speedOfSound

        val classification = when {
            deltaF > 0.05 -> "Higher Pitch (Approaching)"
            deltaF < -0.05 -> "Lower Pitch (Receding)"
            else -> "Negligible Pitch Shift"
        }

        val report = buildString {
            appendLine("ACOUSTIC DOPPLER EFFECT REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Source Frequency (fs):    ${String.format(Locale.US, "%.2f Hz", fs)}")
            appendLine("Air Temperature:          ${input.airTemperatureCelsius} °C")
            appendLine("Calculated Speed of Sound:${String.format(Locale.US, " %.2f m/s (%.1f km/h)", speedOfSound, speedOfSound * 3.6)}")
            appendLine("Source Velocity (vs):     ${String.format(Locale.US, "%+.2f m/s", vs)} (${if (vs >= 0) "towards" else "away"})")
            appendLine("Observer Velocity (vo):   ${String.format(Locale.US, "%+.2f m/s", vo)} (${if (vo >= 0) "towards" else "away"})")
            appendLine("Source Mach Number (M):   ${String.format(Locale.US, "%.3f", mach)}")
            appendLine("--------------------------------------------------")
            appendLine("Observed Frequency (fo):  ${String.format(Locale.US, "%.3f Hz", fo)}")
            appendLine("Frequency Shift (Δf):     ${String.format(Locale.US, "%+.3f Hz", deltaF)} (${String.format(Locale.US, "%+.2f%%", pctShift)})")
            appendLine("Acoustic Perception:      $classification")
        }

        val output = DopplerOutput(
            domain = input.domain.name,
            sourceFrequencyHz = fs,
            observedFrequencyHz = fo,
            frequencyShiftHz = deltaF,
            percentShift = pctShift,
            mediumSpeedMs = speedOfSound,
            machNumberOrBeta = mach,
            redshiftZ = null,
            classification = classification,
            formattedReport = report,
            summary = "Acoustic: $classification, fo = ${String.format(Locale.US, "%.1f", fo)} Hz (Δf: ${String.format(Locale.US, "%+.1f", deltaF)} Hz)"
        )

        return ToolResult.Success(output, System.currentTimeMillis() - startTime, "Calculated acoustic Doppler shift")
    }

    private fun calculateRelativistic(input: DopplerInput, fs: Double, startTime: Long): ToolResult<DopplerOutput> {
        val relVelocityMs = input.relativeVelocityKmS * 1000.0
        val beta = relVelocityMs / SPEED_OF_LIGHT_MS

        if (abs(beta) >= 1.0) {
            return ToolResult.Failure("Relative velocity must be strictly less than the speed of light c (|β| < 1.0).")
        }

        // Relativistic longitudinal Doppler shift:
        // fo = fs * sqrt((1 + beta) / (1 - beta))  [where beta > 0 means approaching]
        val factor = sqrt((1.0 + beta) / (1.0 - beta))
        val fo = fs * factor
        val deltaF = fo - fs
        val pctShift = (deltaF / fs) * 100.0

        // Redshift parameter z = (fs / fo) - 1
        val z = (1.0 / factor) - 1.0

        val classification = when {
            beta > 0 -> "Blueshift (Approaching / Wavelength Compression)"
            beta < 0 -> "Redshift (Receding / Cosmological Expansion)"
            else -> "Zero Shift (Stationary)"
        }

        val report = buildString {
            appendLine("RELATIVISTIC OPTICAL DOPPLER EFFECT REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Source Frequency (fs):    ${String.format(Locale.US, "%.4e Hz", fs)}")
            appendLine("Relative Velocity (v):    ${String.format(Locale.US, "%+.2f km/s", input.relativeVelocityKmS)}")
            appendLine("Speed of Light Fraction β:${String.format(Locale.US, " %+.6f c", beta)}")
            appendLine("--------------------------------------------------")
            appendLine("Observed Frequency (fo):  ${String.format(Locale.US, "%.4e Hz", fo)}")
            appendLine("Frequency Shift (Δf):     ${String.format(Locale.US, "%+.4e Hz", deltaF)} (${String.format(Locale.US, "%+.3f%%", pctShift)})")
            appendLine("Redshift Parameter (z):   ${String.format(Locale.US, "%+.6f", z)}")
            appendLine("Spectral Classification:  $classification")
        }

        val output = DopplerOutput(
            domain = input.domain.name,
            sourceFrequencyHz = fs,
            observedFrequencyHz = fo,
            frequencyShiftHz = deltaF,
            percentShift = pctShift,
            mediumSpeedMs = SPEED_OF_LIGHT_MS,
            machNumberOrBeta = beta,
            redshiftZ = z,
            classification = classification,
            formattedReport = report,
            summary = "Relativistic: $classification, z = ${String.format(Locale.US, "%+.4f", z)}, β = ${String.format(Locale.US, "%.4f", beta)}c"
        )

        return ToolResult.Success(output, System.currentTimeMillis() - startTime, "Calculated relativistic Doppler shift")
    }
}
