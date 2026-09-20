package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5DataToolsTest {

    private val yamlTool = YamlToJsonTool()
    private val statsTool = CsvStatsTool()

    @Test
    fun testYamlToJson_conversion() = runTest {
        val yaml = """
            name: OfflineToolbox
            version: 1.0
            enabled: true
            tags:
              - offline
              - utility
        """.trimIndent()

        val result = yamlTool.execute(YamlJsonInput(yaml, YamlJsonMode.YAML_TO_JSON))
        assertTrue(result is ToolResult.Success)
        val json = (result as ToolResult.Success).data.convertedContent
        assertTrue(json.contains("OfflineToolbox"))
        assertTrue(json.contains("version"))
        assertTrue(json.contains("true"))
    }

    @Test
    fun testJsonToYaml_conversion() = runTest {
        val json = """
            {
              "service": "security",
              "port": 443,
              "active": true
            }
        """.trimIndent()

        val result = yamlTool.execute(YamlJsonInput(json, YamlJsonMode.JSON_TO_YAML))
        assertTrue(result is ToolResult.Success)
        val yaml = (result as ToolResult.Success).data.convertedContent
        assertTrue(yaml.contains("service: security"))
        assertTrue(yaml.contains("port: 443"))
        assertTrue(yaml.contains("active: true"))
    }

    @Test
    fun testCsvStats_numericAndCategorical() = runTest {
        val csv = """
            Name,Age,Score
            Alice,20,85.5
            Bob,25,90.0
            Charlie,30,95.5
            David,25,
        """.trimIndent()

        val result = statsTool.execute(CsvStatsInput(csv))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data

        assertEquals(4, data.totalRowCount)
        assertEquals(3, data.totalColumnCount)

        val ageCol = data.columns.find { it.name == "Age" }
        assertNotNull(ageCol)
        assertEquals(4, ageCol?.nonNullCount)
        assertEquals(0, ageCol?.nullCount)
        assertEquals(3, ageCol?.uniqueValuesCount)
        assertEquals(20.0, ageCol?.minNumeric!!, 0.01)
        assertEquals(30.0, ageCol?.maxNumeric!!, 0.01)
        assertEquals(25.0, ageCol?.meanNumeric!!, 0.01)

        val scoreCol = data.columns.find { it.name == "Score" }
        assertNotNull(scoreCol)
        assertEquals(3, scoreCol?.nonNullCount)
        assertEquals(1, scoreCol?.nullCount) // David has empty score
    }

    @Test
    fun testCsvStats_emptyInput() = runTest {
        val result = statsTool.execute(CsvStatsInput(""))
        assertTrue(result is ToolResult.Failure)
    }
}
