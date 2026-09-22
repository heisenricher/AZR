package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10DataToolsTest {

    private val geoJsonTool = GeoJsonValidatorTool()
    private val varintTool = ProtobufVarintDecoderTool()

    @Test
    fun testGeoJsonValidator_validPolygonAndPoint() = runTest {
        val sampleGeoJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Polygon",
                "coordinates": [
                  [
                    [10.0, 20.0],
                    [15.0, 20.0],
                    [15.0, 25.0],
                    [10.0, 25.0],
                    [10.0, 20.0]
                  ]
                ]
              }
            },
            {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [12.0, 22.0]
              }
            }
          ]
        }
        """.trimIndent()

        val res = geoJsonTool.execute(GeoJsonValidatorInput(geoJsonString = sampleGeoJson))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.isValid)
        assertEquals("FeatureCollection", data.rootType)
        assertEquals(2, data.featureCount)
        assertEquals(10.0, data.boundingBox[0], 0.001) // minLon
        assertEquals(20.0, data.boundingBox[1], 0.001) // minLat
        assertEquals(15.0, data.boundingBox[2], 0.001) // maxLon
        assertEquals(25.0, data.boundingBox[3], 0.001) // maxLat
    }

    @Test
    fun testGeoJsonValidator_emptyOrMalformedFails() = runTest {
        val res = geoJsonTool.execute(GeoJsonValidatorInput(geoJsonString = "{ not json }"))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testProtobufVarint_decodeHex150() = runTest {
        val res = varintTool.execute(
            ProtobufVarintInput(inputString = "96 01", mode = VarintOperationMode.DECODE_HEX_BYTES)
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(150UL, data.unsignedValue)
        assertEquals(2, data.totalBytesConsumed)
        assertEquals(2, data.steps.size)
        assertTrue(data.steps[0].continuationBit)
        assertTrue(!data.steps[1].continuationBit)
    }

    @Test
    fun testProtobufVarint_encodeInteger150() = runTest {
        val res = varintTool.execute(
            ProtobufVarintInput(inputString = "150", mode = VarintOperationMode.ENCODE_INTEGER)
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("96 01", data.encodedHex)
        assertEquals(150UL, data.unsignedValue)
    }

    @Test
    fun testProtobufVarint_decodeZigZag() = runTest {
        // In Protobuf ZigZag:
        // 0 -> 0
        // -1 -> 1 (0x01)
        // 1 -> 2 (0x02)
        // -2 -> 3 (0x03)
        val resNeg1 = varintTool.execute(
            ProtobufVarintInput(inputString = "01", mode = VarintOperationMode.DECODE_HEX_BYTES)
        )
        assertTrue(resNeg1 is ToolResult.Success)
        assertEquals(-1, (resNeg1 as ToolResult.Success).data.signedZigZag32)

        val resPos1 = varintTool.execute(
            ProtobufVarintInput(inputString = "02", mode = VarintOperationMode.DECODE_HEX_BYTES)
        )
        assertTrue(resPos1 is ToolResult.Success)
        assertEquals(1, (resPos1 as ToolResult.Success).data.signedZigZag32)
    }
}
