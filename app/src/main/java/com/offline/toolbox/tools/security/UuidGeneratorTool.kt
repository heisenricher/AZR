package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

data class UuidConfig(
    val count: Int = 1,
    val uppercase: Boolean = false,
    val includeHyphens: Boolean = true,
    val includeBraces: Boolean = false
)

class UuidGeneratorTool : Tool<UuidConfig, String> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "uuid_generator",
        name = "UUID / GUID Generator",
        description = "Generate cryptographically secure UUID v4 identifiers with bulk count and format options.",
        category = ToolCategory.SECURITY,
        tags = listOf("uuid", "guid", "generator", "unique id", "v4", "crypto", "random"),
        inputType = ToolDataType.NONE,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Fingerprint"
    )

    private val secureRandom = SecureRandom()

    override suspend fun execute(input: UuidConfig): ToolResult<String> {
        val startTime = System.currentTimeMillis()
        val count = input.count.coerceIn(1, 100)

        val list = mutableListOf<String>()
        for (i in 0 until count) {
            val randomBytes = ByteArray(16)
            secureRandom.nextBytes(randomBytes)
            // Set version 4 and variant
            randomBytes[6] = (randomBytes[6].toInt() and 0x0f or 0x40).toByte()
            randomBytes[8] = (randomBytes[8].toInt() and 0x3f or 0x80.toInt()).toByte()

            var msb = 0L
            var lsb = 0L
            for (j in 0..7) msb = (msb shl 8) or (randomBytes[j].toLong() and 0xff)
            for (j in 8..15) lsb = (lsb shl 8) or (randomBytes[j].toLong() and 0xff)

            val uuid = UUID(msb, lsb)
            var formatted = uuid.toString()

            if (!input.includeHyphens) {
                formatted = formatted.replace("-", "")
            }
            if (input.uppercase) {
                formatted = formatted.uppercase(Locale.US)
            }
            if (input.includeBraces) {
                formatted = "{$formatted}"
            }
            list.add(formatted)
        }

        val result = list.joinToString("\n")
        val summary = "Generated $count UUID(s)"

        return ToolResult.Success(
            data = result,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
