package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvFilterSortTest {

    private val tool = CsvFilterSortTool()

    private val sampleCsv = """
        Name,Role,Score,City
        Alice,Architect,95,New York
        Bob,Developer,80,Berlin
        Charlie,Developer,90,London
        David,Security,70,Tokyo
        Eve,Architect,95,New York
    """.trimIndent()

    @Test
    fun testNumericFilterGreaterThan() = runBlocking {
        val res = tool.execute(
            CsvFilterSortInput(
                csvData = sampleCsv,
                filterColumn = "Score",
                filterOperator = CsvFilterOperator.GREATER_THAN,
                filterValue = "85"
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // Alice (95), Charlie (90), Eve (95) should remain
        assertEquals(5, data.inputRowCount)
        assertEquals(3, data.outputRowCount)
        assertTrue(data.processedCsv.contains("Alice"))
        assertTrue(data.processedCsv.contains("Charlie"))
        assertTrue(data.processedCsv.contains("Eve"))
        assertFalse(data.processedCsv.contains("Bob"))
        assertFalse(data.processedCsv.contains("David"))
    }

    @Test
    fun testSortDescendingNumeric() = runBlocking {
        val res = tool.execute(
            CsvFilterSortInput(
                csvData = sampleCsv,
                sortColumn = "Score",
                sortOrder = CsvSortOrder.DESCENDING_NUMERIC
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        val lines = data.processedCsv.lines()
        // Top score row should be Alice or Eve (95)
        assertTrue(lines[1].contains("95"))
        // Lowest score row should be David (70)
        assertTrue(lines.last().contains("70"))
    }

    @Test
    fun testDeduplicationAndColumnProjection() = runBlocking {
        val csvWithDuplicate = """
            Id,Name,Country
            1,Alpha,US
            2,Beta,UK
            1,Alpha,US
        """.trimIndent()

        val res = tool.execute(
            CsvFilterSortInput(
                csvData = csvWithDuplicate,
                deduplicateRows = true,
                projectColumns = "Id, Name"
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(3, data.inputRowCount)
        assertEquals(2, data.outputRowCount)
        assertFalse("Country column should be omitted by projection", data.processedCsv.contains("Country"))
        assertTrue("Id column should be kept", data.processedCsv.contains("Id"))
        assertTrue("Name column should be kept", data.processedCsv.contains("Name"))
    }
}
