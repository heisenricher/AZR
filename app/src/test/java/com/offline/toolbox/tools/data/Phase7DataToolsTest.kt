package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7DataToolsTest {

    private val jsonPathTool = JsonPathEvaluatorTool()
    private val tsvCsvTool = TsvToCsvTool()

    private val sampleJson = """
        {
          "store": {
            "book": [
              { "category": "reference", "author": "Nigel Rees", "title": "Sayings of the Century", "price": 8.95 },
              { "category": "fiction", "author": "Evelyn Waugh", "title": "Sword of Honour", "price": 12.99 },
              { "category": "fiction", "author": "Herman Melville", "title": "Moby Dick", "price": 8.99 }
            ],
            "bicycle": {
              "color": "red",
              "price": 19.95
            }
          }
        }
    """.trimIndent()

    @Test
    fun testJsonPath_extractWildcardTitles() = runTest {
        val result = jsonPathTool.execute(JsonPathInput(
            json = sampleJson,
            jsonPath = "$.store.book[*].title"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3, data.matchedCount)
        assertTrue(data.matchedElementsJson.contains("Sayings of the Century"))
        assertTrue(data.matchedElementsJson.contains("Sword of Honour"))
        assertTrue(data.matchedElementsJson.contains("Moby Dick"))
    }

    @Test
    fun testJsonPath_indexedItemAccess() = runTest {
        val result = jsonPathTool.execute(JsonPathInput(
            json = sampleJson,
            jsonPath = "$.store.book[0].author"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1, data.matchedCount)
        assertTrue(data.matchedElementsJson.contains("Nigel Rees"))
    }

    @Test
    fun testJsonPath_nestedObjectProperty() = runTest {
        val result = jsonPathTool.execute(JsonPathInput(
            json = sampleJson,
            jsonPath = "$.store.bicycle.color"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1, data.matchedCount)
        assertTrue(data.matchedElementsJson.contains("red"))
    }

    @Test
    fun testJsonPath_invalidJsonFails() = runTest {
        val result = jsonPathTool.execute(JsonPathInput(
            json = "{ invalid_json: 123 ",
            jsonPath = "$.test"
        ))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testTsvToCsv_convertsCorrectly() = runTest {
        val tsvData = "Name\tDepartment\tSalary\nAlice\tEngineering\t120000\nBob\tDesign\t95000"
        val result = tsvCsvTool.execute(TsvCsvInput(
            content = tsvData,
            mode = TsvCsvMode.TSV_TO_CSV
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3, data.rowCount)
        assertEquals(3, data.columnCount)
        assertTrue(data.convertedContent.contains("Name,Department,Salary"))
        assertTrue(data.convertedContent.contains("Alice,Engineering,120000"))
    }

    @Test
    fun testCsvToTsv_escapedQuotesAndCommas() = runTest {
        val csvData = "Title,Description,Tag\nItem 1,\"Widget, Special\",Tools\nItem 2,Gadget,Hardware"
        val result = tsvCsvTool.execute(TsvCsvInput(
            content = csvData,
            mode = TsvCsvMode.CSV_TO_TSV
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3, data.rowCount)
        assertTrue(data.convertedContent.contains("Widget, Special"))
        assertTrue(data.convertedContent.contains("\t"))
    }
}
