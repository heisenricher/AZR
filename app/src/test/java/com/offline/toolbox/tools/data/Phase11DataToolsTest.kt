package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase11DataToolsTest {

    private val wktTool = WktGeometryParserTool()
    private val bencodeTool = BencodeParserTool()

    @Test
    fun testWkt_point() = runTest {
        val res = wktTool.execute(WktGeometryInput("POINT (12.34 56.78)"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("POINT", data.geometryType)
        assertEquals(1, data.pointCount)
        assertEquals(12.34, data.centroid.x, 1e-4)
        assertEquals(56.78, data.centroid.y, 1e-4)
        assertTrue(data.geoJson.contains(""""type": "Point""""))
    }

    @Test
    fun testWkt_linestring() = runTest {
        val res = wktTool.execute(WktGeometryInput("LINESTRING (0 0, 3 4)"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("LINESTRING", data.geometryType)
        assertEquals(2, data.pointCount)
        assertEquals(5.0, data.lengthOrPerimeter, 1e-4) // 3-4-5 triangle
        assertTrue(data.geoJson.contains(""""type": "LineString""""))
    }

    @Test
    fun testWkt_polygon() = runTest {
        val res = wktTool.execute(WktGeometryInput("POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0))"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("POLYGON", data.geometryType)
        assertEquals(100.0, data.area, 1e-4)
        assertEquals(40.0, data.lengthOrPerimeter, 1e-4)
        assertTrue(data.geoJson.contains(""""type": "Polygon""""))
    }

    @Test
    fun testWkt_invalidSyntax() = runTest {
        val res = wktTool.execute(WktGeometryInput("NOT_GEOMETRY (1 2)"))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testBencode_integerAndString() = runTest {
        val res = bencodeTool.execute(BencodeInput("DECODE", "i1024e"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("1024", data.jsonEquivalent.trim())
        assertEquals("i1024e", data.canonicalBencode)
    }

    @Test
    fun testBencode_dictionaryAndList() = runTest {
        val bencode = "d3:bar4:spam3:fooi42ee"
        val res = bencodeTool.execute(BencodeInput("DECODE", bencode))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.jsonEquivalent.contains(""""bar": "spam""""))
        assertTrue(data.jsonEquivalent.contains(""""foo": 42"""))
        assertEquals(3, data.nodeCount) // dict + 2 entries
    }

    @Test
    fun testBencode_invalidMalformed() = runTest {
        val res = bencodeTool.execute(BencodeInput("DECODE", "d3:fooe"))
        assertTrue(res is ToolResult.Failure)
    }
}
