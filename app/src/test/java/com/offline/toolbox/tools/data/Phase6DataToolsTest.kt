package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase6DataToolsTest {

    private val jsonDiffTool = JsonDiffTool()
    private val ndjsonTool = NdjsonToJsonArrayTool()

    @Test
    fun testJsonDiff_detectModificationsAndAdditions() = runTest {
        val original = """{"name": "Alice", "age": 30, "city": "London"}"""
        val modified = """{"name": "Alice", "age": 31, "city": "London", "verified": true}"""

        val result = jsonDiffTool.execute(JsonDiffInput(original, modified))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(2, data.totalDifferences)
        assertEquals(1, data.additionsCount)
        assertEquals(1, data.modificationsCount)
        assertEquals(0, data.deletionsCount)
        assertTrue(data.rfc6902PatchJson.contains("/age"))
        assertTrue(data.rfc6902PatchJson.contains("/verified"))
    }

    @Test
    fun testJsonDiff_identicalJsonYieldsZeroDiff() = runTest {
        val json = """{"a": 1, "b": [10, 20]}"""
        val result = jsonDiffTool.execute(JsonDiffInput(json, json))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(0, data.totalDifferences)
        assertEquals("[]", data.rfc6902PatchJson)
    }

    @Test
    fun testJsonDiff_invalidJsonFails() = runTest {
        val result = jsonDiffTool.execute(JsonDiffInput("{not valid json}", "{}"))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testNdjson_ndjsonToJsonArray() = runTest {
        val ndjson = """
            {"id": 1, "status": "active"}
            {"id": 2, "status": "pending"}
            {"id": 3, "status": "completed"}
        """.trimIndent()

        val result = ndjsonTool.execute(NdjsonInput(
            content = ndjson,
            mode = NdjsonMode.NDJSON_TO_JSON_ARRAY,
            indentSpaces = 2
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3, data.recordCount)
        assertEquals(0, data.invalidLineCount)
        assertTrue(data.convertedContent.startsWith("["))
        assertTrue(data.convertedContent.endsWith("]"))
    }

    @Test
    fun testNdjson_jsonArrayToNdjson() = runTest {
        val array = """[{"id": 10}, {"id": 20}]"""
        val result = ndjsonTool.execute(NdjsonInput(
            content = array,
            mode = NdjsonMode.JSON_ARRAY_TO_NDJSON
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(2, data.recordCount)
        val lines = data.convertedContent.lines().filter { it.isNotBlank() }
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("10"))
        assertTrue(lines[1].contains("20"))
    }

    @Test
    fun testNdjson_invalidArrayFails() = runTest {
        val notAnArray = """{"id": 1}"""
        val result = ndjsonTool.execute(NdjsonInput(
            content = notAnArray,
            mode = NdjsonMode.JSON_ARRAY_TO_NDJSON
        ))
        assertTrue(result is ToolResult.Failure)
    }
}
