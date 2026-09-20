package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class UserAgentInput(
    val userAgent: String = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.119 Mobile Safari/537.36"
)

data class UserAgentOutput(
    val browserName: String,
    val browserVersion: String,
    val operatingSystem: String,
    val osVersion: String,
    val deviceCategory: String,
    val renderingEngine: String,
    val architecture: String,
    val isBotOrCrawler: Boolean,
    val formattedReport: String,
    val summary: String
)

class UserAgentParserTool : Tool<UserAgentInput, UserAgentOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "user_agent_parser_tool",
        name = "User-Agent Client & Platform Parser",
        description = "Parse and inspect browser User-Agent strings entirely offline to identify browser, engine, OS, and device categories.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("useragent", "ua", "browser", "device", "client", "os", "platform", "android", "chrome", "safari"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Devices"
    )

    override suspend fun execute(input: UserAgentInput): ToolResult<UserAgentOutput> {
        val startTime = System.currentTimeMillis()
        val ua = input.userAgent.trim()

        if (ua.isEmpty()) {
            return ToolResult.Failure("User-Agent string cannot be empty.")
        }

        // Bot / Crawler detection
        val botRegex = Regex("(bot|crawler|spider|crawling|slurp|duckduckbot|facebookexternalhit|bingbot|googlebot)", RegexOption.IGNORE_CASE)
        val isBot = botRegex.containsMatchIn(ua)

        // OS detection
        val (os, osVer) = detectOS(ua)

        // Browser & Engine detection
        val (browser, browserVer, engine) = detectBrowserAndEngine(ua)

        // Device category
        val device = detectDevice(ua, os)

        // Architecture
        val arch = detectArch(ua)

        val report = buildString {
            appendLine("USER-AGENT CLIENT INSPECTION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Raw String:      $ua")
            appendLine()
            appendLine("CLIENT DETAILS")
            appendLine("• Browser:       $browser ($browserVer)")
            appendLine("• Engine:        $engine")
            appendLine("• OS & Version:  $os $osVer")
            appendLine("• Device Form:   $device")
            appendLine("• Architecture:  $arch")
            appendLine("• Bot/Crawler:   ${if (isBot) "Yes (Automated Agent)" else "No (End User Client)"}")
        }

        val summary = "$browser $browserVer on $os ($device)"

        return ToolResult.Success(
            data = UserAgentOutput(
                browserName = browser,
                browserVersion = browserVer,
                operatingSystem = os,
                osVersion = osVer,
                deviceCategory = device,
                renderingEngine = engine,
                architecture = arch,
                isBotOrCrawler = isBot,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun detectOS(ua: String): Pair<String, String> {
        val lower = ua.lowercase(Locale.ROOT)
        return when {
            lower.contains("android") -> {
                val match = Regex("android[\\s/]([0-9.]+)").find(lower)
                Pair("Android", match?.groupValues?.get(1) ?: "Unknown Version")
            }
            lower.contains("iphone") || lower.contains("ipad") || lower.contains("ipod") -> {
                val match = Regex("os ([0-9_]+) like mac os x").find(lower)
                val ver = match?.groupValues?.get(1)?.replace('_', '.') ?: "iOS"
                Pair("Apple iOS", ver)
            }
            lower.contains("macintosh") || lower.contains("mac os x") -> {
                val match = Regex("mac os x ([0-9_]+)").find(lower)
                val ver = match?.groupValues?.get(1)?.replace('_', '.') ?: "macOS"
                Pair("macOS", ver)
            }
            lower.contains("windows nt 10.0") -> Pair("Windows", "10 / 11")
            lower.contains("windows nt 6.3") -> Pair("Windows", "8.1")
            lower.contains("windows nt 6.1") -> Pair("Windows", "7")
            lower.contains("windows") -> Pair("Windows", "Unknown Version")
            lower.contains("cros") -> Pair("ChromeOS", "")
            lower.contains("linux") -> Pair("Linux", "Generic")
            else -> Pair("Unknown OS", "")
        }
    }

    private fun detectBrowserAndEngine(ua: String): Triple<String, String, String> {
        val lower = ua.lowercase(Locale.ROOT)
        return when {
            lower.contains("edg/") -> {
                val ver = Regex("edg/([0-9.]+)").find(lower)?.groupValues?.get(1) ?: ""
                Triple("Microsoft Edge", ver, "Blink")
            }
            lower.contains("opr/") || lower.contains("opera") -> {
                val ver = Regex("opr/([0-9.]+)").find(lower)?.groupValues?.get(1) ?: ""
                Triple("Opera", ver, "Blink")
            }
            lower.contains("brave") -> {
                Triple("Brave", "", "Blink")
            }
            lower.contains("chrome/") && !lower.contains("chromium/") -> {
                val ver = Regex("chrome/([0-9.]+)").find(lower)?.groupValues?.get(1) ?: ""
                Triple("Google Chrome", ver, "Blink")
            }
            lower.contains("firefox/") -> {
                val ver = Regex("firefox/([0-9.]+)").find(lower)?.groupValues?.get(1) ?: ""
                Triple("Mozilla Firefox", ver, "Gecko")
            }
            lower.contains("version/") && lower.contains("safari/") -> {
                val ver = Regex("version/([0-9.]+)").find(lower)?.groupValues?.get(1) ?: ""
                Triple("Apple Safari", ver, "WebKit")
            }
            lower.contains("curl/") -> {
                val ver = Regex("curl/([0-9.]+)").find(lower)?.groupValues?.get(1) ?: ""
                Triple("cURL Utility", ver, "Command Line Tool")
            }
            else -> Triple("Generic / Unknown Browser", "", "Unknown Engine")
        }
    }

    private fun detectDevice(ua: String, os: String): String {
        val lower = ua.lowercase(Locale.ROOT)
        return when {
            lower.contains("ipad") || lower.contains("tablet") -> "Tablet"
            lower.contains("mobile") || os == "Android" || os == "Apple iOS" -> "Mobile / Phone"
            lower.contains("smart-tv") || lower.contains("appletv") || lower.contains("googletv") -> "Smart TV"
            else -> "Desktop / Laptop"
        }
    }

    private fun detectArch(ua: String): String {
        val lower = ua.lowercase(Locale.ROOT)
        return when {
            lower.contains("x86_64") || lower.contains("win64") || lower.contains("x64") || lower.contains("amd64") -> "64-bit (x86_64)"
            lower.contains("arm64") || lower.contains("aarch64") -> "64-bit ARM (AArch64)"
            lower.contains("arm") -> "32-bit ARM"
            lower.contains("i686") || lower.contains("i386") -> "32-bit (x86)"
            else -> "Standard / Unspecified"
        }
    }
}
