package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConvertersTest {

    // --- LengthConverterTool ---
    @Test
    fun testLengthConverter_kmToMeters() = runBlocking {
        val tool = LengthConverterTool()
        val result = tool.execute(LengthConverterInput(1.0, LengthUnit.KILOMETER, LengthUnit.METER))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1000.0, data.resultValue, 0.001)
    }

    @Test
    fun testLengthConverter_inchesToCentimeters() = runBlocking {
        val tool = LengthConverterTool()
        val result = tool.execute(LengthConverterInput(1.0, LengthUnit.INCH, LengthUnit.CENTIMETER))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(2.54, data.resultValue, 0.001)
    }

    // --- WeightConverterTool ---
    @Test
    fun testWeightConverter_kgToPounds() = runBlocking {
        val tool = WeightConverterTool()
        val result = tool.execute(WeightConverterInput(1.0, WeightUnit.KILOGRAM, WeightUnit.POUND))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(2.20462, data.resultValue, 0.01)
    }

    @Test
    fun testWeightConverter_gramsToKg() = runBlocking {
        val tool = WeightConverterTool()
        val result = tool.execute(WeightConverterInput(1000.0, WeightUnit.GRAM, WeightUnit.KILOGRAM))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1.0, data.resultValue, 0.001)
    }

    // --- TemperatureConverterTool ---
    @Test
    fun testTemperatureConverter_celsiusToFahrenheitAndKelvin() = runBlocking {
        val tool = TemperatureConverterTool()
        val result = tool.execute(TemperatureConverterInput(0.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(32.0, data.resultValue, 0.01)
        assertEquals(273.15, data.kelvin, 0.01)

        val boilResult = tool.execute(TemperatureConverterInput(100.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT))
        assertTrue(boilResult is ToolResult.Success)
        assertEquals(212.0, (boilResult as ToolResult.Success).data.resultValue, 0.01)
    }

    // --- DataStorageConverterTool ---
    @Test
    fun testDataStorageConverter_binaryAndDecimal() = runBlocking {
        val tool = DataStorageConverterTool()
        // 1 GB -> MB in binary base (1024)
        val binResult = tool.execute(DataStorageInput(1.0, StorageUnit.GIGABYTE, StorageUnit.MEGABYTE, useBinaryBase1024 = true))
        assertTrue(binResult is ToolResult.Success)
        assertEquals(1024.0, (binResult as ToolResult.Success).data.resultValue, 0.001)

        // 1 GB -> MB in decimal base (1000)
        val decResult = tool.execute(DataStorageInput(1.0, StorageUnit.GIGABYTE, StorageUnit.MEGABYTE, useBinaryBase1024 = false))
        assertTrue(decResult is ToolResult.Success)
        assertEquals(1000.0, (decResult as ToolResult.Success).data.resultValue, 0.001)
    }

    // --- SpeedConverterTool ---
    @Test
    fun testSpeedConverter_kmhToMphAndMs() = runBlocking {
        val tool = SpeedConverterTool()
        val result = tool.execute(SpeedConverterInput(100.0, SpeedUnit.KILOMETERS_PER_HOUR, SpeedUnit.MILES_PER_HOUR))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(62.1371, data.resultValue, 0.05)

        val msResult = tool.execute(SpeedConverterInput(1.0, SpeedUnit.METERS_PER_SECOND, SpeedUnit.KILOMETERS_PER_HOUR))
        assertTrue(msResult is ToolResult.Success)
        assertEquals(3.6, (msResult as ToolResult.Success).data.resultValue, 0.001)
    }

    // --- RomanNumeralConverterTool ---
    @Test
    fun testRomanNumeral_numberToRoman() = runBlocking {
        val tool = RomanNumeralConverterTool()
        val res1 = tool.execute(RomanNumeralInput("2024", RomanMode.NUMBER_TO_ROMAN))
        assertTrue(res1 is ToolResult.Success)
        assertEquals("MMXXIV", (res1 as ToolResult.Success).data)

        val res2 = tool.execute(RomanNumeralInput("3999", RomanMode.NUMBER_TO_ROMAN))
        assertTrue(res2 is ToolResult.Success)
        assertEquals("MMMCMXCIX", (res2 as ToolResult.Success).data)
    }

    @Test
    fun testRomanNumeral_romanToNumber() = runBlocking {
        val tool = RomanNumeralConverterTool()
        val res = tool.execute(RomanNumeralInput("MMXXIV", RomanMode.ROMAN_TO_NUMBER))
        assertTrue(res is ToolResult.Success)
        assertEquals("2024", (res as ToolResult.Success).data)
    }

    @Test
    fun testRomanNumeral_outOfRangeOrInvalidFails() = runBlocking {
        val tool = RomanNumeralConverterTool()
        val resOver = tool.execute(RomanNumeralInput("4000", RomanMode.NUMBER_TO_ROMAN))
        assertTrue(resOver is ToolResult.Failure)

        val resZero = tool.execute(RomanNumeralInput("0", RomanMode.NUMBER_TO_ROMAN))
        assertTrue(resZero is ToolResult.Failure)

        val resInvalidChar = tool.execute(RomanNumeralInput("MMXXZ", RomanMode.ROMAN_TO_NUMBER))
        assertTrue(resInvalidChar is ToolResult.Failure)
    }
}
