package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class SvgPathInput(
    val pathData: String = "M 10 80 Q 52.5 10, 95 80 T 180 80 Z",
    val viewportWidth: Double = 200.0,
    val viewportHeight: Double = 200.0,
    val fillColorHex: String = "#3F51B5"
)

data class SvgBoundingBox(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double,
    val width: Double,
    val height: Double
)

data class SvgPathOutput(
    val totalCommands: Int,
    val commandCounts: Map<String, Int>,
    val boundingBox: SvgBoundingBox,
    val isClosed: Boolean,
    val androidVectorDrawableXml: String,
    val formattedReport: String,
    val summary: String
)

class SvgPathDataInspectorTool : Tool<SvgPathInput, SvgPathOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "svg_path_data_inspector_tool",
        name = "SVG Path Data Inspector & Android Vector Converter",
        description = "Parse SVG vector path data (d='...'), extract command distributions, calculate geometric bounding boxes, and generate production-ready Android VectorDrawable XML.",
        category = ToolCategory.MEDIA,
        tags = listOf("svg", "vector", "path", "android vector", "vectordrawable", "xml", "bounding box", "bezier", "graphics", "ui"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Image"
    )

    private val commandRegex = Regex("([MmLlHhVvCcSsQqTtAaZz])([^MmLlHhVvCcSsQqTtAaZz]*)")
    private val numberRegex = Regex("[-+]?(?:\\d*\\.\\d+|\\d+)(?:[eE][-+]?\\d+)?")

    override suspend fun execute(input: SvgPathInput): ToolResult<SvgPathOutput> {
        val startTime = System.currentTimeMillis()
        val rawPath = input.pathData.trim()
        if (rawPath.isBlank()) {
            return ToolResult.Failure("SVG path data cannot be empty.")
        }

        val matches = commandRegex.findAll(rawPath).toList()
        if (matches.isEmpty()) {
            return ToolResult.Failure("No valid SVG path commands (M, L, C, Q, Z, etc.) found in input.")
        }

        val counts = mutableMapOf<String, Int>()
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        var curX = 0.0
        var curY = 0.0
        var isClosed = false

        for (m in matches) {
            val cmd = m.groupValues[1]
            val argsText = m.groupValues[2]
            counts[cmd] = counts.getOrDefault(cmd, 0) + 1

            val isRelative = cmd[0].isLowerCase()
            val cmdUpper = cmd[0].uppercaseChar()

            val nums = numberRegex.findAll(argsText).mapNotNull { it.value.toDoubleOrNull() }.toList()

            when (cmdUpper) {
                'M', 'L', 'T' -> {
                    for (i in nums.indices step 2) {
                        if (i + 1 < nums.size) {
                            val x = if (isRelative) curX + nums[i] else nums[i]
                            val y = if (isRelative) curY + nums[i + 1] else nums[i + 1]
                            curX = x
                            curY = y
                            minX = min(minX, curX)
                            maxX = max(maxX, curX)
                            minY = min(minY, curY)
                            maxY = max(maxY, curY)
                        }
                    }
                }
                'H' -> {
                    for (xVal in nums) {
                        curX = if (isRelative) curX + xVal else xVal
                        minX = min(minX, curX)
                        maxX = max(maxX, curX)
                    }
                }
                'V' -> {
                    for (yVal in nums) {
                        curY = if (isRelative) curY + yVal else yVal
                        minY = min(minY, curY)
                        maxY = max(maxY, curY)
                    }
                }
                'C' -> {
                    for (i in nums.indices step 6) {
                        if (i + 5 < nums.size) {
                            val x1 = if (isRelative) curX + nums[i] else nums[i]
                            val y1 = if (isRelative) curY + nums[i + 1] else nums[i + 1]
                            val x2 = if (isRelative) curX + nums[i + 2] else nums[i + 2]
                            val y2 = if (isRelative) curY + nums[i + 3] else nums[i + 3]
                            val x = if (isRelative) curX + nums[i + 4] else nums[i + 4]
                            val y = if (isRelative) curY + nums[i + 5] else nums[i + 5]
                            minX = min(minX, min(x1, min(x2, x)))
                            maxX = max(maxX, max(x1, max(x2, x)))
                            minY = min(minY, min(y1, min(y2, y)))
                            maxY = max(maxY, max(y1, max(y2, y)))
                            curX = x
                            curY = y
                        }
                    }
                }
                'S', 'Q' -> {
                    for (i in nums.indices step 4) {
                        if (i + 3 < nums.size) {
                            val x1 = if (isRelative) curX + nums[i] else nums[i]
                            val y1 = if (isRelative) curY + nums[i + 1] else nums[i + 1]
                            val x = if (isRelative) curX + nums[i + 2] else nums[i + 2]
                            val y = if (isRelative) curY + nums[i + 3] else nums[i + 3]
                            minX = min(minX, min(x1, x))
                            maxX = max(maxX, max(x1, x))
                            minY = min(minY, min(y1, y))
                            maxY = max(maxY, max(y1, y))
                            curX = x
                            curY = y
                        }
                    }
                }
                'A' -> {
                    for (i in nums.indices step 7) {
                        if (i + 6 < nums.size) {
                            val x = if (isRelative) curX + nums[i + 5] else nums[i + 5]
                            val y = if (isRelative) curY + nums[i + 6] else nums[i + 6]
                            minX = min(minX, x)
                            maxX = max(maxX, x)
                            minY = min(minY, y)
                            maxY = max(maxY, y)
                            curX = x
                            curY = y
                        }
                    }
                }
                'Z' -> {
                    isClosed = true
                }
            }
        }

        if (minX == Double.MAX_VALUE) {
            minX = 0.0
            minY = 0.0
            maxX = 0.0
            maxY = 0.0
        }

        val boxWidth = max(0.0, maxX - minX)
        val boxHeight = max(0.0, maxY - minY)
        val bbox = SvgBoundingBox(minX, minY, maxX, maxY, boxWidth, boxHeight)

        val vpW = if (input.viewportWidth > 0) input.viewportWidth else max(100.0, maxX)
        val vpH = if (input.viewportHeight > 0) input.viewportHeight else max(100.0, maxY)
        val colorHex = if (input.fillColorHex.startsWith("#")) input.fillColorHex else "#${input.fillColorHex}"

        val vectorXml = buildString {
            appendLine("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"")
            appendLine("    android:width=\"${vpW.toInt()}dp\"")
            appendLine("    android:height=\"${vpH.toInt()}dp\"")
            appendLine("    android:viewportWidth=\"$vpW\"")
            appendLine("    android:viewportHeight=\"$vpH\">")
            appendLine("    <path")
            appendLine("        android:fillColor=\"$colorHex\"")
            appendLine("        android:pathData=\"$rawPath\" />")
            append("</vector>")
        }

        val elapsed = System.currentTimeMillis() - startTime
        val report = buildString {
            appendLine("=== SVG PATH DATA INSPECTION REPORT ===")
            appendLine("Total Path Commands: ${matches.size}")
            appendLine("Closed Path (Z/z):   $isClosed")
            appendLine("----------------------------------------")
            appendLine("2D BOUNDING BOX (COORDINATES):")
            appendLine("  X Span: [${String.format(Locale.US, "%.2f", minX)}, ${String.format(Locale.US, "%.2f", maxX)}] (Width: ${String.format(Locale.US, "%.2f", boxWidth)})")
            appendLine("  Y Span: [${String.format(Locale.US, "%.2f", minY)}, ${String.format(Locale.US, "%.2f", maxY)}] (Height: ${String.format(Locale.US, "%.2f", boxHeight)})")
            appendLine("----------------------------------------")
            appendLine("COMMAND FREQUENCY DISTRIBUTION:")
            counts.entries.sortedByDescending { it.value }.forEach { (cmd, count) ->
                val desc = when (cmd[0].uppercaseChar()) {
                    'M' -> "MoveTo"
                    'L' -> "LineTo"
                    'H' -> "Horizontal Line"
                    'V' -> "Vertical Line"
                    'C' -> "Cubic Bézier"
                    'S' -> "Smooth Cubic Bézier"
                    'Q' -> "Quadratic Bézier"
                    'T' -> "Smooth Quad Bézier"
                    'A' -> "Elliptical Arc"
                    'Z' -> "ClosePath"
                    else -> "Command"
                }
                val mode = if (cmd[0].isUpperCase()) "Absolute" else "Relative"
                appendLine("  %-4s : %-3d times  ($mode $desc)".format(Locale.US, cmd, count))
            }
            appendLine("----------------------------------------")
            appendLine("ANDROID VECTOR DRAWABLE XML PREVIEW:")
            appendLine(vectorXml)
        }

        val summaryText = "Parsed ${matches.size} commands | BBox: ${String.format(Locale.US, "%.0fx%.0f", boxWidth, boxHeight)} | Closed: $isClosed"

        return ToolResult.Success(
            data = SvgPathOutput(
                totalCommands = matches.size,
                commandCounts = counts,
                boundingBox = bbox,
                isClosed = isClosed,
                androidVectorDrawableXml = vectorXml,
                formattedReport = report,
                summary = summaryText
            ),
            executionTimeMs = elapsed,
            summary = summaryText
        )
    }
}
