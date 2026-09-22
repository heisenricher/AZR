package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

data class Coordinate2D(val x: Double, val y: Double) {
    override fun toString(): String = String.format(Locale.US, "(%.6f, %.6f)", x, y)
}

data class BoundingBox2D(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double
) {
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
    override fun toString(): String = String.format(
        Locale.US,
        "[MinX: %.4f, MinY: %.4f, MaxX: %.4f, MaxY: %.4f]",
        minX, minY, maxX, maxY
    )
}

data class WktGeometryInput(
    val wktString: String = "POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))"
)

data class WktGeometryOutput(
    val geometryType: String,
    val pointCount: Int,
    val boundingBox: BoundingBox2D,
    val lengthOrPerimeter: Double,
    val area: Double,
    val centroid: Coordinate2D,
    val geoJson: String,
    val formattedReport: String,
    val summary: String
)

class WktGeometryParserTool : Tool<WktGeometryInput, WktGeometryOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "wkt_geometry_parser_tool",
        name = "WKT Geometry Parser & GeoJSON Converter",
        description = "Parse OGC Well-Known Text (WKT) 2D geometries (POINT, LINESTRING, POLYGON, MULTIPOINT), calculate perimeter/area/bounds, and convert to GeoJSON.",
        category = ToolCategory.DATA,
        tags = listOf("wkt", "geometry", "gis", "geojson", "polygon", "linestring", "coordinates", "spatial", "bounding box"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "MapPin"
    )

    override suspend fun execute(input: WktGeometryInput): ToolResult<WktGeometryOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.wktString.trim()

        if (raw.isBlank()) {
            return ToolResult.Failure("WKT input string cannot be empty.")
        }

        val typeRegex = Regex("""^([A-Za-z]+)\s*\((.*)\)$""", RegexOption.DOT_MATCHES_ALL)
        val match = typeRegex.find(raw)
            ?: return ToolResult.Failure("Invalid WKT syntax. Expected GEOMETRYTYPE (...) format, e.g., 'POINT (10 20)' or 'POLYGON ((...))'.")

        val geomType = match.groupValues[1].uppercase(Locale.US)
        val innerContent = match.groupValues[2].trim()

        return try {
            when (geomType) {
                "POINT" -> parsePoint(innerContent, startTime)
                "LINESTRING" -> parseLineString(innerContent, startTime)
                "POLYGON" -> parsePolygon(innerContent, startTime)
                "MULTIPOINT" -> parseMultiPoint(innerContent, startTime)
                else -> ToolResult.Failure("Unsupported WKT geometry type '$geomType'. Supported types: POINT, LINESTRING, POLYGON, MULTIPOINT.")
            }
        } catch (e: Exception) {
            ToolResult.Failure("Error parsing WKT coordinates: ${e.message}")
        }
    }

    private fun parseCoords(str: String): List<Coordinate2D> {
        val pairs = str.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return pairs.map { pair ->
            val nums = pair.replace("(", "").replace(")", "").trim().split(Regex("""\s+"""))
            if (nums.size < 2) throw IllegalArgumentException("Malformed coordinate pair: '$pair'")
            val x = nums[0].toDoubleOrNull() ?: throw IllegalArgumentException("Invalid X coordinate '${nums[0]}'")
            val y = nums[1].toDoubleOrNull() ?: throw IllegalArgumentException("Invalid Y coordinate '${nums[1]}'")
            Coordinate2D(x, y)
        }
    }

    private fun computeBounds(coords: List<Coordinate2D>): BoundingBox2D {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (c in coords) {
            if (c.x < minX) minX = c.x
            if (c.x > maxX) maxX = c.x
            if (c.y < minY) minY = c.y
            if (c.y > maxY) maxY = c.y
        }
        return BoundingBox2D(minX, minY, maxX, maxY)
    }

    private fun parsePoint(inner: String, startTime: Long): ToolResult<WktGeometryOutput> {
        val coords = parseCoords(inner)
        if (coords.isEmpty()) throw IllegalArgumentException("Point has no coordinates.")
        val p = coords.first()
        val bbox = BoundingBox2D(p.x, p.y, p.x, p.y)
        val geoJson = """{"type": "Point", "coordinates": [${p.x}, ${p.y}]}"""

        val report = buildString {
            appendLine("OGC WELL-KNOWN TEXT (WKT) PARSER REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Geometry Type:     POINT")
            appendLine("Coordinates:       X = ${p.x}, Y = ${p.y}")
            appendLine("Bounding Box:      $bbox")
            appendLine("--------------------------------------------------")
            appendLine("GeoJSON:")
            appendLine(geoJson)
        }

        return ToolResult.Success(
            data = WktGeometryOutput(
                geometryType = "POINT",
                pointCount = 1,
                boundingBox = bbox,
                lengthOrPerimeter = 0.0,
                area = 0.0,
                centroid = p,
                geoJson = geoJson,
                formattedReport = report,
                summary = "POINT at (${p.x}, ${p.y})"
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Parsed WKT POINT"
        )
    }

    private fun parseLineString(inner: String, startTime: Long): ToolResult<WktGeometryOutput> {
        val coords = parseCoords(inner)
        if (coords.size < 2) throw IllegalArgumentException("LineString must contain at least 2 points.")

        var totalLen = 0.0
        var sumX = 0.0
        var sumY = 0.0
        for (i in 0 until coords.size - 1) {
            val dx = coords[i + 1].x - coords[i].x
            val dy = coords[i + 1].y - coords[i].y
            totalLen += hypot(dx, dy)
            sumX += coords[i].x
            sumY += coords[i].y
        }
        sumX += coords.last().x
        sumY += coords.last().y

        val bbox = computeBounds(coords)
        val centroid = Coordinate2D(sumX / coords.size, sumY / coords.size)
        val coordsJson = coords.joinToString(", ") { "[${it.x}, ${it.y}]" }
        val geoJson = """{"type": "LineString", "coordinates": [$coordsJson]}"""

        val report = buildString {
            appendLine("OGC WELL-KNOWN TEXT (WKT) PARSER REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Geometry Type:     LINESTRING")
            appendLine("Vertex Count:      ${coords.size}")
            appendLine(String.format(Locale.US, "Total Length:      %.4f units", totalLen))
            appendLine(String.format(Locale.US, "Centroid:          (%.4f, %.4f)", centroid.x, centroid.y))
            appendLine("Bounding Box:      $bbox")
            appendLine("--------------------------------------------------")
            appendLine("GeoJSON:")
            appendLine(geoJson)
        }

        return ToolResult.Success(
            data = WktGeometryOutput(
                geometryType = "LINESTRING",
                pointCount = coords.size,
                boundingBox = bbox,
                lengthOrPerimeter = totalLen,
                area = 0.0,
                centroid = centroid,
                geoJson = geoJson,
                formattedReport = report,
                summary = String.format(Locale.US, "LINESTRING (%d vertices, length: %.2f)", coords.size, totalLen)
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Parsed WKT LINESTRING"
        )
    }

    private fun parsePolygon(inner: String, startTime: Long): ToolResult<WktGeometryOutput> {
        // Extract rings: e.g., ((x y, x y), (x y, x y))
        val ringRegex = Regex("""\(([^()]+)\)""")
        val rings = ringRegex.findAll(inner).map { it.groupValues[1] }.toList()

        if (rings.isEmpty()) throw IllegalArgumentException("Polygon must have at least one ring enclosed in parentheses.")

        val outerCoords = parseCoords(rings.first())
        if (outerCoords.size < 4) throw IllegalArgumentException("Polygon ring must have at least 4 coordinate pairs (first and last matching).")

        // Shoelace formula for area
        var doubleArea = 0.0
        var perimeter = 0.0
        var sumX = 0.0
        var sumY = 0.0

        for (i in 0 until outerCoords.size - 1) {
            val c1 = outerCoords[i]
            val c2 = outerCoords[i + 1]
            val cross = (c1.x * c2.y) - (c2.x * c1.y)
            doubleArea += cross
            perimeter += hypot(c2.x - c1.x, c2.y - c1.y)
            sumX += c1.x
            sumY += c1.y
        }
        val area = abs(doubleArea) / 2.0
        val bbox = computeBounds(outerCoords)
        val centroid = Coordinate2D(sumX / (outerCoords.size - 1), sumY / (outerCoords.size - 1))

        val ringsJson = rings.joinToString(", ") { ringStr ->
            val cList = parseCoords(ringStr)
            val ringJson = cList.joinToString(", ") { "[${it.x}, ${it.y}]" }
            "[$ringJson]"
        }
        val geoJson = """{"type": "Polygon", "coordinates": [$ringsJson]}"""

        val report = buildString {
            appendLine("OGC WELL-KNOWN TEXT (WKT) PARSER REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Geometry Type:     POLYGON")
            appendLine("Rings:             ${rings.size} (1 exterior${if (rings.size > 1) ", ${rings.size - 1} interior" else ""})")
            appendLine("Exterior Vertices: ${outerCoords.size}")
            appendLine(String.format(Locale.US, "Planar Area:       %.4f sq units", area))
            appendLine(String.format(Locale.US, "Perimeter:         %.4f units", perimeter))
            appendLine(String.format(Locale.US, "Centroid:          (%.4f, %.4f)", centroid.x, centroid.y))
            appendLine("Bounding Box:      $bbox")
            appendLine("--------------------------------------------------")
            appendLine("GeoJSON:")
            appendLine(geoJson)
        }

        return ToolResult.Success(
            data = WktGeometryOutput(
                geometryType = "POLYGON",
                pointCount = outerCoords.size,
                boundingBox = bbox,
                lengthOrPerimeter = perimeter,
                area = area,
                centroid = centroid,
                geoJson = geoJson,
                formattedReport = report,
                summary = String.format(Locale.US, "POLYGON (Area: %.2f, Perimeter: %.2f)", area, perimeter)
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Parsed WKT POLYGON"
        )
    }

    private fun parseMultiPoint(inner: String, startTime: Long): ToolResult<WktGeometryOutput> {
        val coords = parseCoords(inner)
        if (coords.isEmpty()) throw IllegalArgumentException("MultiPoint must contain at least 1 point.")

        val bbox = computeBounds(coords)
        val centroid = Coordinate2D(
            coords.map { it.x }.average(),
            coords.map { it.y }.average()
        )
        val coordsJson = coords.joinToString(", ") { "[${it.x}, ${it.y}]" }
        val geoJson = """{"type": "MultiPoint", "coordinates": [$coordsJson]}"""

        val report = buildString {
            appendLine("OGC WELL-KNOWN TEXT (WKT) PARSER REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Geometry Type:     MULTIPOINT")
            appendLine("Point Count:       ${coords.size}")
            appendLine(String.format(Locale.US, "Centroid:          (%.4f, %.4f)", centroid.x, centroid.y))
            appendLine("Bounding Box:      $bbox")
            appendLine("--------------------------------------------------")
            appendLine("GeoJSON:")
            appendLine(geoJson)
        }

        return ToolResult.Success(
            data = WktGeometryOutput(
                geometryType = "MULTIPOINT",
                pointCount = coords.size,
                boundingBox = bbox,
                lengthOrPerimeter = 0.0,
                area = 0.0,
                centroid = centroid,
                geoJson = geoJson,
                formattedReport = report,
                summary = "MULTIPOINT (${coords.size} points)"
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Parsed WKT MULTIPOINT"
        )
    }
}
