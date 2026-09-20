package com.offline.toolbox.features.toolrunner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offline.toolbox.core.designsystem.components.ToolInputField
import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorTempInput
import com.offline.toolbox.tools.color.ColorTemperatureTool
import com.offline.toolbox.tools.data.BinarySearchTreeVisualizerTool
import com.offline.toolbox.tools.data.BstVisualizerInput
import com.offline.toolbox.tools.data.CsvSchemaInput
import com.offline.toolbox.tools.data.CsvToJsonSchemaTool
import com.offline.toolbox.tools.developer.BgpAsnInput
import com.offline.toolbox.tools.developer.BgpAsnLookupTool
import com.offline.toolbox.tools.developer.CrontabDiffInput
import com.offline.toolbox.tools.developer.CrontabScheduleDiffTool
import com.offline.toolbox.tools.developer.CurlCommandParserTool
import com.offline.toolbox.tools.developer.CurlParserInput
import com.offline.toolbox.tools.developer.GitIgnoreGeneratorTool
import com.offline.toolbox.tools.developer.GitIgnoreInput
import com.offline.toolbox.tools.developer.GitIgnorePreset
import com.offline.toolbox.tools.math.AngleUnit
import com.offline.toolbox.tools.math.ConstantCategory
import com.offline.toolbox.tools.math.ConstantsInput
import com.offline.toolbox.tools.math.LoanRefinanceComparatorTool
import com.offline.toolbox.tools.math.RefinanceInput
import com.offline.toolbox.tools.math.ScientificConstantsTool
import com.offline.toolbox.tools.math.TrigInput
import com.offline.toolbox.tools.math.TrigonometricFunctionsTool
import com.offline.toolbox.tools.media.AudioDecibelCalculatorTool
import com.offline.toolbox.tools.media.DecibelCalculationType
import com.offline.toolbox.tools.media.DecibelInput
import com.offline.toolbox.tools.security.HkdfHashAlgorithm
import com.offline.toolbox.tools.security.HkdfInput
import com.offline.toolbox.tools.security.HkdfKeyDerivationTool
import com.offline.toolbox.tools.security.VigenereCipherTool
import com.offline.toolbox.tools.security.VigenereInput
import com.offline.toolbox.tools.security.VigenereMode
import com.offline.toolbox.tools.text.JustifierInput
import com.offline.toolbox.tools.text.ReadabilityInput
import com.offline.toolbox.tools.text.ReadabilityScoreTool
import com.offline.toolbox.tools.text.TextJustifierTool
import kotlinx.coroutines.launch

@Composable
fun CurlCommandParserUI(onResultUpdated: (String, String?) -> Unit) {
    var curlCmd by remember {
        mutableStateOf("""curl -X POST "https://api.example.com/v1/auth" -H "Content-Type: application/json" -H "Authorization: Bearer my-secret-token" -d '{"username":"dev_user","role":"admin"}'""")
    }
    val tool = remember { CurlCommandParserTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(CurlParserInput(curlCommand = curlCmd))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = curlCmd,
            onValueChange = { curlCmd = it; run() },
            label = "cURL Command (e.g. curl -X POST https://...)"
        )
    }
}

@Composable
fun BgpAsnLookupUI(onResultUpdated: (String, String?) -> Unit) {
    var asnQuery by remember { mutableStateOf("15169") }
    val tool = remember { BgpAsnLookupTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(BgpAsnInput(asnQuery = asnQuery))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = asnQuery,
            onValueChange = { asnQuery = it; run() },
            label = "Autonomous System Number (ASPLAIN e.g. 15169, or ASDOT e.g. 0.15169)"
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("15169" to "Google", "13335" to "Cloudflare", "16509" to "AWS", "64512" to "RFC 6996 Private", "65534" to "RFC 6996 16-bit").forEach { (asn, label) ->
                Button(onClick = { asnQuery = asn; run() }) {
                    Text("$label ($asn)")
                }
            }
        }
    }
}

@Composable
fun GitIgnoreGeneratorUI(onResultUpdated: (String, String?) -> Unit) {
    var selectedPresets by remember {
        mutableStateOf(listOf(GitIgnorePreset.ANDROID, GitIgnorePreset.KOTLIN, GitIgnorePreset.GRADLE))
    }
    val tool = remember { GitIgnoreGeneratorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(GitIgnoreInput(selectedPresets = selectedPresets))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.generatedGitIgnore, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(selectedPresets) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Select Tech Stacks & Platforms:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GitIgnorePreset.entries.forEach { preset ->
                FilterChip(
                    selected = selectedPresets.contains(preset),
                    onClick = {
                        selectedPresets = if (selectedPresets.contains(preset)) {
                            selectedPresets - preset
                        } else {
                            selectedPresets + preset
                        }
                    },
                    label = { Text(preset.name) }
                )
            }
        }
    }
}

