package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale

enum class ExifPrivacyRisk {
    HIGH_RISK,   // Contains precise GPS coordinates
    MEDIUM_RISK, // Contains device model, serial, timestamp
    LOW_RISK     // Minimal or stripped metadata
}

data class ExifTag(
    val id: String,
    val name: String,
    val value: String
)

data class ExifInspectorInput(
    // Base64-encoded image (JPEG) or raw hex bytes
    val imageBase64: String = ""
)

data class ExifInspectorOutput(
    val hasExif: Boolean,
    val privacyRisk: ExifPrivacyRisk,
    val privacyWarnings: List<String>,
    val tags: List<ExifTag>,
    val gpsCoordinates: String?,
    val cameraDevice: String?,
    val captureDate: String?,
    val formattedReport: String,
    val summary: String
)

class ExifInspectorTool : Tool<ExifInspectorInput, ExifInspectorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "exif_inspector_tool",
        name = "EXIF Metadata & Privacy Inspector",
        description = "Inspect hidden EXIF camera metadata, timestamps, and detect GPS privacy leaks in photos.",
        category = ToolCategory.MEDIA,
        tags = listOf("exif", "metadata", "privacy", "gps", "camera", "photo", "image", "forensics"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "PhotoCamera"
    )

    override suspend fun execute(input: ExifInspectorInput): ToolResult<ExifInspectorOutput> {
        val startTime = System.currentTimeMillis()

        if (input.imageBase64.isBlank()) {
            return ToolResult.Failure(
                message = "Image data is empty.",
                userGuidance = "Paste a Base64-encoded JPEG image string to inspect EXIF metadata."
            )
        }

        val bytes = try {
            val sanitized = input.imageBase64.substringAfter("base64,").trim()
            Base64.getDecoder().decode(sanitized)
        } catch (e: Exception) {
            return ToolResult.Failure("Invalid Base64 image payload: ${e.message}", cause = e)
        }

        if (bytes.size < 4) {
            return ToolResult.Failure("Data too small to be a valid image.")
        }

        val (tags, gpsString) = parseJpegExif(bytes)

        val warnings = mutableListOf<String>()
        var risk = ExifPrivacyRisk.LOW_RISK

        if (gpsString != null) {
            risk = ExifPrivacyRisk.HIGH_RISK
            warnings.add("CRITICAL PRIVACY LEAK: GPS location is embedded ($gpsString). Anyone who downloads this photo can track exact location coordinates.")
        }

        val cameraMake = tags.find { it.name == "Make" }?.value
        val cameraModel = tags.find { it.name == "Model" }?.value
        val software = tags.find { it.name == "Software" }?.value
        val captureDate = tags.find { it.name == "Date/Time Original" || it.name == "Date/Time" }?.value

        val cameraDevice = when {
            cameraMake != null && cameraModel != null -> "$cameraMake $cameraModel"
            cameraModel != null -> cameraModel
            cameraMake != null -> cameraMake
            else -> null
        }

        if (cameraDevice != null && risk != ExifPrivacyRisk.HIGH_RISK) {
            risk = ExifPrivacyRisk.MEDIUM_RISK
            warnings.add("Device Identity: Photo reveals camera hardware ($cameraDevice).")
        }
        if (captureDate != null && risk == ExifPrivacyRisk.LOW_RISK) {
            risk = ExifPrivacyRisk.MEDIUM_RISK
            warnings.add("Temporal Trace: Exact capture timestamp is recorded ($captureDate).")
        }

        val report = buildString {
            appendLine("EXIF PRIVACY & FORENSIC REPORT")
            appendLine("--------------------------------")
            appendLine("Privacy Risk Level: ${when (risk) {
                ExifPrivacyRisk.HIGH_RISK -> "🔴 HIGH PRIVACY RISK"
                ExifPrivacyRisk.MEDIUM_RISK -> "🟡 MEDIUM PRIVACY RISK"
                ExifPrivacyRisk.LOW_RISK -> "🟢 LOW PRIVACY RISK (Clean or Stripped)"
            }}")
            appendLine("EXIF Header Found:  ${if (tags.isNotEmpty()) "YES (${tags.size} tags)" else "NO"}")
            if (gpsString != null) {
                appendLine("GPS Coordinates:    $gpsString")
            }
            if (cameraDevice != null) {
                appendLine("Device Model:       $cameraDevice")
            }
            if (captureDate != null) {
                appendLine("Capture Timestamp:  $captureDate")
            }
            if (software != null) {
                appendLine("Software/App:       $software")
            }

            if (warnings.isNotEmpty()) {
                appendLine()
                appendLine("PRIVACY AUDIT FINDINGS:")
                warnings.forEach { appendLine("⚠️ $it") }
                appendLine("Recommendation: Strip EXIF metadata before posting publicly or sharing online.")
            }

            if (tags.isNotEmpty()) {
                appendLine()
                appendLine("EXTRACTED METADATA TAGS:")
                tags.forEach { tag ->
                    appendLine("%-22s %s".format(tag.name + ":", tag.value))
                }
            } else {
                appendLine()
                appendLine("No standard EXIF metadata tags found in file.")
            }
        }

        val summary = when (risk) {
            ExifPrivacyRisk.HIGH_RISK -> "ALERT: GPS Embedded ($gpsString)"
            ExifPrivacyRisk.MEDIUM_RISK -> "Found ${tags.size} EXIF tags ($cameraDevice)"
            ExifPrivacyRisk.LOW_RISK -> if (tags.isNotEmpty()) "${tags.size} tags (No GPS)" else "No EXIF tags (Clean)"
        }

        return ToolResult.Success(
            data = ExifInspectorOutput(
                hasExif = tags.isNotEmpty(),
                privacyRisk = risk,
                privacyWarnings = warnings,
                tags = tags,
                gpsCoordinates = gpsString,
                cameraDevice = cameraDevice,
                captureDate = captureDate,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun parseJpegExif(bytes: ByteArray): Pair<List<ExifTag>, String?> {
        val tags = mutableListOf<ExifTag>()
        var gpsString: String? = null

        // Check JPEG SOI marker (0xFFD8)
        if (bytes.size < 4 || (bytes[0].toInt() and 0xFF) != 0xFF || (bytes[1].toInt() and 0xFF) != 0xD8) {
            return Pair(tags, null)
        }

        var offset = 2
        while (offset < bytes.size - 4) {
            if ((bytes[offset].toInt() and 0xFF) != 0xFF) {
                offset++
                continue
            }
            val marker = bytes[offset + 1].toInt() and 0xFF
            if (marker == 0xDA || marker == 0xD9) {
                // Start of Scan or End of Image
                break
            }
            val length = ((bytes[offset + 2].toInt() and 0xFF) shl 8) or (bytes[offset + 3].toInt() and 0xFF)
            if (offset + 2 + length > bytes.size) break

            // APP1 marker (0xFFE1)
            if (marker == 0xE1 && length >= 8) {
                val header = String(bytes, offset + 4, 4, StandardCharsets.US_ASCII)
                if (header == "Exif" && bytes[offset + 8] == 0.toByte() && bytes[offset + 9] == 0.toByte()) {
                    val tiffStart = offset + 10
                    val tiffLength = length - 8
                    if (tiffStart + tiffLength <= bytes.size) {
                        val (parsedTags, gps) = parseTiffHeader(bytes, tiffStart, tiffLength)
                        tags.addAll(parsedTags)
                        if (gps != null) gpsString = gps
                    }
                }
            }
            offset += 2 + length
        }

        return Pair(tags, gpsString)
    }

    private fun parseTiffHeader(bytes: ByteArray, start: Int, length: Int): Pair<List<ExifTag>, String?> {
        val tags = mutableListOf<ExifTag>()
        var gpsResult: String? = null

        if (length < 8) return Pair(tags, null)

        val buffer = ByteBuffer.wrap(bytes, start, length).slice()
        val endian1 = bytes[start].toInt() and 0xFF
        val endian2 = bytes[start + 1].toInt() and 0xFF

        val isLittleEndian = (endian1 == 0x49 && endian2 == 0x49) // "II" Intel
        val isBigEndian = (endian1 == 0x4D && endian2 == 0x4D)    // "MM" Motorola

        if (!isLittleEndian && !isBigEndian) return Pair(tags, null)

        buffer.order(if (isLittleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

        val magic = buffer.getShort(2).toInt() and 0xFFFF
        if (magic != 42) return Pair(tags, null)

        val ifd0Offset = buffer.getInt(4)
        if (ifd0Offset < 8 || ifd0Offset >= length - 2) return Pair(tags, null)

        val subIfdOffsets = mutableListOf<Int>()
        var gpsIfdOffset = -1

        // Parse IFD0
        val ifd0Tags = readIfd(buffer, length, ifd0Offset) { tagId, valueOffset ->
            when (tagId) {
                0x8769 -> subIfdOffsets.add(valueOffset) // Exif SubIFD
                0x8825 -> gpsIfdOffset = valueOffset     // GPS IFD
            }
        }
        tags.addAll(ifd0Tags)

        // Parse SubIFD
        for (subOffset in subIfdOffsets) {
            if (subOffset in 8 until length - 2) {
                tags.addAll(readIfd(buffer, length, subOffset))
            }
        }

        // Parse GPS IFD
        if (gpsIfdOffset in 8 until length - 2) {
            var latRef: String? = null
            var lonRef: String? = null
            var latDeg: Double? = null
            var lonDeg: Double? = null

            val gpsTags = readIfd(buffer, length, gpsIfdOffset) { tagId, _ -> }
            for (t in gpsTags) {
                when (t.id) {
                    "0x0001" -> latRef = t.value.trim()
                    "0x0003" -> lonRef = t.value.trim()
                    "0x0002" -> latDeg = parseGpsCoordinate(t.value)
                    "0x0004" -> lonDeg = parseGpsCoordinate(t.value)
                }
                tags.add(ExifTag(t.id, "GPS: " + t.name, t.value))
            }

            if (latDeg != null && lonDeg != null) {
                val finalLat = if (latRef == "S") -latDeg else latDeg
                val finalLon = if (lonRef == "W") -lonDeg else lonDeg
                gpsResult = "%.5f, %.5f (%s %s)".format(Locale.US, finalLat, finalLon, latRef ?: "N", lonRef ?: "E")
            }
        }

        return Pair(tags, gpsResult)
    }

    private fun readIfd(
        buffer: ByteBuffer,
        maxLen: Int,
        ifdOffset: Int,
        specialTagHandler: ((Int, Int) -> Unit)? = null
    ): List<ExifTag> {
        val list = mutableListOf<ExifTag>()
        if (ifdOffset + 2 > maxLen) return list

        val count = buffer.getShort(ifdOffset).toInt() and 0xFFFF
        var entryOffset = ifdOffset + 2

        for (i in 0 until count) {
            if (entryOffset + 12 > maxLen) break

            val tagId = buffer.getShort(entryOffset).toInt() and 0xFFFF
            val type = buffer.getShort(entryOffset + 2).toInt() and 0xFFFF
            val numComponents = buffer.getInt(entryOffset + 4)
            val valueOffset = buffer.getInt(entryOffset + 8)

            specialTagHandler?.invoke(tagId, valueOffset)

            val tagName = TAG_NAMES[tagId] ?: "Tag 0x%04X".format(tagId)
            val valueStr = readTagValue(buffer, maxLen, type, numComponents, entryOffset + 8)

            if (valueStr.isNotBlank()) {
                list.add(ExifTag("0x%04X".format(tagId), tagName, valueStr))
            }

            entryOffset += 12
        }

        return list
    }

    private fun readTagValue(buffer: ByteBuffer, maxLen: Int, type: Int, count: Int, inlineOffset: Int): String {
        return try {
            when (type) {
                2 -> { // ASCII
                    val strOffset = if (count <= 4) inlineOffset else buffer.getInt(inlineOffset)
                    if (strOffset < 0 || strOffset + count > maxLen) return ""
                    val array = ByteArray(count)
                    val oldPos = buffer.position()
                    buffer.position(strOffset)
                    buffer.get(array)
                    buffer.position(oldPos)
                    String(array, StandardCharsets.UTF_8).trimEnd('\u0000', ' ')
                }
                3 -> { // SHORT
                    val valShort = buffer.getShort(inlineOffset).toInt() and 0xFFFF
                    valShort.toString()
                }
                4 -> { // LONG
                    val valInt = buffer.getInt(inlineOffset).toLong() and 0xFFFFFFFFL
                    valInt.toString()
                }
                5, 10 -> { // RATIONAL or SRATIONAL
                    val ratOffset = buffer.getInt(inlineOffset)
                    if (ratOffset < 0 || ratOffset + 8 * count > maxLen) return ""
                    val parts = mutableListOf<String>()
                    for (c in 0 until count) {
                        val num = buffer.getInt(ratOffset + c * 8)
                        val den = buffer.getInt(ratOffset + c * 8 + 4)
                        if (den != 0) {
                            parts.add("$num/$den")
                        } else {
                            parts.add("$num/1")
                        }
                    }
                    parts.joinToString(" ")
                }
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseGpsCoordinate(coordStr: String): Double? {
        return try {
            val parts = coordStr.split(Regex("[^0-9./-]+")).filter { it.isNotBlank() }
            if (parts.size >= 3) {
                val d = evalFraction(parts[0])
                val m = evalFraction(parts[1])
                val s = evalFraction(parts[2])
                d + (m / 60.0) + (s / 3600.0)
            } else if (parts.isNotEmpty()) {
                evalFraction(parts[0])
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun evalFraction(str: String): Double {
        return if (str.contains('/')) {
            val f = str.split('/')
            val num = f[0].toDouble()
            val den = f[1].toDouble()
            if (den != 0.0) num / den else 0.0
        } else {
            str.toDoubleOrNull() ?: 0.0
        }
    }

    companion object {
        private val TAG_NAMES = mapOf(
            0x010F to "Make",
            0x0110 to "Model",
            0x0112 to "Orientation",
            0x011A to "XResolution",
            0x011B to "YResolution",
            0x0128 to "ResolutionUnit",
            0x0131 to "Software",
            0x0132 to "Date/Time",
            0x0213 to "YCbCrPositioning",
            0x829A to "Exposure Time",
            0x829D to "F-Number",
            0x8822 to "Exposure Program",
            0x8827 to "ISO Speed",
            0x9000 to "Exif Version",
            0x9003 to "Date/Time Original",
            0x9004 to "Date/Time Digitized",
            0x920A to "Focal Length",
            0xA002 to "Pixel X Dimension",
            0xA003 to "Pixel Y Dimension",
            0x0000 to "GPSVersionID",
            0x0001 to "GPSLatitudeRef",
            0x0002 to "GPSLatitude",
            0x0003 to "GPSLongitudeRef",
            0x0004 to "GPSLongitude",
            0x0005 to "GPSAltitudeRef",
            0x0006 to "GPSAltitude"
        )
    }
}
