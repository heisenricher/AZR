package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class GeoHashInput(
    val operation: String = "ENCODE", // ENCODE or DECODE
    val latitude: Double = 37.774929,
    val longitude: Double = -122.419416,
    val precision: Int = 9,
    val geohash: String = "9q8yyk8y5"
)

data class GeoHashBoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val centerLat: Double,
    val centerLon: Double,
    val latError: Double,
    val lonError: Double
)

data class GeoHashNeighbor(
    val direction: String,
    val hash: String
)

data class GeoHashOutput(
    val operation: String,
    val geohash: String,
    val latitude: Double,
    val longitude: Double,
    val precision: Int,
    val boundingBox: GeoHashBoundingBox,
    val neighbors: List<GeoHashNeighbor>,
    val approxWidthKm: Double,
    val approxHeightKm: Double,
    val formattedReport: String,
    val summary: String
)

class GeoHashCodecTool : Tool<GeoHashInput, GeoHashOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "geohash_codec_tool",
        name = "GeoHash Spatial Index Codec",
        description = "Encode latitude/longitude coordinates to Geohash spatial index strings, decode to coordinate bounding boxes with resolution errors, and compute 8-neighbor grids.",
        category = ToolCategory.DATA,
        tags = listOf("geohash", "gis", "coordinates", "latitude", "longitude", "spatial", "bounding box", "neighbors", "geo"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "MapPin"
    )

    private val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    private val charMap = base32.withIndex().associate { it.value to it.index }

    private fun decodeToBox(hash: String): GeoHashBoundingBox {
        var isEven = true
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0

        val cleanHash = hash.trim().lowercase(Locale.US)
        for (c in cleanHash) {
            val cd = charMap[c] ?: 0
            for (j in 4 downTo 0) {
                val mask = 1 shl j
                if (isEven) {
                    val lonMid = (lonMin + lonMax) / 2
                    if ((cd and mask) != 0) {
                        lonMin = lonMid
                    } else {
                        lonMax = lonMid
                    }
                } else {
                    val latMid = (latMin + latMax) / 2
                    if ((cd and mask) != 0) {
                        latMin = latMid
                    } else {
                        latMax = latMid
                    }
                }
                isEven = !isEven
            }
        }

        val cLat = (latMin + latMax) / 2.0
        val cLon = (lonMin + lonMax) / 2.0
        return GeoHashBoundingBox(
            minLat = latMin,
            maxLat = latMax,
            minLon = lonMin,
            maxLon = lonMax,
            centerLat = cLat,
            centerLon = cLon,
            latError = (latMax - latMin) / 2.0,
            lonError = (lonMax - lonMin) / 2.0
        )
    }

    private fun encode(lat: Double, lon: Double, precision: Int): String {
        var isEven = true
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0

        val hash = StringBuilder()
        var ch = 0
        var bit = 0

        while (hash.length < precision) {
            if (isEven) {
                val lonMid = (lonMin + lonMax) / 2
                if (lon >= lonMid) {
                    ch = ch or (1 shl (4 - bit))
                    lonMin = lonMid
                } else {
                    lonMax = lonMid
                }
            } else {
                val latMid = (latMin + latMax) / 2
                if (lat >= latMid) {
                    ch = ch or (1 shl (4 - bit))
                    latMin = latMid
                } else {
                    latMax = latMid
                }
            }

            isEven = !isEven
            bit++
            if (bit == 5) {
                hash.append(base32[ch])
                bit = 0
                ch = 0
            }
        }
        return hash.toString()
    }

    private fun getNeighbors(box: GeoHashBoundingBox, precision: Int): List<GeoHashNeighbor> {
        val latSpan = box.maxLat - box.minLat
        val lonSpan = box.maxLon - box.minLon

        val directions = listOf(
            Triple("North", 1.0, 0.0),
            Triple("North-East", 1.0, 1.0),
            Triple("East", 0.0, 1.0),
            Triple("South-East", -1.0, 1.0),
            Triple("South", -1.0, 0.0),
            Triple("South-West", -1.0, -1.0),
            Triple("West", 0.0, -1.0),
            Triple("North-West", 1.0, -1.0)
        )

        return directions.map { (dir, dLatMult, dLonMult) ->
            var nLat = box.centerLat + dLatMult * latSpan
            var nLon = box.centerLon + dLonMult * lonSpan
            if (nLat > 90.0) nLat = 90.0
            if (nLat < -90.0) nLat = -90.0
            if (nLon > 180.0) nLon -= 360.0
            if (nLon < -180.0) nLon += 360.0
            GeoHashNeighbor(dir, encode(nLat, nLon, precision))
        }
    }

    override suspend fun execute(input: GeoHashInput): ToolResult<GeoHashOutput> {
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
                val prec = input.precision.coerceIn(1, 12)
                val hash = encode(lat, lon, prec)
                val box = decodeToBox(hash)
                val neighbors = getNeighbors(box, prec)

                // Approx km: 1 deg lat ~ 111 km, 1 deg lon ~ 111 * cos(lat) km
                val heightKm = (box.maxLat - box.minLat) * 111.0
                val widthKm = (box.maxLon - box.minLon) * 111.0 * kotlin.math.cos(Math.toRadians(box.centerLat))

                val elapsed = System.currentTimeMillis() - startTime
                val report = buildString {
                    appendLine("=== GEOHASH ENCODING ===")
                    appendLine("Target Geohash: $hash (Precision: $prec chars)")
                    appendLine("Coordinates: (${String.format(Locale.US, "%.6f", lat)}, ${String.format(Locale.US, "%.6f", lon)})")
                    appendLine("Bounding Box:")
                    appendLine("  Latitude:  [${String.format(Locale.US, "%.6f", box.minLat)} .. ${String.format(Locale.US, "%.6f", box.maxLat)}] (±${String.format(Locale.US, "%.6f", box.latError)})")
                    appendLine("  Longitude: [${String.format(Locale.US, "%.6f", box.minLon)} .. ${String.format(Locale.US, "%.6f", box.maxLon)}] (±${String.format(Locale.US, "%.6f", box.lonError)})")
                    appendLine("Cell Dimensions: ~${String.format(Locale.US, "%.3f", widthKm)} km x ${String.format(Locale.US, "%.3f", heightKm)} km")
                    appendLine("----------------------------------------")
                    appendLine("8 Surrounding Neighbors:")
                    neighbors.forEach { n ->
                        appendLine("  %-12s : %s".format(Locale.US, n.direction, n.hash))
                    }
                }

                return ToolResult.Success(
                    data = GeoHashOutput(
                        operation = "ENCODE",
                        geohash = hash,
                        latitude = lat,
                        longitude = lon,
                        precision = prec,
                        boundingBox = box,
                        neighbors = neighbors,
                        approxWidthKm = kotlin.math.abs(widthKm),
                        approxHeightKm = kotlin.math.abs(heightKm),
                        formattedReport = report,
                        summary = "Encoded ($lat, $lon) to Geohash '$hash' (cell: ~${String.format(Locale.US, "%.2f", widthKm)}x${String.format(Locale.US, "%.2f", heightKm)} km)."
                    ),
                    executionTimeMs = elapsed,
                    summary = "Encoded coordinates to '$hash'"
                )
            }
            "DECODE" -> {
                val clean = input.geohash.trim().lowercase(Locale.US)
                if (clean.isBlank()) {
                    return ToolResult.Failure("Geohash input cannot be empty.")
                }
                for (c in clean) {
                    if (c !in base32) {
                        return ToolResult.Failure("Invalid Geohash character '$c'. Must be in base32: $base32")
                    }
                }
                val box = decodeToBox(clean)
                val prec = clean.length
                val neighbors = getNeighbors(box, prec)

                val heightKm = (box.maxLat - box.minLat) * 111.0
                val widthKm = (box.maxLon - box.minLon) * 111.0 * kotlin.math.cos(Math.toRadians(box.centerLat))

                val elapsed = System.currentTimeMillis() - startTime
                val report = buildString {
                    appendLine("=== GEOHASH DECODING ===")
                    appendLine("Source Geohash: $clean (Length: $prec)")
                    appendLine("Decoded Center: (${String.format(Locale.US, "%.6f", box.centerLat)}, ${String.format(Locale.US, "%.6f", box.centerLon)})")
                    appendLine("Lat Margin: ±${String.format(Locale.US, "%.6f", box.latError)} deg | Lon Margin: ±${String.format(Locale.US, "%.6f", box.lonError)} deg")
                    appendLine("Bounding Box:")
                    appendLine("  South-West: (${String.format(Locale.US, "%.6f", box.minLat)}, ${String.format(Locale.US, "%.6f", box.minLon)})")
                    appendLine("  North-East: (${String.format(Locale.US, "%.6f", box.maxLat)}, ${String.format(Locale.US, "%.6f", box.maxLon)})")
                    appendLine("Cell Dimensions: ~${String.format(Locale.US, "%.3f", widthKm)} km x ${String.format(Locale.US, "%.3f", heightKm)} km")
                    appendLine("----------------------------------------")
                    appendLine("8 Surrounding Neighbors:")
                    neighbors.forEach { n ->
                        appendLine("  %-12s : %s".format(Locale.US, n.direction, n.hash))
                    }
                }

                return ToolResult.Success(
                    data = GeoHashOutput(
                        operation = "DECODE",
                        geohash = clean,
                        latitude = box.centerLat,
                        longitude = box.centerLon,
                        precision = prec,
                        boundingBox = box,
                        neighbors = neighbors,
                        approxWidthKm = kotlin.math.abs(widthKm),
                        approxHeightKm = kotlin.math.abs(heightKm),
                        formattedReport = report,
                        summary = "Decoded '$clean' to Center (${String.format(Locale.US, "%.5f", box.centerLat)}, ${String.format(Locale.US, "%.5f", box.centerLon)})."
                    ),
                    executionTimeMs = elapsed,
                    summary = "Decoded '$clean' to lat/lon box"
                )
            }
            else -> {
                return ToolResult.Failure("Unknown operation '$op'. Supported: ENCODE, DECODE.")
            }
        }
    }
}
