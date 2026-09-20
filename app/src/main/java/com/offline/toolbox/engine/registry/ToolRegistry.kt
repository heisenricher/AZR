package com.offline.toolbox.engine.registry

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.tools.color.ColorConverterTool
import com.offline.toolbox.tools.converter.AreaConverterTool
import com.offline.toolbox.tools.converter.DataStorageConverterTool
import com.offline.toolbox.tools.converter.LengthConverterTool
import com.offline.toolbox.tools.converter.RomanNumeralConverterTool
import com.offline.toolbox.tools.converter.SpeedConverterTool
import com.offline.toolbox.tools.converter.TemperatureConverterTool
import com.offline.toolbox.tools.converter.VolumeConverterTool
import com.offline.toolbox.tools.converter.WeightConverterTool
import com.offline.toolbox.tools.data.CsvFilterSortTool
import com.offline.toolbox.tools.data.CsvToJsonTool
import com.offline.toolbox.tools.data.JsonToCsvTool
import com.offline.toolbox.tools.datetime.CountdownCalculatorTool
import com.offline.toolbox.tools.datetime.DateAddSubtractTool
import com.offline.toolbox.tools.datetime.DateDifferenceTool
import com.offline.toolbox.tools.developer.Base32Tool
import com.offline.toolbox.tools.developer.Base64Tool
import com.offline.toolbox.tools.developer.HtmlEntityTool
import com.offline.toolbox.tools.developer.HtmlToMarkdownTool
import com.offline.toolbox.tools.developer.JsonFormatterTool
import com.offline.toolbox.tools.developer.JwtDecoderTool
import com.offline.toolbox.tools.developer.MarkdownPreviewTool
import com.offline.toolbox.tools.developer.NumberBaseConverterTool
import com.offline.toolbox.tools.developer.RegexTesterTool
import com.offline.toolbox.tools.developer.SqlFormatterTool
import com.offline.toolbox.tools.developer.UnixTimestampTool
import com.offline.toolbox.tools.developer.UrlEncoderTool
import com.offline.toolbox.tools.developer.XmlFormatterTool
import com.offline.toolbox.tools.documents.PdfGeneratorTool
import com.offline.toolbox.tools.file.FileChecksumTool
import com.offline.toolbox.tools.file.ZipArchiveTool
import com.offline.toolbox.tools.generator.QrPayloadBuilderTool
import com.offline.toolbox.tools.generator.RandomChoiceTool
import com.offline.toolbox.tools.generator.RandomNumberTool
import com.offline.toolbox.tools.math.AverageCalculatorTool
import com.offline.toolbox.tools.math.BmiCalculatorTool
import com.offline.toolbox.tools.math.CompoundInterestTool
import com.offline.toolbox.tools.math.DiscountCalculatorTool
import com.offline.toolbox.tools.math.FractionCalculatorTool
import com.offline.toolbox.tools.math.LoanEmiCalculatorTool
import com.offline.toolbox.tools.math.PercentageCalculatorTool
import com.offline.toolbox.tools.math.TipCalculatorTool
import com.offline.toolbox.tools.media.ExifInspectorTool
import com.offline.toolbox.tools.media.QrMatrixGeneratorTool
import com.offline.toolbox.tools.security.HashCheckerTool
import com.offline.toolbox.tools.security.HashGeneratorTool
import com.offline.toolbox.tools.security.PasswordGeneratorTool
import com.offline.toolbox.tools.security.PasswordStrengthTool
import com.offline.toolbox.tools.security.UuidGeneratorTool
import com.offline.toolbox.tools.text.AsciiArtBannerTool
import com.offline.toolbox.tools.text.FindAndReplaceTool
import com.offline.toolbox.tools.text.LineOperationsTool
import com.offline.toolbox.tools.text.LoremIpsumGeneratorTool
import com.offline.toolbox.tools.text.MorseCodeTool
import com.offline.toolbox.tools.text.Rot13CipherTool
import com.offline.toolbox.tools.text.SlugGeneratorTool
import com.offline.toolbox.tools.text.StringInspectorTool
import com.offline.toolbox.tools.text.TextCaseConverterTool
import com.offline.toolbox.tools.text.TextCleanerTool
import com.offline.toolbox.tools.text.TextDiffTool
import com.offline.toolbox.tools.text.WordCounterTool

