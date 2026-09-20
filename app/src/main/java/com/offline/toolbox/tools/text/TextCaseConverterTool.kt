package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class CaseType(val displayName: String) {
    UPPERCASE("UPPERCASE"),
    LOWERCASE("lowercase"),
    TITLE_CASE("Title Case"),
    CAMEL_CASE("camelCase"),
    PASCAL_CASE("PascalCase"),
    SNAKE_CASE("snake_case"),
    KEBAB_CASE("kebab-case"),
    CONSTANT_CASE("CONSTANT_CASE"),
    ALTERNATING_CASE("aLtErNaTiNg cAsE"),
    INVERT_CASE("iNVERT cASE")
}

data class TextCaseInput(
    val text: String,
    val targetCase: CaseType
)

class TextCaseConverterTool : Tool<TextCaseInput, String> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "text_case_converter",
        name = "Text Case Converter",
        description = "Convert text between uppercase, lowercase, title, camel, snake, kebab, pascal, and constant cases.",
        category = ToolCategory.TEXT,
        tags = listOf("case", "capitalize", "camel", "snake", "kebab", "upper", "lower"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FormatSize"
    )

    override suspend fun execute(input: TextCaseInput): ToolResult<String> {
        val startTime = System.currentTimeMillis()
        val text = input.text
        if (text.isEmpty()) {
            return ToolResult.Success("", System.currentTimeMillis() - startTime, "Empty input")
        }

        val result = when (input.targetCase) {
            CaseType.UPPERCASE -> text.uppercase(Locale.getDefault())
            CaseType.LOWERCASE -> text.lowercase(Locale.getDefault())
            CaseType.TITLE_CASE -> toTitleCase(text)
            CaseType.CAMEL_CASE -> toCamelCase(text)
            CaseType.PASCAL_CASE -> toPascalCase(text)
            CaseType.SNAKE_CASE -> toDelimitedCase(text, "_")
            CaseType.KEBAB_CASE -> toDelimitedCase(text, "-")
            CaseType.CONSTANT_CASE -> toDelimitedCase(text, "_").uppercase(Locale.getDefault())
            CaseType.ALTERNATING_CASE -> toAlternatingCase(text)
            CaseType.INVERT_CASE -> toInvertCase(text)
        }

        return ToolResult.Success(
            data = result,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Converted to ${input.targetCase.displayName}"
        )
    }

    private fun extractWords(text: String): List<String> {
        // Split by whitespace, underscores, hyphens, or camelCase transitions
        val words = mutableListOf<String>()
        val currentWord = StringBuilder()

        for (i in text.indices) {
            val c = text[i]
            if (c.isWhitespace() || c == '_' || c == '-') {
                if (currentWord.isNotEmpty()) {
                    words.add(currentWord.toString())
                    currentWord.clear()
                }
            } else if (c.isUpperCase() && currentWord.isNotEmpty() &&
                (i + 1 < text.length && text[i + 1].isLowerCase() || text[i - 1].isLowerCase())
            ) {
                words.add(currentWord.toString())
                currentWord.clear()
                currentWord.append(c)
            } else {
                currentWord.append(c)
            }
        }
        if (currentWord.isNotEmpty()) {
            words.add(currentWord.toString())
        }
        return words
    }

    private fun toTitleCase(text: String): String {
        return text.split("\n").joinToString("\n") { line ->
            line.split(" ").joinToString(" ") { word ->
                if (word.isEmpty()) ""
                else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }
    }

    private fun toCamelCase(text: String): String {
        val words = extractWords(text)
        if (words.isEmpty()) return ""
        val first = words[0].lowercase(Locale.getDefault())
        val rest = words.drop(1).joinToString("") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
        }
        return first + rest
    }

    private fun toPascalCase(text: String): String {
        val words = extractWords(text)
        return words.joinToString("") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
        }
    }

    private fun toDelimitedCase(text: String, delimiter: String): String {
        val words = extractWords(text)
        return words.joinToString(delimiter) { it.lowercase(Locale.getDefault()) }
    }

    private fun toAlternatingCase(text: String): String {
        var toUpper = false
        val sb = StringBuilder()
        for (c in text) {
            if (c.isLetter()) {
                sb.append(if (toUpper) c.uppercaseChar() else c.lowercaseChar())
                toUpper = !toUpper
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun toInvertCase(text: String): String {
        val sb = StringBuilder()
        for (c in text) {
            if (c.isUpperCase()) sb.append(c.lowercaseChar())
            else if (c.isLowerCase()) sb.append(c.uppercaseChar())
            else sb.append(c)
        }
        return sb.toString()
    }
}
