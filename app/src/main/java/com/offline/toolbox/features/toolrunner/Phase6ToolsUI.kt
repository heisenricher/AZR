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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offline.toolbox.core.designsystem.components.ToolInputField
import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.HtmlColorNameInput
import com.offline.toolbox.tools.color.HtmlColorNameTool
import com.offline.toolbox.tools.data.JsonDiffInput
import com.offline.toolbox.tools.data.JsonDiffTool
import com.offline.toolbox.tools.data.NdjsonInput
import com.offline.toolbox.tools.data.NdjsonMode
import com.offline.toolbox.tools.data.NdjsonToJsonArrayTool
import com.offline.toolbox.tools.developer.IPv6SubnetCalculatorTool
import com.offline.toolbox.tools.developer.IPv6SubnetInput
import com.offline.toolbox.tools.developer.PortLookupInput
import com.offline.toolbox.tools.developer.PortLookupTool
import com.offline.toolbox.tools.math.CurrencyPreset
import com.offline.toolbox.tools.math.DenominationCalculatorTool
import com.offline.toolbox.tools.math.DenominationInput
import com.offline.toolbox.tools.math.PrimeFactorInput
import com.offline.toolbox.tools.math.PrimeFactorizationTool
import com.offline.toolbox.tools.math.ScientificNotationInput
import com.offline.toolbox.tools.math.ScientificNotationTool
import com.offline.toolbox.tools.media.DpiDensityCalculatorTool
import com.offline.toolbox.tools.media.DpiDensityInput
import com.offline.toolbox.tools.media.FrequencyNoteInput
import com.offline.toolbox.tools.media.FrequencyToNoteTool
import com.offline.toolbox.tools.security.BcryptInput
import com.offline.toolbox.tools.security.BcryptWorkFactorTool
import com.offline.toolbox.tools.security.SymmetricAlgorithm
import com.offline.toolbox.tools.security.SymmetricCipherInput
import com.offline.toolbox.tools.security.SymmetricCipherTool
import com.offline.toolbox.tools.security.SymmetricMode
import com.offline.toolbox.tools.text.AnagramInput
import com.offline.toolbox.tools.text.AnagramSolverTool
import com.offline.toolbox.tools.text.TextWrapInput
import com.offline.toolbox.tools.text.TextWrapTool
import com.offline.toolbox.tools.text.ZalgoInput
import com.offline.toolbox.tools.text.ZalgoIntensity
import com.offline.toolbox.tools.text.ZalgoMode
import com.offline.toolbox.tools.text.ZalgoTextTool
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. IPV6 SUBNET CALCULATOR UI
// ----------------------------------------------------
@Composable
fun IPv6SubnetCalculatorToolUI(
    tool: IPv6SubnetCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var address by remember { mutableStateOf("2001:0db8:85a3::8a2e:0370:7334") }
    var prefix by remember { mutableIntStateOf(64) }
    val scope = rememberCoroutineScope()

    fun calculate() {
        scope.launch {
            when (val res = tool.execute(IPv6SubnetInput(address, prefix))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { calculate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = address,
            onValueChange = { address = it; calculate() },
            label = "IPv6 Address",
            placeholder = "e.g. 2001:db8::1 or fe80::1"
        )

        Text("Subnet Prefix: /$prefix", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = prefix.toFloat(),
            onValueChange = { prefix = it.toInt(); calculate() },
            valueRange = 1f..128f,
            steps = 126
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(32, 48, 56, 64, 96, 128).forEach { p ->
                FilterChip(
                    selected = prefix == p,
                    onClick = { prefix = p; calculate() },
                    label = { Text("/$p") }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 2. PORT LOOKUP TOOL UI
// ----------------------------------------------------
@Composable
fun PortLookupToolUI(
    tool: PortLookupTool,
    onOutputChange: (String) -> Unit
) {
    var query by remember { mutableStateOf("443") }
    val scope = rememberCoroutineScope()

    fun lookup(q: String) {
        scope.launch {
            when (val res = tool.execute(PortLookupInput(q))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { lookup(query) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = query,
            onValueChange = { query = it; lookup(it) },
            label = "Port Number or Service Name",
            placeholder = "e.g. 80, 443, SSH, Redis, DNS"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("22", "80", "443", "3306", "5432", "6379", "3389", "27017").forEach { sample ->
                FilterChip(
                    selected = query == sample,
                    onClick = { query = sample; lookup(sample) },
                    label = { Text(sample) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 3. SYMMETRIC CIPHER (AES-GCM) UI
// ----------------------------------------------------
@Composable
fun SymmetricCipherToolUI(
    tool: SymmetricCipherTool,
    onOutputChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("Confidential Offline Message 2026") }
    var passphrase by remember { mutableStateOf("my-secret-vault-password") }
    var mode by remember { mutableStateOf(SymmetricMode.ENCRYPT) }
    var algorithm by remember { mutableStateOf(SymmetricAlgorithm.AES_256_GCM) }
    val scope = rememberCoroutineScope()

    fun runCipher() {
        scope.launch {
            when (val res = tool.execute(SymmetricCipherInput(text, passphrase, mode, algorithm))) {
                is ToolResult.Success -> {
                    val out = buildString {
                        appendLine(res.data.result)
                        appendLine()
                        appendLine(res.data.formattedReport)
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runCipher() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == SymmetricMode.ENCRYPT, onClick = { mode = SymmetricMode.ENCRYPT; runCipher() }, label = { Text("Encrypt") })
            FilterChip(selected = mode == SymmetricMode.DECRYPT, onClick = { mode = SymmetricMode.DECRYPT; runCipher() }, label = { Text("Decrypt") })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SymmetricAlgorithm.entries.forEach { algo ->
                FilterChip(
                    selected = algorithm == algo,
                    onClick = { algorithm = algo; runCipher() },
                    label = { Text(algo.name) }
                )
            }
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it; runCipher() },
            label = if (mode == SymmetricMode.ENCRYPT) "Plaintext" else "Base64 Ciphertext Payload",
            minLines = 3
        )

        ToolInputField(
            value = passphrase,
            onValueChange = { passphrase = it; runCipher() },
            label = "Secret Passphrase / Key",
            placeholder = "Enter encryption key"
        )
    }
}

// ----------------------------------------------------
// 4. BCRYPT WORK FACTOR ANALYZER UI
// ----------------------------------------------------
@Composable
fun BcryptWorkFactorToolUI(
    tool: BcryptWorkFactorTool,
    onOutputChange: (String) -> Unit
) {
    var input by remember { mutableStateOf("12") }
    val scope = rememberCoroutineScope()

    fun evaluate(valStr: String) {
        scope.launch {
            when (val res = tool.execute(BcryptInput(valStr))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { evaluate(input) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = input,
            onValueChange = { input = it; evaluate(it) },
            label = "Bcrypt Hash or Cost Integer (4 - 31)",
            placeholder = "e.g. 12 or \$2a\$12\$..."
        )

        Text("Evaluate Standard Cost Parameters:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("8", "10", "12", "14", "16").forEach { c ->
                FilterChip(
                    selected = input == c,
                    onClick = { input = c; evaluate(c) },
                    label = { Text("Cost $c") }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 5. JSON DIFF TOOL UI
// ----------------------------------------------------
@Composable
fun JsonDiffToolUI(
    tool: JsonDiffTool,
    onOutputChange: (String) -> Unit
) {
    var original by remember {
        mutableStateOf(
            """
            {
              "name": "Alex",
              "role": "User",
              "tags": ["offline", "mobile"]
            }
            """.trimIndent()
        )
    }
    var modified by remember {
        mutableStateOf(
            """
            {
              "name": "Alex",
              "role": "Administrator",
              "active": true,
              "tags": ["offline", "android", "privacy"]
            }
            """.trimIndent()
        )
    }
    val scope = rememberCoroutineScope()

    fun runDiff() {
        scope.launch {
            when (val res = tool.execute(JsonDiffInput(original, modified))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runDiff() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = original,
            onValueChange = { original = it; runDiff() },
            label = "Original JSON Document",
            minLines = 4
        )

        ToolInputField(
            value = modified,
            onValueChange = { modified = it; runDiff() },
            label = "Modified JSON Document",
            minLines = 4
        )

        Button(onClick = { runDiff() }, modifier = Modifier.fillMaxWidth()) {
            Text("Re-compute JSON Structural Diff")
        }
    }
}

// ----------------------------------------------------
// 6. NDJSON / JSON LINES CONVERTER UI
// ----------------------------------------------------
@Composable
fun NdjsonToJsonArrayToolUI(
    tool: NdjsonToJsonArrayTool,
    onOutputChange: (String) -> Unit
) {
    var mode by remember { mutableStateOf(NdjsonMode.NDJSON_TO_JSON_ARRAY) }
    var content by remember {
        mutableStateOf(
            """
            {"id": 1, "service": "Core", "status": "UP"}
            {"id": 2, "service": "Database", "status": "UP"}
            {"id": 3, "service": "Storage", "status": "READY"}
            """.trimIndent()
        )
    }
    val scope = rememberCoroutineScope()

    fun convert() {
        scope.launch {
            when (val res = tool.execute(NdjsonInput(content, mode))) {
                is ToolResult.Success -> onOutputChange(res.data.convertedContent)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { convert() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == NdjsonMode.NDJSON_TO_JSON_ARRAY,
                onClick = { mode = NdjsonMode.NDJSON_TO_JSON_ARRAY; convert() },
                label = { Text("NDJSON → JSON Array") }
            )
            FilterChip(
                selected = mode == NdjsonMode.JSON_ARRAY_TO_NDJSON,
                onClick = { mode = NdjsonMode.JSON_ARRAY_TO_NDJSON; convert() },
                label = { Text("JSON Array → NDJSON") }
            )
        }

        ToolInputField(
            value = content,
            onValueChange = { content = it; convert() },
            label = if (mode == NdjsonMode.NDJSON_TO_JSON_ARRAY) "NDJSON Lines Input" else "JSON Array Input",
            minLines = 5
        )
    }
}

// ----------------------------------------------------
// 7. SCIENTIFIC NOTATION UI
// ----------------------------------------------------
@Composable
fun ScientificNotationToolUI(
    tool: ScientificNotationTool,
    onOutputChange: (String) -> Unit
) {
    var numberStr by remember { mutableStateOf("1234500000") }
    var sigDigits by remember { mutableIntStateOf(4) }
    val scope = rememberCoroutineScope()

    fun convert() {
        scope.launch {
            when (val res = tool.execute(ScientificNotationInput(numberStr, sigDigits))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { convert() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = numberStr,
            onValueChange = { numberStr = it; convert() },
            label = "Input Number",
            placeholder = "e.g. 1234500000 or 0.0000456"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("1234500000", "0.0000456", "300000000", "0.000000001", "602200000000000000000000").forEach { sample ->
                FilterChip(
                    selected = numberStr == sample,
                    onClick = { numberStr = sample; convert() },
                    label = { Text(sample.take(8)) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 8. DENOMINATION CALCULATOR UI
// ----------------------------------------------------
@Composable
fun DenominationCalculatorToolUI(
    tool: DenominationCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var amountStr by remember { mutableStateOf("387.65") }
    var currency by remember { mutableStateOf(CurrencyPreset.USD) }
    val scope = rememberCoroutineScope()

    fun calculate() {
        val amt = amountStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(DenominationInput(amt, currency))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { calculate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CurrencyPreset.entries.forEach { curr ->
                FilterChip(
                    selected = currency == curr,
                    onClick = { currency = curr; calculate() },
                    label = { Text("${curr.symbol} ${curr.name}") }
                )
            }
        }

        ToolInputField(
            value = amountStr,
            onValueChange = { amountStr = it; calculate() },
            label = "Cash Amount (${currency.symbol})",
            placeholder = "e.g. 387.65"
        )
    }
}

// ----------------------------------------------------
// 9. PRIME FACTORIZATION UI
// ----------------------------------------------------
@Composable
fun PrimeFactorizationToolUI(
    tool: PrimeFactorizationTool,
    onOutputChange: (String) -> Unit
) {
    var numStr by remember { mutableStateOf("360") }
    val scope = rememberCoroutineScope()

    fun factorize() {
        val n = numStr.toLongOrNull() ?: 360L
        scope.launch {
            when (val res = tool.execute(PrimeFactorInput(n))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { factorize() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = numStr,
            onValueChange = { numStr = it; factorize() },
            label = "Positive Integer",
            placeholder = "e.g. 360 or 982451653"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("360", "1024", "997", "123456", "104729").forEach { sample ->
                FilterChip(
                    selected = numStr == sample,
                    onClick = { numStr = sample; factorize() },
                    label = { Text(sample) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 10. TEXT WRAP UI
// ----------------------------------------------------
@Composable
fun TextWrapToolUI(
    tool: TextWrapTool,
    onOutputChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("AZR is an offline Android toolbox built with Jetpack Compose Material 3 and designed to provide privacy-first utilities without network permissions.") }
    var width by remember { mutableIntStateOf(40) }
    val scope = rememberCoroutineScope()

    fun wrap() {
        scope.launch {
            when (val res = tool.execute(TextWrapInput(text = text, columnWidth = width))) {
                is ToolResult.Success -> onOutputChange(res.data.wrappedText)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { wrap() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Column Width: $width characters", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 40, 60, 72, 80).forEach { w ->
                FilterChip(
                    selected = width == w,
                    onClick = { width = w; wrap() },
                    label = { Text("$w chars") }
                )
            }
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it; wrap() },
            label = "Text to Wrap",
            minLines = 4
        )
    }
}

// ----------------------------------------------------
// 11. ZALGO TEXT UI
// ----------------------------------------------------
@Composable
fun ZalgoTextToolUI(
    tool: ZalgoTextTool,
    onOutputChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("HE COMES TO DESTROY BUGS") }
    var mode by remember { mutableStateOf(ZalgoMode.CORRUPT_ZALGO) }
    var intensity by remember { mutableStateOf(ZalgoIntensity.MEDIUM) }
    val scope = rememberCoroutineScope()

    fun transform() {
        scope.launch {
            when (val res = tool.execute(ZalgoInput(text = text, mode = mode, intensity = intensity))) {
                is ToolResult.Success -> onOutputChange(res.data.resultText)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { transform() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == ZalgoMode.CORRUPT_ZALGO, onClick = { mode = ZalgoMode.CORRUPT_ZALGO; transform() }, label = { Text("Corrupt Zalgo") })
            FilterChip(selected = mode == ZalgoMode.CLEAN_ZALGO, onClick = { mode = ZalgoMode.CLEAN_ZALGO; transform() }, label = { Text("Sanitize (Clean)") })
        }

        if (mode == ZalgoMode.CORRUPT_ZALGO) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZalgoIntensity.entries.forEach { lvl ->
                    FilterChip(
                        selected = intensity == lvl,
                        onClick = { intensity = lvl; transform() },
                        label = { Text(lvl.name) }
                    )
                }
            }
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it; transform() },
            label = "Input Text",
            minLines = 3
        )
    }
}

// ----------------------------------------------------
// 12. ANAGRAM SOLVER UI
// ----------------------------------------------------
@Composable
fun AnagramSolverToolUI(
    tool: AnagramSolverTool,
    onOutputChange: (String) -> Unit
) {
    var word1 by remember { mutableStateOf("listen") }
    var word2 by remember { mutableStateOf("silent") }
    val scope = rememberCoroutineScope()

    fun solve() {
        scope.launch {
            when (val res = tool.execute(AnagramInput(word1, word2))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { solve() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolInputField(value = word1, onValueChange = { word1 = it; solve() }, label = "Primary Text", modifier = Modifier.weight(1f))
            ToolInputField(value = word2, onValueChange = { word2 = it; solve() }, label = "Secondary Text", modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(Pair("listen", "silent"), Pair("debit card", "bad credit"), Pair("racecar", "racecar")).forEach { (p1, p2) ->
                FilterChip(
                    selected = word1 == p1 && word2 == p2,
                    onClick = { word1 = p1; word2 = p2; solve() },
                    label = { Text("$p1 / $p2") }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 13. FREQUENCY TO NOTE UI
// ----------------------------------------------------
@Composable
fun FrequencyToNoteToolUI(
    tool: FrequencyToNoteTool,
    onOutputChange: (String) -> Unit
) {
    var freqStr by remember { mutableStateOf("440.0") }
    val scope = rememberCoroutineScope()

    fun tune() {
        val f = freqStr.toDoubleOrNull() ?: 440.0
        scope.launch {
            when (val res = tool.execute(FrequencyNoteInput(f))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { tune() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = freqStr,
            onValueChange = { freqStr = it; tune() },
            label = "Acoustic Frequency (Hz)",
            placeholder = "e.g. 440.0 or 261.63"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            mapOf("A4 (440Hz)" to "440.0", "C4 (261.6Hz)" to "261.63", "E4 (329.6Hz)" to "329.63", "G4 (392Hz)" to "392.0").forEach { (label, hz) ->
                FilterChip(
                    selected = freqStr == hz,
                    onClick = { freqStr = hz; tune() },
                    label = { Text(label) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 14. DPI DENSITY CALCULATOR UI
// ----------------------------------------------------
@Composable
fun DpiDensityCalculatorToolUI(
    tool: DpiDensityCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var widthStr by remember { mutableStateOf("1080") }
    var heightStr by remember { mutableStateOf("2400") }
    var diagStr by remember { mutableStateOf("6.5") }
    val scope = rememberCoroutineScope()

    fun compute() {
        val w = widthStr.toIntOrNull() ?: 1080
        val h = heightStr.toIntOrNull() ?: 2400
        val d = diagStr.toDoubleOrNull() ?: 6.5
        scope.launch {
            when (val res = tool.execute(DpiDensityInput(w, h, d))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { compute() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolInputField(value = widthStr, onValueChange = { widthStr = it; compute() }, label = "Width (px)", modifier = Modifier.weight(1f))
            ToolInputField(value = heightStr, onValueChange = { heightStr = it; compute() }, label = "Height (px)", modifier = Modifier.weight(1f))
            ToolInputField(value = diagStr, onValueChange = { diagStr = it; compute() }, label = "Diagonal (\")", modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("1080x2400 (6.5\")", "1440x3120 (6.7\")", "720x1600 (6.5\")", "1920x1080 (24\")").forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = {
                        val parts = preset.split(" ")
                        val dims = parts[0].split("x")
                        widthStr = dims[0]
                        heightStr = dims[1]
                        diagStr = parts[1].replace("(", "").replace("\")", "")
                        compute()
                    },
                    label = { Text(preset) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 15. HTML COLOR NAME MATCHER UI
// ----------------------------------------------------
@Composable
fun HtmlColorNameToolUI(
    tool: HtmlColorNameTool,
    onOutputChange: (String) -> Unit
) {
    var query by remember { mutableStateOf("#3B82F6") }
    val scope = rememberCoroutineScope()

    fun match() {
        scope.launch {
            when (val res = tool.execute(HtmlColorNameInput(query))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { match() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = query,
            onValueChange = { query = it; match() },
            label = "Hex Code or Named Color",
            placeholder = "e.g. #3B82F6 or coral or rebeccapurple"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("#3B82F6", "#FF7F50", "#663399", "#2E8B57", "#4682B4", "#DC143C").forEach { sample ->
                FilterChip(
                    selected = query.equals(sample, ignoreCase = true),
                    onClick = { query = sample; match() },
                    label = { Text(sample) }
                )
            }
        }
    }
}
