package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONObject
import java.util.Locale

data class HarSlowRequest(
    val method: String,
    val url: String,
    val status: Int,
    val timeMs: Double
)

data class HarAnalyzerInput(
    val harContent: String = """
        {
          "log": {
            "version": "1.2",
            "creator": { "name": "WebInspector", "version": "537.36" },
            "entries": [
              {
                "startedDateTime": "2026-03-22T08:00:00.000Z",
                "time": 42.5,
                "request": { "method": "GET", "url": "https://api.example.com/v1/health", "headers": [] },
                "response": {
                  "status": 200,
                  "statusText": "OK",
                  "headers": [
                    { "name": "Content-Type", "value": "application/json" },
                    { "name": "Strict-Transport-Security", "value": "max-age=31536000" }
                  ],
                  "content": { "size": 128, "mimeType": "application/json" }
                },
                "timings": { "dns": 5.0, "connect": 10.0, "send": 1.5, "wait": 20.0, "receive": 6.0 }
              },
              {
                "startedDateTime": "2026-03-22T08:00:01.000Z",
                "time": 320.1,
                "request": { "method": "POST", "url": "https://api.example.com/v1/auth/login", "headers": [] },
                "response": {
                  "status": 200,
                  "statusText": "OK",
                  "headers": [
                    { "name": "Content-Type", "value": "application/json" },
                    { "name": "X-Frame-Options", "value": "DENY" }
                  ],
                  "content": { "size": 512, "mimeType": "application/json" }
                },
                "timings": { "dns": 1.0, "connect": 15.0, "send": 2.0, "wait": 290.0, "receive": 12.1 }
              },
              {
                "startedDateTime": "2026-03-22T08:00:02.000Z",
                "time": 85.0,
                "request": { "method": "GET", "url": "https://api.example.com/v1/users/404", "headers": [] },
                "response": {
                  "status": 404,
                  "statusText": "Not Found",
                  "headers": [],
                  "content": { "size": 64, "mimeType": "application/json" }
                },
                "timings": { "dns": 0.0, "connect": 0.0, "send": 1.0, "wait": 80.0, "receive": 4.0 }
              }
            ]
          }
        }
    """.trimIndent()
)

data class HarAnalyzerOutput(
    val totalRequests: Int,
    val status2xxCount: Int,
    val status3xxCount: Int,
    val status4xxCount: Int,
    val status5xxCount: Int,
    val totalTransferredBytes: Long,
    val averageLatencyMs: Double,
    val maxLatencyMs: Double,
    val slowestRequests: List<HarSlowRequest>,
    val methodCounts: Map<String, Int>,
    val formattedReport: String,
    val summary: String
)

