package com.offline.toolbox.tools.converter

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

enum class StorageUnit(val symbol: String, val displayName: String, val powerIndex: Int) {
    BYTE("B", "Bytes", 0),
    KILOBYTE("KB", "Kilobytes", 1),
    MEGABYTE("MB", "Megabytes", 2),
    GIGABYTE("GB", "Gigabytes", 3),
    TERABYTE("TB", "Terabytes", 4),
    PETABYTE("PB", "Petabytes", 5)
}

data class DataStorageInput(
    val value: Double,
    val fromUnit: StorageUnit = StorageUnit.MEGABYTE,
    val toUnit: StorageUnit = StorageUnit.GIGABYTE,
    val useBinaryBase1024: Boolean = true
)

data class DataStorageOutput(
    val resultValue: Double,
    val resultFormatted: String,
    val allConversions: Map<StorageUnit, Double>,
    val totalBits: Double,
    val summary: String
)

class DataStorageConverterTool : Tool<DataStorageInput, DataStorageOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "data_storage_converter",
        name = "Data Storage Converter",
        description = "Convert between Bytes, KB, MB, GB, TB, and PB with Decimal (1000) or Binary (1024) base.",
        category = ToolCategory.CONVERTER,
        tags = listOf("data", "storage", "bytes", "mb", "gb", "kb", "tb", "memory", "file size", "binary"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "SdStorage"
    )

    override suspend fun execute(input: DataStorageInput): ToolResult<DataStorageOutput> {
        val startTime = System.currentTimeMillis()
        val base = if (input.useBinaryBase1024) 1024.0 else 1000.0

        // Convert input value to total bytes
        val totalBytes = input.value * Math.pow(base, input.fromUnit.powerIndex.toDouble())
        val converted = totalBytes / Math.pow(base, input.toUnit.powerIndex.toDouble())

        val allMap = mutableMapOf<StorageUnit, Double>()
        for (unit in StorageUnit.values()) {
            allMap[unit] = totalBytes / Math.pow(base, unit.powerIndex.toDouble())
        }

        val baseLabel = if (input.useBinaryBase1024) "1024 (Binary/IEC)" else "1000 (Decimal/SI)"
        val formattedResult = "${formatNumber(converted)} ${input.toUnit.symbol}"
        val summary = "${formatNumber(input.value)} ${input.fromUnit.symbol} = $formattedResult [Base $baseLabel]"

        return ToolResult.Success(
            data = DataStorageOutput(
                resultValue = converted,
                resultFormatted = formattedResult,
                allConversions = allMap,
                totalBits = totalBytes * 8.0,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun formatNumber(v: Double): String {
        return if (v == v.toLong().toDouble()) {
            v.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
        }
    }
}