@Composable
fun CrontabScheduleDiffUI(onResultUpdated: (String, String?) -> Unit) {
    var cronA by remember { mutableStateOf("*/15 * * * *") }
    var cronB by remember { mutableStateOf("0 * * * *") }
    val tool = remember { CrontabScheduleDiffTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(CrontabDiffInput(cronA = cronA, cronB = cronB))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = cronA,
            onValueChange = { cronA = it; run() },
            label = "Cron Expression A (e.g. */15 * * * *)"
        )
        ToolInputField(
            value = cronB,
            onValueChange = { cronB = it; run() },
            label = "Cron Expression B (e.g. 0 * * * *)"
        )
    }
}

@Composable
fun HkdfKeyDerivationUI(onResultUpdated: (String, String?) -> Unit) {
    var ikm by remember { mutableStateOf("Master-Input-Keying-Material-2026") }
    var salt by remember { mutableStateOf("CryptographicSaltValue") }
    var info by remember { mutableStateOf("AZR-App-Subkey-Encryption") }
    var lengthBytes by remember { mutableIntStateOf(32) }
    var algo by remember { mutableStateOf(HkdfHashAlgorithm.SHA256) }

    val tool = remember { HkdfKeyDerivationTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(HkdfInput(
                ikm = ikm,
                salt = salt,
                info = info,
                outputKeyLengthBytes = lengthBytes,
                algorithm = algo
            ))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, "Derived Key: ${res.data.derivedKeyHex}")
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(algo, lengthBytes) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HkdfHashAlgorithm.entries.forEach { a ->
                FilterChip(
                    selected = algo == a,
                    onClick = { algo = a },
                    label = { Text(a.name) }
                )
            }
        }
        ToolInputField(
            value = ikm,
            onValueChange = { ikm = it; run() },
            label = "Input Keying Material (IKM)"
        )
        ToolInputField(
            value = salt,
            onValueChange = { salt = it; run() },
            label = "Salt (Empty defaults to zeros)"
        )
        ToolInputField(
            value = info,
            onValueChange = { info = it; run() },
            label = "Application Context Info"
        )
        Text("Derived Key Length (Bytes):", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(16 to "128-bit (16B)", 32 to "256-bit (32B)", 64 to "512-bit (64B)").forEach { (bytes, label) ->
                FilterChip(
                    selected = lengthBytes == bytes,
                    onClick = { lengthBytes = bytes },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
fun VigenereCipherUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("ATTACK AT DAWN ON THE SHORELINE") }
    var key by remember { mutableStateOf("LEMON") }
    var mode by remember { mutableStateOf(VigenereMode.ENCRYPT) }
    val tool = remember { VigenereCipherTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(VigenereInput(text = text, key = key, mode = mode))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.result, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(mode) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VigenereMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m.name) }
                )
            }
        }
        ToolInputField(
            value = text,
            onValueChange = { text = it; run() },
            label = "Message Text"
        )
        ToolInputField(
            value = key,
            onValueChange = { key = it; run() },
            label = "Cipher Keyword / Secret Key"
        )
    }
}

@Composable
fun CsvToJsonSchemaUI(onResultUpdated: (String, String?) -> Unit) {
    var csvText by remember {
        mutableStateOf("""
            id,name,email,is_active,score,created_at
            1,Alice,alice@example.com,true,95.5,2026-01-15T08:30:00Z
            2,Bob,bob@example.com,false,82.0,2026-02-20T14:15:00Z
            3,Charlie,charlie@example.com,true,99.2,2026-03-01T19:45:00Z
        """.trimIndent())
    }
    val tool = remember { CsvToJsonSchemaTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(CsvSchemaInput(csvContent = csvText))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.jsonSchema, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = csvText,
            onValueChange = { csvText = it; run() },
            label = "CSV Input (Header + Rows)"
        )
    }
}

@Composable
fun BinarySearchTreeVisualizerUI(onResultUpdated: (String, String?) -> Unit) {
    var numbersText by remember { mutableStateOf("50, 30, 70, 20, 40, 60, 80, 10, 25") }
    val tool = remember { BinarySearchTreeVisualizerTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(BstVisualizerInput(numbersList = numbersText))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = numbersText,
            onValueChange = { numbersText = it; run() },
            label = "Integer Sequence (Comma/space separated)"
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { numbersText = "10, 20, 30, 40, 50, 60, 70"; run() }) {
                Text("Skewed Right")
            }
            Button(onClick = { numbersText = "40, 20, 60, 10, 30, 50, 70"; run() }) {
                Text("Balanced AVL")
            }
            Button(onClick = { numbersText = "8, 3, 10, 1, 6, 14, 4, 7, 13"; run() }) {
                Text("Classic BST")
            }
        }
    }
}

