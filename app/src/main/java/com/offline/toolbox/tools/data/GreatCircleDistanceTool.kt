package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GreatCircleInput(
    val lat1: Double = 37.774929,  // San Francisco
    val lon1: Double = -122.419416,
    val lat2: Double = 40.712776,  // New York City
    val lon2: Double = -74.005974,
    val calculationModel: String = "WGS84_ELLIPSOID" // WGS84_ELLIPSOID or SPHERICAL_HAVERSINE
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class GreatCircleOutput(
    val distanceKm: Double,
    val distanceMiles: Double,
    val distanceNauticalMiles: Double,
    val distanceMeters: Double,
    val initialBearingDeg: Double,
    val initialCompassHeading: String,
    val finalBearingDeg: Double,
    val finalCompassHeading: String,
    val midpoint: GeoPoint,
    val antipodalOrigin: GeoPoint,
    val modelUsed: String,
    val formattedReport: String,
    val summary: String
)

class GreatCircleDistanceTool : Tool<GreatCircleInput, GreatCircleOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "great_circle_distance_tool",
        name = "Great-Circle Geodesic Distance & Bearing Navigator",
        description = "Compute great-circle geodesic distances, initial/final azimuth bearings, 16-point compass headings, and midpoints between geographic coordinates using Haversine and WGS-84 geodesic algorithms.",
        category = ToolCategory.DATA,
        tags = listOf("great circle", "haversine", "distance", "bearing", "geodesic", "gis", "coordinates", "gps", "navigation", "flight path"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Navigation"
    )

    private val compassRose16 = arrayOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    )

    private fun degreesToCompass(deg: Double): String {
        val norm = (deg % 360.0 + 360.0) % 360.0
        val idx = ((norm + 11.25) / 22.5).toInt() % 16
        return compassRose16[idx]
    }

    private fun toRad(deg: Double): Double = deg * PI / 180.0
    private fun toDeg(rad: Double): Double = rad * 180.0 / PI

    // Haversine formula (Mean Earth Radius = 6371.0088 km)
    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0088
        val phi1 = toRad(lat1)
        val phi2 = toRad(lat2)
        val deltaPhi = toRad(lat2 - lat1)
        val deltaLambda = toRad(lon2 - lon1)

        val a = sin(deltaPhi / 2.0) * sin(deltaPhi / 2.0) +
            cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0) * sin(deltaLambda / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }

    // Vincenty inverse solution for WGS-84 ellipsoid
    private fun vincentyDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val a = 6378137.0 // semi-major axis (meters)
        val f = 1.0 / 298.257223563 // flattening
        val b = (1.0 - f) * a

        val phi1 = toRad(lat1)
        val phi2 = toRad(lat2)
        val u1 = atan2((1.0 - f) * sin(phi1), cos(phi1))
        val u2 = atan2((1.0 - f) * sin(phi2), cos(phi2))
        val l = toRad(lon2 - lon1)

        var lambda = l
        var lambdaP: Double
        var iterLimit = 100

        var sinSigma: Double
        var cosSigma: Double
        var sigma: Double
        var sinAlpha: Double
        var cosSqAlpha: Double
        var cos2SigmaM: Double

        do {
            val sinLambda = sin(lambda)
            val cosLambda = cos(lambda)
            sinSigma = sqrt(
                (cos(u2) * sinLambda) * (cos(u2) * sinLambda) +
                    (cos(u1) * sin(u2) - sin(u1) * cos(u2) * cosLambda) *
                    (cos(u1) * sin(u2) - sin(u1) * cos(u2) * cosLambda)
            )
            if (sinSigma == 0.0) return 0.0 // coincident points

            cosSigma = sin(u1) * sin(u2) + cos(u1) * cos(u2) * cosLambda
            sigma = atan2(sinSigma, cosSigma)
            sinAlpha = cos(u1) * cos(u2) * sinLambda / sinSigma
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha
            cos2SigmaM = if (cosSqAlpha != 0.0) {
                cosSigma - 2.0 * sin(u1) * sin(u2) / cosSqAlpha
            } else 0.0

            val cCoeff = f / 16.0 * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha))
            lambdaP = lambda
            lambda = l + (1.0 - cCoeff) * f * sinAlpha *
                (sigma + cCoeff * sinSigma * (cos2SigmaM + cCoeff * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)))
        } while (kotlin.math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0)

        if (iterLimit == 0) {
            // Fallback to Haversine if non-convergent (e.g. nearly antipodal)
            return haversineDistanceKm(lat1, lon1, lat2, lon2)
        }

        val uSq = cosSqAlpha * (a * a - b * b) / (b * b)
        val bigA = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)))
        val bigB = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)))
        val deltaSigma = bigB * sinSigma * (cos2SigmaM + bigB / 4.0 *
            (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
                bigB / 6.0 * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)))

        val s = b * bigA * (sigma - deltaSigma) // distance in meters
        return s / 1000.0 // return km
    }

    private fun calculateInitialBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = toRad(lat1)
        val phi2 = toRad(lat2)
        val deltaLambda = toRad(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return (toDeg(theta) + 360.0) % 360.0
    }

    private fun calculateMidpoint(lat1: Double, lon1: Double, lat2: Double, lon2: Double): GeoPoint {
        val phi1 = toRad(lat1)
        val phi2 = toRad(lat2)
        val lambda1 = toRad(lon1)
        val deltaLambda = toRad(lon2 - lon1)

        val bx = cos(phi2) * cos(deltaLambda)
        val by = cos(phi2) * sin(deltaLambda)

        val midPhi = atan2(
            sin(phi1) + sin(phi2),
            sqrt((cos(phi1) + bx) * (cos(phi1) + bx) + by * by)
        )
        val midLambda = lambda1 + atan2(by, cos(phi1) + bx)

        val midLat = toDeg(midPhi)
        var midLon = toDeg(midLambda)
        while (midLon > 180.0) midLon -= 360.0
        while (midLon < -180.0) midLon += 360.0

        return GeoPoint(midLat, midLon)
    }

    override suspend fun execute(input: GreatCircleInput): ToolResult<GreatCircleOutput> {
        val startTime = System.currentTimeMillis()

        if (input.lat1 !in -90.0..90.0 || input.lat2 !in -90.0..90.0) {
            return ToolResult.Failure("Latitude values must be between -90.0 and +90.0 degrees.")
        }
        if (input.lon1 !in -180.0..180.0 || input.lon2 !in -180.0..180.0) {
            return ToolResult.Failure("Longitude values must be between -180.0 and +180.0 degrees.")
        }

        val useVincenty = !input.calculationModel.trim().uppercase(Locale.US).contains("SPHERICAL")
        val distKm = if (useVincenty) {
            vincentyDistanceKm(input.lat1, input.lon1, input.lat2, input.lon2)
        } else {
            haversineDistanceKm(input.lat1, input.lon1, input.lat2, input.lon2)
        }

        val distMiles = distKm * 0.621371192
        val distNm = distKm / 1.852
        val distMeters = distKm * 1000.0

        val initBearing = calculateInitialBearing(input.lat1, input.lon1, input.lat2, input.lon2)
        val revBearing = calculateInitialBearing(input.lat2, input.lon2, input.lat1, input.lon1)
        val finalBearing = (revBearing + 180.0) % 360.0

        val initCompass = degreesToCompass(initBearing)
        val finalCompass = degreesToCompass(finalBearing)

        val midPoint = calculateMidpoint(input.lat1, input.lon1, input.lat2, input.lon2)

        // Antipodal of origin: -lat, (lon + 180) mod 360
        var antiLon = input.lon1 + 180.0
        if (antiLon > 180.0) antiLon -= 360.0
        val antipodal = GeoPoint(-input.lat1, antiLon)

        val modelName = if (useVincenty) "WGS-84 Ellipsoidal Geodesic (Vincenty)" else "Spherical Haversine (R = 6371.01 km)"
        val elapsed = System.currentTimeMillis() - startTime

        val report = buildString {
            appendLine("=== GREAT-CIRCLE GEODESIC NAVIGATION REPORT ===")
            appendLine("Model:               $modelName")
            appendLine("Origin (P1):         ${String.format(Locale.US, "%.6f°", input.lat1)}, ${String.format(Locale.US, "%.6f°", input.lon1)}")
            appendLine("Destination (P2):    ${String.format(Locale.US, "%.6f°", input.lat2)}, ${String.format(Locale.US, "%.6f°", input.lon2)}")
            appendLine("----------------------------------------")
            appendLine("GEODESIC DISTANCE:")
            appendLine("  Kilometers:        ${String.format(Locale.US, "%,.3f", distKm)} km")
            appendLine("  Statute Miles:     ${String.format(Locale.US, "%,.3f", distMiles)} mi")
            appendLine("  Nautical Miles:    ${String.format(Locale.US, "%,.3f", distNm)} NM")
            appendLine("  Meters:            ${String.format(Locale.US, "%,.1f", distMeters)} m")
            appendLine("----------------------------------------")
            appendLine("AZIMUTH / BEARING:")
            appendLine("  Initial Course:    ${String.format(Locale.US, "%.2f°", initBearing)} ($initCompass)")
            appendLine("  Final Course:      ${String.format(Locale.US, "%.2f°", finalBearing)} ($finalCompass)")
            appendLine("----------------------------------------")
            appendLine("NAVIGATION WAYPOINTS:")
            appendLine("  Great-Circle Midpoint: ${String.format(Locale.US, "%.6f°", midPoint.latitude)}, ${String.format(Locale.US, "%.6f°", midPoint.longitude)}")
            appendLine("  Antipodal Origin:      ${String.format(Locale.US, "%.6f°", antipodal.latitude)}, ${String.format(Locale.US, "%.6f°", antipodal.longitude)}")
        }

        val summaryText = "${String.format(Locale.US, "%,.1f", distKm)} km (${String.format(Locale.US, "%,.1f", distNm)} NM) -> Course: ${String.format(Locale.US, "%.1f°", initBearing)} ($initCompass)"

        return ToolResult.Success(
            data = GreatCircleOutput(
                distanceKm = distKm,
                distanceMiles = distMiles,
                distanceNauticalMiles = distNm,
                distanceMeters = distMeters,
                initialBearingDeg = initBearing,
                initialCompassHeading = initCompass,
                finalBearingDeg = finalBearing,
                finalCompassHeading = finalCompass,
                midpoint = midPoint,
                antipodalOrigin = antipodal,
                modelUsed = modelName,
                formattedReport = report,
                summary = summaryText
            ),
            executionTimeMs = elapsed,
            summary = summaryText
        )
    }
}