class HarAnalyzerTool : Tool<HarAnalyzerInput, HarAnalyzerOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "har_analyzer_tool",
        name = "HTTP Archive (HAR) Offline Log Analyzer",
        description = "Parse HTTP Archive (.har) logs offline, analyze waterfall timings, status code breakdowns, payload sizes, and inspect slowest API calls.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("har", "http archive", "network", "performance", "waterfall", "api", "latency", "developer", "log"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Timeline"
    )

    override suspend fun execute(input: HarAnalyzerInput): ToolResult<HarAnalyzerOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.harContent.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("HAR content cannot be empty.")
        }

        try {
            val root = JSONObject(raw)
            val logObj = root.optJSONObject("log")
                ?: return ToolResult.Failure("Invalid HAR file: Missing root 'log' object.")

            val entriesArray = logObj.optJSONArray("entries")
                ?: return ToolResult.Failure("Invalid HAR file: Missing 'entries' array in log.")

            val count = entriesArray.length()
            if (count == 0) {
                return ToolResult.Failure("HAR log contains 0 network entries.")
            }

            var s2xx = 0
            var s3xx = 0
            var s4xx = 0
            var s5xx = 0
            var totalBytes = 0L
            var totalTime = 0.0
            var maxTime = 0.0

            val methodMap = mutableMapOf<String, Int>()
            val requestList = mutableListOf<HarSlowRequest>()

            for (i in 0 until count) {
                val entry = entriesArray.optJSONObject(i) ?: continue
                val time = entry.optDouble("time", 0.0)
                if (time > maxTime) maxTime = time
                totalTime += time

                val req = entry.optJSONObject("request")
                val method = req?.optString("method", "GET")?.uppercase(Locale.US) ?: "GET"
                val url = req?.optString("url", "") ?: ""

                methodMap[method] = methodMap.getOrDefault(method, 0) + 1

                val res = entry.optJSONObject("response")
                val status = res?.optInt("status", 0) ?: 0
                when (status) {
                    in 200..299 -> s2xx++
                    in 300..399 -> s3xx++
                    in 400..499 -> s4xx++
                    in 500..599 -> s5xx++
                }

                val content = res?.optJSONObject("content")
                val size = content?.optLong("size", 0L) ?: 0L
                if (size > 0) totalBytes += size

                requestList.add(HarSlowRequest(method, url, status, time))
            }

            val avgTime = if (count > 0) totalTime / count else 0.0
            val slowest = requestList.sortedByDescending { it.timeMs }.take(5)

            val report = buildString {
                appendLine("HTTP ARCHIVE (.HAR) PERFORMANCE AUDIT")
                appendLine("--------------------------------------------------")
                appendLine("Total Requests:       $count")
                appendLine("Total Payload Size:   ${String.format(Locale.US, "%,d", totalBytes)} bytes (${String.format(Locale.US, "%.2f", totalBytes / 1024.0)} KB)")
                appendLine("Average Latency:      ${String.format(Locale.US, "%.2f", avgTime)} ms")
                appendLine("Max Peak Latency:     ${String.format(Locale.US, "%.2f", maxTime)} ms")
                appendLine()
                appendLine("HTTP STATUS DISTRIBUTION:")
                appendLine("• 2xx Success:        $s2xx (${String.format(Locale.US, "%.1f", (s2xx.toDouble() / count) * 100)}%)")
                appendLine("• 3xx Redirection:    $s3xx (${String.format(Locale.US, "%.1f", (s3xx.toDouble() / count) * 100)}%)")
                appendLine("• 4xx Client Error:   $s4xx (${String.format(Locale.US, "%.1f", (s4xx.toDouble() / count) * 100)}%)")
                appendLine("• 5xx Server Error:   $s5xx (${String.format(Locale.US, "%.1f", (s5xx.toDouble() / count) * 100)}%)")
                appendLine()
                appendLine("REQUEST METHODS:")
                methodMap.forEach { (m, c) -> appendLine("• $m: $c") }
                appendLine()
                appendLine("TOP SLOWEST NETWORK REQUESTS:")
                slowest.forEachIndexed { idx, s ->
                    appendLine("${idx + 1}. [${s.method}] (${s.status}) ${String.format(Locale.US, "%.1f", s.timeMs)} ms")
                    appendLine("   URL: ${s.url}")
                }
            }

            val summary = "$count requests | Avg: ${String.format(Locale.US, "%.0f", avgTime)} ms | 2xx: $s2xx, 4xx/5xx: ${s4xx + s5xx}"

            return ToolResult.Success(
                data = HarAnalyzerOutput(
                    totalRequests = count,
                    status2xxCount = s2xx,
                    status3xxCount = s3xx,
                    status4xxCount = s4xx,
                    status5xxCount = s5xx,
                    totalTransferredBytes = totalBytes,
                    averageLatencyMs = avgTime,
                    maxLatencyMs = maxTime,
                    slowestRequests = slowest,
                    methodCounts = methodMap,
                    formattedReport = report,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            return ToolResult.Failure("Failed to parse HAR JSON: ${e.message}")
        }
    }
}
