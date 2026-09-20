package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.Base64

class MediaToolsTest {

    private val qrTool = QrMatrixGeneratorTool()
    private val exifTool = ExifInspectorTool()

    @Test
    fun testQrMatrixGeneration() = runBlocking {
        val payload = "https://offline.tools"
        val res = qrTool.execute(
            QrMatrixInput(
                payload = payload,
                errorCorrection = QrErrorCorrection.M
            )
        )

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.dimension in 21..41)
        assertEquals(data.dimension, data.matrix.size)
        assertEquals(data.dimension, data.matrix[0].size)

        // Top-left finder pattern outer box: (0,0) to (6,6)
        assertTrue("Top-left finder origin should be black", data.matrix[0][0])
        assertTrue("Top-left finder center should be black", data.matrix[3][3])
        assertNotNull(data.asciiArt)
        assertTrue(data.asciiArt.contains("██"))
        assertTrue(data.svgMarkup.contains("<svg") && data.svgMarkup.contains("</svg>"))
    }

    @Test
    fun testExifInspectorWithoutExif() = runBlocking {
        // Minimal valid JPEG with only SOI (FF D8) and EOI (FF D9)
        val minimalJpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xD9.toByte()
        )
        val base64 = Base64.getEncoder().encodeToString(minimalJpeg)

        val res = exifTool.execute(ExifInspectorInput(imageBase64 = base64))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(false, data.hasExif)
        assertEquals(ExifPrivacyRisk.LOW_RISK, data.privacyRisk)
    }

    @Test
    fun testExifInspectorWithGpsLeakDetection() = runBlocking {
        // Synthesize a JPEG with APP1 EXIF segment containing GPS tags
        val jpegWithExif = createSyntheticJpegWithGps()
        val base64 = Base64.getEncoder().encodeToString(jpegWithExif)

        val res = exifTool.execute(ExifInspectorInput(imageBase64 = base64))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data

        assertTrue("Should detect EXIF header", data.hasExif)
        assertEquals("Should flag HIGH_RISK due to GPS leak", ExifPrivacyRisk.HIGH_RISK, data.privacyRisk)
        assertNotNull(data.gpsCoordinates)
        assertTrue(data.privacyWarnings.any { it.contains("GPS location is embedded") })
        assertEquals("PixelPhone", data.cameraDevice)
    }

    private fun createSyntheticJpegWithGps(): ByteArray {
        val baos = ByteArrayOutputStream()
        // SOI
        baos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))

        // Build TIFF block
        val tiffBuffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN)
        val tiffStart = 0
        tiffBuffer.put(0x49.toByte()) // 'I'
        tiffBuffer.put(0x49.toByte()) // 'I'
        tiffBuffer.putShort(42.toShort()) // 42
        tiffBuffer.putInt(8) // offset to IFD0 (8)

        // IFD0 at offset 8
        // Count of tags in IFD0: 2 (Make, GPS IFD pointer)
        tiffBuffer.position(8)
        tiffBuffer.putShort(2.toShort())

        // Tag 1: Make (0x010F), ASCII (2), count 11, offset 40
        tiffBuffer.putShort(0x010F.toShort())
        tiffBuffer.putShort(2.toShort())
        tiffBuffer.putInt(11)
        tiffBuffer.putInt(40) // string at offset 40

        // Tag 2: GPS IFD Pointer (0x8825), LONG (4), count 1, value 60
        tiffBuffer.putShort(0x8825.toShort())
        tiffBuffer.putShort(4.toShort())
        tiffBuffer.putInt(1)
        tiffBuffer.putInt(60) // GPS IFD at offset 60

        // End of IFD0: Next IFD offset = 0
        tiffBuffer.putInt(0)

        // String at offset 40: "PixelPhone\0"
        tiffBuffer.position(40)
        tiffBuffer.put("PixelPhone\u0000".toByteArray(StandardCharsets.US_ASCII))

        // GPS IFD at offset 60
        // Count of GPS tags: 4 (LatitudeRef, Latitude, LongitudeRef, Longitude)
        tiffBuffer.position(60)
        tiffBuffer.putShort(4.toShort())

        // GPS Tag 1: GPSLatitudeRef (0x0001), ASCII (2), count 2, value "N\0" inline
        tiffBuffer.putShort(0x0001.toShort())
        tiffBuffer.putShort(2.toShort())
        tiffBuffer.putInt(2)
        tiffBuffer.put('N'.code.toByte())
        tiffBuffer.put(0.toByte())
        tiffBuffer.put(0.toByte())
        tiffBuffer.put(0.toByte())

        // GPS Tag 2: GPSLatitude (0x0002), RATIONAL (5), count 3, offset 120
        tiffBuffer.putShort(0x0002.toShort())
        tiffBuffer.putShort(5.toShort())
        tiffBuffer.putInt(3)
        tiffBuffer.putInt(120)

        // GPS Tag 3: GPSLongitudeRef (0x0003), ASCII (2), count 2, value "W\0" inline
        tiffBuffer.putShort(0x0003.toShort())
        tiffBuffer.putShort(2.toShort())
        tiffBuffer.putInt(2)
        tiffBuffer.put('W'.code.toByte())
        tiffBuffer.put(0.toByte())
        tiffBuffer.put(0.toByte())
        tiffBuffer.put(0.toByte())

        // GPS Tag 4: GPSLongitude (0x0004), RATIONAL (5), count 3, offset 144
        tiffBuffer.putShort(0x0004.toShort())
        tiffBuffer.putShort(5.toShort())
        tiffBuffer.putInt(3)
        tiffBuffer.putInt(144)

        // End of GPS IFD
        tiffBuffer.putInt(0)

        // GPS Latitude values at offset 120 (3 RATIONALs: 37/1 deg, 46/1 min, 30/1 sec)
        tiffBuffer.position(120)
        tiffBuffer.putInt(37); tiffBuffer.putInt(1)
        tiffBuffer.putInt(46); tiffBuffer.putInt(1)
        tiffBuffer.putInt(30); tiffBuffer.putInt(1)

        // GPS Longitude values at offset 144 (3 RATIONALs: 122/1 deg, 25/1 min, 10/1 sec)
        tiffBuffer.position(144)
        tiffBuffer.putInt(122); tiffBuffer.putInt(1)
        tiffBuffer.putInt(25); tiffBuffer.putInt(1)
        tiffBuffer.putInt(10); tiffBuffer.putInt(1)

        val tiffBytes = tiffBuffer.array().copyOf(200)

        // APP1 Marker: 0xFFE1, length (2 bytes), "Exif\0\0" (6 bytes) + tiffBytes
        val app1Length = 2 + 6 + tiffBytes.size
        baos.write(0xFF)
        baos.write(0xE1)
        baos.write((app1Length ushr 8) and 0xFF)
        baos.write(app1Length and 0xFF)
        baos.write("Exif\u0000\u0000".toByteArray(StandardCharsets.US_ASCII))
        baos.write(tiffBytes)

        // EOI
        baos.write(byteArrayOf(0xFF.toByte(), 0xD9.toByte()))
        return baos.toByteArray()
    }
}
