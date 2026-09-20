package com.offline.toolbox.tools.datetime

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DateTimeToolsTest {

    // --- DateAddSubtractTool ---
    @Test
    fun testDateAddSubtract_addDaysAndWeeks() = runBlocking {
        val tool = DateAddSubtractTool()
        val start = LocalDate.of(2024, 1, 1)

        val addDaysRes = tool.execute(DateAddSubtractInput(startDate = start, operation = DateOperation.ADD, amount = 10, unit = DateUnit.DAYS))
        assertTrue(addDaysRes is ToolResult.Success)
        assertEquals(LocalDate.of(2024, 1, 11), (addDaysRes as ToolResult.Success).data.resultDate)

        val subWeeksRes = tool.execute(DateAddSubtractInput(startDate = LocalDate.of(2024, 1, 15), operation = DateOperation.SUBTRACT, amount = 2, unit = DateUnit.WEEKS))
        assertTrue(subWeeksRes is ToolResult.Success)
        assertEquals(LocalDate.of(2024, 1, 1), (subWeeksRes as ToolResult.Success).data.resultDate)
    }

    @Test
    fun testDateAddSubtract_businessDays() = runBlocking {
        val tool = DateAddSubtractTool()
        // Friday Jan 5, 2024 + 1 business day -> Monday Jan 8, 2024
        val friday = LocalDate.of(2024, 1, 5)
        val result = tool.execute(DateAddSubtractInput(startDate = friday, operation = DateOperation.ADD, amount = 1, unit = DateUnit.BUSINESS_DAYS))
        assertTrue(result is ToolResult.Success)
        assertEquals(LocalDate.of(2024, 1, 8), (result as ToolResult.Success).data.resultDate)
    }

    // --- CountdownCalculatorTool ---
    @Test
    fun testCountdownCalculator_futureAndPast() = runBlocking {
        val tool = CountdownCalculatorTool()
        val future = LocalDateTime.now().plusDays(5).plusHours(2)
        val futureRes = tool.execute(future)
        assertTrue(futureRes is ToolResult.Success)
        val futureData = (futureRes as ToolResult.Success).data
        assertFalse(futureData.isPastEvent)
        assertEquals(5L, futureData.totalDays)

        val past = LocalDateTime.now().minusDays(3)
        val pastRes = tool.execute(past)
        assertTrue(pastRes is ToolResult.Success)
        val pastData = (pastRes as ToolResult.Success).data
        assertTrue(pastData.isPastEvent)
        assertEquals(3L, pastData.totalDays)
    }
}
