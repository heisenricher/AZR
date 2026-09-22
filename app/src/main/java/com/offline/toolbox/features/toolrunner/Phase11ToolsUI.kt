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
import com.offline.toolbox.tools.color.ColorDeltaE2000Tool
import com.offline.toolbox.tools.color.DeltaEInput
import com.offline.toolbox.tools.data.BencodeInput
import com.offline.toolbox.tools.data.BencodeParserTool
import com.offline.toolbox.tools.data.WktGeometryInput
import com.offline.toolbox.tools.data.WktGeometryParserTool
import com.offline.toolbox.tools.developer.HmacTotpUriBuilderTool
import com.offline.toolbox.tools.developer.HmacTotpUriInput
import com.offline.toolbox.tools.developer.NginxConfigValidatorTool
import com.offline.toolbox.tools.developer.NginxValidatorInput
import com.offline.toolbox.tools.developer.SqlitePragmaInput
import com.offline.toolbox.tools.developer.SqlitePragmaInspectorTool
import com.offline.toolbox.tools.developer.SqliteTuningProfile
import com.offline.toolbox.tools.developer.TotpUriOperationMode
import com.offline.toolbox.tools.developer.UnixUmaskCalculatorTool
import com.offline.toolbox.tools.developer.UnixUmaskInput
import com.offline.toolbox.tools.math.BondYieldToMaturityTool
import com.offline.toolbox.tools.math.BondYtmInput
import com.offline.toolbox.tools.math.KinematicsInput
import com.offline.toolbox.tools.math.KinematicsTrajectoryTool
import com.offline.toolbox.tools.math.MonteCarloPiInput
import com.offline.toolbox.tools.math.MonteCarloPiSimulatorTool
import com.offline.toolbox.tools.media.ReverbRt60AcousticTool
import com.offline.toolbox.tools.media.ReverbRt60Input
import com.offline.toolbox.tools.security.Bip39MnemonicEntropyTool
import com.offline.toolbox.tools.security.Bip39ValidatorInput
import com.offline.toolbox.tools.security.OtpOperationMode
import com.offline.toolbox.tools.security.VernamOneTimePadTool
import com.offline.toolbox.tools.security.VernamOtpInput
import com.offline.toolbox.tools.text.LevenshteinMatrixInput
import com.offline.toolbox.tools.text.LevenshteinMatrixVisualizerTool
import com.offline.toolbox.tools.text.RleInput
import com.offline.toolbox.tools.text.RunLengthEncodingTool
import kotlinx.coroutines.launch

// 1. Nginx Config Validator UI
@Composable
fun NginxConfigValidatorUI(onResultUpdated: (String, String?) -> Unit) {
    var configText by remember {
        mutableStateOf(
            """server {
    listen 80;
    server_name example.com;
    return 301 https://${'$'}host${'$'}request_uri;
}

server {
    listen 443 ssl http2;
    server_name example.com;
    ssl_certificate /etc/ssl/cert.pem;
    ssl_certificate_key /etc/ssl/key.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host ${'$'}host;
        proxy_set_header X-Real-IP ${'$'}remote_addr;
    }
}"""
        )
    }
    val tool = remember { NginxConfigValidatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(NginxValidatorInput(configText))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Validation Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = {
                    configText = """server {
    listen 80;
    server_name localhost;
    location / {
        root /var/www/html;
        index index.html index.htm;
    }
}"""
                    run()
                },
                label = { Text("Static Web") }
            )
            FilterChip(
                selected = false,
                onClick = {
                    configText = """server {
    listen 443 ssl;
    server_name api.example.com;
    ssl_certificate /etc/ssl/cert.pem;
    ssl_certificate_key /etc/ssl/key.pem;

    location /api/ {
        proxy_pass http://backend_cluster;
        proxy_set_header X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
        proxy_set_header Host ${'$'}http_host;
    }
}"""
                    run()
                },
                label = { Text("Reverse Proxy") }
            )
        }

        ToolInputField(
            value = configText,
            onValueChange = { configText = it },
            label = "Nginx Configuration Script",
            minLines = 6,
            maxLines = 14
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Validate Nginx Configuration")
        }
    }
}

