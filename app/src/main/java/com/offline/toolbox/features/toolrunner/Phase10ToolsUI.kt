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
import com.offline.toolbox.tools.color.ColorHarmonyMixingTool
import com.offline.toolbox.tools.color.ColorMixingInput
import com.offline.toolbox.tools.data.GeoJsonValidatorInput
import com.offline.toolbox.tools.data.GeoJsonValidatorTool
import com.offline.toolbox.tools.data.ProtobufVarintDecoderTool
import com.offline.toolbox.tools.data.ProtobufVarintInput
import com.offline.toolbox.tools.data.VarintOperationMode
import com.offline.toolbox.tools.developer.HarAnalyzerInput
import com.offline.toolbox.tools.developer.HarAnalyzerTool
import com.offline.toolbox.tools.developer.JsonWebKeyInput
import com.offline.toolbox.tools.developer.JsonWebKeyTool
import com.offline.toolbox.tools.developer.PrometheusMetricInput
import com.offline.toolbox.tools.developer.PrometheusMetricParserTool
import com.offline.toolbox.tools.developer.SshKeyFingerprintTool
import com.offline.toolbox.tools.developer.SshKeyInput
import com.offline.toolbox.tools.math.BlackScholesInput
import com.offline.toolbox.tools.math.BlackScholesOptionPricerTool
import com.offline.toolbox.tools.math.DopplerDomain
import com.offline.toolbox.tools.math.DopplerEffectCalculatorTool
import com.offline.toolbox.tools.math.DopplerInput
import com.offline.toolbox.tools.math.RomanConversionMode
import com.offline.toolbox.tools.math.RomanNumeralsAdvancedTool
import com.offline.toolbox.tools.math.RomanNumeralsInput
import com.offline.toolbox.tools.media.BeatsBinauralAcousticTool
import com.offline.toolbox.tools.media.BeatsBinauralInput
import com.offline.toolbox.tools.security.Argon2CalculatorInput
import com.offline.toolbox.tools.security.Argon2ParameterCalculatorTool
import com.offline.toolbox.tools.security.Argon2Profile
import com.offline.toolbox.tools.security.Argon2Variant
import com.offline.toolbox.tools.security.RailFenceCipherTool
import com.offline.toolbox.tools.security.RailFenceInput
import com.offline.toolbox.tools.security.RailFenceMode
import com.offline.toolbox.tools.text.SoundexMetaphoneInput
import com.offline.toolbox.tools.text.SoundexMetaphoneTool
import com.offline.toolbox.tools.text.TextStatisticsNgramInput
import com.offline.toolbox.tools.text.TextStatisticsNgramTool
import kotlinx.coroutines.launch

@Composable
fun HarAnalyzerUI(onResultUpdated: (String, String?) -> Unit) {
    var harText by remember {
        mutableStateOf(
            """{
  "log": {
    "version": "1.2",
    "creator": { "name": "OfflineBrowser", "version": "1.0" },
    "entries": [
      {
        "time": 45.2,
        "request": { "method": "GET", "url": "https://api.example.com/v1/user", "headers": [] },
        "response": {
          "status": 200,
          "headers": [{ "name": "Strict-Transport-Security", "value": "max-age=31536000" }],
          "content": { "size": 1024, "mimeType": "application/json" }
        },
        "timings": { "dns": 5.0, "connect": 12.0, "send": 1.0, "wait": 22.0, "receive": 5.2 }
      },
      {
        "time": 180.5,
        "request": { "method": "POST", "url": "https://api.example.com/v1/auth", "headers": [] },
        "response": {
          "status": 401,
          "headers": [],
          "content": { "size": 128, "mimeType": "application/json" }
        },
        "timings": { "dns": 0.0, "connect": 0.0, "send": 1.5, "wait": 175.0, "receive": 4.0 }
      }
    ]
  }
}"""
        )
    }
    val tool = remember { HarAnalyzerTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(HarAnalyzerInput(harContent = harText))
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
            value = harText,
            onValueChange = { harText = it; run() },
            label = "HTTP Archive (.har) JSON Content"
        )
    }
}

@Composable
fun PrometheusMetricParserUI(onResultUpdated: (String, String?) -> Unit) {
    var metricText by remember {
        mutableStateOf(
            """# HELP http_requests_total Total number of HTTP requests processed.
# TYPE http_requests_total counter
http_requests_total{method="POST",handler="/login",status="200"} 1027
http_requests_total{method="GET",handler="/health",status="200"} 45091
# HELP jvm_memory_used_bytes Current JVM memory in bytes.
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap"} 134217728"""
        )
    }
    val tool = remember { PrometheusMetricParserTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(PrometheusMetricInput(expositionText = metricText))
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
            value = metricText,
            onValueChange = { metricText = it; run() },
            label = "Prometheus Exposition Metric Text"
        )
    }
}

