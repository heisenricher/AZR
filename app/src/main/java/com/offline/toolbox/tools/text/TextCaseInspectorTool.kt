package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class TextCaseInspectorInput(
    val identifier: String = "offlineAndroidUtilityToolbox"
)

data class TextCaseInspectorOutput(
    val detectedCaseStyle: String,
    val extractedTokens: List<String>,
    val tokenCount: Int,
    val isPureIdentifier: Boolean,
    val suggestedConstantCase: String,
    val suggestedCamelCase: String,
    val suggestedSnakeCase: String,
    val suggestedKebabCase: String,
    val suggestedPascalCase: String,
    val formattedReport: String,
    val summary: String
)

class TextCaseInspectorTool : Tool<TextCaseInspectorInput, TextCaseInspectorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "text_case_inspector_tool",
        name = "Text Case Style Detector & Tokenizer",
        description = "Detect naming conventions (camelCase, PascalCase, snake_case, kebab-case, CONSTANT_CASE) and split tokens.",
        category = ToolCategory.TEXT,
        tags = listOf("case", "casing", "naming", "camelCase", "snake_case", "kebab-case", "pascal", "tokens", "identifier"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FormatSize"
    )

    override suspend fun execute(input: TextCaseInspectorInput): ToolResult<TextCaseInspectorOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.identifier.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input text cannot be empty.")
        }

        val style = detectStyle(raw)
        val tokens = tokenize(raw)

        val camel = tokens.mapIndexed { idx, t ->
            val lower = t.lowercase(Locale.ROOT)
            if (idx == 0) lower else lower.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }.joinToString("")

        val pascal = tokens.joinToString("") {
            it.lowercase(Locale.ROOT).replaceFirstChar { c -> c.titlecase(Locale.ROOT) }
        }

        val snake = tokens.joinToString("_") { it.lowercase(Locale.ROOT) }
        val kebab = tokens.joinToString("-") { it.lowercase(Locale.ROOT) }
        val constant = tokens.joinToString("_") { it.uppercase(Locale.ROOT) }

        val isPure = raw.all { it.isLetterOrDigit() || it == '_' || it == '-' }

        val report = buildString {
            appendLine("IDENTIFIER CASING INSPECTION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Original Input:     $raw")
            appendLine("Detected Style:     $style")
            appendLine("Extracted Tokens:   ${tokens.joinToString(", ") { "\"$it\"" }} (${tokens.size} words)")
            appendLine("Pure Identifier:    ${if (isPure) "Yes (Valid Code Symbol)" else "No (Contains Spaces/Punctuation)"}")
            appendLine()
            appendLine("UNIFIED CONVENTIONS")
            appendLine("• camelCase:        $camel")
            appendLine("• PascalCase:       $pascal")
            appendLine("• snake_case:       $snake")
            appendLine("• kebab-case:       $kebab")
            appendLine("• CONSTANT_CASE:    $constant")
        }

        val summary = "$raw → $style (${tokens.size} tokens)"

        return ToolResult.Success(
            data = TextCaseInspectorOutput(
                detectedCaseStyle = style,
                extractedTokens = tokens,
                tokenCount = tokens.size,
                isPureIdentifier = isPure,
                suggestedConstantCase = constant,
                suggestedCamelCase = camel,
                suggestedSnakeCase = snake,
                suggestedKebabCase = kebab,
                suggestedPascalCase = pascal,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun detectStyle(s: String): String {
        return when {
            s.contains("_") && s.all { it.isUpperCase() || it == '_' || it.isDigit() } -> "CONSTANT_CASE (SCREAMING_SNAKE_CASE)"
            s.contains("_") && s.all { it.isLowerCase() || it == '_' || it.isDigit() } -> "snake_case"
            s.contains("-") && s.all { it.isLowerCase() || it == '-' || it.isDigit() } -> "kebab-case (train-case)"
            s.contains(" ") -> "Natural Language / Multi-Word String"
            s.firstOrNull()?.isUpperCase() == true && s.any { it.isLowerCase() } -> "PascalCase (UpperCamelCase)"
            s.firstOrNull()?.isLowerCase() == true && s.any { it.isUpperCase() } -> "camelCase (lowerCamelCase)"
            s.all { it.isLowerCase() } -> "lowercase (flatcase)"
            s.all { it.isUpperCase() } -> "UPPERCASE"
            else -> "Mixed / Unspecified"
        }
    }

    private fun tokenize(s: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()

        for (i in s.indices) {
            val c = s[i]
            if (c == '_' || c == '-' || c.isWhitespace()) {
                if (current.isNotEmpty()) {
                    result.add(current.toString())
                    current.clear()
                }
            } else if (c.isUpperCase()) {
                if (current.isNotEmpty() && s[i - 1].isLowerCase()) {
                    result.add(current.toString())
                    current.clear()
                }
                current.append(c)
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result.filter { it.isNotBlank() }
    }
}
