package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12DataToolsTest {

    private val geohashTool = GeoHashCodecTool()
    private val cborTool = CborHexInspectorTool()

    @Test
    fun testGeoHash_encodeAndDecode() = runTest {
        val lat = 37.774929
        val lon = -122.419416
        val encodeRes = geohashTool.execute(
            GeoHashInput(operation = "ENCODE", latitude = lat, longitude = lon, precision = 9)
        )
        assertTrue(encodeRes is ToolResult.Success)
        val hash = (encodeRes as ToolResult.Success).data.geohash
        assertTrue(hash.startsWith("9q8yy"))
        assertEquals(8, (encodeRes as ToolResult.Success).data.neighbors.size)

        val decodeRes = geohashTool.execute(
            GeoHashInput(operation = "DECODE", geohash = hash)
        )
        assertTrue(decodeRes is ToolResult.Success)
        val data = (decodeRes as ToolResult.Success).data
        assertEquals(lat, data.latitude, 0.001)
        assertEquals(lon, data.longitude, 0.001)
    }

    @Test
    fun testGeoHash_neighbors() = runTest {
        val res = geohashTool.execute(
            GeoHashInput(operation = "ENCODE", latitude = 40.7128, longitude = -74.0060, precision = 6)
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        val dirs = data.neighbors.map { it.direction }
        assertTrue(dirs.contains("North"))
        assertTrue(dirs.contains("South"))
        assertTrue(dirs.contains("East"))
        assertTrue(dirs.contains("West"))
    }

    @Test
    fun testCborInspector_mapWithStrings() = runTest {
        // a1 (map of 1) 64 (text length 4) 6e616d65 ("name") 65 (text length 5) 416c696365 ("Alice")
        val hex = "a1646e616d6565416c696365"
        val res = cborTool.execute(CborInput(hex))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.jsonRepresentation.contains("\"name\""))
        assertTrue(data.jsonRepresentation.contains("\"Alice\""))
        assertEquals(12, data.byteCount)
    }

    @Test
    fun testCborInspector_array() = runTest {
        // 83 (array of 3) 01 (1) 02 (2) 03 (3)
        val hex = "83010203"
        val res = cborTool.execute(CborInput(hex))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("[1,2,3]", data.jsonRepresentation.replace(Regex("\\s+"), ""))
        assertEquals(4, data.byteCount)
    }
}
