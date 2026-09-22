package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class ParsedMetricSample(
    val name: String,
    val labels: Map<String, String>,
    val value: Double,
    val timestamp: Long?
)

data class PrometheusMetricInput(
    val expositionText: String = """
        # HELP http_requests_total The total number of HTTP requests.
        # TYPE http_requests_total counter
        http_requests_total{method="post",handler="/login",code="200"} 1027 1395066363000
        http_requests_total{method="post",handler="/login",code="500"} 12
        http_requests_total{method="get",handler="/items",code="200"} 8431

        # HELP node_memory_MemAvailable_bytes Memory information field MemAvailable_bytes.
        # TYPE node_memory_MemAvailable_bytes gauge
        node_memory_MemAvailable_bytes{instance="10.0.0.1:9100",job="node"} 4.194304e+09

        # HELP rpc_duration_seconds A summary of the RPC duration in seconds.
        # TYPE rpc_duration_seconds summary
        rpc_duration_seconds{quantile="0.5"} 0.045
        rpc_duration_seconds{quantile="0.9"} 0.120
        rpc_duration_seconds{quantile="0.99"} 0.850
        rpc_duration_seconds_sum 128.4
        rpc_duration_seconds_count 1450
    """.trimIndent()
) {
    val metricExpositionText: String get() = expositionText
}

typealias PrometheusInput = PrometheusMetricInput

data class PrometheusOutput(
    val totalSamples: Int,
    val uniqueMetricNames: Int,
    val typeBreakdown: Map<String, Int>,
    val parsedSamples: List<ParsedMetricSample>,
    val uniqueLabels: Set<String>,
    val formattedReport: String,
    val summary: String
)