// 2. SQLite PRAGMA Inspector UI
@Composable
fun SqlitePragmaInspectorUI(onResultUpdated: (String, String?) -> Unit) {
    var dbSizeMb by remember { mutableDoubleStateOf(128.0) }
    var readers by remember { mutableIntStateOf(10) }
    var writers by remember { mutableIntStateOf(2) }
    var durability by remember { mutableStateOf("NORMAL") }

    val tool = remember { SqlitePragmaInspectorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val prof = when (durability) {
                "NORMAL" -> SqliteTuningProfile.ANDROID_ROOM_RECOMMENDED
                "FULL" -> SqliteTuningProfile.MAX_DURABILITY_FINANCIAL
                "OFF" -> SqliteTuningProfile.IN_MEMORY_FAST_INGESTION
                else -> SqliteTuningProfile.CUSTOM
            }
            when (val res = tool.execute(SqlitePragmaInput(profile = prof, customCacheSizeKiB = (dbSizeMb * 1024).toInt()))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Inspection Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = durability == "NORMAL",
                onClick = { durability = "NORMAL"; run() },
                label = { Text("WAL Normal (Fast)") }
            )
            FilterChip(
                selected = durability == "FULL",
                onClick = { durability = "FULL"; run() },
                label = { Text("Full Durability") }
            )
            FilterChip(
                selected = durability == "OFF",
                onClick = { durability = "OFF"; run() },
                label = { Text("Off (In-Memory)") }
            )
        }

        ToolInputField(
            value = dbSizeMb.toString(),
            onValueChange = { dbSizeMb = it.toDoubleOrNull() ?: 10.0 },
            label = "Estimated DB Size (MB)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = readers.toString(),
            onValueChange = { readers = it.toIntOrNull() ?: 1 },
            label = "Concurrent Readers",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = writers.toString(),
            onValueChange = { writers = it.toIntOrNull() ?: 1 },
            label = "Concurrent Writers",
            minLines = 1,
            maxLines = 1
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Generate SQLite PRAGMA Script")
        }
    }
}

// 3. HMAC TOTP URI Builder UI
@Composable
fun HmacTotpUriBuilderUI(onResultUpdated: (String, String?) -> Unit) {
    var account by remember { mutableStateOf("user@example.com") }
    var issuer by remember { mutableStateOf("OfflineToolbox") }
    var secret by remember { mutableStateOf("JBSWY3DPEHPK3PXP") }
    var digits by remember { mutableIntStateOf(6) }
    var period by remember { mutableIntStateOf(30) }

    val tool = remember { HmacTotpUriBuilderTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val input = HmacTotpUriInput(
                mode = TotpUriOperationMode.BUILD_KEY_URI,
                accountName = account,
                issuer = issuer,
                base32Secret = secret,
                algorithm = "SHA1",
                digits = digits,
                periodSeconds = period
            )
            when (val res = tool.execute(input)) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Build Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = digits == 6 && period == 30,
                onClick = { digits = 6; period = 30; run() },
                label = { Text("Standard Google Auth (6-digit, 30s)") }
            )
            FilterChip(
                selected = digits == 8 && period == 60,
                onClick = { digits = 8; period = 60; run() },
                label = { Text("Extended (8-digit, 60s)") }
            )
        }

        ToolInputField(
            value = account,
            onValueChange = { account = it },
            label = "Account / User",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = issuer,
            onValueChange = { issuer = it },
            label = "Issuer / Organization",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = secret,
            onValueChange = { secret = it },
            label = "Base32 Shared Secret",
            minLines = 1,
            maxLines = 2
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Build TOTP Key URI")
        }
    }
}