@Composable
fun SshKeyFingerprintUI(onResultUpdated: (String, String?) -> Unit) {
    var keyText by remember {
        mutableStateOf("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl dev@offline.toolbox")
    }
    val tool = remember { SshKeyFingerprintTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(SshKeyInput(publicKeyString = keyText))
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
            value = keyText,
            onValueChange = { keyText = it; run() },
            label = "OpenSSH Public Key (ssh-ed25519, ssh-rsa, ecdsa-...)"
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                keyText = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl dev@offline.toolbox"
                run()
            }) {
                Text("Ed25519 Sample")
            }
            Button(onClick = {
                keyText = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQC1sC+X5T1N0j6uYFh8rG9c6y6/4WdevKey offline@prod"
                run()
            }) {
                Text("RSA Sample")
            }
        }
    }
}

@Composable
fun JsonWebKeyUI(onResultUpdated: (String, String?) -> Unit) {
    var jwkText by remember {
        mutableStateOf(
            """{
  "kty": "RSA",
  "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fTr4Cqhe07dXg-9_11Qz0Y31_79GzK-j80e791e84",
  "e": "AQAB",
  "alg": "RS256",
  "kid": "key-2026-01"
}"""
        )
    }
    val tool = remember { JsonWebKeyTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(JsonWebKeyInput(jwkJson = jwkText))
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
            value = jwkText,
            onValueChange = { jwkText = it; run() },
            label = "JSON Web Key (JWK) JSON Payload"
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                jwkText = """{
  "kty": "RSA",
  "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fTr4Cqhe07dXg-9_11Qz0Y31_79GzK-j80e791e84",
  "e": "AQAB"
}"""
                run()
            }) {
                Text("RSA JWK")
            }
            Button(onClick = {
                jwkText = """{
  "kty": "EC",
  "crv": "P-256",
  "x": "f83OJ3D2xFMTbKEHfgkUXOcWgGPLOmpcvEAChN9GMjc",
  "y": "x_daQauBhQ0tZxFlGQM2x1BgGhDAOGx6ychIt69R_es"
}"""
                run()
            }) {
                Text("EC P-256 JWK")
            }
        }
    }
}

@Composable
fun Argon2ParameterCalculatorUI(onResultUpdated: (String, String?) -> Unit) {
    var variant by remember { mutableStateOf(Argon2Variant.ARGON2ID) }
    var profile by remember { mutableStateOf(Argon2Profile.RFC_RECOMMENDED_FIRST) }
    val tool = remember { Argon2ParameterCalculatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(Argon2CalculatorInput(variant = variant, profile = profile))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Argon2 Variant:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Argon2Variant.values().forEach { v ->
                FilterChip(
                    selected = variant == v,
                    onClick = { variant = v; run() },
                    label = { Text(v.name) }
                )
            }
        }
        Text("Target Hardware Profile:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Argon2Profile.values().forEach { p ->
                FilterChip(
                    selected = profile == p,
                    onClick = { profile = p; run() },
                    label = { Text(p.displayName.split(" (")[0]) }
                )
            }
        }
    }
}

@Composable
fun RailFenceCipherUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("WE ARE DISCOVERED FLEE AT ONCE") }
    var rails by remember { mutableIntStateOf(3) }
    var mode by remember { mutableStateOf(RailFenceMode.ENCRYPT) }
    val tool = remember { RailFenceCipherTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(RailFenceInput(text = text, rails = rails, mode = mode))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == RailFenceMode.ENCRYPT,
                onClick = { mode = RailFenceMode.ENCRYPT; run() },
                label = { Text("Encrypt") }
            )
            FilterChip(
                selected = mode == RailFenceMode.DECRYPT,
                onClick = { mode = RailFenceMode.DECRYPT; run() },
                label = { Text("Decrypt") }
            )
        }
        ToolInputField(
            value = text,
            onValueChange = { text = it; run() },
            label = "Text to Process"
        )
        ToolInputField(
            value = rails.toString(),
            onValueChange = { rails = it.toIntOrNull()?.coerceIn(2, 20) ?: 3; run() },
            label = "Number of Rails (Depth 2 - 20)"
        )
    }
}

@Composable
fun GeoJsonValidatorUI(onResultUpdated: (String, String?) -> Unit) {
    var geoJson by remember {
        mutableStateOf(
            """{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": { "name": "Central Park" },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[
          [-73.973, 40.764],
          [-73.981, 40.768],
          [-73.958, 40.800],
          [-73.949, 40.796],
          [-73.973, 40.764]
        ]]
      }
    }
  ]
}"""
        )
    }
    val tool = remember { GeoJsonValidatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(GeoJsonValidatorInput(geoJsonString = geoJson))
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
            value = geoJson,
            onValueChange = { geoJson = it; run() },
            label = "GeoJSON String (FeatureCollection / Geometry)"
        )
    }
}

