package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class Rot13Input(
    val text: String,
    val shift: Int = 13,
    val rotateDigits: Boolean = false
)

data class Rot13Output(
    val transformedText: String,
    val shift: Int,
    val summary: String
)

class Rot13CipherTool : Tool<Rot13Input, Rot13Output> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "rot13_cipher",
        name = "ROT13 & Caesar Cipher",
        description = "Encrypt or decrypt text using classic ROT13 or custom Caesar rotation shift (1 to 25).",
        category = ToolCategory.TEXT,
        tags = listOf("rot13", "caesar", "cipher", "encrypt", "decrypt", "rotate", "shift", "obfuscate"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Lock"
    )

    override suspend fun execute(input: Rot13Input): ToolResult<Rot13Output> {
        val startTime = System.currentTimeMillis()
        val shift = ((input.shift % 26) + 26) % 26
        val digitShift = ((input.shift % 10) + 10) % 10

        val sb = StringBuilder(input.text.length)
        for (ch in input.text) {
            when {
                ch in 'a'..'z' -> {
                    val rotated = 'a' + ((ch - 'a' + shift) % 26)
                    sb.append(rotated)
                }
                ch in 'A'..'Z' -> {
                    val rotated = 'A' + ((ch - 'A' + shift) % 26)
                    sb.append(rotated)
                }
                input.rotateDigits && ch in '0'..'9' -> {
                    val rotated = '0' + ((ch - '0' + digitShift) % 10)
                    sb.append(rotated)
                }
                else -> sb.append(ch)
            }
        }

        val resultText = sb.toString()
        val summary = "Shifted by $shift position(s)"

        return ToolResult.Success(
            data = Rot13Output(
                transformedText = resultText,
                shift = shift,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
