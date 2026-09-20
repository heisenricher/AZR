package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase8TextToolsTest {

    private val tableTool = MarkdownTableFormatterTool()
    private val simTool = StringSimilarityTool()

    @Test
    fun testTable_formatsPaddedMarkdown() = runTest {
        val raw = """
            City,Country,Population
            Tokyo,Japan,37400000
            Delhi,India,29300000
        """.trimIndent()
        val result = tableTool.execute(TableFormatterInput(
            content = raw,
            alignment = TableAlignment.LEFT
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3, data.rowCount)
        assertEquals(3, data.columnCount)
        assertTrue(data.formattedTable.contains("| City"))
        assertTrue(data.formattedTable.contains("| :---"))
        assertTrue(data.formattedTable.contains("| Tokyo"))
    }

    @Test
    fun testStringSimilarity_levenshteinAndJaroWinkler() = runTest {
        // "kitten" vs "sitting": Levenshtein = 3 (replace k->s, e->i, add g)
        val result = simTool.execute(StringSimilarityInput(
            stringA = "kitten",
            stringB = "sitting"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(3, data.levenshteinDistance)
        assertTrue(data.levenshteinSimilarityPercentage > 50.0)
        assertTrue(data.jaroWinklerSimilarity > 0.70)
        assertTrue(data.sorensenDiceCoefficient > 0.30)
    }

    @Test
    fun testStringSimilarity_identicalStrings() = runTest {
        val result = simTool.execute(StringSimilarityInput(
            stringA = "identical",
            stringB = "identical"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(0, data.levenshteinDistance)
        assertEquals(100.0, data.levenshteinSimilarityPercentage, 0.001)
        assertEquals(1.0, data.jaroWinklerSimilarity, 0.001)
        assertEquals(0, data.hammingDistance)
        assertEquals(1.0, data.sorensenDiceCoefficient, 0.001)
    }
}