@Composable
fun ProtobufVarintDecoderUI(onResultUpdated: (String, String?) -> Unit) {
    var inputStr by remember { mutableStateOf("96 01") }
    var mode by remember { mutableStateOf(VarintOperationMode.DECODE_HEX_BYTES) }
    val tool = remember { ProtobufVarintDecoderTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(ProtobufVarintInput(inputString = inputStr, mode = mode))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == VarintOperationMode.DECODE_HEX_BYTES,
                onClick = { mode = VarintOperationMode.DECODE_HEX_BYTES; run() },
                label = { Text("Decode Hex Bytes") }
            )
            FilterChip(
                selected = mode == VarintOperationMode.ENCODE_INTEGER,
                onClick = { mode = VarintOperationMode.ENCODE_INTEGER; run() },
                label = { Text("Encode Integer") }
            )
        }
        ToolInputField(
            value = inputStr,
            onValueChange = { inputStr = it; run() },
            label = if (mode == VarintOperationMode.DECODE_HEX_BYTES) "Hex Bytes (e.g. 96 01 or AC 02)" else "Decimal Integer (e.g. 150 or -75)"
        )
    }
}

@Composable
fun BlackScholesOptionPricerUI(onResultUpdated: (String, String?) -> Unit) {
    var spot by remember { mutableDoubleStateOf(100.0) }
    var strike by remember { mutableDoubleStateOf(100.0) }
    var years by remember { mutableDoubleStateOf(1.0) }
    var rate by remember { mutableDoubleStateOf(5.0) }
    var vol by remember { mutableDoubleStateOf(20.0) }
    val tool = remember { BlackScholesOptionPricerTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(
                BlackScholesInput(
                    spotPrice = spot,
                    strikePrice = strike,
                    timeToMaturityYears = years,
                    riskFreeRatePercent = rate,
                    volatilityPercent = vol
                )
            )
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = spot.toString(),
                    onValueChange = { spot = it.toDoubleOrNull() ?: spot; run() },
                    label = "Spot Price ($)"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = strike.toString(),
                    onValueChange = { strike = it.toDoubleOrNull() ?: strike; run() },
                    label = "Strike Price ($)"
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = years.toString(),
                    onValueChange = { years = it.toDoubleOrNull() ?: years; run() },
                    label = "Years to Expiry"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = vol.toString(),
                    onValueChange = { vol = it.toDoubleOrNull() ?: vol; run() },
                    label = "Volatility (%)"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = rate.toString(),
                    onValueChange = { rate = it.toDoubleOrNull() ?: rate; run() },
                    label = "Risk-Free Rate (%)"
                )
            }
        }
    }
}

@Composable
fun DopplerEffectCalculatorUI(onResultUpdated: (String, String?) -> Unit) {
    var domain by remember { mutableStateOf(DopplerDomain.ACOUSTIC_SOUND) }
    var freq by remember { mutableDoubleStateOf(1000.0) }
    var sourceVel by remember { mutableDoubleStateOf(30.0) }
    var tempC by remember { mutableDoubleStateOf(20.0) }
    var relKmS by remember { mutableDoubleStateOf(30000.0) }
    val tool = remember { DopplerEffectCalculatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(
                DopplerInput(
                    domain = domain,
                    sourceFrequencyHz = freq,
                    sourceVelocityMs = sourceVel,
                    airTemperatureCelsius = tempC,
                    relativeVelocityKmS = relKmS
                )
            )
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = domain == DopplerDomain.ACOUSTIC_SOUND,
                onClick = { domain = DopplerDomain.ACOUSTIC_SOUND; run() },
                label = { Text("Acoustic Sound") }
            )
            FilterChip(
                selected = domain == DopplerDomain.OPTICAL_RELATIVISTIC,
                onClick = { domain = DopplerDomain.OPTICAL_RELATIVISTIC; run() },
                label = { Text("Relativistic Optical") }
            )
        }
        ToolInputField(
            value = freq.toString(),
            onValueChange = { freq = it.toDoubleOrNull() ?: freq; run() },
            label = "Source Frequency (Hz)"
        )
        if (domain == DopplerDomain.ACOUSTIC_SOUND) {
            ToolInputField(
                value = sourceVel.toString(),
                onValueChange = { sourceVel = it.toDoubleOrNull() ?: sourceVel; run() },
                label = "Source Speed (+ approaching, - receding m/s)"
            )
            ToolInputField(
                value = tempC.toString(),
                onValueChange = { tempC = it.toDoubleOrNull() ?: tempC; run() },
                label = "Air Temperature (°C)"
            )
        } else {
            ToolInputField(
                value = relKmS.toString(),
                onValueChange = { relKmS = it.toDoubleOrNull() ?: relKmS; run() },
                label = "Relative Velocity (+ approaching, - receding km/s)"
            )
        }
    }
}

