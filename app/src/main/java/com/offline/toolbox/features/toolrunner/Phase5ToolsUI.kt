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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offline.toolbox.core.designsystem.components.ToolInputField
import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorPaletteGeneratorTool
import com.offline.toolbox.tools.color.ColorPaletteInput
import com.offline.toolbox.tools.color.PaletteHarmony
import com.offline.toolbox.tools.data.CsvStatsInput
import com.offline.toolbox.tools.data.CsvStatsTool
import com.offline.toolbox.tools.data.YamlJsonInput
import com.offline.toolbox.tools.data.YamlJsonMode
import com.offline.toolbox.tools.data.YamlToJsonTool
import com.offline.toolbox.tools.developer.CronExpressionTool
import com.offline.toolbox.tools.developer.CronInput
import com.offline.toolbox.tools.developer.MacAddressInput
import com.offline.toolbox.tools.developer.MacAddressTool
import com.offline.toolbox.tools.developer.PipelineChainInput
import com.offline.toolbox.tools.developer.PipelineChainingTool
import com.offline.toolbox.tools.developer.SubnetCalculatorTool
import com.offline.toolbox.tools.developer.SubnetInput
import com.offline.toolbox.tools.math.CourseEntry
import com.offline.toolbox.tools.math.FuelCostCalculatorTool
import com.offline.toolbox.tools.math.FuelCostInput
import com.offline.toolbox.tools.math.FuelUnit
import com.offline.toolbox.tools.math.GpaCalculatorTool
import com.offline.toolbox.tools.math.GpaInput
import com.offline.toolbox.tools.math.GpaScale
import com.offline.toolbox.tools.media.AspectRatioCalculatorTool
import com.offline.toolbox.tools.media.AspectRatioInput
import com.offline.toolbox.tools.security.HmacAlgorithm
import com.offline.toolbox.tools.security.HmacGeneratorTool
import com.offline.toolbox.tools.security.HmacInput
import com.offline.toolbox.tools.security.KeyEncoding
import com.offline.toolbox.tools.security.RsaKeyPairInput
import com.offline.toolbox.tools.security.RsaKeyPairTool
import com.offline.toolbox.tools.security.RsaKeySize
import com.offline.toolbox.tools.text.LeetInput
import com.offline.toolbox.tools.text.LeetLevel
import com.offline.toolbox.tools.text.LeetMode
import com.offline.toolbox.tools.text.LeetspeakTool
import com.offline.toolbox.tools.text.TextBinaryHexInput
import com.offline.toolbox.tools.text.TextBinaryHexMode
import com.offline.toolbox.tools.text.TextBinaryHexTool
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. SUBNET CALCULATOR UI
// ----------------------------------------------------
@Composable
fun SubnetCalculatorToolUI(
    tool: SubnetCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var ip by remember { mutableStateOf("192.168.1.100") }
    var prefixLength by remember { mutableIntStateOf(24) }
    val scope = rememberCoroutineScope()

    fun calculate() {
        scope.launch {
            when (val res = tool.execute(SubnetInput(ip, prefixLength))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Network Address:      ${d.networkAddress}")
                        appendLine("Broadcast Address:    ${d.broadcastAddress}")
                        appendLine("Subnet Mask:          ${d.subnetMask}")
                        appendLine("Wildcard Mask:        ${d.wildcardMask}")
                        appendLine("Usable Host Range:    ${d.firstUsableIp} - ${d.lastUsableIp}")
                        appendLine("Total Addresses:      ${d.totalHosts}")
                        appendLine("Usable Hosts:         ${d.usableHosts}")
                        appendLine("IP Class:             ${d.ipClass}")
                        appendLine("Binary Subnet Mask:   ${d.binarySubnetMask}")
                        appendLine("Private Network:      ${if (d.isPrivateIp) "Yes" else "No (Public Internet)"}")
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { calculate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = ip,
            onValueChange = { ip = it; calculate() },
            label = "IPv4 Address",
            placeholder = "e.g. 192.168.1.1"
        )

        Text("Subnet Prefix: /$prefixLength", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = prefixLength.toFloat(),
            onValueChange = { prefixLength = it.toInt(); calculate() },
            valueRange = 1f..32f,
            steps = 30
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(8, 16, 24, 28, 30).forEach { p ->
                FilterChip(
                    selected = prefixLength == p,
                    onClick = { prefixLength = p; calculate() },
                    label = { Text("/$p") }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 2. MAC ADDRESS TOOL UI
// ----------------------------------------------------
@Composable
fun MacAddressToolUI(
    tool: MacAddressTool,
    onOutputChange: (String) -> Unit
) {
    var macInput by remember { mutableStateOf("00:1A:2B:3C:4D:5E") }
    val scope = rememberCoroutineScope()

    fun analyze() {
        scope.launch {
            when (val res = tool.execute(MacAddressInput(macInput))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Colon Format:        ${d.colonFormat}")
                        appendLine("Hyphen Format:       ${d.hyphenFormat}")
                        appendLine("Cisco Dot Format:    ${d.ciscoFormat}")
                        appendLine("Raw Hex:             ${d.rawHex}")
                        appendLine("OUI Vendor:          ${d.vendorOui ?: "Unknown Vendor"}")
                        appendLine("Transmission:        ${if (d.isMulticast) "Multicast" else "Unicast"}")
                        appendLine("Administration:      ${if (d.isLocallyAdministered) "Locally Administered (LAA)" else "Universally Administered (UAA / OUI Enforced)"}")
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { analyze() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = macInput,
            onValueChange = { macInput = it; analyze() },
            label = "MAC Address",
            placeholder = "e.g. 00:1A:2B:3C:4D:5E or 001a2b3c4d5e"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("00:1A:2B:3C:4D:5E", "B8:27:EB:12:34:56", "3C:D9:2B:AA:BB:CC", "FF:FF:FF:FF:FF:FF").forEach { sample ->
                FilterChip(
                    selected = macInput == sample,
                    onClick = { macInput = sample; analyze() },
                    label = { Text(sample.take(8)) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 3. CRON EXPRESSION TOOL UI
// ----------------------------------------------------
@Composable
fun CronExpressionToolUI(
    tool: CronExpressionTool,
    onOutputChange: (String) -> Unit
) {
    var cronExpr by remember { mutableStateOf("*/15 * * * *") }
    val scope = rememberCoroutineScope()

    fun evaluate() {
        scope.launch {
            when (val res = tool.execute(CronInput(cronExpr))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Natural Language Translation:")
                        appendLine(d.humanDescription)
                        appendLine()
                        appendLine("Upcoming 5 Projected Runs:")
                        d.nextRuns.forEachIndexed { i, time ->
                            appendLine("${i + 1}. $time")
                        }
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { evaluate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = cronExpr,
            onValueChange = { cronExpr = it; evaluate() },
            label = "5-Field Cron Expression",
            placeholder = "min hour day month day-of-week (e.g. 0 9 * * 1-5)"
        )

        Text("Quick Presets:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            mapOf(
                "Every 15m" to "*/15 * * * *",
                "Daily 9 AM" to "0 9 * * *",
                "Weekdays 9 AM" to "0 9 * * 1-5",
                "Hourly" to "0 * * * *",
                "Midnight 1st" to "0 0 1 * *"
            ).forEach { (label, expr) ->
                FilterChip(
                    selected = cronExpr == expr,
                    onClick = { cronExpr = expr; evaluate() },
                    label = { Text(label) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 4. HMAC GENERATOR UI
// ----------------------------------------------------
@Composable
fun HmacGeneratorToolUI(
    tool: HmacGeneratorTool,
    onOutputChange: (String) -> Unit
) {
    var message by remember { mutableStateOf("Secret offline authentication payload") }
    var key by remember { mutableStateOf("offline-key-2026") }
    var algorithm by remember { mutableStateOf(HmacAlgorithm.HMAC_SHA256) }
    var keyEncoding by remember { mutableStateOf(KeyEncoding.UTF8_TEXT) }
    val scope = rememberCoroutineScope()

    fun generate() {
        scope.launch {
            when (val res = tool.execute(HmacInput(message, key, algorithm, keyEncoding))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Algorithm:   ${d.algorithm}")
                        appendLine("Key Bits:    ${d.keyBits} bits")
                        appendLine()
                        appendLine("HMAC Hex:")
                        appendLine(d.hexSignature)
                        appendLine()
                        appendLine("HMAC Base64:")
                        appendLine(d.base64Signature)
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { generate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = message,
            onValueChange = { message = it; generate() },
            label = "Message / Payload",
            minLines = 2
        )

        ToolInputField(
            value = key,
            onValueChange = { key = it; generate() },
            label = "Secret Key",
            placeholder = "Enter authentication secret key"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            HmacAlgorithm.entries.forEach { algo ->
                FilterChip(
                    selected = algorithm == algo,
                    onClick = { algorithm = algo; generate() },
                    label = { Text(algo.label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = keyEncoding == KeyEncoding.UTF8_TEXT,
                onClick = { keyEncoding = KeyEncoding.UTF8_TEXT; generate() },
                label = { Text("Plain Text Key") }
            )
            FilterChip(
                selected = keyEncoding == KeyEncoding.HEX_STRING,
                onClick = { keyEncoding = KeyEncoding.HEX_STRING; generate() },
                label = { Text("Hex Key") }
            )
        }
    }
}

// ----------------------------------------------------
// 5. RSA KEY PAIR GENERATOR UI
// ----------------------------------------------------
@Composable
fun RsaKeyPairToolUI(
    tool: RsaKeyPairTool,
    onOutputChange: (String) -> Unit
) {
    var keySize by remember { mutableStateOf(RsaKeySize.RSA_2048) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun generate() {
        isGenerating = true
        onOutputChange("Generating ${keySize.bits}-bit RSA key pair on-device... Please wait...")
        scope.launch {
            when (val res = tool.execute(RsaKeyPairInput(keySize))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Key Size: ${d.keySizeBits} bits (${d.algorithm})")
                        appendLine()
                        appendLine(d.publicKeyPem)
                        appendLine()
                        appendLine(d.privateKeyPem)
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
            isGenerating = false
        }
    }

    LaunchedEffect(Unit) { generate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Key Size (bits):", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RsaKeySize.entries.forEach { size ->
                FilterChip(
                    selected = keySize == size,
                    onClick = { keySize = size; generate() },
                    label = { Text("${size.bits} bit") }
                )
            }
        }

        Button(
            onClick = { generate() },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isGenerating) "Generating Key Pair..." else "Regenerate New RSA Key Pair")
        }
    }
}

// ----------------------------------------------------
// 6. YAML <-> JSON CONVERTER UI
// ----------------------------------------------------
@Composable
fun YamlToJsonToolUI(
    tool: YamlToJsonTool,
    onOutputChange: (String) -> Unit
) {
    var mode by remember { mutableStateOf(YamlJsonMode.YAML_TO_JSON) }
    var content by remember {
        mutableStateOf(
            """
            app:
              name: Offline Toolbox
              version: 1.0.0
              features:
                - zero_network
                - high_performance
                - fully_offline
            """.trimIndent()
        )
    }
    val scope = rememberCoroutineScope()

    fun convert() {
        scope.launch {
            when (val res = tool.execute(YamlJsonInput(content, mode))) {
                is ToolResult.Success -> onOutputChange(res.data.convertedContent)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { convert() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == YamlJsonMode.YAML_TO_JSON,
                onClick = { mode = YamlJsonMode.YAML_TO_JSON; convert() },
                label = { Text("YAML to JSON") }
            )
            FilterChip(
                selected = mode == YamlJsonMode.JSON_TO_YAML,
                onClick = { mode = YamlJsonMode.JSON_TO_YAML; convert() },
                label = { Text("JSON to YAML") }
            )
        }

        ToolInputField(
            value = content,
            onValueChange = { content = it; convert() },
            label = if (mode == YamlJsonMode.YAML_TO_JSON) "YAML Input" else "JSON Input",
            minLines = 6
        )
    }
}

// ----------------------------------------------------
// 7. CSV STATS & PROFILER UI
// ----------------------------------------------------
@Composable
fun CsvStatsToolUI(
    tool: CsvStatsTool,
    onOutputChange: (String) -> Unit
) {
    var csvText by remember {
        mutableStateOf(
            """
            Name,Age,Salary,Department
            Alice,29,75000,Engineering
            Bob,34,82000,Design
            Charlie,29,69000,Engineering
            Diana,41,96000,Management
            Evan,25,54000,Marketing
            """.trimIndent()
        )
    }
    val scope = rememberCoroutineScope()

    fun profile() {
        scope.launch {
            when (val res = tool.execute(CsvStatsInput(csvText))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Total Rows:    ${d.totalRowCount}")
                        appendLine("Total Columns: ${d.totalColumnCount}")
                        appendLine()
                        appendLine("Columnar Statistical Profiles:")
                        appendLine("--------------------------------------------------")
                        d.columns.forEach { col ->
                            appendLine("Column: [${col.name}] (Type: ${col.inferredType})")
                            appendLine("  Non-Null: ${col.nonNullCount} | Nulls: ${col.nullCount} | Unique: ${col.uniqueValuesCount}")
                            if (col.minNumeric != null) {
                                appendLine("  Min: ${col.minNumeric} | Max: ${col.maxNumeric} | Avg: ${col.meanNumeric}")
                            }
                            appendLine()
                        }
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { profile() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = csvText,
            onValueChange = { csvText = it; profile() },
            label = "CSV Tabular Data",
            minLines = 5
        )

        Button(onClick = { profile() }, modifier = Modifier.fillMaxWidth()) {
            Text("Analyze CSV Statistics")
        }
    }
}

// ----------------------------------------------------
// 8. COLOR PALETTE GENERATOR UI
// ----------------------------------------------------
@Composable
fun ColorPaletteGeneratorToolUI(
    tool: ColorPaletteGeneratorTool,
    onOutputChange: (String) -> Unit
) {
    var baseHex by remember { mutableStateOf("#3B82F6") }
    var harmony by remember { mutableStateOf(PaletteHarmony.COMPLEMENTARY) }
    val scope = rememberCoroutineScope()

    fun generate() {
        scope.launch {
            when (val res = tool.execute(ColorPaletteInput(baseHex, harmony))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Base Color:    ${d.baseColor.hex}")
                        appendLine("Harmony Model: ${d.harmony.label}")
                        appendLine()
                        appendLine("Generated Palette (${d.palette.size} Shades):")
                        d.palette.forEachIndexed { i, c ->
                            appendLine("${i + 1}. ${c.hex} | RGB${c.rgb} | HSL(${c.hsl.first.toInt()}°, ${(c.hsl.second * 100).toInt()}%, ${(c.hsl.third * 100).toInt()}%) | Light: ${c.isLight}")
                        }
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { generate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = baseHex,
            onValueChange = { baseHex = it; generate() },
            label = "Base Color Hex",
            placeholder = "#3B82F6"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            PaletteHarmony.entries.forEach { harm ->
                FilterChip(
                    selected = harmony == harm,
                    onClick = { harmony = harm; generate() },
                    label = { Text(harm.name.replace("_", " ")) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("#3B82F6", "#10B981", "#EF4444", "#F59E0B", "#8B5CF6", "#EC4899").forEach { sampleHex ->
                FilterChip(
                    selected = baseHex.equals(sampleHex, ignoreCase = true),
                    onClick = { baseHex = sampleHex; generate() },
                    label = { Text(sampleHex) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 9. ASPECT RATIO CALCULATOR UI
// ----------------------------------------------------
@Composable
fun AspectRatioToolUI(
    tool: AspectRatioCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var widthStr by remember { mutableStateOf("1920") }
    var heightStr by remember { mutableStateOf("1080") }
    var newWidthStr by remember { mutableStateOf("1280") }
    val scope = rememberCoroutineScope()

    fun calculate() {
        val w = widthStr.toIntOrNull() ?: 1920
        val h = heightStr.toIntOrNull() ?: 1080
        val nw = newWidthStr.toIntOrNull()
        scope.launch {
            when (val res = tool.execute(AspectRatioInput(w, h, nw))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Simplified Ratio:    ${d.ratioSimplified}")
                        appendLine("Decimal Ratio:       ${d.decimalRatio}")
                        appendLine("Standard Category:   ${d.standardMatch}")
                        if (d.scaledDimensions != null) {
                            appendLine()
                            appendLine("Scaled Proportions:")
                            appendLine("${d.scaledDimensions.first} x ${d.scaledDimensions.second}")
                        }
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { calculate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolInputField(
                value = widthStr,
                onValueChange = { widthStr = it; calculate() },
                label = "Width (px)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = heightStr,
                onValueChange = { heightStr = it; calculate() },
                label = "Height (px)",
                modifier = Modifier.weight(1f)
            )
        }

        ToolInputField(
            value = newWidthStr,
            onValueChange = { newWidthStr = it; calculate() },
            label = "Target Width for Scaling (Optional)",
            placeholder = "e.g. 1280"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("1920x1080", "1280x720", "3840x2160", "2560x1440", "1080x1080", "1080x1920").forEach { dim ->
                val parts = dim.split("x")
                FilterChip(
                    selected = widthStr == parts[0] && heightStr == parts[1],
                    onClick = { widthStr = parts[0]; heightStr = parts[1]; calculate() },
                    label = { Text(dim) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 10. FUEL COST CALCULATOR UI
// ----------------------------------------------------
@Composable
fun FuelCostToolUI(
    tool: FuelCostCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var distanceStr by remember { mutableStateOf("350") }
    var efficiencyStr by remember { mutableStateOf("7.5") }
    var priceStr by remember { mutableStateOf("1.45") }
    var passengersStr by remember { mutableStateOf("2") }
    val scope = rememberCoroutineScope()

    fun calculate() {
        val dist = distanceStr.toDoubleOrNull() ?: 350.0
        val eff = efficiencyStr.toDoubleOrNull() ?: 7.5
        val price = priceStr.toDoubleOrNull() ?: 1.45
        val pass = passengersStr.toIntOrNull() ?: 1
        scope.launch {
            when (val res = tool.execute(FuelCostInput(dist, eff, FuelUnit.METRIC_L_PER_100KM, price, pass))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Total Fuel Consumed: ${d.totalFuelConsumed} L")
                        appendLine("Total Fuel Cost:     $${d.totalTripCost}")
                        appendLine("Cost Per Km:         $${d.costPerDistanceUnit}")
                        appendLine("Cost Per Passenger:  $${d.costPerPassenger}")
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { calculate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolInputField(
                value = distanceStr,
                onValueChange = { distanceStr = it; calculate() },
                label = "Distance (km)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = efficiencyStr,
                onValueChange = { efficiencyStr = it; calculate() },
                label = "Efficiency (L/100km)",
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolInputField(
                value = priceStr,
                onValueChange = { priceStr = it; calculate() },
                label = "Fuel Price ($ / L)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = passengersStr,
                onValueChange = { passengersStr = it; calculate() },
                label = "Passengers",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ----------------------------------------------------
// 11. GPA CALCULATOR UI
// ----------------------------------------------------
@Composable
fun GpaCalculatorToolUI(
    tool: GpaCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var scale by remember { mutableStateOf(GpaScale.SCALE_4_0) }
    var courseGrade1 by remember { mutableStateOf("A") }
    var courseCredits1 by remember { mutableStateOf("4") }
    var courseGrade2 by remember { mutableStateOf("B+") }
    var courseCredits2 by remember { mutableStateOf("3") }
    var courseGrade3 by remember { mutableStateOf("A-") }
    var courseCredits3 by remember { mutableStateOf("3") }
    val scope = rememberCoroutineScope()

    fun calculate() {
        val courses = listOf(
            CourseEntry("Course 1", courseCredits1.toDoubleOrNull() ?: 3.0, courseGrade1),
            CourseEntry("Course 2", courseCredits2.toDoubleOrNull() ?: 3.0, courseGrade2),
            CourseEntry("Course 3", courseCredits3.toDoubleOrNull() ?: 3.0, courseGrade3)
        )
        scope.launch {
            when (val res = tool.execute(GpaInput(courses, scale))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Semester GPA:       ${d.semesterGpa}")
                        appendLine("Cumulative CGPA:    ${d.cumulativeGpa}")
                        appendLine("Semester Credits:   ${d.semesterCredits}")
                        appendLine("Total Credits:      ${d.totalCreditsOverall}")
                        appendLine("Honors Status:      ${d.honorsStatus}")
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { calculate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GpaScale.entries.forEach { sc ->
                FilterChip(
                    selected = scale == sc,
                    onClick = { scale = sc; calculate() },
                    label = { Text(sc.label.take(16)) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolInputField(value = courseGrade1, onValueChange = { courseGrade1 = it; calculate() }, label = "Course 1 Grade", modifier = Modifier.weight(1f))
            ToolInputField(value = courseCredits1, onValueChange = { courseCredits1 = it; calculate() }, label = "Credits", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolInputField(value = courseGrade2, onValueChange = { courseGrade2 = it; calculate() }, label = "Course 2 Grade", modifier = Modifier.weight(1f))
            ToolInputField(value = courseCredits2, onValueChange = { courseCredits2 = it; calculate() }, label = "Credits", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolInputField(value = courseGrade3, onValueChange = { courseGrade3 = it; calculate() }, label = "Course 3 Grade", modifier = Modifier.weight(1f))
            ToolInputField(value = courseCredits3, onValueChange = { courseCredits3 = it; calculate() }, label = "Credits", modifier = Modifier.weight(1f))
        }
    }
}

// ----------------------------------------------------
// 12. TEXT BINARY & HEX TOOL UI
// ----------------------------------------------------
@Composable
fun TextBinaryHexToolUI(
    tool: TextBinaryHexTool,
    onOutputChange: (String) -> Unit
) {
    var input by remember { mutableStateOf("Offline Android Toolbox") }
    var mode by remember { mutableStateOf(TextBinaryHexMode.TEXT_TO_ALL) }
    val scope = rememberCoroutineScope()

    fun runConvert() {
        scope.launch {
            when (val res = tool.execute(TextBinaryHexInput(input, mode))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Text:          ${d.text}")
                        appendLine("Byte Count:    ${d.byteCount} bytes (${d.charCount} chars)")
                        appendLine()
                        appendLine("8-bit Binary:")
                        appendLine(d.binary)
                        appendLine()
                        appendLine("Hex (Spaced):")
                        appendLine(d.hexSpaced)
                        appendLine()
                        appendLine("Hex (Continuous):")
                        appendLine(d.hexContinuous)
                        appendLine()
                        appendLine("Hex (0x Prefixed):")
                        appendLine(d.hexPrefixed)
                        appendLine()
                        appendLine("Decimal Bytes:")
                        appendLine(d.decimal)
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runConvert() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TextBinaryHexMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m; runConvert() },
                    label = { Text(m.name.replace("_", " ")) }
                )
            }
        }

        ToolInputField(
            value = input,
            onValueChange = { input = it; runConvert() },
            label = "Input Data",
            minLines = 3
        )
    }
}

// ----------------------------------------------------
// 13. LEETSPEAK GENERATOR UI
// ----------------------------------------------------
@Composable
fun LeetspeakToolUI(
    tool: LeetspeakTool,
    onOutputChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("Antigravity Android Offline Toolbox 2026") }
    var level by remember { mutableStateOf(LeetLevel.BASIC) }
    var mode by remember { mutableStateOf(LeetMode.ENCODE) }
    val scope = rememberCoroutineScope()

    fun transform() {
        scope.launch {
            when (val res = tool.execute(LeetInput(text, level, mode))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine(d.resultText)
                        appendLine()
                        appendLine("Substituted Characters: ${d.substitutedCount} (${d.substitutionPercentage}%)")
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { transform() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == LeetMode.ENCODE, onClick = { mode = LeetMode.ENCODE; transform() }, label = { Text("Encode") })
            FilterChip(selected = mode == LeetMode.DECODE, onClick = { mode = LeetMode.DECODE; transform() }, label = { Text("Decode") })
        }

        if (mode == LeetMode.ENCODE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LeetLevel.entries.forEach { lvl ->
                    FilterChip(
                        selected = level == lvl,
                        onClick = { level = lvl; transform() },
                        label = { Text(lvl.name.replace("_", " ")) }
                    )
                }
            }
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it; transform() },
            label = "Source Text",
            minLines = 3
        )
    }
}

// ----------------------------------------------------
// 14. MULTI-TOOL PIPELINE CHAINER UI
// ----------------------------------------------------
@Composable
fun PipelineChainingToolUI(
    tool: PipelineChainingTool,
    onOutputChange: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("   Offline Utility Suite 2026 - Premium Fast & Secure   ") }
    var selectedRecipeId by remember { mutableStateOf("clean_slug") }
    val scope = rememberCoroutineScope()

    fun runPipeline() {
        scope.launch {
            when (val res = tool.execute(PipelineChainInput(inputText, selectedRecipeId))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Pipeline Execution: SUCCESS (${d.executionTimeMs} ms)")
                        appendLine("==================================================")
                        appendLine("Final Transformed Output:")
                        appendLine(d.finalOutput)
                        appendLine()
                        appendLine("Step-by-Step Intermediate Audit Trail:")
                        d.fullResult.stepResults.forEach { step ->
                            appendLine("  [Step ${step.stepIndex}] ${step.toolName} (${step.durationMs}ms)")
                            appendLine("    → Input:  \"${step.inputData.take(30)}\"")
                            appendLine("    → Output: \"${step.outputData.take(30)}\"")
                        }
                    }
                    onOutputChange(out)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runPipeline() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Pipeline Recipe:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            mapOf(
                "clean_slug" to "Clean & Slugify",
                "hacker_obfuscate" to "Hacker Obfuscate",
                "dev_payload_hash" to "Dev Checksum",
                "morse_secret" to "Morse & Hex"
            ).forEach { (id, label) ->
                FilterChip(
                    selected = selectedRecipeId == id,
                    onClick = { selectedRecipeId = id; runPipeline() },
                    label = { Text(label) }
                )
            }
        }

        ToolInputField(
            value = inputText,
            onValueChange = { inputText = it; runPipeline() },
            label = "Pipeline Input Payload",
            minLines = 3
        )

        Button(onClick = { runPipeline() }, modifier = Modifier.fillMaxWidth()) {
            Text("Execute Multi-Tool Pipeline")
        }
    }
}
