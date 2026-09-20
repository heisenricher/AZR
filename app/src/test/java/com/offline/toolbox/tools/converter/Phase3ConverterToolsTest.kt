package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3ConverterToolsTest {

    // --- AreaConverterTool ---
    @Test
    fun testAreaConverter_sqKmToSqMeters() = runBlocking {
        val tool = AreaConverterTool()
        val result = tool.execute(AreaConverterInput(1.0, AreaUnit.SQUARE_KILOMETER, AreaUnit.SQUARE_METER))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1_000_000.0, data.resultValue, 0.01)
    }

    @Test
    fun testAreaConverter_hectaresAndAcres() = runBlocking {
        val tool = AreaConverterTool()
        val result = tool.execute(AreaConverterInput(1.0, AreaUnit.HECTARE, AreaUnit.SQUARE_METER))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(10_000.0, data.resultValue, 0.01)

        val acreResult = tool.execute(AreaConverterInput(1.0, AreaUnit.ACRE, AreaUnit.SQUARE_METER))
        assertTrue(acreResult is ToolResult.Success)
        assertEquals(4046.856, (acreResult as ToolResult.Success).data.resultValue, 0.01)
    }

    // --- VolumeConverterTool ---
    @Test
    fun testVolumeConverter_literToMilliliters() = runBlocking {
        val tool = VolumeConverterTool()
        val result = tool.execute(VolumeConverterInput(1.0, VolumeUnit.LITER, VolumeUnit.MILLILITER))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1000.0, data.resultValue, 0.01)
    }

    @Test
    fun testVolumeConverter_gallonsToLiters() = runBlocking {
        val tool = VolumeConverterTool()
        val result = tool.execute(VolumeConverterInput(1.0, VolumeUnit.GALLON_US, VolumeUnit.LITER))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3.78541, data.resultValue, 0.01)
    }
}
