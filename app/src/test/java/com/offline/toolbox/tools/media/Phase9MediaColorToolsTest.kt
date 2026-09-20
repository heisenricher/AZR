package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorTempInput
import com.offline.toolbox.tools.color.ColorTemperatureTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9MediaColorToolsTest {

    private val colorTempTool = ColorTemperatureTool()
    private val decibelTool = AudioDecibelCalculatorTool()

    @Test
    fun testColorTemperature_kelvinToRgbConversion() = runTest {
        // 6500 K standard D65 daylight
        val res6500 = colorTempTool.execute(ColorTempInput(temperatureKelvin = 6500))
        assertTrue(res6500 is ToolResult.Success)
        val data6500 = (res6500 as ToolResult.Success).data
        assertEquals(6500, data6500.kelvin)
        assertTrue(data6500.hexColor.startsWith("#"))
        assertTrue(data6500.formattedReport.contains("6500"))

        // 1850 K candlelight: warm orange/red (R should dominate B)
        val res1850 = colorTempTool.execute(ColorTempInput(temperatureKelvin = 1850))
        assertTrue(res1850 is ToolResult.Success)
        val data1850 = (res1850 as ToolResult.Success).data
        assertTrue(data1850.red > data1850.blue)

        // 10000 K blue sky: cool blue (B should dominate R)
        val res10000 = colorTempTool.execute(ColorTempInput(temperatureKelvin = 10000))
        assertTrue(res10000 is ToolResult.Success)
        val data10000 = (res10000 as ToolResult.Success).data
        assertTrue(data10000.blue > data10000.red)
    }

    @Test
    fun testColorTemperature_outOfRangeFails() = runTest {
        val res = colorTempTool.execute(ColorTempInput(temperatureKelvin = 500)) // < 1000 K
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testAudioDecibel_splSoundPressure() = runTest {
        // 1.0 Pa relative to 20 µPa is 20 * log10(50000) = 93.9794 dB SPL (~94 dB SPL)
        val res = decibelTool.execute(DecibelInput(
            calculationType = DecibelCalculationType.SOUND_PRESSURE_LEVEL,
            value = 1.0
        ))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(93.98, data.decibels, 0.05)
    }

    @Test
    fun testAudioDecibel_dbuStandardStudioLevel() = runTest {
        // 1.228 V into 0.775 V reference is +4 dBu (standard pro audio line level)
        val res = decibelTool.execute(DecibelInput(
            calculationType = DecibelCalculationType.VOLTAGE_DBU,
            value = 1.228
        ))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(4.0, data.decibels, 0.05)
    }

    @Test
    fun testAudioDecibel_inverseSquareLawAttenuation() = runTest {
        // Initial SPL 90 dB at 1m. At 2m, SPL drops by 20*log10(2) ~ 6.02 dB -> ~83.98 dB
        val res = decibelTool.execute(DecibelInput(
            calculationType = DecibelCalculationType.DISTANCE_ATTENUATION,
            value = 90.0,
            initialDistanceMeters = 1.0,
            targetDistanceMeters = 2.0
        ))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(83.98, data.decibels, 0.05)
    }

    @Test
    fun testAudioDecibel_negativeValuesFails() = runTest {
        val res = decibelTool.execute(DecibelInput(
            calculationType = DecibelCalculationType.SOUND_PRESSURE_LEVEL,
            value = -5.0
        ))
        assertTrue(res is ToolResult.Failure)
    }
}
