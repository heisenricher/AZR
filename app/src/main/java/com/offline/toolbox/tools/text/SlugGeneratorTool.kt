package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.text.Normalizer
import java.util.Locale

data class SlugInput(
    val text: String,
    val separator: String = "-",
    val lowercase: Boolean = true,
    val removeAccents: Boolean = true
)

data class SlugOutput(
    val slug: String,
    val originalLength: Int,
    val slugLength: Int,
    val summary: String
)

class SlugGeneratorTool : Tool<SlugInput, SlugOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "slug_generator",
        name = "URL Slug Generator",
        description = "Generate clean, SEO-friendly URL slugs by removing accents, special characters, and normalizing spaces.",
        category = ToolCategory.TEXT,
        tags = listOf("slug", "url", "seo", "kebab", "permalink", "normalize", "clean"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Link"
    )

    override suspend fun execute(input: SlugInput): ToolResult<SlugOutput> {
        val startTime = System.currentTimeMillis()
        var text = input.text.trim()

        if (text.isEmpty()) {
            return ToolResult.Success(
                data = SlugOutput("", 0, 0, "Empty input"),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        if (input.removeAccents) {
            text = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        }

        if (input.lowercase) {
            text = text.lowercase(Locale.ROOT)
        }

        // Replace non-alphanumeric chars with separator
        val sep = if (input.separator.isNotEmpty()) input.separator else "-"
        val cleaned = text
            .replace(Regex("[^a-zA-Z0-9]+"), sep)
            .replace(Regex("${Regex.escape(sep)}+"), sep)
            .trim { it == sep[0] }

        val summary = "Generated slug (${cleaned.length} chars)"

        return ToolResult.Success(
            data = SlugOutput(
                slug = cleaned,
                originalLength = input.text.length,
                slugLength = cleaned.length,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
