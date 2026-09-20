package com.offline.toolbox.tools.color

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.sqrt

data class NamedColorEntry(
    val name: String,
    val hex: String,
    val r: Int,
    val g: Int,
    val b: Int
)

data class HtmlColorNameInput(
    val queryOrHex: String = "#3B82F6"
)

data class HtmlColorNameOutput(
    val query: String,
    val closestNamedColor: String,
    val closestHex: String,
    val euclideanDistance: Double,
    val exactMatch: Boolean,
    val allMatches: List<NamedColorEntry>,
    val formattedReport: String,
    val summary: String
)

class HtmlColorNameTool : Tool<HtmlColorNameInput, HtmlColorNameOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "html_color_name_tool",
        name = "CSS & HTML Named Color Matcher",
        description = "Lookup 140+ official W3C/CSS named colors or find the nearest named color match for any custom hex code.",
        category = ToolCategory.COLOR,
        tags = listOf("color", "name", "css", "html", "w3c", "hex", "rgb", "palette", "rebeccapurple"),
        inputType = ToolDataType.COLOR,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "ColorLens"
    )

    private val namedColors = listOf(
        NamedColorEntry("aliceblue", "#F0F8FF", 240, 248, 255),
        NamedColorEntry("antiquewhite", "#FAEBD7", 250, 235, 215),
        NamedColorEntry("aqua", "#00FFFF", 0, 255, 255),
        NamedColorEntry("aquamarine", "#7FFFD4", 127, 255, 212),
        NamedColorEntry("azure", "#F0FFFF", 240, 255, 255),
        NamedColorEntry("beige", "#F5F5DC", 245, 245, 220),
        NamedColorEntry("bisque", "#FFE4C4", 255, 228, 196),
        NamedColorEntry("black", "#000000", 0, 0, 0),
        NamedColorEntry("blanchedalmond", "#FFEBCD", 255, 235, 205),
        NamedColorEntry("blue", "#0000FF", 0, 0, 255),
        NamedColorEntry("blueviolet", "#8A2BE2", 138, 43, 226),
        NamedColorEntry("brown", "#A52A2A", 165, 42, 42),
        NamedColorEntry("burlywood", "#DEB887", 222, 184, 135),
        NamedColorEntry("cadetblue", "#5F9EA0", 95, 158, 160),
        NamedColorEntry("chartreuse", "#7FFF00", 127, 255, 0),
        NamedColorEntry("chocolate", "#D2691E", 210, 105, 30),
        NamedColorEntry("coral", "#FF7F50", 255, 127, 80),
        NamedColorEntry("cornflowerblue", "#6495ED", 100, 149, 237),
        NamedColorEntry("cornsilk", "#FFF8DC", 255, 248, 220),
        NamedColorEntry("crimson", "#DC143C", 220, 20, 60),
        NamedColorEntry("cyan", "#00FFFF", 0, 255, 255),
        NamedColorEntry("darkblue", "#00008B", 0, 0, 139),
        NamedColorEntry("darkcyan", "#008B8B", 0, 139, 139),
        NamedColorEntry("darkgoldenrod", "#B8860B", 184, 134, 11),
        NamedColorEntry("darkgray", "#A9A9A9", 169, 169, 169),
        NamedColorEntry("darkgreen", "#006400", 0, 100, 0),
        NamedColorEntry("darkkhaki", "#BDB76B", 189, 183, 107),
        NamedColorEntry("darkmagenta", "#8B008B", 139, 0, 139),
        NamedColorEntry("darkolivegreen", "#556B2F", 85, 107, 47),
        NamedColorEntry("darkorange", "#FF8C00", 255, 140, 0),
        NamedColorEntry("darkorchid", "#9932CC", 153, 50, 204),
        NamedColorEntry("darkred", "#8B0000", 139, 0, 0),
        NamedColorEntry("darksalmon", "#E9967A", 233, 150, 122),
        NamedColorEntry("darkseagreen", "#8FBC8F", 143, 188, 143),
        NamedColorEntry("darkslateblue", "#483D8B", 72, 61, 139),
        NamedColorEntry("darkslategray", "#2F4F4F", 47, 79, 79),
        NamedColorEntry("darkturquoise", "#00CED1", 0, 206, 209),
        NamedColorEntry("darkviolet", "#9400D3", 148, 0, 211),
        NamedColorEntry("deeppink", "#FF1493", 255, 20, 147),
        NamedColorEntry("deepskyblue", "#00BFFF", 0, 191, 255),
        NamedColorEntry("dimgray", "#696969", 105, 105, 105),
        NamedColorEntry("dodgerblue", "#1E90FF", 30, 144, 255),
        NamedColorEntry("firebrick", "#B22222", 178, 34, 34),
        NamedColorEntry("floralwhite", "#FFFAF0", 255, 250, 240),
        NamedColorEntry("forestgreen", "#228B22", 34, 139, 34),
        NamedColorEntry("fuchsia", "#FF00FF", 255, 0, 255),
        NamedColorEntry("gainsboro", "#DCDCDC", 220, 220, 220),
        NamedColorEntry("ghostwhite", "#F8F8FF", 248, 248, 255),
        NamedColorEntry("gold", "#FFD700", 255, 215, 0),
        NamedColorEntry("goldenrod", "#DAA520", 218, 165, 32),
        NamedColorEntry("gray", "#808080", 128, 128, 128),
        NamedColorEntry("green", "#008000", 0, 128, 0),
        NamedColorEntry("greenyellow", "#ADFF2F", 173, 255, 47),
        NamedColorEntry("honeydew", "#F0FFF0", 240, 255, 240),
        NamedColorEntry("hotpink", "#FF69B4", 255, 105, 180),
        NamedColorEntry("indianred", "#CD5C5C", 205, 92, 92),
        NamedColorEntry("indigo", "#4B0082", 75, 0, 130),
        NamedColorEntry("ivory", "#FFFFF0", 255, 255, 240),
        NamedColorEntry("khaki", "#F0E68C", 240, 230, 140),
        NamedColorEntry("lavender", "#E6E6FA", 230, 230, 250),
        NamedColorEntry("lavenderblush", "#FFF0F5", 255, 240, 245),
        NamedColorEntry("lawngreen", "#7CFC00", 124, 252, 0),
        NamedColorEntry("lemonchiffon", "#FFFACD", 255, 250, 205),
        NamedColorEntry("lightblue", "#ADD8E6", 173, 216, 230),
        NamedColorEntry("lightcoral", "#F08080", 240, 128, 128),
        NamedColorEntry("lightcyan", "#E0FFFF", 224, 255, 255),
        NamedColorEntry("lightgoldenrodyellow", "#FAFAD2", 250, 250, 210),
        NamedColorEntry("lightgray", "#D3D3D3", 211, 211, 211),
        NamedColorEntry("lightgreen", "#90EE90", 144, 238, 144),
        NamedColorEntry("lightpink", "#FFB6C1", 255, 182, 193),
        NamedColorEntry("lightsalmon", "#FFA07A", 255, 160, 122),
        NamedColorEntry("lightseagreen", "#20B2AA", 32, 178, 170),
        NamedColorEntry("lightskyblue", "#87CEFA", 135, 206, 250),
        NamedColorEntry("lightslategray", "#778899", 119, 136, 153),
        NamedColorEntry("lightsteelblue", "#B0C4DE", 176, 196, 222),
        NamedColorEntry("lightyellow", "#FFFFE0", 255, 255, 224),
        NamedColorEntry("lime", "#00FF00", 0, 255, 0),
        NamedColorEntry("limegreen", "#32CD32", 50, 205, 50),
        NamedColorEntry("linen", "#FAF0E6", 250, 240, 230),
        NamedColorEntry("magenta", "#FF00FF", 255, 0, 255),
        NamedColorEntry("maroon", "#800000", 128, 0, 0),
        NamedColorEntry("mediumaquamarine", "#66CDAA", 102, 205, 170),
        NamedColorEntry("mediumblue", "#0000CD", 0, 0, 205),
        NamedColorEntry("mediumorchid", "#BA55D3", 186, 85, 211),
        NamedColorEntry("mediumpurple", "#9370DB", 147, 112, 219),
        NamedColorEntry("mediumseagreen", "#3CB371", 60, 179, 113),
        NamedColorEntry("mediumslateblue", "#7B68EE", 123, 104, 238),
        NamedColorEntry("mediumspringgreen", "#00FA9A", 0, 250, 154),
        NamedColorEntry("mediumturquoise", "#48D1CC", 72, 209, 204),
        NamedColorEntry("mediumvioletred", "#C71585", 199, 21, 133),
        NamedColorEntry("midnightblue", "#191970", 25, 25, 112),
        NamedColorEntry("mintcream", "#F5FFFA", 245, 255, 250),
        NamedColorEntry("mistyrose", "#FFE4E1", 255, 228, 225),
        NamedColorEntry("moccasin", "#FFE4B5", 255, 228, 181),
        NamedColorEntry("navajowhite", "#FFDEAD", 255, 222, 173),
        NamedColorEntry("navy", "#000080", 0, 0, 128),
        NamedColorEntry("oldlace", "#FDF5E6", 253, 245, 230),
        NamedColorEntry("olive", "#808000", 128, 128, 0),
        NamedColorEntry("olivedrab", "#6B8E23", 107, 142, 35),
        NamedColorEntry("orange", "#FFA500", 255, 165, 0),
        NamedColorEntry("orangered", "#FF4500", 255, 69, 0),
        NamedColorEntry("orchid", "#DA70D6", 218, 112, 214),
        NamedColorEntry("palegoldenrod", "#EEE8AA", 238, 232, 170),
        NamedColorEntry("palegreen", "#98FB98", 152, 251, 152),
        NamedColorEntry("paleturquoise", "#AFEEEE", 175, 238, 238),
        NamedColorEntry("palevioletred", "#DB7093", 219, 112, 147),
        NamedColorEntry("papayawhip", "#FFEFD5", 255, 239, 213),
        NamedColorEntry("peachpuff", "#FFDAB9", 255, 218, 185),
        NamedColorEntry("peru", "#CD853F", 205, 133, 63),
        NamedColorEntry("pink", "#FFC0CB", 255, 192, 203),
        NamedColorEntry("plum", "#DDA0DD", 221, 160, 221),
        NamedColorEntry("powderblue", "#B0E0E6", 176, 224, 230),
        NamedColorEntry("purple", "#800080", 128, 0, 128),
        NamedColorEntry("rebeccapurple", "#663399", 102, 51, 153),
        NamedColorEntry("red", "#FF0000", 255, 0, 0),
        NamedColorEntry("rosybrown", "#BC8F8F", 188, 143, 143),
        NamedColorEntry("royalblue", "#4169E1", 65, 105, 225),
        NamedColorEntry("saddlebrown", "#8B4513", 139, 69, 19),
        NamedColorEntry("salmon", "#FA8072", 250, 128, 114),
        NamedColorEntry("sandybrown", "#F4A460", 244, 164, 96),
        NamedColorEntry("seagreen", "#2E8B57", 46, 139, 87),
        NamedColorEntry("seashell", "#FFF5EE", 255, 245, 238),
        NamedColorEntry("sienna", "#A0522D", 160, 82, 45),
        NamedColorEntry("silver", "#C0C0C0", 192, 192, 192),
        NamedColorEntry("skyblue", "#87CEEB", 135, 206, 235),
        NamedColorEntry("slateblue", "#6A5ACD", 106, 90, 205),
        NamedColorEntry("slategray", "#708090", 112, 128, 144),
        NamedColorEntry("snow", "#FFFAFA", 255, 250, 250),
        NamedColorEntry("springgreen", "#00FF7F", 0, 255, 127),
        NamedColorEntry("steelblue", "#4682B4", 70, 130, 180),
        NamedColorEntry("tan", "#D2B48C", 210, 180, 140),
        NamedColorEntry("teal", "#008080", 0, 128, 128),
        NamedColorEntry("thistle", "#D8BFD8", 216, 191, 216),
        NamedColorEntry("tomato", "#FF6347", 255, 99, 71),
        NamedColorEntry("turquoise", "#40E0D0", 64, 224, 208),
        NamedColorEntry("violet", "#EE82EE", 238, 130, 238),
        NamedColorEntry("wheat", "#F5DEB3", 245, 222, 179),
        NamedColorEntry("white", "#FFFFFF", 255, 255, 255),
        NamedColorEntry("whitesmoke", "#F5F5F5", 245, 245, 245),
        NamedColorEntry("yellow", "#FFFF00", 255, 255, 0),
        NamedColorEntry("yellowgreen", "#9ACD32", 154, 205, 50)
    )

    override suspend fun execute(input: HtmlColorNameInput): ToolResult<HtmlColorNameOutput> {
        val startTime = System.currentTimeMillis()
        val q = input.queryOrHex.trim()

        if (q.isEmpty()) {
            return ToolResult.Failure("Query or hex code cannot be empty.")
        }

        // Check if query is a hex code (e.g. "#3B82F6" or "3B82F6")
        val cleanHex = q.removePrefix("#")
        val isHex = (cleanHex.length == 6 && cleanHex.all { it in "0123456789abcdefABCDEF" })

        if (isHex) {
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)

            var closest = namedColors.first()
            var minDistance = Double.MAX_VALUE

            for (nc in namedColors) {
                val dist = sqrt((r - nc.r).toDouble() * (r - nc.r) + (g - nc.g).toDouble() * (g - nc.g) + (b - nc.b).toDouble() * (b - nc.b))
                if (dist < minDistance) {
                    minDistance = dist
                    closest = nc
                }
            }

            val exact = minDistance == 0.0
            val report = buildString {
                appendLine("W3C CSS NAMED COLOR MATCHER")
                appendLine("--------------------------------------------------")
                appendLine("Input Hex:        #${cleanHex.uppercase(Locale.ROOT)} (RGB: $r, $g, $b)")
                appendLine("Closest Name:     ${closest.name}")
                appendLine("Official Hex:     ${closest.hex}")
                appendLine("Official RGB:     RGB(${closest.r}, ${closest.g}, ${closest.b})")
                appendLine("Color Distance:   ${String.format(Locale.US, "%.2f", minDistance)} (Euclidean ΔE)")
                appendLine("Exact Match:      ${if (exact) "YES" else "NO"}")
            }

            val summary = if (exact) "Exact match: ${closest.name}" else "Nearest: ${closest.name} (ΔE ${String.format(Locale.US, "%.1f", minDistance)})"

            return ToolResult.Success(
                data = HtmlColorNameOutput(
                    query = q,
                    closestNamedColor = closest.name,
                    closestHex = closest.hex,
                    euclideanDistance = minDistance,
                    exactMatch = exact,
                    allMatches = listOf(closest),
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } else {
            // Text search by name
            val qLower = q.lowercase(Locale.ROOT)
            val matches = namedColors.filter { it.name.contains(qLower) }

            val closest = matches.firstOrNull() ?: namedColors.first()
            val report = buildString {
                appendLine("W3C CSS NAMED COLOR DIRECTORY")
                appendLine("--------------------------------------------------")
                appendLine("Query: \"$q\" | Matches Found: ${matches.size}")
                appendLine()
                if (matches.isEmpty()) {
                    appendLine("No standard named color matching '$q'.")
                } else {
                    matches.forEach { c ->
                        appendLine("• ${c.name.padEnd(20)} ${c.hex.padEnd(10)} RGB(${c.r}, ${c.g}, ${c.b})")
                    }
                }
            }

            val summary = if (matches.isNotEmpty()) "Found ${matches.size} matching color(s) for '$q'" else "No named color matches for '$q'"

            return ToolResult.Success(
                data = HtmlColorNameOutput(
                    query = q,
                    closestNamedColor = closest.name,
                    closestHex = closest.hex,
                    euclideanDistance = 0.0,
                    exactMatch = matches.any { it.name.equals(qLower, ignoreCase = true) },
                    allMatches = matches,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        }
    }
}