// 4. Unix Umask Calculator UI
@Composable
fun UnixUmaskCalculatorUI(onResultUpdated: (String, String?) -> Unit) {
    var umask by remember { mutableStateOf("022") }

    val tool = remember { UnixUmaskCalculatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(UnixUmaskInput(umask))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Calculation Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = umask == "022",
                onClick = { umask = "022"; run() },
                label = { Text("022 (Standard)") }
            )
            FilterChip(
                selected = umask == "027",
                onClick = { umask = "027"; run() },
                label = { Text("027 (Group Read)") }
            )
            FilterChip(
                selected = umask == "077",
                onClick = { umask = "077"; run() },
                label = { Text("077 (Private)") }
            )
        }

        ToolInputField(
            value = umask,
            onValueChange = { umask = it },
            label = "Octal Umask (e.g. 022, 027, 077)",
            minLines = 1,
            maxLines = 1
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Calculate File & Directory Permissions")
        }
    }
}

// 5. Vernam One-Time Pad UI
@Composable
fun VernamOneTimePadUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("CONFIDENTIAL TRANSMISSION") }
    var keyHex by remember { mutableStateOf("") }
    var operation by remember { mutableStateOf("ENCRYPT") }

    val tool = remember { VernamOneTimePadTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val mode = if (operation == "ENCRYPT") {
                if (keyHex.isBlank()) OtpOperationMode.ENCRYPT_GENERATE_KEY else OtpOperationMode.ENCRYPT_WITH_KEY
            } else {
                OtpOperationMode.DECRYPT_WITH_KEY
            }
            when (val res = tool.execute(VernamOtpInput(content = text, keyOrPadHex = keyHex, mode = mode))) {
                is ToolResult.Success -> {
                    keyHex = res.data.padHex
                    onResultUpdated(res.data.formattedReport, res.data.summary)
                }
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "OTP Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = operation == "ENCRYPT",
                onClick = { operation = "ENCRYPT"; run() },
                label = { Text("Encrypt & Generate Key") }
            )
            FilterChip(
                selected = operation == "DECRYPT",
                onClick = { operation = "DECRYPT"; run() },
                label = { Text("Decrypt with Key") }
            )
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it },
            label = if (operation == "ENCRYPT") "Plaintext to Encrypt" else "Hex Ciphertext to Decrypt",
            minLines = 2,
            maxLines = 4
        )
        ToolInputField(
            value = keyHex,
            onValueChange = { keyHex = it },
            label = "Hex One-Time Pad Key (Empty to auto-generate)",
            minLines = 2,
            maxLines = 4
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (operation == "ENCRYPT") "Encrypt with One-Time Pad" else "Decrypt with One-Time Pad")
        }
    }
}

// 6. BIP-39 Mnemonic Seed & Entropy Validator UI
@Composable
fun Bip39MnemonicEntropyUI(onResultUpdated: (String, String?) -> Unit) {
    var phrase by remember {
        mutableStateOf("abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about")
    }

    val tool = remember { Bip39MnemonicEntropyTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(Bip39ValidatorInput(phrase))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Invalid Seed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = {
                    phrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
                    run()
                },
                label = { Text("12-Word Standard Vector") }
            )
            FilterChip(
                selected = false,
                onClick = {
                    phrase = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong"
                    run()
                },
                label = { Text("Zoo Vector") }
            )
        }

        ToolInputField(
            value = phrase,
            onValueChange = { phrase = it },
            label = "BIP-39 Mnemonic Seed Phrase (12, 15, 18, 21, or 24 words)",
            minLines = 3,
            maxLines = 6
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Validate Seed Phrase & Entropy")
        }
    }
}

