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
import com.offline.toolbox.tools.color.ColorBlendInput
import com.offline.toolbox.tools.color.ColorHexAlphaBlenderTool
import com.offline.toolbox.tools.data.CborHexInspectorTool
import com.offline.toolbox.tools.data.CborInput
import com.offline.toolbox.tools.data.GeoHashCodecTool
import com.offline.toolbox.tools.data.GeoHashInput
import com.offline.toolbox.tools.developer.ApacheHtaccessInput
import com.offline.toolbox.tools.developer.ApacheHtaccessValidatorTool
import com.offline.toolbox.tools.developer.DockerfileLinterInput
import com.offline.toolbox.tools.developer.DockerfileLinterTool
import com.offline.toolbox.tools.developer.KubeYamlInput
import com.offline.toolbox.tools.developer.KubeYamlResourceInspectorTool
import com.offline.toolbox.tools.developer.SystemdServiceUnitValidatorTool
import com.offline.toolbox.tools.developer.SystemdUnitInput
import com.offline.toolbox.tools.math.AnnuityCalculatorTool
import com.offline.toolbox.tools.math.AnnuityInput
import com.offline.toolbox.tools.math.HeatIndexWindChillTool
import com.offline.toolbox.tools.math.RocketInput
import com.offline.toolbox.tools.math.TsiolkovskyRocketEquationTool
import com.offline.toolbox.tools.math.WeatherIndexInput
import com.offline.toolbox.tools.media.SnrAudioCalculatorTool
import com.offline.toolbox.tools.media.SnrAudioInput
import com.offline.toolbox.tools.security.AffineCipherInput
import com.offline.toolbox.tools.security.AffineCipherTool
import com.offline.toolbox.tools.security.PolybiusInput
import com.offline.toolbox.tools.security.PolybiusSquareCipherTool
import com.offline.toolbox.tools.text.CaesarBreakerInput
import com.offline.toolbox.tools.text.CaesarBruteForceBreakerTool
import com.offline.toolbox.tools.text.PorterStemmerTool
import com.offline.toolbox.tools.text.StemmerInput
import kotlinx.coroutines.launch

