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
import com.offline.toolbox.tools.color.ColorPaletteGeneratorTool
import com.offline.toolbox.tools.data.CsvStatsTool
import com.offline.toolbox.tools.data.YamlToJsonTool
import com.offline.toolbox.tools.developer.CronExpressionTool
import com.offline.toolbox.tools.developer.MacAddressTool
import com.offline.toolbox.tools.developer.PipelineChainingTool
import com.offline.toolbox.tools.developer.SubnetCalculatorTool
import com.offline.toolbox.tools.math.FuelCostCalculatorTool
import com.offline.toolbox.tools.math.GpaCalculatorTool
import com.offline.toolbox.tools.media.AspectRatioCalculatorTool
import com.offline.toolbox.tools.security.HmacGeneratorTool
import com.offline.toolbox.tools.security.RsaKeyPairTool
import com.offline.toolbox.tools.text.LeetspeakTool
import com.offline.toolbox.tools.text.TextBinaryHexTool
import com.offline.toolbox.tools.developer.IPv6SubnetCalculatorTool
import com.offline.toolbox.tools.developer.PortLookupTool
import com.offline.toolbox.tools.security.SymmetricCipherTool
import com.offline.toolbox.tools.security.BcryptWorkFactorTool
import com.offline.toolbox.tools.data.JsonDiffTool
import com.offline.toolbox.tools.data.NdjsonToJsonArrayTool
import com.offline.toolbox.tools.math.ScientificNotationTool
import com.offline.toolbox.tools.math.DenominationCalculatorTool
import com.offline.toolbox.tools.math.PrimeFactorizationTool
import com.offline.toolbox.tools.text.TextWrapTool
import com.offline.toolbox.tools.text.ZalgoTextTool
import com.offline.toolbox.tools.text.AnagramSolverTool
import com.offline.toolbox.tools.media.FrequencyToNoteTool
import com.offline.toolbox.tools.media.DpiDensityCalculatorTool
import com.offline.toolbox.tools.color.HtmlColorNameTool
import com.offline.toolbox.tools.developer.UserAgentParserTool
import com.offline.toolbox.tools.developer.SemVerComparatorTool
import com.offline.toolbox.tools.developer.ChmodPermissionsCalculatorTool
import com.offline.toolbox.tools.developer.HttpHeaderInspectorTool
import com.offline.toolbox.tools.security.TotpGeneratorTool
import com.offline.toolbox.tools.security.PassphraseDicewareTool
import com.offline.toolbox.tools.data.JsonPathEvaluatorTool
import com.offline.toolbox.tools.data.TsvToCsvTool
import com.offline.toolbox.tools.math.MatrixCalculatorTool
import com.offline.toolbox.tools.math.StatisticsDistributionTool
import com.offline.toolbox.tools.math.CompoundAnnualGrowthRateTool
import com.offline.toolbox.tools.text.TextCaseInspectorTool
import com.offline.toolbox.tools.text.NatoPhoneticTool
import com.offline.toolbox.tools.media.BpmTapperTool
import com.offline.toolbox.tools.color.ColorBlindnessSimulatorTool
import com.offline.toolbox.tools.developer.SubnetSupernetCalculatorTool
import com.offline.toolbox.tools.developer.DnsRecordParserTool
import com.offline.toolbox.tools.developer.DockerComposeValidatorTool
import com.offline.toolbox.tools.developer.SqlDialectConverterTool
import com.offline.toolbox.tools.security.ShamirSecretSharingTool
import com.offline.toolbox.tools.security.SteganographyTextTool
import com.offline.toolbox.tools.data.XmlToJsonConverterTool
import com.offline.toolbox.tools.data.HexDumpViewerTool
import com.offline.toolbox.tools.math.PolynomialRootSolverTool
import com.offline.toolbox.tools.math.VectorMathCalculatorTool
import com.offline.toolbox.tools.math.BmrTdeeCalculatorTool
import com.offline.toolbox.tools.text.MarkdownTableFormatterTool
import com.offline.toolbox.tools.text.StringSimilarityTool
import com.offline.toolbox.tools.color.WcagApcaContrastTool
import com.offline.toolbox.tools.media.AudioFrequencyIntervalTool
import com.offline.toolbox.tools.developer.CurlCommandParserTool
import com.offline.toolbox.tools.developer.BgpAsnLookupTool
import com.offline.toolbox.tools.developer.GitIgnoreGeneratorTool
import com.offline.toolbox.tools.developer.CrontabScheduleDiffTool
import com.offline.toolbox.tools.security.HkdfKeyDerivationTool
import com.offline.toolbox.tools.security.VigenereCipherTool
import com.offline.toolbox.tools.data.CsvToJsonSchemaTool
import com.offline.toolbox.tools.data.BinarySearchTreeVisualizerTool
import com.offline.toolbox.tools.math.LoanRefinanceComparatorTool
import com.offline.toolbox.tools.math.TrigonometricFunctionsTool
import com.offline.toolbox.tools.math.ScientificConstantsTool
import com.offline.toolbox.tools.text.ReadabilityScoreTool
import com.offline.toolbox.tools.text.TextJustifierTool
import com.offline.toolbox.tools.color.ColorTemperatureTool
import com.offline.toolbox.tools.media.AudioDecibelCalculatorTool

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

        // Phase 5 Additions (75 Tools Milestone)
        // Developer & Network
        register(SubnetCalculatorTool())
        register(MacAddressTool())
        register(CronExpressionTool())
        register(PipelineChainingTool())

        // Security & Cryptography
        register(HmacGeneratorTool())
        register(RsaKeyPairTool())

        // Data & Formats
        register(YamlToJsonTool())
        register(CsvStatsTool())

        // Media, Visual & Color
        register(ColorPaletteGeneratorTool())
        register(AspectRatioCalculatorTool())

        // Math & Finance
        register(FuelCostCalculatorTool())
        register(GpaCalculatorTool())

        // Text & Encoding
        register(TextBinaryHexTool())
        register(LeetspeakTool())

        // Phase 6 Additions (90 Tools Milestone)
        // Developer & Network
        register(IPv6SubnetCalculatorTool())
        register(PortLookupTool())

        // Security & Cryptography
        register(SymmetricCipherTool())
        register(BcryptWorkFactorTool())

        // Data & Formats
        register(JsonDiffTool())
        register(NdjsonToJsonArrayTool())

        // Math, Science & Finance
        register(ScientificNotationTool())
        register(DenominationCalculatorTool())
        register(PrimeFactorizationTool())

        // Text & Writing
        register(TextWrapTool())
        register(ZalgoTextTool())
        register(AnagramSolverTool())

        // Media, Color & Acoustics
        register(FrequencyToNoteTool())
        register(DpiDensityCalculatorTool())
        register(HtmlColorNameTool())

        // Phase 7 Additions (105 Tools Milestone - The Grand Century)
        // Developer & System Administration
        register(UserAgentParserTool())
        register(SemVerComparatorTool())
        register(ChmodPermissionsCalculatorTool())
        register(HttpHeaderInspectorTool())

        // Security & Privacy
        register(TotpGeneratorTool())
        register(PassphraseDicewareTool())

        // Data & Formats
        register(JsonPathEvaluatorTool())
        register(TsvToCsvTool())

        // Math, Science & Finance
        register(MatrixCalculatorTool())
        register(StatisticsDistributionTool())
        register(CompoundAnnualGrowthRateTool())

        // Text & Linguistics
        register(TextCaseInspectorTool())
        register(NatoPhoneticTool())

        // Media, Audio & Color
        register(BpmTapperTool())
        register(ColorBlindnessSimulatorTool())

        // --- Phase 8: The Sovereign Suite (120 Tools Milestone) ---
        // Developer & Networking
        register(SubnetSupernetCalculatorTool())
        register(DnsRecordParserTool())
        register(DockerComposeValidatorTool())
        register(SqlDialectConverterTool())

        // Security & Cryptography
        register(ShamirSecretSharingTool())
        register(SteganographyTextTool())

        // Data & Formats
        register(XmlToJsonConverterTool())
        register(HexDumpViewerTool())

        // Math, Science & Health
        register(PolynomialRootSolverTool())
        register(VectorMathCalculatorTool())
        register(BmrTdeeCalculatorTool())

        // Text & Typography
        register(MarkdownTableFormatterTool())
        register(StringSimilarityTool())

        // Media, Color & Acoustics
        register(WcagApcaContrastTool())
        register(AudioFrequencyIntervalTool())

        // --- Phase 9: The Apex Zenith (135 Tools Milestone) ---
        // Developer & Cloud/DevOps
        register(CurlCommandParserTool())
        register(BgpAsnLookupTool())
        register(GitIgnoreGeneratorTool())
        register(CrontabScheduleDiffTool())

        // Security & Cryptography
        register(HkdfKeyDerivationTool())
        register(VigenereCipherTool())

        // Data & Formats
        register(CsvToJsonSchemaTool())
        register(BinarySearchTreeVisualizerTool())

        // Math, Finance & Science
        register(LoanRefinanceComparatorTool())
        register(TrigonometricFunctionsTool())
        register(ScientificConstantsTool())

        // Text & Typography
        register(ReadabilityScoreTool())
        register(TextJustifierTool())

        // Media, Color & Acoustics
        register(ColorTemperatureTool())
        register(AudioDecibelCalculatorTool())
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