// 7. WKT Geometry Parser UI
@Composable
fun WktGeometryParserUI(onResultUpdated: (String, String?) -> Unit) {
    var wkt by remember { mutableStateOf("POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))") }

    val tool = remember { WktGeometryParserTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(WktGeometryInput(wkt))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "WKT Parse Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = { wkt = "POINT (30 10)"; run() },
                label = { Text("Point") }
            )
            FilterChip(
                selected = false,
                onClick = { wkt = "LINESTRING (30 10, 10 30, 40 40)"; run() },
                label = { Text("LineString") }
            )
            FilterChip(
                selected = false,
                onClick = { wkt = "POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))"; run() },
                label = { Text("Polygon") }
            )
            FilterChip(
                selected = false,
                onClick = { wkt = "MULTIPOINT ((10 40), (40 30), (20 20), (30 10))"; run() },
                label = { Text("MultiPoint") }
            )
        }

        ToolInputField(
            value = wkt,
            onValueChange = { wkt = it },
            label = "OGC Well-Known Text (WKT) String",
            minLines = 3,
            maxLines = 6
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Parse WKT & Convert to GeoJSON")
        }
    }
}

// 8. BitTorrent Bencode Parser UI
@Composable
fun BencodeParserUI(onResultUpdated: (String, String?) -> Unit) {
    var payload by remember { mutableStateOf("d4:info4:hash4:name6:ubuntu8:piecessi256ee") }

    val tool = remember { BencodeParserTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(BencodeInput("DECODE", payload))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Bencode Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = { payload = "d4:name6:ubuntu8:category4:dist5:peersi128ee"; run() },
                label = { Text("Sample Torrent Dict") }
            )
            FilterChip(
                selected = false,
                onClick = { payload = "l4:spami42e5:helloe"; run() },
                label = { Text("List with String & Int") }
            )
        }

        ToolInputField(
            value = payload,
            onValueChange = { payload = it },
            label = "BitTorrent Bencode Data String",
            minLines = 3,
            maxLines = 6
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Parse Bencode to JSON Tree")
        }
    }
}

// 9. Monte Carlo Pi Simulator UI
@Composable
fun MonteCarloPiSimulatorUI(onResultUpdated: (String, String?) -> Unit) {
    var samples by remember { mutableIntStateOf(100000) }
    var seed by remember { mutableStateOf("42") }

    val tool = remember { MonteCarloPiSimulatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val seedVal = seed.toLongOrNull() ?: 42L
            when (val res = tool.execute(MonteCarloPiInput(samples, seedVal))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Simulation Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = samples == 10000,
                onClick = { samples = 10000; run() },
                label = { Text("10,000 Points") }
            )
            FilterChip(
                selected = samples == 100000,
                onClick = { samples = 100000; run() },
                label = { Text("100,000 Points") }
            )
            FilterChip(
                selected = samples == 500000,
                onClick = { samples = 500000; run() },
                label = { Text("500,000 Points") }
            )
        }

        ToolInputField(
            value = samples.toString(),
            onValueChange = { samples = it.toIntOrNull() ?: 10000 },
            label = "Sample Count (N)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = seed,
            onValueChange = { seed = it },
            label = "Random Seed (0 for system entropy)",
            minLines = 1,
            maxLines = 1
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Simulate Pi with Monte Carlo")
        }
    }
}

// 10. Bond Yield to Maturity UI
@Composable
fun BondYieldToMaturityUI(onResultUpdated: (String, String?) -> Unit) {
    var faceValue by remember { mutableDoubleStateOf(1000.0) }
    var marketPrice by remember { mutableDoubleStateOf(950.0) }
    var couponRate by remember { mutableDoubleStateOf(5.0) }
    var years by remember { mutableDoubleStateOf(10.0) }
    var freq by remember { mutableIntStateOf(2) }

    val tool = remember { BondYieldToMaturityTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(BondYtmInput(faceValue, marketPrice, couponRate, years, freq))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Solver Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = marketPrice == 950.0,
                onClick = { marketPrice = 950.0; run() },
                label = { Text("Discount ($950)") }
            )
            FilterChip(
                selected = marketPrice == 1000.0,
                onClick = { marketPrice = 1000.0; run() },
                label = { Text("Par ($1,000)") }
            )
            FilterChip(
                selected = marketPrice == 1080.0,
                onClick = { marketPrice = 1080.0; run() },
                label = { Text("Premium ($1,080)") }
            )
        }

        ToolInputField(
            value = faceValue.toString(),
            onValueChange = { faceValue = it.toDoubleOrNull() ?: 1000.0 },
            label = "Face Value ($)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = marketPrice.toString(),
            onValueChange = { marketPrice = it.toDoubleOrNull() ?: 950.0 },
            label = "Market Price ($)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = couponRate.toString(),
            onValueChange = { couponRate = it.toDoubleOrNull() ?: 5.0 },
            label = "Annual Coupon Rate (%)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = years.toString(),
            onValueChange = { years = it.toDoubleOrNull() ?: 10.0 },
            label = "Years to Maturity",
            minLines = 1,
            maxLines = 1
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Calculate YTM, Duration & Convexity")
        }
    }
}