@Composable
fun LoanRefinanceComparatorUI(onResultUpdated: (String, String?) -> Unit) {
    var currentBalanceText by remember { mutableStateOf("300000") }
    var currentRateText by remember { mutableStateOf("6.5") }
    var currentRemainingMonthsText by remember { mutableStateOf("300") }
    var newRateText by remember { mutableStateOf("5.0") }
    var newTermMonthsText by remember { mutableStateOf("300") }
    var closingCostsText by remember { mutableStateOf("4500") }

    val tool = remember { LoanRefinanceComparatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val balance = currentBalanceText.toDoubleOrNull() ?: 0.0
            val curRate = currentRateText.toDoubleOrNull() ?: 0.0
            val curMonths = currentRemainingMonthsText.toIntOrNull() ?: 0
            val newRate = newRateText.toDoubleOrNull() ?: 0.0
            val newMonths = newTermMonthsText.toIntOrNull() ?: 0
            val costs = closingCostsText.toDoubleOrNull() ?: 0.0

            val res = tool.execute(RefinanceInput(
                currentPrincipalBalance = balance,
                currentAnnualRatePct = curRate,
                remainingTermMonths = curMonths,
                newAnnualRatePct = newRate,
                newTermMonths = newMonths,
                closingCosts = costs
            ))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = currentBalanceText,
            onValueChange = { currentBalanceText = it; run() },
            label = "Remaining Loan Balance ($)"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = currentRateText,
                    onValueChange = { currentRateText = it; run() },
                    label = "Current Rate (%)"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = currentRemainingMonthsText,
                    onValueChange = { currentRemainingMonthsText = it; run() },
                    label = "Remaining Months"
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = newRateText,
                    onValueChange = { newRateText = it; run() },
                    label = "Refinance Rate (%)"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = newTermMonthsText,
                    onValueChange = { newTermMonthsText = it; run() },
                    label = "New Term (Months)"
                )
            }
        }
        ToolInputField(
            value = closingCostsText,
            onValueChange = { closingCostsText = it; run() },
            label = "Refinance Closing Costs / Fees ($)"
        )
    }
}

@Composable
fun TrigonometricFunctionsUI(onResultUpdated: (String, String?) -> Unit) {
    var angleValueText by remember { mutableStateOf("45") }
    var angleUnit by remember { mutableStateOf(AngleUnit.DEGREES) }
    val tool = remember { TrigonometricFunctionsTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val angle = angleValueText.toDoubleOrNull() ?: 0.0
            val res = tool.execute(TrigInput(angleValue = angle, unit = angleUnit))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(angleUnit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AngleUnit.entries.forEach { u ->
                FilterChip(
                    selected = angleUnit == u,
                    onClick = { angleUnit = u },
                    label = { Text(u.name) }
                )
            }
        }
        ToolInputField(
            value = angleValueText,
            onValueChange = { angleValueText = it; run() },
            label = "Angle Value (${angleUnit.name.lowercase()})"
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("0", "30", "45", "60", "90", "180", "270", "360").forEach { preset ->
                Button(onClick = { angleUnit = AngleUnit.DEGREES; angleValueText = preset; run() }) {
                    Text("$preset°")
                }
            }
        }
    }
}

@Composable
fun ScientificConstantsUI(onResultUpdated: (String, String?) -> Unit) {
    var category by remember { mutableStateOf(ConstantCategory.ALL) }
    var queryText by remember { mutableStateOf("") }
    val tool = remember { ScientificConstantsTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(ConstantsInput(query = queryText, category = category))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(category) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = queryText,
            onValueChange = { queryText = it; run() },
            label = "Search Constant (Name, Symbol or Unit)"
        )
        Text("Filter by Domain Category:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConstantCategory.entries.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat.displayName) }
                )
            }
        }
    }
}

@Composable
fun ReadabilityScoreUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember {
        mutableStateOf("""
            The quick brown fox jumps over the lazy dog. Reading ease is an essential aspect of clear communication.
            When sentences are concise and words are familiar, readers grasp complex concepts with minimal cognitive overhead.
            Offline utility software delivers deterministic computation directly on modern mobile architectures without compromising private user data.
        """.trimIndent())
    }
    val tool = remember { ReadabilityScoreTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(ReadabilityInput(text = text))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = text,
            onValueChange = { text = it; run() },
            label = "Prose / Document Text"
        )
    }
}

