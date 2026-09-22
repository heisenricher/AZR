package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class GeoJsonValidatorInput(
    val geoJsonString: String = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": { "name": "Central Park", "category": "Park" },
              "geometry": {
                "type": "Polygon",
                "coordinates": [
                  [
                    [-73.973, 40.764],
                    [-73.981, 40.768],
                    [-73.958, 40.800],
                    [-73.949, 40.796],
                    [-73.973, 40.764]
                  ]
                ]
              }
            },
            {
              "type": "Feature",
              "properties": { "name": "Belvedere Castle", "elevation": 40 },
              "geometry": {
                "type": "Point",
                "coordinates": [-73.969, 40.779]
              }
            }
          ]
        }
    """.trimIndent()
)

data class GeoJsonValidatorOutput(
    val isValid: Boolean,
    val rootType: String,
    val featureCount: Int,
    val geometryCounts: Map<String, Int>,
    val totalCoordinates: Int,
    val boundingBox: List<Double>, // [minLon, minLat, maxLon, maxLat]
    val centroid: List<Double>,   // [centroidLon, centroidLat]
    val formattedReport: String,
    val summary: String
)

class GeoJsonValidatorTool : Tool<GeoJsonValidatorInput, GeoJsonValidatorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "geo_json_validator_tool",
        name = "GeoJSON Validator & Geometry Inspector",
        description = "Validate RFC 7946 GeoJSON geometries, compute bounding boxes [minX, minY, maxX, maxY], and calculate centroids.",
        category = ToolCategory.DATA,
        tags = listOf("geojson", "rfc7946", "gis", "coordinates", "geometry", "polygon", "point", "linestring", "bounding box", "centroid"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Layers"
    )

    override suspend fun execute(input: GeoJsonValidatorInput): ToolResult<GeoJsonValidatorOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.geoJsonString.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("GeoJSON payload cannot be empty.")
        }

        val json: JSONObject
        try {
            json = JSONObject(raw)
        } catch (e: Exception) {
            return ToolResult.Failure("Invalid JSON structure: ${e.message}")
        }

        val rootType = json.optString("type", "")
        if (rootType.isEmpty()) {
            return ToolResult.Failure("GeoJSON object must contain a 'type' property.")
        }

        val coordinatesList = mutableListOf<Pair<Double, Double>>()
        val geometryCounts = mutableMapOf<String, Int>()
        var featureCount = 0
        val validationErrors = mutableListOf<String>()

        when (rootType) {
            "FeatureCollection" -> {
                val features = json.optJSONArray("features")
                if (features == null) {
                    return ToolResult.Failure("FeatureCollection missing 'features' array.")
                }
                featureCount = features.length()
                for (i in 0 until features.length()) {
                    val feat = features.optJSONObject(i)
                    if (feat == null) {
                        validationErrors.add("Feature at index $i is not a valid JSON object.")
                        continue
                    }
                    val geom = feat.optJSONObject("geometry")
                    if (geom != null) {
                        processGeometry(geom, geometryCounts, coordinatesList, validationErrors)
                    }
                }
            }
            "Feature" -> {
                featureCount = 1
                val geom = json.optJSONObject("geometry")
                if (geom != null) {
                    processGeometry(geom, geometryCounts, coordinatesList, validationErrors)
                } else {
                    validationErrors.add("Feature has null or missing geometry.")
                }
            }
            else -> {
                // Direct geometry
                processGeometry(json, geometryCounts, coordinatesList, validationErrors)
            }
        }

        if (coordinatesList.isEmpty()) {
            return ToolResult.Failure("No valid coordinate pairs found in GeoJSON.")
        }

        // Validate coordinate bounds (lon: -180 to 180, lat: -90 to 90)
        var invalidRangePoints = 0
        for ((lon, lat) in coordinatesList) {
            if (lon < -180.0 || lon > 180.0 || lat < -90.0 || lat > 90.0) {
                invalidRangePoints++
            }
        }
        if (invalidRangePoints > 0) {
            validationErrors.add("$invalidRangePoints point(s) exceed WGS84 bounds (longitude [-180, 180], latitude [-90, 90]).")
        }

        val minLon = coordinatesList.minOf { it.first }
        val maxLon = coordinatesList.maxOf { it.first }
        val minLat = coordinatesList.minOf { it.second }
        val maxLat = coordinatesList.maxOf { it.second }
        val bbox = listOf(minLon, minLat, maxLon, maxLat)

        val avgLon = coordinatesList.sumOf { it.first } / coordinatesList.size
        val avgLat = coordinatesList.sumOf { it.second } / coordinatesList.size
        val centroid = listOf(avgLon, avgLat)

        val isValid = validationErrors.isEmpty()

        val report = buildString {
            appendLine("RFC 7946 GEOJSON VALIDATION & GEOMETRY REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Status:               ${if (isValid) "VALID GEOJSON" else "INVALID GEOJSON"}")
            appendLine("Root Object Type:     $rootType")
            appendLine("Total Features:       $featureCount")
            appendLine("Total Coordinates:    ${coordinatesList.size} point pairs")
            appendLine("--------------------------------------------------")
            appendLine("GEOMETRY BREAKDOWN:")
            geometryCounts.forEach { (type, count) ->
                appendLine(" - $type: $count")
            }
            appendLine("--------------------------------------------------")
            appendLine("BOUNDING BOX [minLon, minLat, maxLon, maxLat]:")
            appendLine("[${String.format(Locale.US, "%.6f", minLon)}, ${String.format(Locale.US, "%.6f", minLat)}, ${String.format(Locale.US, "%.6f", maxLon)}, ${String.format(Locale.US, "%.6f", maxLat)}]")
            appendLine("CENTROID COORDINATE [lon, lat]:")
            appendLine("[${String.format(Locale.US, "%.6f", avgLon)}, ${String.format(Locale.US, "%.6f", avgLat)}]")
            if (validationErrors.isNotEmpty()) {
                appendLine("--------------------------------------------------")
                appendLine("VALIDATION WARNINGS / ERRORS:")
                validationErrors.forEach { appendLine(" * $it") }
            }
        }

        val output = GeoJsonValidatorOutput(
            isValid = isValid,
            rootType = rootType,
            featureCount = featureCount,
            geometryCounts = geometryCounts,
            totalCoordinates = coordinatesList.size,
            boundingBox = bbox,
            centroid = centroid,
            formattedReport = report,
            summary = "GeoJSON ($rootType): ${coordinatesList.size} pts, bbox=[${String.format(Locale.US, "%.4f", minLon)}, ${String.format(Locale.US, "%.4f", minLat)}, ${String.format(Locale.US, "%.4f", maxLon)}, ${String.format(Locale.US, "%.4f", maxLat)}]"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "GeoJSON validation completed (${if (isValid) "Valid" else "Errors found"})"
        )
    }

    private fun processGeometry(
        geom: JSONObject,
        counts: MutableMap<String, Int>,
        coords: MutableList<Pair<Double, Double>>,
        errors: MutableList<String>
    ) {
        val type = geom.optString("type", "Unknown")
        counts[type] = counts.getOrDefault(type, 0) + 1

        val rawCoords = geom.optJSONArray("coordinates") ?: return

        when (type) {
            "Point" -> extractPoint(rawCoords, coords, errors)
            "MultiPoint", "LineString" -> {
                for (i in 0 until rawCoords.length()) {
                    rawCoords.optJSONArray(i)?.let { extractPoint(it, coords, errors) }
                }
            }
            "MultiLineString", "Polygon" -> {
                for (i in 0 until rawCoords.length()) {
                    val ring = rawCoords.optJSONArray(i) ?: continue
                    if (type == "Polygon" && ring.length() < 4) {
                        errors.add("Polygon linear ring must contain at least 4 positions.")
                    }
                    for (j in 0 until ring.length()) {
                        ring.optJSONArray(j)?.let { extractPoint(it, coords, errors) }
                    }
                }
            }
            "MultiPolygon" -> {
                for (i in 0 until rawCoords.length()) {
                    val poly = rawCoords.optJSONArray(i) ?: continue
                    for (j in 0 until poly.length()) {
                        val ring = poly.optJSONArray(j) ?: continue
                        for (k in 0 until ring.length()) {
                            ring.optJSONArray(k)?.let { extractPoint(it, coords, errors) }
                        }
                    }
                }
            }
            "GeometryCollection" -> {
                val geoms = geom.optJSONArray("geometries") ?: return
                for (i in 0 until geoms.length()) {
                    geoms.optJSONObject(i)?.let { processGeometry(it, counts, coords, errors) }
                }
            }
        }
    }

    private fun extractPoint(arr: JSONArray, coords: MutableList<Pair<Double, Double>>, errors: MutableList<String>) {
        if (arr.length() < 2) {
            errors.add("Point coordinate array must contain at least [lon, lat].")
            return
        }
        val lon = arr.optDouble(0, Double.NaN)
        val lat = arr.optDouble(1, Double.NaN)
        if (!lon.isNaN() && !lat.isNaN()) {
            coords.add(Pair(lon, lat))
        }
    }
}