// 11. Kinematics Trajectory UI
@Composable
fun KinematicsTrajectoryUI(onResultUpdated: (String, String?) -> Unit) {
    var v0 by remember { mutableDoubleStateOf(50.0) }
    var angle by remember { mutableDoubleStateOf(45.0) }
    var h0 by remember { mutableDoubleStateOf(0.0) }

    val tool = remember { KinematicsTrajectoryTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(KinematicsInput(v0, angle, h0))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Kinematics Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = angle == 45.0,
                onClick = { angle = 45.0; h0 = 0.0; run() },
                label = { Text("45° Flat Ground") }
            )
            FilterChip(
                selected = angle == 30.0,
                onClick = { angle = 30.0; h0 = 0.0; run() },
                label = { Text("30° Low Arc") }
            )
            FilterChip(
                selected = h0 == 15.0,
                onClick = { angle = 35.0; h0 = 15.0; run() },
                label = { Text("15m Cliff Launch") }
            )
        }

        ToolInputField(
            value = v0.toString(),
            onValueChange = { v0 = it.toDoubleOrNull() ?: 10.0 },
            label = "Initial Velocity (m/s)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = angle.toString(),
            onValueChange = { angle = it.toDoubleOrNull() ?: 45.0 },
            label = "Launch Angle (degrees)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = h0.toString(),
            onValueChange = { h0 = it.toDoubleOrNull() ?: 0.0 },
            label = "Initial Height (meters)",
            minLines = 1,
            maxLines = 1
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Calculate Ballistic Trajectory")
        }
    }
}

// 12. Run-Length Encoding UI
@Composable
fun RunLengthEncodingUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("WWWWWWWWWWWWBWWWWWWWWWWWWBBBWWWWWWWWWWWWWWWWWWWWWWWWB") }
    var operation by remember { mutableStateOf("ENCODE") }

    val tool = remember { RunLengthEncodingTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(RleInput(operation, text))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "RLE Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = operation == "ENCODE",
                onClick = { operation = "ENCODE"; run() },
                label = { Text("Encode") }
            )
            FilterChip(
                selected = operation == "DECODE",
                onClick = { operation = "DECODE"; run() },
                label = { Text("Decode") }
            )
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it },
            label = if (operation == "ENCODE") "Uncompressed String" else "RLE Encoded Stream (e.g. 12W1B12W3B)",
            minLines = 3,
            maxLines = 6
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (operation == "ENCODE") "Compress with RLE" else "Decompress RLE Stream")
        }
    }
}

