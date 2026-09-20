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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offline.toolbox.core.designsystem.components.ToolInputField
import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorBlindnessInput
import com.offline.toolbox.tools.color.ColorBlindnessSimulatorTool
import com.offline.toolbox.tools.color.ColorBlindnessType
import com.offline.toolbox.tools.data.JsonPathInput
import com.offline.toolbox.tools.data.JsonPathEvaluatorTool
import com.offline.toolbox.tools.data.TsvCsvInput
import com.offline.toolbox.tools.data.TsvCsvMode
import com.offline.toolbox.tools.data.TsvToCsvTool
import com.offline.toolbox.tools.developer.ChmodInput
import com.offline.toolbox.tools.developer.ChmodPermissionsCalculatorTool
import com.offline.toolbox.tools.developer.HttpHeaderInput
import com.offline.toolbox.tools.developer.HttpHeaderInspectorTool
import com.offline.toolbox.tools.developer.SemVerComparatorTool
import com.offline.toolbox.tools.developer.SemVerInput
import com.offline.toolbox.tools.developer.UserAgentInput
import com.offline.toolbox.tools.developer.UserAgentParserTool
import com.offline.toolbox.tools.math.CagrInput
import com.offline.toolbox.tools.math.CompoundAnnualGrowthRateTool
import com.offline.toolbox.tools.math.MatrixCalculatorTool
import com.offline.toolbox.tools.math.MatrixInput
import com.offline.toolbox.tools.math.StatisticsDistributionInput
import com.offline.toolbox.tools.math.StatisticsDistributionTool
import com.offline.toolbox.tools.media.BpmInput
import com.offline.toolbox.tools.media.BpmTapperTool
import com.offline.toolbox.tools.security.DicewareInput
import com.offline.toolbox.tools.security.PassphraseDicewareTool
import com.offline.toolbox.tools.security.TotpGeneratorTool
import com.offline.toolbox.tools.security.TotpInput
import com.offline.toolbox.tools.text.NatoInput
import com.offline.toolbox.tools.text.NatoMode
import com.offline.toolbox.tools.text.NatoPhoneticTool
import com.offline.toolbox.tools.text.TextCaseInspectorInput
import com.offline.toolbox.tools.text.TextCaseInspectorTool
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. USER-AGENT CLIENT & PLATFORM PARSER UI
// ----------------------------------------------------
@Composable
fun UserAgentParserToolUI(
    tool: UserAgentParserTool,
    onOutputChange: (String) -> Unit
) {
    var uaText by remember {
        mutableStateOf("Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.119 Mobile Safari/537.36")
    }
    val scope = rememberCoroutineScope()

    fun parseUa(s: String) {
        scope.launch {
            when (val res = tool.execute(UserAgentInput(s))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { parseUa(uaText) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = uaText,
            onValueChange = { uaText = it; parseUa(it) },
            label = "Browser User-Agent String",
            minLines = 3
        )

        Text("Sample User-Agents:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(
                "Android Pixel" to "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.119 Mobile Safari/537.36",
                "iPhone Safari" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
                "Desktop Chrome" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                "Googlebot" to "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
            ).forEach { (label, sample) ->
                FilterChip(
                    selected = uaText == sample,
                    onClick = { uaText = sample; parseUa(sample) },
                    label = { Text(label) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 2. SEMVER 2.0 EVALUATOR UI
// ----------------------------------------------------
@Composable
fun SemVerComparatorToolUI(
    tool: SemVerComparatorTool,
    onOutputChange: (String) -> Unit
) {
    var verA by remember { mutableStateOf("2.4.1") }
    var verB by remember { mutableStateOf("2.5.0-alpha.1") }
    var constraint by remember { mutableStateOf("^2.4.0") }
    val scope = rememberCoroutineScope()

    fun evaluate() {
        scope.launch {
            when (val res = tool.execute(SemVerInput(verA, verB, constraint))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { evaluate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ToolInputField(
                value = verA,
                onValueChange = { verA = it; evaluate() },
                label = "Version A",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = verB,
                onValueChange = { verB = it; evaluate() },
                label = "Version B (Optional)",
                modifier = Modifier.weight(1f)
            )
        }

        ToolInputField(
            value = constraint,
            onValueChange = { constraint = it; evaluate() },
            label = "Range Constraint (e.g. ^2.4.0, ~1.2.0, >=1.0.0)",
            placeholder = "^2.0.0"
        )
    }
}

// ----------------------------------------------------
// 3. CHMOD UNIX PERMISSIONS UI
// ----------------------------------------------------
@Composable
fun ChmodPermissionsCalculatorToolUI(
    tool: ChmodPermissionsCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var permInput by remember { mutableStateOf("755") }
    val scope = rememberCoroutineScope()

    fun calculate(v: String) {
        scope.launch {
            when (val res = tool.execute(ChmodInput(v))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { calculate(permInput) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = permInput,
            onValueChange = { permInput = it; calculate(it) },
            label = "Octal (e.g. 755) or Symbolic (-rwxr-xr-x)",
            placeholder = "755 or rwxr-xr-x"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("755", "644", "700", "777", "600", "400", "1777").forEach { preset ->
                FilterChip(
                    selected = permInput == preset,
                    onClick = { permInput = preset; calculate(preset) },
                    label = { Text(preset) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 4. HTTP HEADER INSPECTOR UI
// ----------------------------------------------------
@Composable
fun HttpHeaderInspectorToolUI(
    tool: HttpHeaderInspectorTool,
    onOutputChange: (String) -> Unit
) {
    var headersText by remember {
        mutableStateOf(
            "HTTP/1.1 200 OK\n" +
            "Content-Type: text/html; charset=UTF-8\n" +
            "Strict-Transport-Security: max-age=31536000; includeSubDomains\n" +
            "X-Frame-Options: SAMEORIGIN\n" +
            "X-Content-Type-Options: nosniff\n" +
            "Referrer-Policy: strict-origin-when-cross-origin"
        )
    }
    val scope = rememberCoroutineScope()

    fun audit(h: String) {
        scope.launch {
            when (val res = tool.execute(HttpHeaderInput(h))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { audit(headersText) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = headersText,
            onValueChange = { headersText = it; audit(it) },
            label = "Raw HTTP Response Headers",
            minLines = 6
        )
    }
}

// ----------------------------------------------------
// 5. TOTP / 2FA AUTHENTICATOR UI
// ----------------------------------------------------
@Composable
fun TotpGeneratorToolUI(
    tool: TotpGeneratorTool,
    onOutputChange: (String) -> Unit
) {
    var secret by remember { mutableStateOf("JBSWY3DPEHPK3PXP") }
    var step by remember { mutableIntStateOf(30) }
    var digits by remember { mutableIntStateOf(6) }
    val scope = rememberCoroutineScope()

    fun generate() {
        scope.launch {
            when (val res = tool.execute(TotpInput(secret, step, digits))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { generate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = secret,
            onValueChange = { secret = it; generate() },
            label = "Base32 Secret Key",
            placeholder = "e.g. JBSWY3DPEHPK3PXP"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = step == 30, onClick = { step = 30; generate() }, label = { Text("30s Step") })
            FilterChip(selected = step == 60, onClick = { step = 60; generate() }, label = { Text("60s Step") })
            FilterChip(selected = digits == 6, onClick = { digits = 6; generate() }, label = { Text("6 Digits") })
            FilterChip(selected = digits == 8, onClick = { digits = 8; generate() }, label = { Text("8 Digits") })
        }

        Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh 2FA Code")
        }
    }
}

// ----------------------------------------------------
// 6. DICEWARE PASSPHRASE GENERATOR UI
// ----------------------------------------------------
@Composable
fun PassphraseDicewareToolUI(
    tool: PassphraseDicewareTool,
    onOutputChange: (String) -> Unit
) {
    var wordCount by remember { mutableIntStateOf(5) }
    var delimiter by remember { mutableStateOf("-") }
    var cap by remember { mutableStateOf(true) }
    var num by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun generate() {
        scope.launch {
            when (val res = tool.execute(DicewareInput(wordCount, delimiter, cap, num))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { generate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Word Count: $wordCount words", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = wordCount.toFloat(),
            onValueChange = { wordCount = it.toInt() },
            onValueChangeFinished = { generate() },
            valueRange = 3f..8f,
            steps = 4
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("-" to "Hyphen", "_" to "Underscore", " " to "Space", "." to "Dot").forEach { (d, label) ->
                FilterChip(
                    selected = delimiter == d,
                    onClick = { delimiter = d; generate() },
                    label = { Text(label) }
                )
            }
        }

        Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) {
            Text("Roll Dice & Generate Passphrase")
        }
    }
}

// ----------------------------------------------------
// 7. JSONPATH EVALUATOR UI
// ----------------------------------------------------
@Composable
fun JsonPathEvaluatorToolUI(
    tool: JsonPathEvaluatorTool,
    onOutputChange: (String) -> Unit
) {
    var jsonText by remember {
        mutableStateOf(
            "{\n  \"store\": {\n    \"book\": [\n      { \"title\": \"Sayings of the Century\", \"price\": 8.95 },\n      { \"title\": \"Sword of Honour\", \"price\": 12.99 }\n    ]\n  }\n}"
        )
    }
    var queryPath by remember { mutableStateOf("$.store.book[*].title") }
    val scope = rememberCoroutineScope()

    fun evaluate() {
        scope.launch {
            when (val res = tool.execute(JsonPathInput(jsonText, queryPath))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { evaluate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = queryPath,
            onValueChange = { queryPath = it; evaluate() },
            label = "JSONPath Expression",
            placeholder = "$.store.book[*].title"
        )

        ToolInputField(
            value = jsonText,
            onValueChange = { jsonText = it; evaluate() },
            label = "JSON Content",
            minLines = 6
        )
    }
}

// ----------------------------------------------------
// 8. TSV <-> CSV CONVERTER UI
// ----------------------------------------------------
@Composable
fun TsvToCsvToolUI(
    tool: TsvToCsvTool,
    onOutputChange: (String) -> Unit
) {
    var content by remember { mutableStateOf("Name\tRole\tDepartment\nAlice\tArchitect\tEngineering\nBob\tDesigner\tProduct") }
    var mode by remember { mutableStateOf(TsvCsvMode.TSV_TO_CSV) }
    val scope = rememberCoroutineScope()

    fun convert() {
        scope.launch {
            when (val res = tool.execute(TsvCsvInput(content, mode))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { convert() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == TsvCsvMode.TSV_TO_CSV, onClick = { mode = TsvCsvMode.TSV_TO_CSV; convert() }, label = { Text("TSV → CSV") })
            FilterChip(selected = mode == TsvCsvMode.CSV_TO_TSV, onClick = { mode = TsvCsvMode.CSV_TO_TSV; convert() }, label = { Text("CSV → TSV") })
        }

        ToolInputField(
            value = content,
            onValueChange = { content = it; convert() },
            label = "Tabular Content",
            minLines = 5
        )
    }
}

// ----------------------------------------------------
// 9. MATRIX CALCULATOR UI
// ----------------------------------------------------
@Composable
fun MatrixCalculatorToolUI(
    tool: MatrixCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var matrixStr by remember { mutableStateOf("1, 2, 3\n0, 1, 4\n5, 6, 0") }
    var scalar by remember { mutableDoubleStateOf(2.0) }
    val scope = rememberCoroutineScope()

    fun calculate() {
        scope.launch {
            when (val res = tool.execute(MatrixInput(matrixStr, scalar))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { calculate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = matrixStr,
            onValueChange = { matrixStr = it; calculate() },
            label = "Matrix Elements (Rows on new lines, separated by commas or spaces)",
            minLines = 4
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "3x3 Default" to "1, 2, 3\n0, 1, 4\n5, 6, 0",
                "2x2 Standard" to "4, 7\n2, 6",
                "3x3 Identity" to "1, 0, 0\n0, 1, 0\n0, 0, 1"
            ).forEach { (label, preset) ->
                FilterChip(
                    selected = matrixStr == preset,
                    onClick = { matrixStr = preset; calculate() },
                    label = { Text(label) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 10. STATISTICS DISTRIBUTION UI
// ----------------------------------------------------
@Composable
fun StatisticsDistributionToolUI(
    tool: StatisticsDistributionTool,
    onOutputChange: (String) -> Unit
) {
    var xStr by remember { mutableStateOf("1.96") }
    var muStr by remember { mutableStateOf("0.0") }
    var sigmaStr by remember { mutableStateOf("1.0") }
    val scope = rememberCoroutineScope()

    fun compute() {
        val x = xStr.toDoubleOrNull() ?: 0.0
        val mu = muStr.toDoubleOrNull() ?: 0.0
        val sigma = sigmaStr.toDoubleOrNull() ?: 1.0
        scope.launch {
            when (val res = tool.execute(StatisticsDistributionInput(x, mu, sigma))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { compute() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ToolInputField(
                value = xStr,
                onValueChange = { xStr = it; compute() },
                label = "Evaluation Point (x)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = muStr,
                onValueChange = { muStr = it; compute() },
                label = "Mean (μ)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = sigmaStr,
                onValueChange = { sigmaStr = it; compute() },
                label = "Std Dev (σ)",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ----------------------------------------------------
// 11. CAGR CALCULATOR UI
// ----------------------------------------------------
@Composable
fun CompoundAnnualGrowthRateToolUI(
    tool: CompoundAnnualGrowthRateTool,
    onOutputChange: (String) -> Unit
) {
    var initialStr by remember { mutableStateOf("10000") }
    var finalStr by remember { mutableStateOf("25000") }
    var yearsStr by remember { mutableStateOf("5") }
    val scope = rememberCoroutineScope()

    fun calculate() {
        val p0 = initialStr.toDoubleOrNull() ?: 10000.0
        val p1 = finalStr.toDoubleOrNull() ?: 25000.0
        val n = yearsStr.toDoubleOrNull() ?: 5.0
        scope.launch {
            when (val res = tool.execute(CagrInput(p0, p1, n))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { calculate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ToolInputField(
                value = initialStr,
                onValueChange = { initialStr = it; calculate() },
                label = "Initial Investment ($)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = finalStr,
                onValueChange = { finalStr = it; calculate() },
                label = "Final Value ($)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = yearsStr,
                onValueChange = { yearsStr = it; calculate() },
                label = "Years",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ----------------------------------------------------
// 12. TEXT CASE INSPECTOR UI
// ----------------------------------------------------
@Composable
fun TextCaseInspectorToolUI(
    tool: TextCaseInspectorTool,
    onOutputChange: (String) -> Unit
) {
    var identifier by remember { mutableStateOf("offlineAndroidUtilityToolbox") }
    val scope = rememberCoroutineScope()

    fun inspect(id: String) {
        scope.launch {
            when (val res = tool.execute(TextCaseInspectorInput(id))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { inspect(identifier) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = identifier,
            onValueChange = { identifier = it; inspect(it) },
            label = "Input Identifier / Code Symbol",
            placeholder = "e.g. camelCase, snake_case, PascalCase"
        )
    }
}

// ----------------------------------------------------
// 13. NATO PHONETIC ALPHABET UI
// ----------------------------------------------------
@Composable
fun NatoPhoneticToolUI(
    tool: NatoPhoneticTool,
    onOutputChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("AZR Offline 2026") }
    var mode by remember { mutableStateOf(NatoMode.TEXT_TO_PHONETIC) }
    val scope = rememberCoroutineScope()

    fun translate() {
        scope.launch {
            when (val res = tool.execute(NatoInput(text, mode))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { translate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == NatoMode.TEXT_TO_PHONETIC, onClick = { mode = NatoMode.TEXT_TO_PHONETIC; translate() }, label = { Text("Text → NATO Phonetic") })
            FilterChip(selected = mode == NatoMode.PHONETIC_TO_TEXT, onClick = { mode = NatoMode.PHONETIC_TO_TEXT; translate() }, label = { Text("NATO Phonetic → Text") })
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it; translate() },
            label = "Input Text / Phonetic Spelling",
            minLines = 2
        )
    }
}

// ----------------------------------------------------
// 14. METRONOME & BPM TAPPER UI
// ----------------------------------------------------
@Composable
fun BpmTapperToolUI(
    tool: BpmTapperTool,
    onOutputChange: (String) -> Unit
) {
    var directBpm by remember { mutableDoubleStateOf(120.0) }
    val taps = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()

    fun updateBpm() {
        scope.launch {
            when (val res = tool.execute(BpmInput(taps.toList(), directBpm))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { updateBpm() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    taps.add(System.currentTimeMillis())
                    updateBpm()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Tap Tempo 🎵 (${taps.size})")
            }

            Button(
                onClick = {
                    taps.clear()
                    updateBpm()
                }
            ) {
                Text("Reset")
            }
        }

        Text("Direct BPM Setting: ${directBpm.toInt()} BPM", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = directBpm.toFloat(),
            onValueChange = { directBpm = it.toDouble(); updateBpm() },
            valueRange = 40f..240f,
            steps = 199
        )
    }
}

// ----------------------------------------------------
// 15. COLOR BLINDNESS SIMULATOR UI
// ----------------------------------------------------
@Composable
fun ColorBlindnessSimulatorToolUI(
    tool: ColorBlindnessSimulatorTool,
    onOutputChange: (String) -> Unit
) {
    var hex by remember { mutableStateOf("#E11D48") }
    var deficiency by remember { mutableStateOf(ColorBlindnessType.DEUTERANOPIA) }
    val scope = rememberCoroutineScope()

    fun simulate() {
        scope.launch {
            when (val res = tool.execute(ColorBlindnessInput(hex, deficiency))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { simulate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = hex,
            onValueChange = { hex = it; simulate() },
            label = "HEX Color Code",
            placeholder = "#E11D48"
        )

        Text("Simulate Deficiency:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            ColorBlindnessType.entries.forEach { def ->
                FilterChip(
                    selected = deficiency == def,
                    onClick = { deficiency = def; simulate() },
                    label = { Text(def.displayName) }
                )
            }
        }
    }
}
