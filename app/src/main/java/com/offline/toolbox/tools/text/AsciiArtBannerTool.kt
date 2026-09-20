package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class AsciiBannerStyle(val label: String) {
    BLOCK_FONT("Block Font (5-Row Standard)"),
    BOX_BORDER_SINGLE("Boxed Border (Single ┌─┐)"),
    BOX_BORDER_DOUBLE("Boxed Border (Double ╔═╗)"),
    BOX_BORDER_ROUNDED("Boxed Border (Rounded ╭─╮)"),
    BOX_BORDER_ASCII("Boxed Border (ASCII +-+)")
}

data class AsciiArtBannerInput(
    val text: String = "",
    val style: AsciiBannerStyle = AsciiBannerStyle.BLOCK_FONT,
    val padding: Int = 2
)

data class AsciiArtBannerOutput(
    val banner: String,
    val lineCount: Int,
    val characterCount: Int,
    val summary: String
)

class AsciiArtBannerTool : Tool<AsciiArtBannerInput, AsciiArtBannerOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "ascii_art_banner_tool",
        name = "ASCII Art Banner & Box Generator",
        description = "Generate large ASCII banners, terminal headers, and decorative unicode box cards.",
        category = ToolCategory.TEXT,
        tags = listOf("ascii", "banner", "art", "box", "border", "figlet", "header", "terminal"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "TextFields"
    )

    override suspend fun execute(input: AsciiArtBannerInput): ToolResult<AsciiArtBannerOutput> {
        val startTime = System.currentTimeMillis()

        if (input.text.isBlank()) {
            return ToolResult.Failure(
                message = "Input text is empty.",
                userGuidance = "Type text to generate an ASCII art banner."
            )
        }

        return try {
            val banner = when (input.style) {
                AsciiBannerStyle.BLOCK_FONT -> renderBlockFont(input.text.trim())
                AsciiBannerStyle.BOX_BORDER_SINGLE -> renderBox(input.text.trim(), '┌', '┐', '└', '┘', '─', '│', input.padding)
                AsciiBannerStyle.BOX_BORDER_DOUBLE -> renderBox(input.text.trim(), '╔', '╗', '╚', '╝', '═', '║', input.padding)
                AsciiBannerStyle.BOX_BORDER_ROUNDED -> renderBox(input.text.trim(), '╭', '╮', '╰', '╯', '─', '│', input.padding)
                AsciiBannerStyle.BOX_BORDER_ASCII -> renderBox(input.text.trim(), '+', '+', '+', '+', '-', '|', input.padding)
            }

            val lines = banner.lines()
            val summary = "Generated ASCII banner (${lines.size} lines, style: ${input.style.label})"

            ToolResult.Success(
                data = AsciiArtBannerOutput(
                    banner = banner,
                    lineCount = lines.size,
                    characterCount = banner.length,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("Failed to render ASCII banner: ${e.message}", cause = e)
        }
    }

    private fun renderBox(
        text: String,
        tl: Char, tr: Char, bl: Char, br: Char,
        h: Char, v: Char,
        padding: Int
    ): String {
        val lines = text.lines()
        val maxLen = lines.maxOfOrNull { it.length } ?: 0
        val contentWidth = maxLen + (padding * 2)
        val horizontalBorder = h.toString().repeat(contentWidth)
        val padSpace = " ".repeat(padding)

        val sb = StringBuilder()
        sb.append(tl).append(horizontalBorder).append(tr).append("\n")

        for (line in lines) {
            val rightPad = " ".repeat(maxLen - line.length)
            sb.append(v).append(padSpace).append(line).append(rightPad).append(padSpace).append(v).append("\n")
        }

        sb.append(bl).append(horizontalBorder).append(br)
        return sb.toString()
    }

    private fun renderBlockFont(text: String): String {
        val uppercase = text.uppercase(Locale.US)
        val rows = Array(5) { StringBuilder() }

        for (ch in uppercase) {
            val glyph = GLYPHS[ch] ?: GLYPHS['?']!!
            for (r in 0 until 5) {
                rows[r].append(glyph[r]).append(" ")
            }
        }

        return rows.joinToString("\n") { it.toString().trimEnd() }
    }

    companion object {
        private val GLYPHS = mapOf(
            'A' to arrayOf("  █  ", " █ █ ", "█████", "█   █", "█   █"),
            'B' to arrayOf("████ ", "█   █", "████ ", "█   █", "████ "),
            'C' to arrayOf(" ████", "█    ", "█    ", "█    ", " ████"),
            'D' to arrayOf("████ ", "█   █", "█   █", "█   █", "████ "),
            'E' to arrayOf("█████", "█    ", "████ ", "█    ", "█████"),
            'F' to arrayOf("█████", "█    ", "████ ", "█    ", "█    "),
            'G' to arrayOf(" ████", "█    ", "█  ██", "█   █", " ████"),
            'H' to arrayOf("█   █", "█   █", "█████", "█   █", "█   █"),
            'I' to arrayOf("███", " █ ", " █ ", " █ ", "███"),
            'J' to arrayOf("  ███", "   █ ", "   █ ", "█  █ ", " ██  "),
            'K' to arrayOf("█   █", "█  █ ", "███  ", "█  █ ", "█   █"),
            'L' to arrayOf("█    ", "█    ", "█    ", "█    ", "█████"),
            'M' to arrayOf("█   █", "██ ██", "█ █ █", "█   █", "█   █"),
            'N' to arrayOf("█   █", "██  █", "█ █ █", "█  ██", "█   █"),
            'O' to arrayOf(" ███ ", "█   █", "█   █", "█   █", " ███ "),
            'P' to arrayOf("████ ", "█   █", "████ ", "█    ", "█    "),
            'Q' to arrayOf(" ███ ", "█   █", "█ █ █", " ███ ", "    █"),
            'R' to arrayOf("████ ", "█   █", "████ ", "█  █ ", "█   █"),
            'S' to arrayOf(" ████", "█    ", " ███ ", "    █", "████ "),
            'T' to arrayOf("█████", "  █  ", "  █  ", "  █  ", "  █  "),
            'U' to arrayOf("█   █", "█   █", "█   █", "█   █", " ███ "),
            'V' to arrayOf("█   █", "█   █", "█   █", " █ █ ", "  █  "),
            'W' to arrayOf("█   █", "█   █", "█ █ █", "██ ██", "█   █"),
            'X' to arrayOf("█   █", " █ █ ", "  █  ", " █ █ ", "█   █"),
            'Y' to arrayOf("█   █", " █ █ ", "  █  ", "  █  ", "  █  "),
            'Z' to arrayOf("█████", "   █ ", "  █  ", " █   ", "█████"),
            '0' to arrayOf("█████", "█  ██", "█ █ █", "██  █", "█████"),
            '1' to arrayOf(" ██ ", "  █ ", "  █ ", "  █ ", "████"),
            '2' to arrayOf("█████", "    █", "█████", "█    ", "█████"),
            '3' to arrayOf("█████", "    █", " ████", "    █", "█████"),
            '4' to arrayOf("█   █", "█   █", "█████", "    █", "    █"),
            '5' to arrayOf("█████", "█    ", "████ ", "    █", "████ "),
            '6' to arrayOf("█████", "█    ", "█████", "█   █", "█████"),
            '7' to arrayOf("█████", "   █ ", "  █  ", " █   ", "█    "),
            '8' to arrayOf("█████", "█   █", "█████", "█   █", "█████"),
            '9' to arrayOf("█████", "█   █", "█████", "    █", "█████"),
            '!' to arrayOf(" █ ", " █ ", " █ ", "   ", " █ "),
            '?' to arrayOf("████ ", "    █", "  ██ ", "     ", "  █  "),
            '-' to arrayOf("     ", "     ", "█████", "     ", "     "),
            '+' to arrayOf("     ", "  █  ", "█████", "  █  ", "     "),
            '=' to arrayOf("     ", "█████", "     ", "█████", "     "),
            '.' to arrayOf("   ", "   ", "   ", "   ", " █ "),
            ':' to arrayOf("   ", " █ ", "   ", " █ ", "   "),
            ' ' to arrayOf("   ", "   ", "   ", "   ", "   ")
        )
    }
}