class PrometheusMetricParserTool : Tool<PrometheusMetricInput, PrometheusOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "prometheus_metric_parser_tool",
        name = "Prometheus & OpenTelemetry Metric Exposition Parser",
        description = "Parse standard Prometheus text exposition metrics, validate counter/gauge/histogram/summary syntax, and inspect label key-value pairs.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("prometheus", "opentelemetry", "metrics", "devops", "monitoring", "gauge", "counter", "histogram", "labels"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Speed"
    )

    private val metricNameRegex = Regex("^[a-zA-Z_:][a-zA-Z0-9_:]*$")

    override suspend fun execute(input: PrometheusMetricInput): ToolResult<PrometheusOutput> {
        val startTime = System.currentTimeMillis()
        val lines = input.expositionText.lines()

        val typeMap = mutableMapOf<String, String>() // metric_name -> type
        val samples = mutableListOf<ParsedMetricSample>()
        val allLabels = mutableSetOf<String>()
        val errors = mutableListOf<String>()

        for ((idx, rawLine) in lines.withIndex()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#")) {
                // Check for TYPE or HELP
                val parts = line.substring(1).trim().split(Regex("\\s+"), 3)
                if (parts.size >= 3) {
                    val directive = parts[0].uppercase(Locale.US)
                    val metricName = parts[1]
                    val extra = parts[2]
                    if (directive == "TYPE") {
                        typeMap[metricName] = extra.lowercase(Locale.US)
                    }
                }
                continue
            }

            // Parse metric sample line: name{labels} value [timestamp]
            try {
                val braceStart = line.indexOf('{')
                val name: String
                val labelsMap = mutableMapOf<String, String>()
                val remainder: String

                if (braceStart != -1) {
                    name = line.substring(0, braceStart).trim()
                    val braceEnd = line.indexOf('}', braceStart)
                    if (braceEnd == -1) {
                        errors.add("Line ${idx + 1}: Unclosed '{' in label list.")
                        continue
                    }
                    val labelsContent = line.substring(braceStart + 1, braceEnd).trim()
                    if (labelsContent.isNotEmpty()) {
                        // Match label="value" pairs
                        val labelMatches = Regex("([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\"([^\"]*)\"").findAll(labelsContent)
                        for (m in labelMatches) {
                            val k = m.groupValues[1]
                            val v = m.groupValues[2]
                            labelsMap[k] = v
                            allLabels.add(k)
                        }
                    }
                    remainder = line.substring(braceEnd + 1).trim()
                } else {
                    val spaceIdx = line.indexOfFirst { it.isWhitespace() }
                    if (spaceIdx == -1) {
                        errors.add("Line ${idx + 1}: Missing numeric sample value.")
                        continue
                    }
                    name = line.substring(0, spaceIdx).trim()
                    remainder = line.substring(spaceIdx).trim()
                }

                if (!metricNameRegex.matches(name)) {
                    errors.add("Line ${idx + 1}: Invalid metric identifier syntax '$name'.")
                    continue
                }

                val tokens = remainder.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (tokens.isEmpty()) {
                    errors.add("Line ${idx + 1}: Missing numeric value for metric '$name'.")
                    continue
                }

                val value = tokens[0].toDoubleOrNull()
                    ?: run {
                        errors.add("Line ${idx + 1}: Cannot parse float value '${tokens[0]}' for '$name'.")
                        return@run null
                    } ?: continue

                val timestamp = if (tokens.size >= 2) tokens[1].toLongOrNull() else null

                samples.add(ParsedMetricSample(name, labelsMap, value, timestamp))
            } catch (e: Exception) {
                errors.add("Line ${idx + 1}: Syntax parsing error: ${e.message}")
            }
        }

        if (samples.isEmpty()) {
            val errStr = if (errors.isNotEmpty()) ": " + errors.take(3).joinToString("; ") else ""
            return ToolResult.Failure("No valid Prometheus metric samples detected in input$errStr.")
        }

        val typeCounts = mutableMapOf<String, Int>()
        for (sample in samples) {
            val t = typeMap[sample.name] ?: "untyped"
            typeCounts[t] = typeCounts.getOrDefault(t, 0) + 1
        }

        val uniqueNames = samples.map { it.name }.distinct()

        val report = buildString {
            appendLine("PROMETHEUS & OPENTELEMETRY METRICS AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Total Series Samples: ${samples.size}")
            appendLine("Unique Metric Names:  ${uniqueNames.size}")
            appendLine("Unique Label Keys:    ${allLabels.size}")
            appendLine()
            appendLine("METRIC TYPE DISTRIBUTION:")
            typeCounts.forEach { (t, count) -> appendLine("• ${t.uppercase(Locale.US)}: $count samples") }
            appendLine()
            if (allLabels.isNotEmpty()) {
                appendLine("LABELS IDENTIFIED:")
                appendLine(allLabels.sorted().joinToString(", ") { "[$it]" })
                appendLine()
            }
            appendLine("PARSED METRIC SAMPLE SERIES:")
            samples.take(15).forEachIndexed { i, s ->
                val labelStr = if (s.labels.isNotEmpty()) "{${s.labels.entries.joinToString(",") { "${it.key}=\"${it.value}\"" }}}" else ""
                val tsStr = if (s.timestamp != null) " (ts: ${s.timestamp})" else ""
                appendLine("${i + 1}. ${s.name}$labelStr → ${s.value}$tsStr")
            }
            if (samples.size > 15) {
                appendLine("... and ${samples.size - 15} more samples.")
            }
            if (errors.isNotEmpty()) {
                appendLine()
                appendLine("SYNTAX WARNINGS (${errors.size}):")
                errors.take(5).forEach { appendLine("• $it") }
            }
        }

        val summary = "${samples.size} samples | ${uniqueNames.size} metrics | ${allLabels.size} labels"

        return ToolResult.Success(
            data = PrometheusOutput(
                totalSamples = samples.size,
                uniqueMetricNames = uniqueNames.size,
                typeBreakdown = typeCounts,
                parsedSamples = samples,
                uniqueLabels = allLabels,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
