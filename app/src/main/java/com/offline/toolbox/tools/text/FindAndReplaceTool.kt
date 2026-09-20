package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.regex.PatternSyntaxException

data class FindAndReplaceInput(
    val text: String,
    val findQuery: String,
    val replacement: String,
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val useRegex: Boolean = false
)

data class FindAndReplaceOutput(
    val resultText: String,
    val matchCount: Int,
    val summary: String
)

class FindAndReplaceTool : Tool<FindAndReplaceInput, FindAndReplaceOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "find_and_replace",
        name = "Find & Replace",
        description = "Find and replace text with case sensitivity, whole-word matching, and regex support.",
        category = ToolCategory.TEXT,
        tags = listOf("find", "replace", "search", "substitute", "regex", "words"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FindReplace"
    )

    override suspend fun execute(input: FindAndReplaceInput): ToolResult<FindAndReplaceOutput> {
        val startTime = System.currentTimeMillis()
        if (input.text.isEmpty()) {
            return ToolResult.Success(
                FindAndReplaceOutput("", 0, "Empty input text"),
                System.currentTimeMillis() - startTime
            )
        }
        if (input.findQuery.isEmpty()) {
            return ToolResult.Failure(
                message = "Search query is empty.",
                userGuidance = "Specify the string or pattern to find."
            )
        }

        return try {
            val regex = when {
                input.useRegex -> {
                    val options = if (!input.caseSensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()
                    Regex(input.findQuery, options)
                }
                input.wholeWord -> {
                    val escaped = Regex.escape(input.findQuery)
                    val pattern = "\\b$escaped\\b"
                    val options = if (!input.caseSensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()
                    Regex(pattern, options)
                }
                else -> {
                    val escaped = Regex.escape(input.findQuery)
                    val options = if (!input.caseSensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()
                    Regex(escaped, options)
                }
            }

            val matches = regex.findAll(input.text).count()
            val replaced = regex.replace(input.text, input.replacement)

            val summary = "Replaced $matches occurrence(s)"
            ToolResult.Success(
                data = FindAndReplaceOutput(
                    resultText = replaced,
                    matchCount = matches,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: PatternSyntaxException) {
            ToolResult.Failure(
                message = "Invalid regular expression: ${e.description}",
                userGuidance = "Check for unescaped special regex characters like brackets, parentheses, or unclosed ranges.",
                cause = e
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Find and replace failed: ${e.message}",
                cause = e
            )
        }
    }
}
