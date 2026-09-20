package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Random

enum class ZalgoIntensity(val range: IntRange) {
    MINIMAL(1..2),
    MEDIUM(3..5),
    EXTREME(8..14)
}

enum class ZalgoMode {
    CORRUPT_ZALGO,
    CLEAN_ZALGO
}

data class ZalgoInput(
    val text: String = "HE COMES TO DESTROY BUGS",
    val mode: ZalgoMode = ZalgoMode.CORRUPT_ZALGO,
    val intensity: ZalgoIntensity = ZalgoIntensity.MEDIUM,
    val addUp: Boolean = true,
    val addMiddle: Boolean = true,
    val addDown: Boolean = true
)

data class ZalgoOutput(
    val resultText: String,
    val mode: ZalgoMode,
    val marksAddedOrRemoved: Int,
    val summary: String
)

class ZalgoTextTool : Tool<ZalgoInput, ZalgoOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "zalgo_text_tool",
        name = "Zalgo Glitch Text & Sanitizer",
        description = "Generate chaotic cursed glitch text using Unicode combining diacritics, or sanitize and strip existing zalgo artifacts.",
        category = ToolCategory.TEXT,
        tags = listOf("zalgo", "glitch", "cursed", "unicode", "diacritics", "scary", "corrupt", "clean"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "AutoFixHigh"
    )

    // Unicode combining characters
    private val zalgoUp = listOf(
        '\u030d', '\u030e', '\u0304', '\u0305', '\u033f', '\u0311', '\u0306', '\u0310',
        '\u0352', '\u0357', '\u0351', '\u0307', '\u0308', '\u030a', '\u0342', '\u0343',
        '\u0344', '\u034a', '\u034b', '\u034c', '\u0350', '\u0300', '\u0301', '\u0302'
    )
    private val zalgoMiddle = listOf(
        '\u0315', '\u031b', '\u0340', '\u0341', '\u0358', '\u0321', '\u0322', '\u0327',
        '\u0328', '\u0334', '\u0335', '\u0336', '\u0337', '\u0338', '\u0360', '\u0361'
    )
    private val zalgoDown = listOf(
        '\u0316', '\u0317', '\u0318', '\u0319', '\u031c', '\u031d', '\u031e', '\u031f',
        '\u0320', '\u0324', '\u0325', '\u0326', '\u0329', '\u032a', '\u032b', '\u032c',
        '\u032d', '\u032e', '\u032f', '\u0330', '\u0331', '\u0332', '\u0333', '\u0339'
    )

    private val allZalgoSet = (zalgoUp + zalgoMiddle + zalgoDown).toSet()

    override suspend fun execute(input: ZalgoInput): ToolResult<ZalgoOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.text

        if (raw.isEmpty()) {
            return ToolResult.Success(
                ZalgoOutput("", input.mode, 0, "Empty input")
            )
        }

        return when (input.mode) {
            ZalgoMode.CLEAN_ZALGO -> {
                var removed = 0
                val sb = StringBuilder()
                for (ch in raw) {
                    if (allZalgoSet.contains(ch) || (ch.code in 0x0300..0x036F)) {
                        removed++
                    } else {
                        sb.append(ch)
                    }
                }
                val cleaned = sb.toString()
                val summary = "Stripped $removed combining diacritical marks"
                ToolResult.Success(
                    data = ZalgoOutput(cleaned, input.mode, removed, summary),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            }
            ZalgoMode.CORRUPT_ZALGO -> {
                val rand = Random(1337)
                val sb = StringBuilder()
                var added = 0

                for (ch in raw) {
                    sb.append(ch)
                    if (ch.isWhitespace()) continue

                    if (input.addUp) {
                        val count = rand.nextInt(input.intensity.range.last - input.intensity.range.first + 1) + input.intensity.range.first
                        repeat(count) {
                            sb.append(zalgoUp[rand.nextInt(zalgoUp.size)])
                            added++
                        }
                    }
                    if (input.addMiddle) {
                        val count = rand.nextInt(input.intensity.range.last - input.intensity.range.first + 1) + input.intensity.range.first
                        repeat(count) {
                            sb.append(zalgoMiddle[rand.nextInt(zalgoMiddle.size)])
                            added++
                        }
                    }
                    if (input.addDown) {
                        val count = rand.nextInt(input.intensity.range.last - input.intensity.range.first + 1) + input.intensity.range.first
                        repeat(count) {
                            sb.append(zalgoDown[rand.nextInt(zalgoDown.size)])
                            added++
                        }
                    }
                }

                val result = sb.toString()
                val summary = "Generated zalgo with $added chaotic marks"

                ToolResult.Success(
                    data = ZalgoOutput(result, input.mode, added, summary),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            }
        }
    }
}