@Composable
fun RomanNumeralsAdvancedUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("2542456") }
    var mode by remember { mutableStateOf(RomanConversionMode.INTEGER_TO_ROMAN) }
    val tool = remember { RomanNumeralsAdvancedTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(RomanNumeralsInput(value = text, mode = mode))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == RomanConversionMode.INTEGER_TO_ROMAN,
                onClick = { mode = RomanConversionMode.INTEGER_TO_ROMAN; run() },
                label = { Text("Integer → Vinculum") }
            )
            FilterChip(
                selected = mode == RomanConversionMode.ROMAN_TO_INTEGER,
                onClick = { mode = RomanConversionMode.ROMAN_TO_INTEGER; run() },
                label = { Text("Vinculum → Integer") }
            )
        }
        ToolInputField(
            value = text,
            onValueChange = { text = it; run() },
            label = if (mode == RomanConversionMode.INTEGER_TO_ROMAN) "Decimal Integer (1 to 3,999,999)" else "Vinculum Roman (e.g. [M][M]D̄ or V̄)"
        )
    }
}

@Composable
fun SoundexMetaphoneUI(onResultUpdated: (String, String?) -> Unit) {
    var word1 by remember { mutableStateOf("Smith") }
    var word2 by remember { mutableStateOf("Smythe") }
    val tool = remember { SoundexMetaphoneTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(SoundexMetaphoneInput(primaryWord = word1, comparisonWord = word2))
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
            value = word1,
            onValueChange = { word1 = it; run() },
            label = "Primary Name / Word"
        )
        ToolInputField(
            value = word2,
            onValueChange = { word2 = it; run() },
            label = "Comparison Word (for sound-alike check)"
        )
    }
}

@Composable
fun TextStatisticsNgramUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember {
        mutableStateOf("The quick brown fox jumps over the lazy dog. The dog barks at the fox, and the fox jumps high into the air.")
    }
    var filterStopwords by remember { mutableStateOf(false) }
    val tool = remember { TextStatisticsNgramTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(TextStatisticsNgramInput(text = text, filterStopwords = filterStopwords))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = filterStopwords,
            onClick = { filterStopwords = !filterStopwords; run() },
            label = { Text("Filter Common English Stopwords") }
        )
        ToolInputField(
            value = text,
            onValueChange = { text = it; run() },
            label = "Prose Text to Analyze"
        )
    }
}

@Composable
fun BeatsBinauralAcousticUI(onResultUpdated: (String, String?) -> Unit) {
    var f1 by remember { mutableDoubleStateOf(432.0) }
    var f2 by remember { mutableDoubleStateOf(440.0) }
    val tool = remember { BeatsBinauralAcousticTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(BeatsBinauralInput(leftChannelHz = f1, rightChannelHz = f2))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = f1.toString(),
                    onValueChange = { f1 = it.toDoubleOrNull() ?: f1; run() },
                    label = "Left Channel (Hz)"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = f2.toString(),
                    onValueChange = { f2 = it.toDoubleOrNull() ?: f2; run() },
                    label = "Right Channel (Hz)"
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Alpha (8 Hz)" to 440.0, "Theta (5 Hz)" to 437.0, "Delta (2 Hz)" to 434.0, "Beta (18 Hz)" to 450.0, "Gamma (40 Hz)" to 472.0).forEach { (label, rightVal) ->
                Button(onClick = { f1 = 432.0; f2 = rightVal; run() }) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
fun ColorHarmonyMixingUI(onResultUpdated: (String, String?) -> Unit) {
    var c1 by remember { mutableStateOf("#FFFF00") }
    var c2 by remember { mutableStateOf("#0000FF") }
    var ratio by remember { mutableIntStateOf(50) }
    val tool = remember { ColorHarmonyMixingTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(ColorMixingInput(color1Hex = c1, color2Hex = c2, ratioColor1Percent = ratio))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = c1,
                    onValueChange = { c1 = it; run() },
                    label = "Color 1 (Hex e.g. #FFFF00)"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ToolInputField(
                    value = c2,
                    onValueChange = { c2 = it; run() },
                    label = "Color 2 (Hex e.g. #0000FF)"
                )
            }
        }
        ToolInputField(
            value = ratio.toString(),
            onValueChange = { ratio = it.toIntOrNull()?.coerceIn(0, 100) ?: 50; run() },
            label = "Color 1 Mix Weight (0% to 100%)"
        )
    }
}
