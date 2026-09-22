package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.min

data class PlusCodeInput(
    val operation: String = "ENCODE", // ENCODE or DECODE
    val latitude: Double = 37.774929,
    val longitude: Double = -122.419416,
    val plusCode: String = "849VQGW8+X6"
)

data class PlusCodeBoundingBox(
    val southLatitude: Double,
    val westLongitude: Double,
    val northLatitude: Double,
    val eastLongitude: Double,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val sizeLatMeters: Double,
    val sizeLonMeters: Double
)

data class PlusCodeOutput(
    val operation: String,
    val code: String,
    val latitude: Double,
    val longitude: Double,
    val boundingBox: PlusCodeBoundingBox,
    val formattedReport: String,
    val summary: String
)

class OpenLocationCodeTool : Tool<PlusCodeInput, PlusCodeOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "open_location_code_tool",
        name = "Open Location Code (Google Plus Codes) Codec",
        description = "Encode latitude/longitude coordinates into Google Plus Codes (Open Location Code / OLC) and decode Plus Codes into geographic bounding boxes offline.",
        category = ToolCategory.DATA,
        tags = listOf("plus codes", "open location code", "olc", "gis", "coordinates", "geocoding", "latitude", "longitude", "offline map"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "MapPin"
    )

    private val codeAlphabet = "23456789CFGHJMPQRVWX"
    private val encodingBase = 20

    private fun encode(lat: Double, lon: Double): String {
        var normLat = lat.coerceIn(-90.0, 90.0) + 90.0
        var normLon = lon
        while (normLon < -180.0) normLon += 360.0
        while (normLon >= 180.0) normLon -= 360.0
        normLon += 180.0

        val sb = StringBuilder()
        var latVal = normLat
        var lonVal = normLon

        var latGrid = 20.0
        var lonGrid = 20.0

        for (i in 0 until 5) {
            val latDigit = (latVal / latGrid).toInt().coerceIn(0, 19)
            val lonDigit = (lonVal / lonGrid).toInt().coerceIn(0, 19)

            sb.append(codeAlphabet[latDigit])
            sb.append(codeAlphabet[lonDigit])

            latVal -= latDigit * latGrid
            lonVal -= lonDigit * lonGrid

            latGrid /= 20.0
            lonGrid /= 20.0

            if (i == 3) {
                sb.append('+')
            }
        }
        return sb.toString()
    }

    private fun decode(code: String): PlusCodeBoundingBox {
        val clean = code.trim().uppercase(Locale.US).replace("+", "")
        if (clean.length < 2 || clean.length % 2 != 0) {
            throw IllegalArgumentException("Plus Code must have an even length of digits (excluding '+').")
        }

        var south = 0.0
        var west = 0.0
        var latGrid = 20.0
        var lonGrid = 20.0

        for (i in 0 until clean.length step 2) {
            val latIdx = codeAlphabet.indexOf(clean[i])
            val lonIdx = codeAlphabet.indexOf(clean[i + 1])
            if (latIdx == -1 || lonIdx == -1) {
                throw IllegalArgumentException("Invalid Plus Code character at index $i in '$code'.")
            }

            south += latIdx * latGrid
            west += lonIdx * lonGrid

            if (i + 2 < clean.length) {
                latGrid /= 20.0
                lonGrid /= 20.0
            }
        }

        val north = south + latGrid
        val east = west + lonGrid

        val sLat = south - 90.0
        val nLat = north - 90.0
        val wLon = west - 180.0
        val eLon = east - 180.0

        val cLat = (sLat + nLat) / 2.0
        val cLon = (wLon + eLon) / 2.0

        val latMeters = (nLat - sLat) * 111319.5
        val lonMeters = (eLon - wLon) * 111319.5 * kotlin.math.cos(Math.toRadians(cLat))

        return PlusCodeBoundingBox(
            southLatitude = sLat,
            westLongitude = wLon,
            northLatitude = nLat,
            eastLongitude = eLon,
            centerLatitude = cLat,
            centerLongitude = cLon,
            sizeLatMeters = kotlin.math.abs(latMeters),
            sizeLonMeters = kotlin.math.abs(lonMeters)
        )
    }

    override suspend fun execute(input: PlusCodeInput): ToolResult<PlusCodeOutput> {
        val startTime = System.currentTimeMillis()
        val op = input.operation.trim().uppercase(Locale.US)

        when (op) {
            "ENCODE" -> {
                val lat = input.latitude
                val lon = input.longitude
                if (lat < -90.0 || lat > 90.0) {
                    return ToolResult.Failure("Latitude must be between -90.0 and +90.0. Provided: $lat")
                }
                if (lon < -180.0 || lon > 180.0) {
                    return ToolResult.Failure("Longitude must be between -180.0 and +180.0. Provided: $lon")
                }

                val code = encode(lat, lon)
                val box = decode(code)

                val elapsed = System.currentTimeMillis() - startTime
                val report = buildString {
                    appendLine("=== OPEN LOCATION CODE (PLUS CODE) ENCODING ===")
                    appendLine("Generated Plus Code: $code")
                    appendLine("Source Coordinates: (${String.format(Locale.US, "%.6f", lat)}, ${String.format(Locale.US, "%.6f", lon)})")
                    appendLine("----------------------------------------")
                    appendLine("BOUNDING BOX AREA:")
                    appendLine("  South-West: (${String.format(Locale.US, "%.6f", box.southLatitude)}, ${String.format(Locale.US, "%.6f", box.westLongitude)})")
                    appendLine("  North-East: (${String.format(Locale.US, "%.6f", box.northLatitude)}, ${String.format(Locale.US, "%.6f", box.eastLongitude)})")
                    appendLine("  Cell Resolution: ~${String.format(Locale.US, "%.1f", box.sizeLonMeters)} m x ${String.format(Locale.US, "%.1f", box.sizeLatMeters)} m")
                }

                return ToolResult.Success(
                    data = PlusCodeOutput(
                        operation = "ENCODE",
                        code = code,
                        latitude = lat,
                        longitude = lon,
                        boundingBox = box,
                        formattedReport = report,
                        summary = "Encoded ($lat, $lon) to Plus Code '$code'."
                    ),
                    executionTimeMs = elapsed,
                    summary = "Encoded to '$code'"
                )
            }
            "DECODE" -> {
                val raw = input.plusCode.trim()
                if (raw.isBlank()) {
                    return ToolResult.Failure("Plus Code input cannot be empty.")
                }

                val box: PlusCodeBoundingBox
                try {
                    box = decode(raw)
                } catch (e: Exception) {
                    return ToolResult.Failure("Failed to decode Plus Code '$raw': ${e.message}")
                }

                val elapsed = System.currentTimeMillis() - startTime
                val report = buildString {
                    appendLine("=== OPEN LOCATION CODE (PLUS CODE) DECODING ===")
                    appendLine("Source Plus Code:  $raw")
                    appendLine("Decoded Center:    (${String.format(Locale.US, "%.6f", box.centerLatitude)}, ${String.format(Locale.US, "%.6f", box.centerLongitude)})")
                    appendLine("----------------------------------------")
                    appendLine("BOUNDING BOX EXTENTS:")
                    appendLine("  Latitude Span:  [${String.format(Locale.US, "%.6f", box.southLatitude)} .. ${String.format(Locale.US, "%.6f", box.northLatitude)}]")
                    appendLine("  Longitude Span: [${String.format(Locale.US, "%.6f", box.westLongitude)} .. ${String.format(Locale.US, "%.6f", box.eastLongitude)}]")
                    appendLine("  Cell Resolution: ~${String.format(Locale.US, "%.1f", box.sizeLonMeters)} m x ${String.format(Locale.US, "%.1f", box.sizeLatMeters)} m")
                }

                return ToolResult.Success(
                    data = PlusCodeOutput(
                        operation = "DECODE",
                        code = raw,
                        latitude = box.centerLatitude,
                        longitude = box.centerLongitude,
                        boundingBox = box,
                        formattedReport = report,
                        summary = "Decoded '$raw' to Center (${String.format(Locale.US, "%.5f", box.centerLatitude)}, ${String.format(Locale.US, "%.5f", box.centerLongitude)})."
                    ),
                    executionTimeMs = elapsed,
                    summary = "Decoded '$raw' to Lat/Lon"
                )
            }
            else -> return ToolResult.Failure("Unknown operation '$op'. Supported: ENCODE, DECODE.")
        }
    }
}