/**
 * Central registry that indexes all available offline utilities.
 * Adding a new tool is fully decoupled: just implement [Tool] and add to this registry.
 */
object ToolRegistry {
    private val registeredTools = mutableMapOf<String, Tool<*, *>>()

    init {
        // Text & Writing
        register(TextCaseConverterTool())
        register(WordCounterTool())
        register(TextCleanerTool())
        register(LineOperationsTool())
        register(FindAndReplaceTool())
        register(LoremIpsumGeneratorTool())
        register(TextDiffTool())
        register(SlugGeneratorTool())
        register(StringInspectorTool())
        register(Rot13CipherTool())

        // Developer
        register(JsonFormatterTool())
        register(Base64Tool())
        register(JwtDecoderTool())
        register(UrlEncoderTool())
        register(HtmlEntityTool())
        register(UnixTimestampTool())
        register(NumberBaseConverterTool())
        register(RegexTesterTool())
        register(SqlFormatterTool())
        register(XmlFormatterTool())
        register(MarkdownPreviewTool())

        // Unit Converters
        register(LengthConverterTool())
        register(WeightConverterTool())
        register(TemperatureConverterTool())
        register(DataStorageConverterTool())
        register(SpeedConverterTool())
        register(RomanNumeralConverterTool())
        register(AreaConverterTool())
        register(VolumeConverterTool())

        // Math & Financial
        register(PercentageCalculatorTool())
        register(TipCalculatorTool())
        register(BmiCalculatorTool())
        register(AverageCalculatorTool())
        register(FractionCalculatorTool())
        register(CompoundInterestTool())
        register(DiscountCalculatorTool())

        // Date & Time
        register(DateDifferenceTool())
        register(DateAddSubtractTool())
        register(CountdownCalculatorTool())

        // Security & Privacy
        register(HashGeneratorTool())
        register(HashCheckerTool())
        register(PasswordGeneratorTool())
        register(PasswordStrengthTool())
        register(UuidGeneratorTool())

        // Color
        register(ColorConverterTool())

        // Data
        register(CsvToJsonTool())
        register(JsonToCsvTool())
        register(CsvFilterSortTool())

        // Generators
        register(RandomNumberTool())
        register(RandomChoiceTool())
        register(QrPayloadBuilderTool())

        // Developer additions
        register(Base32Tool())
        register(HtmlToMarkdownTool())

        // Math additions
        register(LoanEmiCalculatorTool())

        // Text additions
        register(MorseCodeTool())
        register(AsciiArtBannerTool())

        // Media & Forensic
        register(ExifInspectorTool())
        register(QrMatrixGeneratorTool())

        // File & Documents
        register(ZipArchiveTool())
        register(FileChecksumTool())
        register(PdfGeneratorTool())
    }

    fun register(tool: Tool<*, *>) {
        registeredTools[tool.metadata.id] = tool
    }

    fun getTool(id: String): Tool<*, *>? = registeredTools[id]

    fun getAllTools(): List<Tool<*, *>> = registeredTools.values.toList()

    fun getToolsByCategory(category: ToolCategory): List<Tool<*, *>> {
        return registeredTools.values.filter { it.metadata.category == category }
    }

    fun getToolsSupportingCapability(capability: ToolCapability): List<Tool<*, *>> {
        return registeredTools.values.filter { it.metadata.capabilities.contains(capability) }
    }

    /**
     * Find compatible downstream tools for the "Send To" chaining feature.
     */
    fun getCompatibleChainingTools(outputType: ToolDataType, currentToolId: String): List<Tool<*, *>> {
        if (outputType == ToolDataType.NONE) return emptyList()
        return registeredTools.values.filter {
            it.metadata.id != currentToolId &&
                (it.metadata.inputType == outputType ||
                    (outputType == ToolDataType.TEXT && it.metadata.inputType == ToolDataType.JSON) ||
                    (outputType == ToolDataType.JSON && it.metadata.inputType == ToolDataType.TEXT))
        }
    }
}
