package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9DataToolsTest {

    private val schemaTool = CsvToJsonSchemaTool()
    private val bstTool = BinarySearchTreeVisualizerTool()

    @Test
    fun testCsvToJsonSchema_infersTypesAndFormats() = runTest {
        val csv = """
            id,username,email,age,score,is_verified,created_at
            1,alice,alice@company.com,28,95.5,true,2026-03-01T10:00:00Z
            2,bob,bob@company.com,32,88.2,false,2026-03-02T12:30:00Z
            3,carol,carol@company.com,24,91.0,true,2026-03-03T15:45:00Z
        """.trimIndent()

        val res = schemaTool.execute(CsvSchemaInput(csvContent = csv))

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(7, data.columnCount)
        assertEquals(3, data.rowCount)
        assertTrue(data.jsonSchema.contains("http://json-schema.org/draft-07/schema#"))
        assertTrue(data.jsonSchema.contains("\"type\": \"integer\""))
        assertTrue(data.jsonSchema.contains("\"type\": \"boolean\""))
        assertTrue(data.jsonSchema.contains("\"type\": \"string\""))
        assertTrue(data.jsonSchema.contains("\"format\": \"date-time\""))
    }

    @Test
    fun testCsvToJsonSchema_emptyInputFails() = runTest {
        val res = schemaTool.execute(CsvSchemaInput(csvContent = ""))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testBstVisualizer_balancedTreeTraversals() = runTest {
        val numbers = "50, 30, 70, 20, 40, 60, 80"
        val res = bstTool.execute(BstVisualizerInput(numbersList = numbers))

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(7, data.totalNodes)
        assertEquals(3, data.treeHeight)
        assertTrue(data.isAvlBalanced)
        assertEquals(listOf(20, 30, 40, 50, 60, 70, 80), data.inOrderTraversal)
        assertEquals(listOf(50, 30, 20, 40, 70, 60, 80), data.preOrderTraversal)
        assertEquals(listOf(20, 40, 30, 60, 80, 70, 50), data.postOrderTraversal)
        assertTrue(data.asciiTree.isNotEmpty())
    }

    @Test
    fun testBstVisualizer_unbalancedTree() = runTest {
        val numbers = "10, 20, 30, 40, 50" // perfectly right-skewed
        val res = bstTool.execute(BstVisualizerInput(numbersList = numbers))

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(5, data.totalNodes)
        assertEquals(5, data.treeHeight)
        assertFalse(data.isAvlBalanced)
    }

    @Test
    fun testBstVisualizer_emptyListFails() = runTest {
        val res = bstTool.execute(BstVisualizerInput(numbersList = "   "))
        assertTrue(res is ToolResult.Failure)
    }
}