// 13. Levenshtein Matrix Visualizer UI
@Composable
fun LevenshteinMatrixVisualizerUI(onResultUpdated: (String, String?) -> Unit) {
    var source by remember { mutableStateOf("kitten") }
    var target by remember { mutableStateOf("sitting") }

    val tool = remember { LevenshteinMatrixVisualizerTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(LevenshteinMatrixInput(source, target))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Matrix Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = { source = "kitten"; target = "sitting"; run() },
                label = { Text("kitten -> sitting") }
            )
            FilterChip(
                selected = false,
                onClick = { source = "saturday"; target = "sunday"; run() },
                label = { Text("saturday -> sunday") }
            )
            FilterChip(
                selected = false,
                onClick = { source = "algorithm"; target = "altimeter"; run() },
                label = { Text("algorithm -> altimeter") }
            )
        }

        ToolInputField(
            value = source,
            onValueChange = { source = it },
            label = "Source String",
            minLines = 1,
            maxLines = 2
        )
        ToolInputField(
            value = target,
            onValueChange = { target = it },
            label = "Target String",
            minLines = 1,
            maxLines = 2
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Generate 2D DP Matrix & Edit Script")
        }
    }
}

// 14. Reverb RT60 Acoustic UI
@Composable
fun ReverbRt60AcousticUI(onResultUpdated: (String, String?) -> Unit) {
    var length by remember { mutableDoubleStateOf(8.0) }
    var width by remember { mutableDoubleStateOf(6.0) }
    var height by remember { mutableDoubleStateOf(3.0) }
    var preset by remember { mutableStateOf("RECORDING_STUDIO") }

    val tool = remember { ReverbRt60AcousticTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(ReverbRt60Input(length, width, height, 0.15, preset))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "Acoustic Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = preset == "RECORDING_STUDIO",
                onClick = { preset = "RECORDING_STUDIO"; run() },
                label = { Text("Studio (0.45)") }
            )
            FilterChip(
                selected = preset == "CLASSROOM",
                onClick = { preset = "CLASSROOM"; run() },
                label = { Text("Classroom (0.25)") }
            )
            FilterChip(
                selected = preset == "CONCERT_HALL",
                onClick = { preset = "CONCERT_HALL"; run() },
                label = { Text("Concert Hall (0.18)") }
            )
            FilterChip(
                selected = preset == "CATHEDRAL",
                onClick = { preset = "CATHEDRAL"; run() },
                label = { Text("Cathedral (0.05)") }
            )
        }

        ToolInputField(
            value = length.toString(),
            onValueChange = { length = it.toDoubleOrNull() ?: 5.0 },
            label = "Room Length (m)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = width.toString(),
            onValueChange = { width = it.toDoubleOrNull() ?: 4.0 },
            label = "Room Width (m)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = height.toString(),
            onValueChange = { height = it.toDoubleOrNull() ?: 3.0 },
            label = "Room Height (m)",
            minLines = 1,
            maxLines = 1
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Calculate Sabine & Norris-Eyring RT60")
        }
    }
}

// 15. Color CIEDE2000 UI
@Composable
fun ColorDeltaE2000UI(onResultUpdated: (String, String?) -> Unit) {
    var c1 by remember { mutableStateOf("#3498db") }
    var c2 by remember { mutableStateOf("#2980b9") }

    val tool = remember { ColorDeltaE2000Tool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            when (val res = tool.execute(DeltaEInput(c1, c2))) {
                is ToolResult.Success -> onResultUpdated(res.data.formattedReport, res.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${res.message}", "DeltaE Failed")
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = { c1 = "#3498db"; c2 = "#2980b9"; run() },
                label = { Text("Blue Shades") }
            )
            FilterChip(
                selected = false,
                onClick = { c1 = "#e74c3c"; c2 = "#c0392b"; run() },
                label = { Text("Red Shades") }
            )
            FilterChip(
                selected = false,
                onClick = { c1 = "#000000"; c2 = "#050505"; run() },
                label = { Text("Near Black (JND)") }
            )
        }

        ToolInputField(
            value = c1,
            onValueChange = { c1 = it },
            label = "Color 1 (Hex e.g. #3498db)",
            minLines = 1,
            maxLines = 1
        )
        ToolInputField(
            value = c2,
            onValueChange = { c2 = it },
            label = "Color 2 (Hex e.g. #2980b9)",
            minLines = 1,
            maxLines = 1
        )

        Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) {
            Text("Calculate CIEDE2000 (ΔE00) Difference")
        }
    }
}
