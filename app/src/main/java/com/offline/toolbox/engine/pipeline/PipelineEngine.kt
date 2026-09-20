package com.offline.toolbox.engine.pipeline

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.developer.Base32Input
import com.offline.toolbox.tools.developer.Base32Mode
import com.offline.toolbox.tools.developer.Base32Tool
import com.offline.toolbox.tools.developer.Base64Input
import com.offline.toolbox.tools.developer.Base64Mode
import com.offline.toolbox.tools.developer.Base64Tool
import com.offline.toolbox.tools.developer.HtmlEntityInput
import com.offline.toolbox.tools.developer.HtmlEntityMode
import com.offline.toolbox.tools.developer.HtmlEntityTool
import com.offline.toolbox.tools.developer.JsonFormatterInput
import com.offline.toolbox.tools.developer.JsonFormatterTool
import com.offline.toolbox.tools.developer.JsonOperation
import com.offline.toolbox.tools.developer.UrlEncoderInput
import com.offline.toolbox.tools.developer.UrlEncoderTool
import com.offline.toolbox.tools.developer.UrlOperation
import com.offline.toolbox.tools.security.HashAlgorithm
import com.offline.toolbox.tools.security.HashGeneratorTool
import com.offline.toolbox.tools.security.HashInput
import com.offline.toolbox.tools.security.HmacAlgorithm
import com.offline.toolbox.tools.security.HmacGeneratorTool
import com.offline.toolbox.tools.security.HmacInput
import com.offline.toolbox.tools.text.CaseType
import com.offline.toolbox.tools.text.LeetInput
import com.offline.toolbox.tools.text.LeetLevel
import com.offline.toolbox.tools.text.LeetMode
import com.offline.toolbox.tools.text.LeetspeakTool
import com.offline.toolbox.tools.text.MorseCodeInput
import com.offline.toolbox.tools.text.MorseCodeTool
import com.offline.toolbox.tools.text.MorseDirection
import com.offline.toolbox.tools.text.Rot13CipherTool
import com.offline.toolbox.tools.text.Rot13Input
import com.offline.toolbox.tools.text.SlugGeneratorTool
import com.offline.toolbox.tools.text.SlugInput
import com.offline.toolbox.tools.text.TextBinaryHexInput
import com.offline.toolbox.tools.text.TextBinaryHexMode
import com.offline.toolbox.tools.text.TextBinaryHexTool
import com.offline.toolbox.tools.text.TextCaseConverterTool
import com.offline.toolbox.tools.text.TextCaseInput
import com.offline.toolbox.tools.text.TextCleanerInput
import com.offline.toolbox.tools.text.TextCleanerOptions
import com.offline.toolbox.tools.text.TextCleanerTool

data class PipelineStep(
    val toolId: String,
    val toolName: String,
    val config: Map<String, String> = emptyMap()
)

