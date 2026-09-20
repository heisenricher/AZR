package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.regex.PatternSyntaxException

data class RegexTesterInput(
    val regexPattern: String,
    val testText: String,
    val ignoreCase: Boolean = false,
    val multiline: Boolean = false,
    val dotAll: Boolean = false,
    val replacement: String = ""
)

data class RegexMatchDetail(
    val index: Int,
    val value: String,
    val range: String,
    val groups: List<String>
)

data class RegexTesterOutput(
    val isMatch: Boolean,
    val matchCount: Int,
    val matches: List<RegexMatchDetail>,
    val replacedText: String?,
    val summary: String
)

class RegexTesterTool : Tool<RegexTesterInput, RegexTesterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "regex_tester",
        name = "Regex Tester & Matcher",
        description = "Test regular expressions with flags, view matched groups, and preview replacements.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("regex", "regular expression", "test", "match", "groups", "pattern", "replace"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "ManageSearch"
    )

    override suspend fun execute(input: RegexTesterInput): ToolResult<RegexTesterOutput> {
        val startTime = System.currentTimeMillis()

        if (input.regexPattern.isEmpty()) {
            return ToolResult.Failure(
                message = "Regular expression pattern is empty.",
                userGuidance = "Enter a regex pattern such as '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}'."
            )
        }

        val options = mutableSetOf<RegexOption>()
        if (input.ignoreCase) options.add(RegexOption.IGNORE_CASE)
        if (input.multiline) options.add(RegexOption.MULTILINE)
        if (input.dotAll) options.add(RegexOption.DOT_MATCHES_ALL)

        return try {
            val regex = Regex(input.regexPattern, options)
            val matchResults = regex.findAll(input.testText).toList()

            val details = matchResults.take(100).mapIndexed { idx, match ->
                RegexMatchDetail(
                    index = idx + 1,
                    value = match.value,
                    range = "[${match.range.first}, ${match.range.last}]",
                    groups = match.groupValues.drop(1)
                )
            }

            val replaced = if (input.replacement.isNotEmpty()) {
                regex.replace(input.testText, input.replacement)
            } else null

            val summary = if (matchResults.isEmpty()) {
                "No matches found"
            } else {
                "Found ${matchResults.size} match(es)"
            }

            ToolResult.Success(
                data = RegexTesterOutput(
                    isMatch = matchResults.isNotEmpty(),
                    matchCount = matchResults.size,
                    matches = details,
                    replacedText = replaced,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: PatternSyntaxException) {
            ToolResult.Failure(
                message = "Invalid regex syntax: ${e.description}",
                userGuidance = "Error near index ${e.index}. Check unclosed brackets, parenthesis, or escaping.",
                cause = e
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Regex execution failed: ${e.message}",
                cause = e
            )
        }
    }
}