@Composable
fun TextJustifierUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember {
        mutableStateOf("Antigravity offline android utility suite provides over one hundred and thirty-five privacy-first deterministic tools executing completely on-device without internet permissions.")
    }
    var colWidthText by remember { mutableStateOf("45") }

    val tool = remember { TextJustifierTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val w = colWidthText.toIntOrNull() ?: 45
            val res = tool.execute(JustifierInput(text = text, lineWidth = w))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.justifiedText, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = colWidthText,
            onValueChange = { colWidthText = it; run() },
            label = "Column / Line Width (20 - 120 characters)"
        )
        ToolInputField(
            value = text,
            onValueChange = { text = it; run() },
            label = "Input Text"
        )
    }
}

@Composable
fun ColorTemperatureUI(onResultUpdated: (String, String?) -> Unit) {
    var kelvinText by remember { mutableStateOf("6500") }
    val tool = remember { ColorTemperatureTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val k = kelvinText.toIntOrNull() ?: 6500
            val res = tool.execute(ColorTempInput(temperatureKelvin = k))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = kelvinText,
            onValueChange = { kelvinText = it; run() },
            label = "Correlated Color Temperature (1000K - 40000K)"
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1850" to "Candle", "2700" to "Warm Incandescent", "5000" to "Direct Sun", "6500" to "D65 Daylight", "9500" to "Blue Sky").forEach { (k, name) ->
                Button(onClick = { kelvinText = k; run() }) {
                    Text("$name ($k K)")
                }
            }
        }
    }
}

@Composable
fun AudioDecibelCalculatorUI(onResultUpdated: (String, String?) -> Unit) {
    var mode by remember { mutableStateOf(DecibelCalculationType.SOUND_PRESSURE_LEVEL) }
    var valText by remember { mutableStateOf("1.0") }
    var r1Text by remember { mutableStateOf("1.0") }
    var r2Text by remember { mutableStateOf("4.0") }

    val tool = remember { AudioDecibelCalculatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val v = valText.toDoubleOrNull() ?: 1.0
            val r1 = r1Text.toDoubleOrNull() ?: 1.0
            val r2 = r2Text.toDoubleOrNull() ?: 4.0
            val res = tool.execute(DecibelInput(
                calculationType = mode,
                value = v,
                initialDistanceMeters = r1,
                targetDistanceMeters = r2
            ))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(mode) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Calculation Type:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DecibelCalculationType.entries.forEach { m ->
                val label = when (m) {
                    DecibelCalculationType.SOUND_PRESSURE_LEVEL -> "dB SPL (Pressure)"
                    DecibelCalculationType.VOLTAGE_DBU -> "dBu (0.775V)"
                    DecibelCalculationType.VOLTAGE_DBV -> "dBV (1.0V)"
                    DecibelCalculationType.POWER_DBM -> "dBm (1mW)"
                    DecibelCalculationType.DISTANCE_ATTENUATION -> "Distance Loss"
                }
                FilterChip(
                    selected = mode == m,
                    onClick = {
                        mode = m
                        when (m) {
                            DecibelCalculationType.SOUND_PRESSURE_LEVEL -> valText = "1.0"
                            DecibelCalculationType.VOLTAGE_DBU -> valText = "1.228"
                            DecibelCalculationType.VOLTAGE_DBV -> valText = "1.0"
                            DecibelCalculationType.POWER_DBM -> valText = "10.0"
                            DecibelCalculationType.DISTANCE_ATTENUATION -> valText = "90.0"
                        }
                    },
                    label = { Text(label) }
                )
            }
        }
        val mainLabel = when (mode) {
            DecibelCalculationType.SOUND_PRESSURE_LEVEL -> "Measured Sound Pressure (Pascals)"
            DecibelCalculationType.VOLTAGE_DBU -> "RMS Signal Voltage (Volts)"
            DecibelCalculationType.VOLTAGE_DBV -> "RMS Signal Voltage (Volts)"
            DecibelCalculationType.POWER_DBM -> "Signal Power (Milliwatts)"
            DecibelCalculationType.DISTANCE_ATTENUATION -> "Reference Sound Level (dB SPL)"
        }
        ToolInputField(
            value = valText,
            onValueChange = { valText = it; run() },
            label = mainLabel
        )
        if (mode == DecibelCalculationType.DISTANCE_ATTENUATION) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ToolInputField(
                        value = r1Text,
                        onValueChange = { r1Text = it; run() },
                        label = "Ref Distance r1 (m)"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    ToolInputField(
                        value = r2Text,
                        onValueChange = { r2Text = it; run() },
                        label = "Target Distance r2 (m)"
                    )
                }
            }
        }
    }
}
