package com.offline.toolbox.core.model

/**
 * Standard categories organizing all offline toolbox utilities.
 */
enum class ToolCategory(val title: String, val description: String) {
    TEXT("Text & Writing", "Text transformations, counters, cleaners, and formatters"),
    DEVELOPER("Developer", "Encoders, decoders, JSON formatters, hashes, and dev helpers"),
    CONVERTER("Converters", "Unit conversions for length, weight, temperature, and more"),
    MATH("Calculators", "Math, percentages, fractions, and everyday financial helpers"),
    DATETIME("Date & Time", "Date diffs, time zones, countdowns, and unix timestamps"),
    SECURITY("Security & Privacy", "Secure password generators, hashes, checksums, and token generators"),
    COLOR("Color Utilities", "HEX, RGB, HSL conversions, palettes, and contrast checkers"),
    DATA("Data & CSV", "CSV viewers, converters, and data cleaners"),
    MEDIA("Image & Media", "Offline image manipulation, resizers, and metadata"),
    FILE("File Utilities", "File info, checksums, renamers, and safe archives")
}

/**
 * Declared capabilities of a tool for runtime composition, chaining, and UI action binding.
 */
enum class ToolCapability {
    SUPPORTS_CLIPBOARD_PASTE,
    SUPPORTS_CLIPBOARD_COPY,
    SUPPORTS_FILE_INPUT,
    SUPPORTS_FILE_OUTPUT,
    SUPPORTS_CHAINING,
    SUPPORTS_SHARE,
    PURE_CALCULATION,
    REQUIRES_BACKGROUND_DISPATCHER
}

/**
 * Input and output data types for compatibility validation and visual pipeline chaining.
 */
enum class ToolDataType {
    TEXT,
    JSON,
    NUMBER,
    DATE_PAIR,
    COLOR,
    FILE_URI,
    NONE
}

/**
 * Metadata descriptor for each tool.
 */
data class ToolMetadata(
    val id: String,
    val name: String,
    val description: String,
    val category: ToolCategory,
    val tags: List<String> = emptyList(),
    val inputType: ToolDataType = ToolDataType.TEXT,
    val outputType: ToolDataType = ToolDataType.TEXT,
    val capabilities: Set<ToolCapability> = setOf(
        ToolCapability.SUPPORTS_CLIPBOARD_COPY,
        ToolCapability.SUPPORTS_SHARE
    ),
    val iconName: String = "Build"
)

/**
 * Typed execution result with human-readable diagnostic error guidance.
 */
sealed interface ToolResult<out T> {
    data class Success<T>(
        val data: T,
        val executionTimeMs: Long = 0,
        val summary: String? = null
    ) : ToolResult<T>

    data class Failure(
        val message: String,
        val userGuidance: String? = null,
        val cause: Throwable? = null
    ) : ToolResult<Nothing>
}

/**
 * Unified Tool Contract. Every offline tool implements this interface.
 */
interface Tool<in Input, out Output> {
    val metadata: ToolMetadata
    suspend fun execute(input: Input): ToolResult<Output>
}
