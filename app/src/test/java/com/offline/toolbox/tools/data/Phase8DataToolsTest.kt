package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase8DataToolsTest {

    private val xmlJsonTool = XmlToJsonConverterTool()
    private val hexDumpTool = HexDumpViewerTool()

    @Test
    fun testXmlToJson_preservesAttributesAndTags() = runTest {
        val xml = """
            <user id="42">
                <name>Gordon Freeman</name>
                <role>Physicist</role>
            </user>
        """.trimIndent()
        val result = xmlJsonTool.execute(XmlJsonInput(
            content = xml,
            direction = XmlJsonDirection.XML_TO_JSON
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.convertedContent.contains("\"@id\": \"42\""))
        assertTrue(data.convertedContent.contains("\"name\": \"Gordon Freeman\""))
    }

    @Test
    fun testJsonToXml_convertsValidJson() = runTest {
        val json = """
            {
              "project": {
                "name": "AZR",
                "tools": 120
              }
            }
        """.trimIndent()
        val result = xmlJsonTool.execute(XmlJsonInput(
            content = json,
            direction = XmlJsonDirection.JSON_TO_XML
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.convertedContent.contains("<project>"))
        assertTrue(data.convertedContent.contains("<name>AZR</name>"))
    }

    @Test
    fun testHexDump_formatsStandardLines() = runTest {
        val input = "Hello, Hex Dump World!"
        val result = hexDumpTool.execute(HexDumpInput(
            content = input,
            inputMode = HexDumpInputMode.PLAIN_TEXT,
            bytesPerLine = 16
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(input.length, data.totalBytes)
        assertEquals(2, data.totalLines) // 22 bytes / 16 = 2 lines
        assertTrue(data.formattedHexDump.startsWith("00000000"))
        assertTrue(data.formattedHexDump.contains("48 65 6C 6C 6F"))
        assertTrue(data.byteEntropy > 0.0)
    }

    @Test
    fun testHexDump_rawHexMode() = runTest {
        val hexInput = "DEADBEEFCAFE"
        val result = hexDumpTool.execute(HexDumpInput(
            content = hexInput,
            inputMode = HexDumpInputMode.HEX_STRING
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(6, data.totalBytes)
        assertTrue(data.formattedHexDump.contains("DE AD BE EF CA FE"))
    }
}
