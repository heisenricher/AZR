package com.offline.toolbox.features.toolrunner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offline.toolbox.core.database.FavoriteDao
import com.offline.toolbox.core.database.FavoriteToolEntity
import com.offline.toolbox.core.database.RecentDao
import com.offline.toolbox.core.database.RecentToolEntity
import com.offline.toolbox.core.designsystem.components.ToolInputField
import com.offline.toolbox.core.designsystem.components.ToolOutputField
import com.offline.toolbox.core.designsystem.components.ToolScaffold
import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.engine.registry.ToolRegistry
import com.offline.toolbox.tools.color.ColorConverterTool
import com.offline.toolbox.tools.color.ColorOutput
import com.offline.toolbox.tools.datetime.DateDifferenceInput
import com.offline.toolbox.tools.datetime.DateDifferenceOutput
import com.offline.toolbox.tools.datetime.DateDifferenceTool
import com.offline.toolbox.tools.developer.Base64Input
import com.offline.toolbox.tools.developer.Base64Mode
import com.offline.toolbox.tools.developer.Base64Tool
import com.offline.toolbox.tools.developer.JsonFormatterInput
import com.offline.toolbox.tools.developer.JsonFormatterOutput
import com.offline.toolbox.tools.developer.JsonFormatterTool
import com.offline.toolbox.tools.developer.JsonOperation
import com.offline.toolbox.tools.math.PercentageCalculationType
import com.offline.toolbox.tools.math.PercentageCalculatorTool
import com.offline.toolbox.tools.math.PercentageInput
import com.offline.toolbox.tools.math.PercentageOutput
import com.offline.toolbox.tools.security.HashAlgorithm
import com.offline.toolbox.tools.security.HashGeneratorTool
import com.offline.toolbox.tools.security.HashInput
import com.offline.toolbox.tools.security.HashOutput
import com.offline.toolbox.tools.security.PasswordConfig
import com.offline.toolbox.tools.security.PasswordGeneratorTool
import com.offline.toolbox.tools.security.PasswordMode
import com.offline.toolbox.tools.security.PasswordOutput
import com.offline.toolbox.tools.text.CaseType
import com.offline.toolbox.tools.text.TextCaseConverterTool
import com.offline.toolbox.tools.text.TextCaseInput
import com.offline.toolbox.tools.text.WordCountStats
import com.offline.toolbox.tools.text.WordCounterTool
import com.offline.toolbox.tools.converter.DataStorageConverterTool
import com.offline.toolbox.tools.converter.LengthConverterTool
import com.offline.toolbox.tools.converter.RomanNumeralConverterTool
import com.offline.toolbox.tools.converter.SpeedConverterTool
import com.offline.toolbox.tools.converter.TemperatureConverterTool
import com.offline.toolbox.tools.converter.WeightConverterTool
import com.offline.toolbox.tools.data.CsvToJsonTool
import com.offline.toolbox.tools.data.JsonToCsvTool
import com.offline.toolbox.tools.datetime.CountdownCalculatorTool
import com.offline.toolbox.tools.datetime.DateAddSubtractTool
import com.offline.toolbox.tools.developer.HtmlEntityTool
import com.offline.toolbox.tools.developer.JwtDecoderTool
import com.offline.toolbox.tools.developer.NumberBaseConverterTool
import com.offline.toolbox.tools.developer.RegexTesterTool
import com.offline.toolbox.tools.developer.UnixTimestampTool
import com.offline.toolbox.tools.developer.UrlEncoderTool
import com.offline.toolbox.tools.generator.RandomChoiceTool
import com.offline.toolbox.tools.generator.RandomNumberTool
import com.offline.toolbox.tools.math.AverageCalculatorTool
import com.offline.toolbox.tools.math.BmiCalculatorTool
import com.offline.toolbox.tools.math.FractionCalculatorTool
import com.offline.toolbox.tools.math.TipCalculatorTool
import com.offline.toolbox.tools.security.HashCheckerTool
import com.offline.toolbox.tools.security.PasswordStrengthTool
import com.offline.toolbox.tools.security.UuidGeneratorTool
import com.offline.toolbox.tools.text.FindAndReplaceTool
import com.offline.toolbox.tools.text.LineOperationsTool
import com.offline.toolbox.tools.text.LoremIpsumGeneratorTool
import com.offline.toolbox.tools.text.TextCleanerTool
import com.offline.toolbox.tools.text.TextDiffTool
import com.offline.toolbox.tools.converter.AreaConverterTool
import com.offline.toolbox.tools.converter.VolumeConverterTool
import com.offline.toolbox.tools.data.CsvFilterSortTool
import com.offline.toolbox.tools.developer.Base32Tool
import com.offline.toolbox.tools.developer.HtmlToMarkdownTool
import com.offline.toolbox.tools.developer.MarkdownPreviewTool
import com.offline.toolbox.tools.developer.SqlFormatterTool
import com.offline.toolbox.tools.developer.XmlFormatterTool
import com.offline.toolbox.tools.documents.PdfGeneratorTool
import com.offline.toolbox.tools.file.FileChecksumTool
import com.offline.toolbox.tools.file.ZipArchiveTool
import com.offline.toolbox.tools.generator.QrPayloadBuilderTool
import com.offline.toolbox.tools.math.CompoundInterestTool
import com.offline.toolbox.tools.math.DiscountCalculatorTool
import com.offline.toolbox.tools.math.LoanEmiCalculatorTool
import com.offline.toolbox.tools.media.ExifInspectorTool
import com.offline.toolbox.tools.media.QrMatrixGeneratorTool
import com.offline.toolbox.tools.text.AsciiArtBannerTool
import com.offline.toolbox.tools.text.MorseCodeTool
import com.offline.toolbox.tools.text.Rot13CipherTool
import com.offline.toolbox.tools.text.SlugGeneratorTool
import com.offline.toolbox.tools.text.StringInspectorTool
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
import com.offline.toolbox.tools.color.HtmlColorNameTool
import com.offline.toolbox.tools.data.JsonDiffTool
import com.offline.toolbox.tools.data.NdjsonToJsonArrayTool
import com.offline.toolbox.tools.developer.IPv6SubnetCalculatorTool
import com.offline.toolbox.tools.developer.PortLookupTool
import com.offline.toolbox.tools.math.DenominationCalculatorTool
import com.offline.toolbox.tools.math.PrimeFactorizationTool
import com.offline.toolbox.tools.math.ScientificNotationTool
import com.offline.toolbox.tools.media.DpiDensityCalculatorTool
import com.offline.toolbox.tools.media.FrequencyToNoteTool
import com.offline.toolbox.tools.security.BcryptWorkFactorTool
import com.offline.toolbox.tools.security.SymmetricCipherTool
import com.offline.toolbox.tools.text.AnagramSolverTool
import com.offline.toolbox.tools.text.TextWrapTool
import com.offline.toolbox.tools.text.ZalgoTextTool
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
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolRunnerScreen(
    toolId: String,
    initialPayload: String? = null,
    favoriteDao: FavoriteDao,
    recentDao: RecentDao,
    onBackClick: () -> Unit,
    onNavigateToTool: (targetToolId: String, payload: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tool = remember(toolId) { ToolRegistry.getTool(toolId) }

    if (tool == null) {
        Column(modifier = modifier.padding(16.dp)) {
            Text("Tool not found: $toolId", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onBackClick) { Text("Go Back") }
        }
        return
    }

    val isFavorite by favoriteDao.isFavorite(toolId).collectAsState(initial = false)

    // Record usage when entering
    LaunchedEffect(toolId) {
        recentDao.recordUsage(
            RecentToolEntity(
                toolId = toolId,
                lastUsedTimestamp = System.currentTimeMillis()
            )
        )
    }

    // Common Output & Error state
    var outputText by remember { mutableStateOf("") }
    var executionSummary by remember { mutableStateOf<String?>(null) }
    var executionTimeMs by remember { mutableStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorGuidance by remember { mutableStateOf<String?>(null) }
    var showSendToDialog by remember { mutableStateOf(false) }

    val compatibleDownstreamTools = remember(tool) {
        ToolRegistry.getCompatibleChainingTools(tool.metadata.outputType, tool.metadata.id)
    }

    ToolScaffold(
        title = tool.metadata.name,
        categoryName = tool.metadata.category.title,
        isFavorite = isFavorite,
        onBackClick = onBackClick,
        onToggleFavorite = {
            coroutineScope.launch {
                if (isFavorite) favoriteDao.removeFavorite(toolId)
                else favoriteDao.insertFavorite(FavoriteToolEntity(toolId))
            }
        },
        errorMessage = errorMessage,
        errorGuidance = errorGuidance,
        modifier = modifier
    ) {
        when (tool) {
            is TextCaseConverterTool -> {
                TextCaseConverterUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is WordCounterTool -> {
                WordCounterUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { text, summary, time ->
                        outputText = text
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is JsonFormatterTool -> {
                JsonFormatterUI(
                    tool = tool,
                    initialJson = initialPayload ?: "{\n  \"message\": \"Hello, Offline Toolbox!\",\n  \"version\": 1.0\n}",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is Base64Tool -> {
                Base64UI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is HashGeneratorTool -> {
                HashGeneratorUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is PasswordGeneratorTool -> {
                PasswordGeneratorUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is PercentageCalculatorTool -> {
                PercentageCalculatorUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is DateDifferenceTool -> {
                DateDifferenceUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is ColorConverterTool -> {
                ColorConverterUI(
                    tool = tool,
                    initialColor = initialPayload ?: "#3B82F6",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is TextCleanerTool -> {
                TextCleanerUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is LineOperationsTool -> {
                LineOperationsUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is FindAndReplaceTool -> {
                FindAndReplaceUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is LoremIpsumGeneratorTool -> {
                LoremIpsumUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is TextDiffTool -> {
                TextDiffUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is JwtDecoderTool -> {
                JwtDecoderUI(
                    tool = tool,
                    initialToken = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is UrlEncoderTool -> {
                UrlEncoderUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is HtmlEntityTool -> {
                HtmlEntityUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is UnixTimestampTool -> {
                UnixTimestampUI(
                    tool = tool,
                    initialTimestamp = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is NumberBaseConverterTool -> {
                NumberBaseConverterUI(
                    tool = tool,
                    initialValue = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is RegexTesterTool -> {
                RegexTesterUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is LengthConverterTool -> {
                LengthConverterUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is WeightConverterTool -> {
                WeightConverterUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is TemperatureConverterTool -> {
                TemperatureConverterUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is DataStorageConverterTool -> {
                DataStorageConverterUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is SpeedConverterTool -> {
                SpeedConverterUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is RomanNumeralConverterTool -> {
                RomanNumeralUI(
                    tool = tool,
                    initialValue = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is TipCalculatorTool -> {
                TipCalculatorUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is BmiCalculatorTool -> {
                BmiCalculatorUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is AverageCalculatorTool -> {
                AverageCalculatorUI(
                    tool = tool,
                    initialNumbers = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is FractionCalculatorTool -> {
                FractionCalculatorUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is DateAddSubtractTool -> {
                DateAddSubtractUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is CountdownCalculatorTool -> {
                CountdownCalculatorUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is HashCheckerTool -> {
                HashCheckerUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is PasswordStrengthTool -> {
                PasswordStrengthUI(
                    tool = tool,
                    initialPassword = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is UuidGeneratorTool -> {
                UuidGeneratorUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is CsvToJsonTool -> {
                CsvToJsonUI(
                    tool = tool,
                    initialCsv = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is JsonToCsvTool -> {
                JsonToCsvUI(
                    tool = tool,
                    initialJson = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is RandomNumberTool -> {
                RandomNumberUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is RandomChoiceTool -> {
                RandomChoiceUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is SlugGeneratorTool -> {
                SlugGeneratorUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is StringInspectorTool -> {
                StringInspectorUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is Rot13CipherTool -> {
                Rot13CipherUI(
                    tool = tool,
                    initialText = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is SqlFormatterTool -> {
                SqlFormatterUI(
                    tool = tool,
                    initialSql = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is XmlFormatterTool -> {
                XmlFormatterUI(
                    tool = tool,
                    initialXml = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is MarkdownPreviewTool -> {
                MarkdownPreviewUI(
                    tool = tool,
                    initialMd = initialPayload ?: "",
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is CompoundInterestTool -> {
                CompoundInterestUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    },
                    onError = { msg, guidance ->
                        errorMessage = msg
                        errorGuidance = guidance
                    }
                )
            }
            is DiscountCalculatorTool -> {
                DiscountCalculatorUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is AreaConverterTool -> {
                AreaConverterUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is VolumeConverterTool -> {
                VolumeConverterUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is QrPayloadBuilderTool -> {
                QrPayloadBuilderUI(
                    tool = tool,
                    onResult = { res, summary, time ->
                        outputText = res
                        executionSummary = summary
                        executionTimeMs = time
                        errorMessage = null
                        errorGuidance = null
                    }
                )
            }
            is ZipArchiveTool -> {
                ZipArchiveToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "ZIP operation completed"
                        errorMessage = null
                    }
                )
            }
            is FileChecksumTool -> {
                FileChecksumToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Checksum computed"
                        errorMessage = null
                    }
                )
            }
            is ExifInspectorTool -> {
                ExifInspectorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "EXIF audit completed"
                        errorMessage = null
                    }
                )
            }
            is PdfGeneratorTool -> {
                PdfGeneratorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "PDF document generated"
                        errorMessage = null
                    }
                )
            }
            is QrMatrixGeneratorTool -> {
                QrMatrixGeneratorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "QR matrix generated"
                        errorMessage = null
                    }
                )
            }
            is LoanEmiCalculatorTool -> {
                LoanEmiCalculatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "EMI calculated"
                        errorMessage = null
                    }
                )
            }
            is MorseCodeTool -> {
                MorseCodeToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Morse translation completed"
                        errorMessage = null
                    }
                )
            }
            is Base32Tool -> {
                Base32ToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Base32 operation completed"
                        errorMessage = null
                    }
                )
            }
            is HtmlToMarkdownTool -> {
                HtmlToMarkdownToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Converted to Markdown"
                        errorMessage = null
                    }
                )
            }
            is AsciiArtBannerTool -> {
                AsciiArtBannerToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "ASCII banner generated"
                        errorMessage = null
                    }
                )
            }
            is CsvFilterSortTool -> {
                CsvFilterSortToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "CSV filtered & sorted"
                        errorMessage = null
                    }
                )
            }
            is SubnetCalculatorTool -> {
                SubnetCalculatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Subnet calculation completed"
                        errorMessage = null
                    }
                )
            }
            is MacAddressTool -> {
                MacAddressToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "MAC address parsed"
                        errorMessage = null
                    }
                )
            }
            is CronExpressionTool -> {
                CronExpressionToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Cron expression translated"
                        errorMessage = null
                    }
                )
            }
            is HmacGeneratorTool -> {
                HmacGeneratorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "HMAC generated"
                        errorMessage = null
                    }
                )
            }
            is RsaKeyPairTool -> {
                RsaKeyPairToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "RSA key pair generated"
                        errorMessage = null
                    }
                )
            }
            is YamlToJsonTool -> {
                YamlToJsonToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "YAML/JSON transformed"
                        errorMessage = null
                    }
                )
            }
            is CsvStatsTool -> {
                CsvStatsToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "CSV statistical profile generated"
                        errorMessage = null
                    }
                )
            }
            is ColorPaletteGeneratorTool -> {
                ColorPaletteGeneratorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Harmonious palette generated"
                        errorMessage = null
                    }
                )
            }
            is AspectRatioCalculatorTool -> {
                AspectRatioToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Aspect ratio computed"
                        errorMessage = null
                    }
                )
            }
            is FuelCostCalculatorTool -> {
                FuelCostToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Fuel cost analyzed"
                        errorMessage = null
                    }
                )
            }
            is GpaCalculatorTool -> {
                GpaCalculatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "GPA evaluated"
                        errorMessage = null
                    }
                )
            }
            is TextBinaryHexTool -> {
                TextBinaryHexToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Binary/Hex stream converted"
                        errorMessage = null
                    }
                )
            }
            is LeetspeakTool -> {
                LeetspeakToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Leetspeak transformed"
                        errorMessage = null
                    }
                )
            }
            is PipelineChainingTool -> {
                PipelineChainingToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Pipeline executed"
                        errorMessage = null
                    }
                )
            }
            is IPv6SubnetCalculatorTool -> {
                IPv6SubnetCalculatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "IPv6 subnet analyzed"
                        errorMessage = null
                    }
                )
            }
            is PortLookupTool -> {
                PortLookupToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Port directory queried"
                        errorMessage = null
                    }
                )
            }
            is SymmetricCipherTool -> {
                SymmetricCipherToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Cipher payload processed"
                        errorMessage = null
                    }
                )
            }
            is BcryptWorkFactorTool -> {
                BcryptWorkFactorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Bcrypt work factor audited"
                        errorMessage = null
                    }
                )
            }
            is JsonDiffTool -> {
                JsonDiffToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "JSON delta computed"
                        errorMessage = null
                    }
                )
            }
            is NdjsonToJsonArrayTool -> {
                NdjsonToJsonArrayToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "NDJSON / Array converted"
                        errorMessage = null
                    }
                )
            }
            is ScientificNotationTool -> {
                ScientificNotationToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Scientific notation converted"
                        errorMessage = null
                    }
                )
            }
            is DenominationCalculatorTool -> {
                DenominationCalculatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Denominations calculated"
                        errorMessage = null
                    }
                )
            }
            is PrimeFactorizationTool -> {
                PrimeFactorizationToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Prime factorization completed"
                        errorMessage = null
                    }
                )
            }
            is TextWrapTool -> {
                TextWrapToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Text wrapped & formatted"
                        errorMessage = null
                    }
                )
            }
            is ZalgoTextTool -> {
                ZalgoTextToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Zalgo effect applied"
                        errorMessage = null
                    }
                )
            }
            is AnagramSolverTool -> {
                AnagramSolverToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Anagram audit completed"
                        errorMessage = null
                    }
                )
            }
            is FrequencyToNoteTool -> {
                FrequencyToNoteToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Acoustic note tuned"
                        errorMessage = null
                    }
                )
            }
            is DpiDensityCalculatorTool -> {
                DpiDensityCalculatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "DPI density calculated"
                        errorMessage = null
                    }
                )
            }
            is HtmlColorNameTool -> {
                HtmlColorNameToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Color matched"
                        errorMessage = null
                    }
                )
            }
            is UserAgentParserTool -> {
                UserAgentParserToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "User-Agent analyzed"
                        errorMessage = null
                    }
                )
            }
            is SemVerComparatorTool -> {
                SemVerComparatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "SemVer evaluated"
                        errorMessage = null
                    }
                )
            }
            is ChmodPermissionsCalculatorTool -> {
                ChmodPermissionsCalculatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Permissions calculated"
                        errorMessage = null
                    }
                )
            }
            is HttpHeaderInspectorTool -> {
                HttpHeaderInspectorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "HTTP headers audited"
                        errorMessage = null
                    }
                )
            }
            is TotpGeneratorTool -> {
                TotpGeneratorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "2FA TOTP generated"
                        errorMessage = null
                    }
                )
            }
            is PassphraseDicewareTool -> {
                PassphraseDicewareToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Diceware passphrase generated"
                        errorMessage = null
                    }
                )
            }
            is JsonPathEvaluatorTool -> {
                JsonPathEvaluatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "JSONPath query evaluated"
                        errorMessage = null
                    }
                )
            }
            is TsvToCsvTool -> {
                TsvToCsvToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "TSV/CSV converted"
                        errorMessage = null
                    }
                )
            }
            is MatrixCalculatorTool -> {
                MatrixCalculatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Matrix calculation complete"
                        errorMessage = null
                    }
                )
            }
            is StatisticsDistributionTool -> {
                StatisticsDistributionToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Normal distribution evaluated"
                        errorMessage = null
                    }
                )
            }
            is CompoundAnnualGrowthRateTool -> {
                CompoundAnnualGrowthRateToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "CAGR calculated"
                        errorMessage = null
                    }
                )
            }
            is TextCaseInspectorTool -> {
                TextCaseInspectorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Case style analyzed"
                        errorMessage = null
                    }
                )
            }
            is NatoPhoneticTool -> {
                NatoPhoneticToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "NATO phonetic translated"
                        errorMessage = null
                    }
                )
            }
            is BpmTapperTool -> {
                BpmTapperToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Tempo evaluated"
                        errorMessage = null
                    }
                )
            }
            is ColorBlindnessSimulatorTool -> {
                ColorBlindnessSimulatorToolUI(
                    tool = tool,
                    onOutputChange = { out ->
                        outputText = out
                        executionSummary = "Color blindness simulated"
                        errorMessage = null
                    }
                )
            }
            else -> {
                Text("Interface not available for this tool.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ToolOutputField(
            value = outputText,
            summary = executionSummary,
            executionTimeMs = executionTimeMs,
            isMonospace = true,
            onSendToClick = if (compatibleDownstreamTools.isNotEmpty() && outputText.isNotEmpty()) {
                { showSendToDialog = true }
            } else null
        )

        // Send To Chaining Dialog
        if (showSendToDialog) {
            AlertDialog(
                onDismissRequest = { showSendToDialog = false },
                title = { Text("Send Output To...") },
                text = {
                    LazyColumn {
                        items(compatibleDownstreamTools) { target ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        showSendToDialog = false
                                        onNavigateToTool(target.metadata.id, outputText)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = target.metadata.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = target.metadata.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSendToDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// Specialized Tool Sub-UIs
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextCaseConverterUI(
    tool: TextCaseConverterTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedCase by remember { mutableStateOf(CaseType.UPPERCASE) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runTransform(c: CaseType) {
        scope.launch {
            when (val res = tool.execute(TextCaseInput(text, c))) {
                is ToolResult.Success -> onResult(res.data, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    ToolInputField(
        value = text,
        onValueChange = {
            text = it
            runTransform(selectedCase)
        },
        placeholder = "Enter or paste text to convert case..."
    )

    Spacer(modifier = Modifier.height(12.dp))

    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
    ) {
        OutlinedTextField(
            value = selectedCase.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Target Case Style") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false }
        ) {
            CaseType.values().forEach { caseType ->
                DropdownMenuItem(
                    text = { Text(caseType.displayName) },
                    onClick = {
                        selectedCase = caseType
                        dropdownExpanded = false
                        runTransform(caseType)
                    }
                )
            }
        }
    }
}

@Composable
private fun WordCounterUI(
    tool: WordCounterTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(text) {
        when (val res = tool.execute(text)) {
            is ToolResult.Success -> {
                val stats = res.data
                val report = buildString {
                    appendLine("Words:                  ${stats.wordCount}")
                    appendLine("Characters (all):       ${stats.charCountWithSpaces}")
                    appendLine("Characters (no space):  ${stats.charCountWithoutSpaces}")
                    appendLine("Lines:                  ${stats.lineCount} (${stats.nonEmptyLineCount} non-empty)")
                    appendLine("Sentences:              ${stats.sentenceCount}")
                    appendLine("Paragraphs:             ${stats.paragraphCount}")
                    appendLine("Average Word Length:    ${stats.averageWordLength} letters")
                    if (stats.longestWord.isNotEmpty()) {
                        appendLine("Longest Word:           \"${stats.longestWord}\"")
                    }
                    appendLine("Est. Reading Time:      ${stats.readingTimeSeconds / 60}m ${stats.readingTimeSeconds % 60}s (at 200 wpm)")
                    appendLine("Est. Speaking Time:     ${stats.speakingTimeSeconds / 60}m ${stats.speakingTimeSeconds % 60}s (at 130 wpm)")
                }
                onResult(report, res.summary, res.executionTimeMs)
            }
            is ToolResult.Failure -> {}
        }
    }

    ToolInputField(
        value = text,
        onValueChange = { text = it },
        placeholder = "Type or paste text to inspect real-time metrics..."
    )
}

@Composable
private fun JsonFormatterUI(
    tool: JsonFormatterTool,
    initialJson: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var jsonText by remember { mutableStateOf(initialJson) }
    val scope = rememberCoroutineScope()

    fun runJson(op: JsonOperation) {
        scope.launch {
            when (val res = tool.execute(JsonFormatterInput(jsonText, op))) {
                is ToolResult.Success -> onResult(res.data.processedText, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    ToolInputField(
        value = jsonText,
        onValueChange = { jsonText = it },
        placeholder = "Paste JSON object or array here...",
        minLines = 6
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { runJson(JsonOperation.FORMAT_2_SPACES) },
            modifier = Modifier.weight(1f)
        ) {
            Text("Format (2 sp)")
        }
        OutlinedButton(
            onClick = { runJson(JsonOperation.MINIFY) },
            modifier = Modifier.weight(1f)
        ) {
            Text("Minify")
        }
        OutlinedButton(
            onClick = { runJson(JsonOperation.VALIDATE_ONLY) },
            modifier = Modifier.weight(1f)
        ) {
            Text("Validate")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Base64UI(
    tool: Base64Tool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedMode by remember { mutableStateOf(Base64Mode.ENCODE_STANDARD) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runBase64(m: Base64Mode) {
        scope.launch {
            when (val res = tool.execute(Base64Input(text, m))) {
                is ToolResult.Success -> onResult(res.data, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    ToolInputField(
        value = text,
        onValueChange = {
            text = it
            runBase64(selectedMode)
        },
        placeholder = "Enter text to encode or Base64 string to decode..."
    )

    Spacer(modifier = Modifier.height(10.dp))

    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
    ) {
        OutlinedTextField(
            value = selectedMode.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Base64 Operation Mode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false }
        ) {
            Base64Mode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = {
                        selectedMode = mode
                        dropdownExpanded = false
                        runBase64(mode)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HashGeneratorUI(
    tool: HashGeneratorTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedAlgorithm by remember { mutableStateOf(HashAlgorithm.SHA_256) }
    var uppercase by remember { mutableStateOf(false) }
    var salt by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runHash() {
        scope.launch {
            when (val res = tool.execute(HashInput(text, selectedAlgorithm, uppercase, salt))) {
                is ToolResult.Success -> {
                    val out = "${res.data.algorithm} (${res.data.bitLength}-bit):\n${res.data.hash}"
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    ToolInputField(
        value = text,
        onValueChange = {
            text = it
            runHash()
        },
        placeholder = "Enter string to calculate cryptographic hash..."
    )

    Spacer(modifier = Modifier.height(10.dp))

    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
    ) {
        OutlinedTextField(
            value = "${selectedAlgorithm.algorithmName} (${selectedAlgorithm.bitLength}-bit)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Hash Algorithm") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false }
        ) {
            HashAlgorithm.values().forEach { algo ->
                DropdownMenuItem(
                    text = { Text("${algo.algorithmName} (${algo.bitLength}-bit)") },
                    onClick = {
                        selectedAlgorithm = algo
                        dropdownExpanded = false
                        runHash()
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = salt,
        onValueChange = {
            salt = it
            runHash()
        },
        label = { Text("Optional Salt") },
        placeholder = { Text("Appended salt bytes...") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Uppercase Hexadecimal Output", style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = uppercase,
            onCheckedChange = {
                uppercase = it
                runHash()
            }
        )
    }
}

@Composable
private fun PasswordGeneratorUI(
    tool: PasswordGeneratorTool,
    onResult: (String, String?, Long) -> Unit
) {
    var length by remember { mutableIntStateOf(16) }
    var includeUpper by remember { mutableStateOf(true) }
    var includeLower by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(false) }
    var isPassphraseMode by remember { mutableStateOf(false) }
    var wordCount by remember { mutableIntStateOf(4) }
    val scope = rememberCoroutineScope()

    fun generate() {
        scope.launch {
            val config = if (!isPassphraseMode) {
                PasswordConfig(
                    mode = PasswordMode.RANDOM_CHARS,
                    length = length,
                    includeUppercase = includeUpper,
                    includeLowercase = includeLower,
                    includeNumbers = includeNumbers,
                    includeSymbols = includeSymbols,
                    excludeAmbiguous = excludeAmbiguous
                )
            } else {
                PasswordConfig(
                    mode = PasswordMode.MEMORABLE_PASSPHRASE,
                    passphraseWords = wordCount
                )
            }
            when (val res = tool.execute(config)) {
                is ToolResult.Success -> {
                    val report = buildString {
                        appendLine(res.data.password)
                        appendLine()
                        appendLine("Entropy: ${res.data.entropyBits} bits (${res.data.strengthRating})")
                        appendLine("Pool Size: ${res.data.characterPoolSize} characters")
                    }
                    onResult(report, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        generate()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Passphrase Mode (Diceware)", style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = isPassphraseMode,
            onCheckedChange = {
                isPassphraseMode = it
                generate()
            }
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (!isPassphraseMode) {
        Text("Length: $length characters", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = length.toFloat(),
            onValueChange = { length = it.toInt() },
            onValueChangeFinished = { generate() },
            valueRange = 8f..64f,
            steps = 55
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Uppercase (A-Z)")
            Switch(checked = includeUpper, onCheckedChange = { includeUpper = it; generate() })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Lowercase (a-z)")
            Switch(checked = includeLower, onCheckedChange = { includeLower = it; generate() })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Numbers (0-9)")
            Switch(checked = includeNumbers, onCheckedChange = { includeNumbers = it; generate() })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Symbols (!@#$)")
            Switch(checked = includeSymbols, onCheckedChange = { includeSymbols = it; generate() })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Exclude Ambiguous (0, O, 1, l)")
            Switch(checked = excludeAmbiguous, onCheckedChange = { excludeAmbiguous = it; generate() })
        }
    } else {
        Text("Word Count: $wordCount words", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = wordCount.toFloat(),
            onValueChange = { wordCount = it.toInt() },
            onValueChangeFinished = { generate() },
            valueRange = 3f..8f,
            steps = 4
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Button(
        onClick = { generate() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Generate New Password")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PercentageCalculatorUI(
    tool: PercentageCalculatorTool,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var calcType by remember { mutableStateOf(PercentageCalculationType.PERCENT_OF) }
    var xStr by remember { mutableStateOf("15") }
    var yStr by remember { mutableStateOf("80") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runCalc() {
        val x = xStr.toDoubleOrNull() ?: 0.0
        val y = yStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(PercentageInput(calcType, x, y))) {
                is ToolResult.Success -> onResult(res.data.explanation, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runCalc() }

    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
    ) {
        OutlinedTextField(
            value = calcType.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Calculation Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false }
        ) {
            PercentageCalculationType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        calcType = type
                        dropdownExpanded = false
                        runCalc()
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = xStr,
            onValueChange = { xStr = it; runCalc() },
            label = { Text("Value X") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = yStr,
            onValueChange = { yStr = it; runCalc() },
            label = { Text("Value Y") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateDifferenceUI(
    tool: DateDifferenceTool,
    onResult: (String, String?, Long) -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().minusMonths(6)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    val scope = rememberCoroutineScope()

    fun runDate() {
        scope.launch {
            when (val res = tool.execute(DateDifferenceInput(startDate, endDate))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Summary:         ${d.formattedSummary}")
                        appendLine()
                        appendLine("Total Days:      ${d.totalDays}")
                        appendLine("Total Weeks:     ${d.totalWeeks} weeks + ${d.remainingDaysInWeek} days")
                        appendLine("Business Days:   ${d.businessDays} (Mon - Fri)")
                        appendLine("Weekend Days:    ${d.weekendDays} (Sat - Sun)")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runDate() }

    Column {
        Text("Start Date: $startDate", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { startDate = startDate.minusMonths(1); runDate() }) { Text("-1 Month") }
            OutlinedButton(onClick = { startDate = startDate.plusMonths(1); runDate() }) { Text("+1 Month") }
            OutlinedButton(onClick = { startDate = LocalDate.now(); runDate() }) { Text("Today") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("End Date: $endDate", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { endDate = endDate.minusMonths(1); runDate() }) { Text("-1 Month") }
            OutlinedButton(onClick = { endDate = endDate.plusMonths(1); runDate() }) { Text("+1 Month") }
            OutlinedButton(onClick = { endDate = LocalDate.now(); runDate() }) { Text("Today") }
        }
    }
}

@Composable
private fun ColorConverterUI(
    tool: ColorConverterTool,
    initialColor: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var hexText by remember { mutableStateOf(initialColor) }
    val scope = rememberCoroutineScope()

    fun runColor(c: String) {
        scope.launch {
            when (val res = tool.execute(c)) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("HEX (6-char):      ${d.hex6}")
                        appendLine("HEX (8-char):      ${d.hex8}")
                        appendLine("RGB:               ${d.rgbString}")
                        appendLine("HSL:               ${d.hslString}")
                        appendLine("CMYK:              ${d.cmykString}")
                        appendLine()
                        appendLine("Luminance:         ${d.relativeLuminance}")
                        appendLine("Contrast vs White: ${d.contrastWhite}:1 → ${d.wcagWhiteLevel}")
                        appendLine("Contrast vs Black: ${d.contrastBlack}:1 → ${d.wcagBlackLevel}")
                        appendLine("Recommended Text:  ${d.recommendedTextColor}")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runColor(hexText) }

    ToolInputField(
        value = hexText,
        onValueChange = {
            hexText = it
            runColor(it)
        },
        label = "HEX Color Input",
        placeholder = "#3B82F6 or 3B82F6",
        minLines = 1
    )
}