data class PipelineStepResult(
    val stepIndex: Int,
    val toolId: String,
    val toolName: String,
    val inputData: String,
    val outputData: String,
    val durationMs: Long,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class PipelineRecipe(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<PipelineStep>
)

data class PipelineExecutionResult(
    val initialInput: String,
    val finalOutput: String,
    val stepResults: List<PipelineStepResult>,
    val isSuccess: Boolean,
    val totalDurationMs: Long
)

/**
 * Sequential multi-tool execution engine for chaining text, developer, and cryptographic utilities.
 * 100% offline, atomic, produces a complete step-by-step audit trail.
 */
object PipelineEngine {

    val builtInRecipes: List<PipelineRecipe> = listOf(
        PipelineRecipe(
            id = "clean_slug",
            name = "Clean & Sluggify URL",
            description = "Trims whitespace, cleans punctuation, converts to lower case, and creates a clean URL slug.",
            steps = listOf(
                PipelineStep("text_cleaner", "Clean Whitespace", mapOf("trim" to "true", "removeExtraSpaces" to "true")),
                PipelineStep("text_case_converter", "Lower Case", mapOf("mode" to "LOWERCASE")),
                PipelineStep("slug_generator", "Slugify", mapOf("separator" to "-"))
            )
        ),
        PipelineRecipe(
            id = "hacker_obfuscate",
            name = "Hacker Obfuscator",
            description = "Converts plain text to leetspeak, applies ROT13 cipher, and encodes into Base64.",
            steps = listOf(
                PipelineStep("leetspeak", "Leetspeak", mapOf("level" to "BASIC")),
                PipelineStep("rot13_cipher", "ROT13 Cipher"),
                PipelineStep("base64", "Base64 Encode", mapOf("mode" to "ENCODE"))
            )
        ),
        PipelineRecipe(
            id = "dev_payload_hash",
            name = "Dev Payload Checksum",
            description = "Trims and normalizes text, encodes in Base64, and calculates SHA-256 fingerprint.",
            steps = listOf(
                PipelineStep("text_cleaner", "Trim Whitespace", mapOf("trim" to "true")),
                PipelineStep("base64", "Base64 Encode", mapOf("mode" to "ENCODE")),
                PipelineStep("hash_generator", "SHA-256 Hash", mapOf("algorithm" to "SHA_256"))
            )
        ),
        PipelineRecipe(
            id = "morse_secret",
            name = "Morse Code & Hex Stream",
            description = "Converts input text to Morse code dots and dashes, then streams into Hexadecimal.",
            steps = listOf(
                PipelineStep("morse_code", "Encode Morse", mapOf("direction" to "TEXT_TO_MORSE")),
                PipelineStep("text_binary_hex", "Hex Stream", mapOf("mode" to "TEXT_TO_ALL"))
            )
        )
    )

    suspend fun execute(initialInput: String, steps: List<PipelineStep>): PipelineExecutionResult {
        val startTime = System.currentTimeMillis()
        var currentData = initialInput
        val stepResults = mutableListOf<PipelineStepResult>()

        for ((index, step) in steps.withIndex()) {
            val stepStart = System.currentTimeMillis()
            val stepInput = currentData
            try {
                val stepOutput = executeStep(step, stepInput)
                val duration = System.currentTimeMillis() - stepStart
                stepResults.add(
                    PipelineStepResult(
                        stepIndex = index + 1,
                        toolId = step.toolId,
                        toolName = step.toolName,
                        inputData = stepInput,
                        outputData = stepOutput,
                        durationMs = duration,
                        isSuccess = true
                    )
                )
                currentData = stepOutput
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - stepStart
                val errorMsg = e.message ?: "Unknown error executing ${step.toolName}"
                stepResults.add(
                    PipelineStepResult(
                        stepIndex = index + 1,
                        toolId = step.toolId,
                        toolName = step.toolName,
                        inputData = stepInput,
                        outputData = "",
                        durationMs = duration,
                        isSuccess = false,
                        errorMessage = errorMsg
                    )
                )
                return PipelineExecutionResult(
                    initialInput = initialInput,
                    finalOutput = currentData,
                    stepResults = stepResults,
                    isSuccess = false,
                    totalDurationMs = System.currentTimeMillis() - startTime
                )
            }
        }

        return PipelineExecutionResult(
            initialInput = initialInput,
            finalOutput = currentData,
            stepResults = stepResults,
            isSuccess = true,
            totalDurationMs = System.currentTimeMillis() - startTime
        )
    }

    private suspend fun executeStep(step: PipelineStep, input: String): String {
        return when (step.toolId) {
            "text_case_converter" -> {
                val modeStr = step.config["mode"] ?: "UPPERCASE"
                val caseType = try { CaseType.valueOf(modeStr) } catch (_: Exception) { CaseType.UPPERCASE }
                val tool = TextCaseConverterTool()
                val result = tool.execute(TextCaseInput(input, caseType))
                (result as? ToolResult.Success)?.data
                    ?: throw IllegalStateException("Case converter failed")
            }
            "text_cleaner" -> {
                val tool = TextCleanerTool()
                val options = TextCleanerOptions(
                    trimLines = step.config["trim"]?.toBooleanStrictOrNull() ?: true,
                    removeExtraSpaces = step.config["removeExtraSpaces"]?.toBooleanStrictOrNull() ?: true,
                    removeEmptyLines = step.config["removeBlankLines"]?.toBooleanStrictOrNull() ?: false
                )
                val result = tool.execute(TextCleanerInput(text = input, options = options))
                (result as? ToolResult.Success)?.data?.cleanedText
                    ?: throw IllegalStateException("Text cleaner failed")
            }
            "slug_generator" -> {
                val tool = SlugGeneratorTool()
                val sep = step.config["separator"] ?: "-"
                val result = tool.execute(SlugInput(input, sep))
                (result as? ToolResult.Success)?.data?.slug
                    ?: throw IllegalStateException("Slug generation failed")
            }
            "rot13_cipher" -> {
                val tool = Rot13CipherTool()
                val result = tool.execute(Rot13Input(input))
                (result as? ToolResult.Success)?.data?.transformedText
                    ?: throw IllegalStateException("ROT13 failed")
            }
            "base64" -> {
                val modeStr = step.config["mode"] ?: "ENCODE"
                val mode = if (modeStr.equals("DECODE", ignoreCase = true)) Base64Mode.DECODE_STANDARD else Base64Mode.ENCODE_STANDARD
                val tool = Base64Tool()
                val result = tool.execute(Base64Input(input, mode))
                (result as? ToolResult.Success)?.data
                    ?: throw IllegalStateException((result as? ToolResult.Failure)?.message ?: "Base64 failed")
            }
            "base32" -> {
                val modeStr = step.config["mode"] ?: "ENCODE"
                val mode = if (modeStr.equals("DECODE", ignoreCase = true)) Base32Mode.DECODE else Base32Mode.ENCODE
                val tool = Base32Tool()
                val result = tool.execute(Base32Input(input, mode))
                (result as? ToolResult.Success)?.data?.result
                    ?: throw IllegalStateException("Base32 failed")
            }
            "url_encoder" -> {
                val modeStr = step.config["mode"] ?: "ENCODE"
                val op = if (modeStr.equals("DECODE", ignoreCase = true)) UrlOperation.DECODE else UrlOperation.ENCODE
                val tool = UrlEncoderTool()
                val result = tool.execute(UrlEncoderInput(input, op))
                (result as? ToolResult.Success)?.data
                    ?: throw IllegalStateException("URL encoder failed")
            }
            "html_entity" -> {
                val modeStr = step.config["mode"] ?: "ENCODE"
                val mode = if (modeStr.equals("DECODE", ignoreCase = true)) HtmlEntityMode.DECODE else HtmlEntityMode.ENCODE
                val tool = HtmlEntityTool()
                val result = tool.execute(HtmlEntityInput(input, mode))
                (result as? ToolResult.Success)?.data
                    ?: throw IllegalStateException("HTML entity failed")
            }
            "hash_generator" -> {
                val algoStr = step.config["algorithm"] ?: "SHA_256"
                val algo = try { HashAlgorithm.valueOf(algoStr) } catch (_: Exception) { HashAlgorithm.SHA_256 }
                val tool = HashGeneratorTool()
                val result = tool.execute(HashInput(input, algo))
                (result as? ToolResult.Success)?.data?.hash
                    ?: throw IllegalStateException("Hash generator failed")
            }
            "hmac_generator" -> {
                val key = step.config["key"] ?: "default-key"
                val algoStr = step.config["algorithm"] ?: "HMAC_SHA256"
                val algo = try { HmacAlgorithm.valueOf(algoStr) } catch (_: Exception) { HmacAlgorithm.HMAC_SHA256 }
                val tool = HmacGeneratorTool()
                val result = tool.execute(HmacInput(message = input, secretKey = key, algorithm = algo))
                (result as? ToolResult.Success)?.data?.hexSignature
                    ?: throw IllegalStateException("HMAC generator failed")
            }
            "leetspeak" -> {
                val levelStr = step.config["level"] ?: "BASIC"
                val level = try { LeetLevel.valueOf(levelStr) } catch (_: Exception) { LeetLevel.BASIC }
                val modeStr = step.config["mode"] ?: "ENCODE"
                val mode = try { LeetMode.valueOf(modeStr) } catch (_: Exception) { LeetMode.ENCODE }
                val tool = LeetspeakTool()
                val result = tool.execute(LeetInput(input, level, mode))
                (result as? ToolResult.Success)?.data?.resultText
                    ?: throw IllegalStateException("Leetspeak failed")
            }
            "morse_code" -> {
                val actStr = step.config["direction"] ?: "TEXT_TO_MORSE"
                val dir = try { MorseDirection.valueOf(actStr) } catch (_: Exception) { MorseDirection.TEXT_TO_MORSE }
                val tool = MorseCodeTool()
                val result = tool.execute(MorseCodeInput(text = input, direction = dir))
                (result as? ToolResult.Success)?.data?.convertedText
                    ?: throw IllegalStateException("Morse code failed")
            }
            "text_binary_hex" -> {
                val tool = TextBinaryHexTool()
                val result = tool.execute(TextBinaryHexInput(input, TextBinaryHexMode.TEXT_TO_ALL))
                (result as? ToolResult.Success)?.data?.hexContinuous
                    ?: throw IllegalStateException("Binary/Hex failed")
            }
            "json_formatter" -> {
                val tool = JsonFormatterTool()
                val result = tool.execute(JsonFormatterInput(input, JsonOperation.FORMAT_2_SPACES))
                (result as? ToolResult.Success)?.data?.processedText
                    ?: throw IllegalStateException("JSON formatter failed")
            }
            else -> throw IllegalArgumentException("Unsupported pipeline step: ${step.toolId}")
        }
    }
}