// 1. Systemd Service Unit Validator UI
@Composable
fun SystemdServiceUnitValidatorUI(onResultUpdated: (String, String?) -> Unit) {
    var unitContent by remember {
        mutableStateOf(
            """[Unit]
Description=Offline Worker Service
After=network.target local-fs.target

[Service]
Type=notify
ExecStart=/usr/bin/offline-worker --config /etc/worker.conf
Restart=always
RestartSec=5s
User=offlineapp
Group=offlineapp
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true

[Install]
WantedBy=multi-user.target"""
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { SystemdServiceUnitValidatorTool() }

    fun runEvaluation() {
        coroutineScope.launch {
            when (val result = tool.execute(SystemdUnitInput(unitContent))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runEvaluation() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = unitContent,
            onValueChange = { unitContent = it },
            label = "systemd Service Unit (.service)",
            placeholder = "Paste systemd unit text...",
            minLines = 8,
            maxLines = 14
        )

        Button(
            onClick = { runEvaluation() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Validate systemd Unit Directives")
        }
    }
}

// 2. Dockerfile Security & Best-Practices Linter UI
@Composable
fun DockerfileLinterUI(onResultUpdated: (String, String?) -> Unit) {
    var dockerfileText by remember {
        mutableStateOf(
            """FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .

FROM node:20-alpine
WORKDIR /app
COPY --from=build /app/dist ./dist
USER node
EXPOSE 8080
HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/health || exit 1
CMD ["node", "dist/server.js"]"""
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { DockerfileLinterTool() }

    fun runLint() {
        coroutineScope.launch {
            when (val result = tool.execute(DockerfileLinterInput(dockerfileText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runLint() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = dockerfileText,
            onValueChange = { dockerfileText = it },
            label = "Dockerfile Content",
            placeholder = "Paste Dockerfile instructions...",
            minLines = 8,
            maxLines = 14
        )

        Button(
            onClick = { runLint() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lint Dockerfile Instructions")
        }
    }
}

// 3. Apache .htaccess Rewrite & Security Validator UI
@Composable
fun ApacheHtaccessValidatorUI(onResultUpdated: (String, String?) -> Unit) {
    var htaccessText by remember {
        mutableStateOf(
            """Options -Indexes
ServerSignature Off

RewriteEngine On
RewriteBase /

# Enforce HTTPS
RewriteCond %{HTTPS} off
RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]

# Front Controller Routing
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule . /index.html [L,QSA]"""
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { ApacheHtaccessValidatorTool() }

    fun runAudit() {
        coroutineScope.launch {
            when (val result = tool.execute(ApacheHtaccessInput(htaccessText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runAudit() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = htaccessText,
            onValueChange = { htaccessText = it },
            label = "Apache .htaccess Directives",
            placeholder = "Paste .htaccess rewrite rules...",
            minLines = 8,
            maxLines = 14
        )

        Button(
            onClick = { runAudit() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Audit Rewrite Rules & Directives")
        }
    }
}

// 4. Kubernetes YAML Resource Inspector UI
@Composable
fun KubeYamlResourceInspectorUI(onResultUpdated: (String, String?) -> Unit) {
    var yamlText by remember {
        mutableStateOf(
            """apiVersion: apps/v1
kind: Deployment
metadata:
  name: offline-api-service
  namespace: production
spec:
  replicas: 3
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        readOnlyRootFilesystem: true
      containers:
      - name: api
        image: offline-api:1.2.0
        resources:
          requests:
            cpu: 200m
            memory: 256Mi
          limits:
            cpu: 1000m
            memory: 1Gi
        livenessProbe:
          httpGet:
            path: /healthz
            port: 8080"""
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { KubeYamlResourceInspectorTool() }

    fun runInspection() {
        coroutineScope.launch {
            when (val result = tool.execute(KubeYamlInput(yamlText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runInspection() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = yamlText,
            onValueChange = { yamlText = it },
            label = "Kubernetes Manifest (YAML)",
            placeholder = "Paste Deployment/Pod YAML...",
            minLines = 8,
            maxLines = 14
        )

        Button(
            onClick = { runInspection() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Inspect Pod Resources & Security Context")
        }
    }
}

// 5. Polybius Square 5x5 Matrix Cipher UI
@Composable
fun PolybiusSquareCipherUI(onResultUpdated: (String, String?) -> Unit) {
    var operation by remember { mutableStateOf("ENCODE") }
    var text by remember { mutableStateOf("DEFEND THE EAST WALL") }
    var keyword by remember { mutableStateOf("CIPHER") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { PolybiusSquareCipherTool() }

    fun runCipher() {
        coroutineScope.launch {
            when (val result = tool.execute(PolybiusInput(operation, text, keyword))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(operation) { runCipher() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = operation == "ENCODE",
                onClick = { operation = "ENCODE" },
                label = { Text("Encode Matrix") }
            )
            FilterChip(
                selected = operation == "DECODE",
                onClick = { operation = "DECODE" },
                label = { Text("Decode Coordinates") }
            )
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it },
            label = if (operation == "ENCODE") "Plaintext" else "Coordinates (e.g., 14 15 21 15 33 14)",
            placeholder = "Enter text or coordinate pairs...",
            minLines = 2,
            maxLines = 4
        )

        ToolInputField(
            value = keyword,
            onValueChange = { keyword = it },
            label = "Keyword Permutation (Optional)",
            placeholder = "Optional keyword to permute 5x5 grid..."
        )

        Button(
            onClick = { runCipher() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (operation == "ENCODE") "Encode into Polybius Coordinates" else "Decode from Polybius Coordinates")
        }
    }
}

// 6. Affine Modular Arithmetic Cipher UI
@Composable
fun AffineCipherUI(onResultUpdated: (String, String?) -> Unit) {
    var operation by remember { mutableStateOf("ENCRYPT") }
    var text by remember { mutableStateOf("AFFINE CIPHER IS ELEGANT") }
    var aKey by remember { mutableIntStateOf(5) }
    var bKey by remember { mutableIntStateOf(8) }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { AffineCipherTool() }

    fun runCipher() {
        coroutineScope.launch {
            when (val result = tool.execute(AffineCipherInput(operation, text, aKey, bKey))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(operation) { runCipher() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ENCRYPT", "DECRYPT", "BRUTE_FORCE").forEach { op ->
                FilterChip(
                    selected = operation == op,
                    onClick = { operation = op },
                    label = { Text(if (op == "BRUTE_FORCE") "Brute Force" else op.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it },
            label = if (operation == "ENCRYPT") "Plaintext" else "Ciphertext",
            placeholder = "Enter text...",
            minLines = 2,
            maxLines = 4
        )

        if (operation != "BRUTE_FORCE") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolInputField(
                    value = aKey.toString(),
                    onValueChange = { aKey = it.toIntOrNull() ?: aKey },
                    label = "Multiplier a (Coprime to 26)",
                    modifier = Modifier.weight(1f)
                )
                ToolInputField(
                    value = bKey.toString(),
                    onValueChange = { bKey = it.toIntOrNull() ?: bKey },
                    label = "Shift b (0-25)",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = { runCipher() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when (operation) {
                    "ENCRYPT" -> "Encrypt with E(x) = (ax + b) mod 26"
                    "DECRYPT" -> "Decrypt with D(y) = a⁻¹(y - b) mod 26"
                    else -> "Brute Force All 312 Key Combinations"
                }
            )
        }
    }
}

// 7. GeoHash Spatial Index Codec UI
@Composable
fun GeoHashCodecUI(onResultUpdated: (String, String?) -> Unit) {
    var operation by remember { mutableStateOf("ENCODE") }
    var latText by remember { mutableStateOf("37.774929") }
    var lonText by remember { mutableStateOf("-122.419416") }
    var precision by remember { mutableIntStateOf(9) }
    var geohashText by remember { mutableStateOf("9q8yyk8y5") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { GeoHashCodecTool() }

    fun runCodec() {
        val lat = latText.toDoubleOrNull() ?: 37.774929
        val lon = lonText.toDoubleOrNull() ?: -122.419416
        coroutineScope.launch {
            when (val result = tool.execute(GeoHashInput(operation, lat, lon, precision, geohashText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(operation) { runCodec() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = operation == "ENCODE",
                onClick = { operation = "ENCODE" },
                label = { Text("Encode Lat/Lon") }
            )
            FilterChip(
                selected = operation == "DECODE",
                onClick = { operation = "DECODE" },
                label = { Text("Decode Geohash") }
            )
        }

        if (operation == "ENCODE") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolInputField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = "Latitude (-90 to +90)",
                    modifier = Modifier.weight(1f)
                )
                ToolInputField(
                    value = lonText,
                    onValueChange = { lonText = it },
                    label = "Longitude (-180 to +180)",
                    modifier = Modifier.weight(1f)
                )
            }
            ToolInputField(
                value = precision.toString(),
                onValueChange = { precision = it.toIntOrNull()?.coerceIn(1, 12) ?: precision },
                label = "Precision Length (1 - 12 chars)"
            )
        } else {
            ToolInputField(
                value = geohashText,
                onValueChange = { geohashText = it },
                label = "Geohash String",
                placeholder = "e.g., 9q8yyk8y5 or dr5ru7"
            )
        }

        Button(
            onClick = { runCodec() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (operation == "ENCODE") "Encode to Geohash & Expand Neighbors" else "Decode Geohash Bounding Box")
        }
    }
}

// 8. CBOR Hex Inspector & JSON Converter UI
@Composable
fun CborHexInspectorUI(onResultUpdated: (String, String?) -> Unit) {
    var hexText by remember {
        mutableStateOf("a2646e616d6565416c69636563616765181e") // {"name": "Alice", "age": 30}
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { CborHexInspectorTool() }

    fun runInspect() {
        coroutineScope.launch {
            when (val result = tool.execute(CborInput(hexText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runInspect() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = hexText,
            onValueChange = { hexText = it },
            label = "CBOR Hex Encoded Payload",
            placeholder = "Paste CBOR hex bytes (e.g., a1646e616d6565416c696365)...",
            minLines = 3,
            maxLines = 6
        )

        Button(
            onClick = { runInspect() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Parse CBOR Tree to JSON")
        }
    }
}

// 9. Ordinary & Annuity Due Calculator UI
@Composable
fun AnnuityCalculatorUI(onResultUpdated: (String, String?) -> Unit) {
    var solveFor by remember { mutableStateOf("FV") }
    var annuityType by remember { mutableStateOf("ORDINARY") }
    var pmtText by remember { mutableStateOf("500.0") }
    var pvText by remember { mutableStateOf("10000.0") }
    var fvText by remember { mutableStateOf("50000.0") }
    var rateText by remember { mutableStateOf("7.0") }
    var yearsText by remember { mutableStateOf("5.0") }
    var freq by remember { mutableIntStateOf(12) }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { AnnuityCalculatorTool() }

    fun runCalc() {
        val pmt = pmtText.toDoubleOrNull() ?: 500.0
        val pv = pvText.toDoubleOrNull() ?: 0.0
        val fv = fvText.toDoubleOrNull() ?: 0.0
        val rate = rateText.toDoubleOrNull() ?: 7.0
        val yrs = yearsText.toDoubleOrNull() ?: 5.0

        coroutineScope.launch {
            when (val result = tool.execute(AnnuityInput(solveFor, annuityType, pmt, pv, fv, rate, freq, yrs))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(solveFor, annuityType) { runCalc() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Solve For:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("FV" to "Future Value", "PV" to "Present Value", "PMT_FROM_FV" to "PMT from FV", "PMT_FROM_PV" to "PMT from PV").forEach { (target, label) ->
                FilterChip(
                    selected = solveFor == target,
                    onClick = { solveFor = target },
                    label = { Text(label) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = annuityType == "ORDINARY",
                onClick = { annuityType = "ORDINARY" },
                label = { Text("Ordinary (End of Period)") }
            )
            FilterChip(
                selected = annuityType == "DUE",
                onClick = { annuityType = "DUE" },
                label = { Text("Annuity Due (Start of Period)") }
            )
        }

        if (solveFor in listOf("FV", "PV")) {
            ToolInputField(
                value = pmtText,
                onValueChange = { pmtText = it },
                label = "Periodic Payment (PMT $)"
            )
        } else if (solveFor == "PMT_FROM_FV") {
            ToolInputField(
                value = fvText,
                onValueChange = { fvText = it },
                label = "Target Future Value (FV $)"
            )
        } else {
            ToolInputField(
                value = pvText,
                onValueChange = { pvText = it },
                label = "Target Present Value (PV $)"
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = rateText,
                onValueChange = { rateText = it },
                label = "Annual Rate (%)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = yearsText,
                onValueChange = { yearsText = it },
                label = "Duration (Years)",
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { runCalc() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Annuity Amortization & Growth")
        }
    }
}

// 10. Heat Index & Wind Chill Calculator UI
@Composable
fun HeatIndexWindChillUI(onResultUpdated: (String, String?) -> Unit) {
    var mode by remember { mutableStateOf("AUTO") }
    var tempText by remember { mutableStateOf("92.0") }
    var unit by remember { mutableStateOf("FAHRENHEIT") }
    var rhText by remember { mutableStateOf("65.0") }
    var windText by remember { mutableStateOf("15.0") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { HeatIndexWindChillTool() }

    fun runWeather() {
        val t = tempText.toDoubleOrNull() ?: 92.0
        val rh = rhText.toDoubleOrNull() ?: 65.0
        val wind = windText.toDoubleOrNull() ?: 15.0

        coroutineScope.launch {
            when (val result = tool.execute(WeatherIndexInput(mode, t, unit, rh, wind))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(mode, unit) { runWeather() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("AUTO" to "Auto Detect", "HEAT_INDEX" to "Heat Index", "WIND_CHILL" to "Wind Chill").forEach { (m, label) ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = tempText,
                onValueChange = { tempText = it },
                label = "Temperature (${if (unit == "FAHRENHEIT") "°F" else "°C"})",
                modifier = Modifier.weight(1.5f)
            )
            FilterChip(
                selected = unit == "FAHRENHEIT",
                onClick = { unit = if (unit == "FAHRENHEIT") "CELSIUS" else "FAHRENHEIT" },
                label = { Text(unit.take(1)) },
                modifier = Modifier.weight(0.5f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = rhText,
                onValueChange = { rhText = it },
                label = "Relative Humidity (%)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = windText,
                onValueChange = { windText = it },
                label = "Wind Speed (mph)",
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { runWeather() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Apparent 'Feels Like' Temperature")
        }
    }
}

// 11. Tsiolkovsky Rocket Propulsion Equation Solver UI
@Composable
fun TsiolkovskyRocketEquationUI(onResultUpdated: (String, String?) -> Unit) {
    var solveFor by remember { mutableStateOf("DELTA_V") }
    var wetText by remember { mutableStateOf("549054.0") }
    var dryText by remember { mutableStateOf("22200.0") }
    var ispText by remember { mutableStateOf("311.0") }
    var targetDvText by remember { mutableStateOf("9400.0") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { TsiolkovskyRocketEquationTool() }

    fun runRocket() {
        val wet = wetText.toDoubleOrNull() ?: 549054.0
        val dry = dryText.toDoubleOrNull() ?: 22200.0
        val isp = ispText.toDoubleOrNull() ?: 311.0
        val dv = targetDvText.toDoubleOrNull() ?: 9400.0

        coroutineScope.launch {
            when (val result = tool.execute(RocketInput(solveFor, wet, dry, isp, dv))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(solveFor) { runRocket() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "DELTA_V" to "Delta-V (Δv)",
                "PROPELLANT_MASS" to "Propellant Mass",
                "FINAL_DRY_MASS" to "Dry Burnout Mass",
                "REQUIRED_ISP" to "Required Isp"
            ).forEach { (m, label) ->
                FilterChip(
                    selected = solveFor == m,
                    onClick = { solveFor = m },
                    label = { Text(label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = wetText,
                onValueChange = { wetText = it },
                label = "Initial Wet Mass (kg)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = dryText,
                onValueChange = { dryText = it },
                label = "Final Dry Mass (kg)",
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = ispText,
                onValueChange = { ispText = it },
                label = "Specific Impulse Isp (s)",
                modifier = Modifier.weight(1f)
            )
            if (solveFor != "DELTA_V") {
                ToolInputField(
                    value = targetDvText,
                    onValueChange = { targetDvText = it },
                    label = "Target Δv (m/s)",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = { runRocket() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compute Orbital Propulsion Budget")
        }
    }
}

// 12. Porter Stemmer Morphological Analyzer UI
@Composable
fun PorterStemmerUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember {
        mutableStateOf("The developer was connecting multiple microservices, debugging connections, and writing scalable applications.")
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { PorterStemmerTool() }

    fun runStem() {
        coroutineScope.launch {
            when (val result = tool.execute(StemmerInput(text))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runStem() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = text,
            onValueChange = { text = it },
            label = "Input English Text or Word List",
            placeholder = "Enter words to stem...",
            minLines = 3,
            maxLines = 6
        )

        Button(
            onClick = { runStem() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reduce Words to Morphological Base Stems")
        }
    }
}

// 13. Caesar Cipher Automated Frequency Breaker UI
@Composable
fun CaesarBruteForceBreakerUI(onResultUpdated: (String, String?) -> Unit) {
    var cipherText by remember {
        mutableStateOf("Khoor Zruog! Wklv lv dq hqfubswhg phvvdjh xvlqj fdhvdu flskhu.")
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { CaesarBruteForceBreakerTool() }

    fun runCrack() {
        coroutineScope.launch {
            when (val result = tool.execute(CaesarBreakerInput(cipherText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runCrack() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = cipherText,
            onValueChange = { cipherText = it },
            label = "Caesar Ciphertext",
            placeholder = "Paste encrypted text without knowing shift...",
            minLines = 3,
            maxLines = 6
        )

        Button(
            onClick = { runCrack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Auto-Crack Plaintext via Chi-Squared (χ²)")
        }
    }
}

// 14. Audio SNR, THD & ENOB Quality Calculator UI
@Composable
fun SnrAudioCalculatorUI(onResultUpdated: (String, String?) -> Unit) {
    var sigText by remember { mutableStateOf("1.0") }
    var noiseText by remember { mutableStateOf("0.0001") }
    var bitDepth by remember { mutableIntStateOf(16) }
    var harmonicsText by remember { mutableStateOf("0.0005, 0.0002, 0.0001") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { SnrAudioCalculatorTool() }

    fun runAudio() {
        val sig = sigText.toDoubleOrNull() ?: 1.0
        val noise = noiseText.toDoubleOrNull() ?: 0.0001
        val harmonics = harmonicsText.split(",").mapNotNull { it.trim().toDoubleOrNull() }

        coroutineScope.launch {
            when (val result = tool.execute(SnrAudioInput(sig, noise, bitDepth, harmonics))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(bitDepth) { runAudio() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = sigText,
                onValueChange = { sigText = it },
                label = "Signal RMS (V)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = noiseText,
                onValueChange = { noiseText = it },
                label = "Noise Floor RMS (V)",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(16 to "16-Bit Audio", 24 to "24-Bit Studio", 32 to "32-Bit Float").forEach { (b, label) ->
                FilterChip(
                    selected = bitDepth == b,
                    onClick = { bitDepth = b },
                    label = { Text(label) }
                )
            }
        }

        ToolInputField(
            value = harmonicsText,
            onValueChange = { harmonicsText = it },
            label = "Harmonic Voltages (V2, V3, V4... in V)",
            placeholder = "e.g. 0.0005, 0.0002"
        )

        Button(
            onClick = { runAudio() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate SNR, THD & ENOB Audio Metrics")
        }
    }
}

// 15. HEX Alpha Color Porter-Duff Blender UI
@Composable
fun ColorHexAlphaBlenderUI(onResultUpdated: (String, String?) -> Unit) {
    var fgHex by remember { mutableStateOf("#80FF5722") }
    var bgHex by remember { mutableStateOf("#3F51B5") }
    var hexFormat by remember { mutableStateOf("ARGB") }
    var colorSpace by remember { mutableStateOf("SRGB") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { ColorHexAlphaBlenderTool() }

    fun runBlend() {
        coroutineScope.launch {
            when (val result = tool.execute(ColorBlendInput(fgHex, bgHex, hexFormat, colorSpace))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(hexFormat, colorSpace) { runBlend() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = hexFormat == "ARGB",
                onClick = { hexFormat = "ARGB" },
                label = { Text("ARGB Format") }
            )
            FilterChip(
                selected = hexFormat == "RGBA",
                onClick = { hexFormat = "RGBA" },
                label = { Text("RGBA Format") }
            )
            FilterChip(
                selected = colorSpace == "SRGB",
                onClick = { colorSpace = "SRGB" },
                label = { Text("sRGB Space") }
            )
            FilterChip(
                selected = colorSpace == "LINEAR",
                onClick = { colorSpace = "LINEAR" },
                label = { Text("Linear Gamma Space") }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = fgHex,
                onValueChange = { fgHex = it },
                label = "Foreground (Alpha HEX)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = bgHex,
                onValueChange = { bgHex = it },
                label = "Background HEX",
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { runBlend() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Blend Colors via Porter-Duff Source Over")
        }
    }
}
