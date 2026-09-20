package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Base64

enum class Base64Mode(val displayName: String) {
    ENCODE_STANDARD("Encode (Standard)"),
    DECODE_STANDARD("Decode (Standard)"),
    ENCODE_URL_SAFE("Encode (URL Safe)"),
    DECODE_URL_SAFE("Decode (URL Safe)"),
    HEX_TO_BASE64("Hex → Base64"),
    BASE64_TO_HEX("Base64 → Hex")
}

data class Base64Input(
    val content: String,
    val mode: Base64Mode = Base64Mode.ENCODE_STANDARD
)

class Base64Tool : Tool<Base64Input, String> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "base64_converter",
        name = "Base64 Encoder & Decoder",
        description = "Encode and decode standard and URL-safe Base64 strings, with Hex conversion support.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("base64", "encode", "decode", "url-safe", "hex", "binary", "b64"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Code"
    )

    override suspend fun execute(input: Base64Input): ToolResult<String> {
        val startTime = System.currentTimeMillis()
        val text = input.content.trim()

        if (text.isEmpty()) {
            return ToolResult.Success("", System.currentTimeMillis() - startTime, "Empty input")
        }

        return try {
            val result = when (input.mode) {
                Base64Mode.ENCODE_STANDARD -> {
                    val bytes = text.toByteArray(Charsets.UTF_8)
                    Base64.getEncoder().encodeToString(bytes)
                }
                Base64Mode.DECODE_STANDARD -> {
                    val decodedBytes = Base64.getDecoder().decode(text)
                    String(decodedBytes, Charsets.UTF_8)
                }
                Base64Mode.ENCODE_URL_SAFE -> {
                    val bytes = text.toByteArray(Charsets.UTF_8)
                    Base64.getUrlEncoder().encodeToString(bytes)
                }
                Base64Mode.DECODE_URL_SAFE -> {
                    val decodedBytes = Base64.getUrlDecoder().decode(text)
                    String(decodedBytes, Charsets.UTF_8)
                }
                Base64Mode.HEX_TO_BASE64 -> {
                    val cleanHex = text.replace(Regex("[^0-9a-fA-F]"), "")
                    if (cleanHex.length % 2 != 0) {
                        return ToolResult.Failure(
                            message = "Invalid Hex string length (${cleanHex.length}).",
                            userGuidance = "Hexadecimal strings must have an even number of characters (each pair represents one byte)."
                        )
                    }
                    val bytes = ByteArray(cleanHex.length / 2)
                    for (i in bytes.indices) {
                        val index = i * 2
                        bytes[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
                    }
                    Base64.getEncoder().encodeToString(bytes)
                }
                Base64Mode.BASE64_TO_HEX -> {
                    val decodedBytes = Base64.getDecoder().decode(text)
                    decodedBytes.joinToString("") { "%02X".format(it) }
                }
            }

            ToolResult.Success(
                data = result,
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = "${input.mode.displayName} completed"
            )
        } catch (e: IllegalArgumentException) {
            ToolResult.Failure(
                message = "Invalid Base64 input: ${e.message ?: "Illegal character or padding"}",
                userGuidance = "Ensure the input only contains valid Base64 characters (A-Z, a-z, 0-9, +, /, =) without unexpected whitespace or invalid symbols.",
                cause = e
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Conversion failed: ${e.message ?: "Unknown error"}",
                cause = e
            )
        }
    }
}
