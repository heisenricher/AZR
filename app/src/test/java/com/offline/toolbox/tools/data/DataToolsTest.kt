package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataToolsTest {

    // --- CsvToJsonTool ---
    @Test
    fun testCsvToJson_basic() = runBlocking {
        val tool = CsvToJsonTool()
        val csv = "id,name,role\n1,Alice,Engineer\n2,Bob,Designer"
        val result = tool.execute(CsvToJsonInput(csv))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(2, data.rowCount)
        assertEquals(3, data.columnCount)
        assertTrue(data.jsonString.contains("\"name\": \"Alice\""))
        assertTrue(data.jsonString.contains("\"role\": \"Designer\""))
    }

    @Test
    fun testCsvToJson_quotedCommas() = runBlocking {
        val tool = CsvToJsonTool()
        val csv = "name,location\n\"Doe, John\",\"New York, NY\""
        val result = tool.execute(CsvToJsonInput(csv))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1, data.rowCount)
        assertTrue(data.jsonString.contains("\"name\": \"Doe, John\""))
        assertTrue(data.jsonString.contains("\"location\": \"New York, NY\""))
    }

    @Test
    fun testCsvToJson_emptyFails() = runBlocking {
        val tool = CsvToJsonTool()
        val result = tool.execute(CsvToJsonInput(""))
        assertTrue(result is ToolResult.Failure)
    }

    // --- JsonToCsvTool ---
    @Test
    fun testJsonToCsv_basic() = runBlocking {
        val tool = JsonToCsvTool()
        val json = """[{"id": 1, "name": "Alice"}, {"id": 2, "name": "Bob"}]"""
        val result = tool.execute(json)
        assertTrue(result is ToolResult.Success)
        val csv = (result as ToolResult.Success).data
        val lines = csv.lines()
        if (lines[0] == "id,name") {
            assertEquals("1,Alice", lines[1])
            assertEquals("2,Bob", lines[2])
        } else {
            assertEquals("name,id", lines[0])
            assertEquals("Alice,1", lines[1])
            assertEquals("Bob,2", lines[2])
        }
    }

    @Test
    fun testJsonToCsv_emptyOrInvalidFails() = runBlocking {
        val tool = JsonToCsvTool()
        val emptyRes = tool.execute("")
        assertTrue(emptyRes is ToolResult.Failure)

        val invalidRes = tool.execute("invalid json text")
        assertTrue(invalidRes is ToolResult.Failure)
    }
}
