package com.offline.toolbox.tools.datetime

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DateDifferenceTest {

    private lateinit var tool: DateDifferenceTool

    @Before
    fun setUp() {
        tool = DateDifferenceTool()
    }

    @Test
    fun testExactDifferenceOneYear() = runBlocking {
        val start = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2026, 1, 1)
        val res = tool.execute(DateDifferenceInput(start, end))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(1, data.periodYears)
        assertEquals(0, data.periodMonths)
        assertEquals(0, data.periodDays)
        assertEquals(365L, data.totalDays)
    }

    @Test
    fun testBusinessDaysCalculation() = runBlocking {
        // Monday 2026-09-07 to Friday 2026-09-11 is 4 business days
        val monday = LocalDate.of(2026, 9, 7)
        val friday = LocalDate.of(2026, 9, 11)
        val res = tool.execute(DateDifferenceInput(monday, friday))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(4L, data.businessDays)
        assertEquals(0L, data.weekendDays)
    }
}
