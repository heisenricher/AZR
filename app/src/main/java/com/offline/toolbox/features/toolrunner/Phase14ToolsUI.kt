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
import com.offline.toolbox.tools.data.GreatCircleDistanceTool
import com.offline.toolbox.tools.data.GreatCircleInput
import com.offline.toolbox.tools.developer.GitCommitLinterInput
import com.offline.toolbox.tools.developer.GitCommitMessageLinterTool
import com.offline.toolbox.tools.math.ResistorCircuitInput
import com.offline.toolbox.tools.math.ResistorEquivalentCircuitTool
import com.offline.toolbox.tools.media.SvgPathDataInspectorTool
import com.offline.toolbox.tools.media.SvgPathInput
import com.offline.toolbox.tools.security.PlayfairCipherInput
import com.offline.toolbox.tools.security.PlayfairCipherTool
import kotlinx.coroutines.launch

// 1. Conventional Commits 1.0.0 Linter UI (Tool 196)
@Composable
fun GitCommitMessageLinterUI(onResultUpdated: (String, String?) -> Unit) {
    var commitText by remember {
        mutableStateOf(
            """feat(auth)!: add biometric fingerprint authentication

Implement Android BiometricPrompt API for fast local biometric unlock.
Replaces legacy pin-only authentication pipeline.

BREAKING CHANGE: Minimum supported biometric hardware level is BIOMETRIC_STRONG.
Fixes: #1042"""
        )
    }
    var maxHeaderLength by remember { mutableIntStateOf(72) }
    var enforceImperative by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { GitCommitMessageLinterTool() }

    fun runLinter() {
        coroutineScope.launch {
            when (val result = tool.execute(GitCommitLinterInput(commitText, maxHeaderLength, enforceImperative))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(maxHeaderLength, enforceImperative) { runLinter() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = commitText.startsWith("feat(auth)!:"),
                onClick = {
                    commitText = """feat(auth)!: add biometric fingerprint authentication

Implement Android BiometricPrompt API for fast local biometric unlock.
Replaces legacy pin-only authentication pipeline.

BREAKING CHANGE: Minimum supported biometric hardware level is BIOMETRIC_STRONG.
Fixes: #1042"""
                    runLinter()
                },
                label = { Text("Feat (Breaking)") }
            )
            FilterChip(
                selected = commitText.startsWith("fix(database):"),
                onClick = {
                    commitText = """fix(database): resolve room migration foreign key constraint

Ensure SQLite PRAGMA foreign_keys is deferred during table schema migration.
Prevents crash on upgrade from database version 4 to 5.

Closes: #882"""
                    runLinter()
                },
                label = { Text("Fix (Issue)") }
            )
            FilterChip(
                selected = commitText.startsWith("chore(deps):"),
                onClick = {
                    commitText = """chore(deps): bump kotlin to 2.1.0 and compose bom 2024.12.01

Update build dependencies and verify full automated test suite pass rate."""
                    runLinter()
                },
                label = { Text("Chore (Deps)") }
            )
        }

        ToolInputField(
            value = commitText,
            onValueChange = {
                commitText = it
                runLinter()
            },
            label = "Commit Message (Header + Body + Footers)",
            placeholder = "type(scope): imperative subject...",
            minLines = 6
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(50 to "Max 50", 72 to "Max 72 (Standard)", 100 to "Max 100").forEach { (len, label) ->
                FilterChip(
                    selected = maxHeaderLength == len,
                    onClick = { maxHeaderLength = len },
                    label = { Text(label) }
                )
            }
            FilterChip(
                selected = enforceImperative,
                onClick = { enforceImperative = !enforceImperative },
                label = { Text("Enforce Imperative Mood") }
            )
        }

        Button(onClick = { runLinter() }, modifier = Modifier.fillMaxWidth()) {
            Text("Audit Commit Message")
        }
    }
}

// 2. Wheatstone-Playfair Digraph Substitution Cipher UI (Tool 197)
@Composable
fun PlayfairCipherUI(onResultUpdated: (String, String?) -> Unit) {
    var operation by remember { mutableStateOf("ENCRYPT") }
    var text by remember { mutableStateOf("INSTRUMENTS") }
    var keyword by remember { mutableStateOf("MONARCHY") }
    var fillerChar by remember { mutableStateOf("X") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { PlayfairCipherTool() }

    fun runCipher() {
        coroutineScope.launch {
            val fChar = fillerChar.firstOrNull() ?: 'X'
            when (val result = tool.execute(PlayfairCipherInput(operation, text, keyword, fChar))) {
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
            listOf("ENCRYPT" to "Encrypt", "DECRYPT" to "Decrypt").forEach { (op, label) ->
                FilterChip(
                    selected = operation == op,
                    onClick = { operation = op },
                    label = { Text(label) }
                )
            }
        }

        ToolInputField(
            value = text,
            onValueChange = {
                text = it
                runCipher()
            },
            label = "Input Text",
            placeholder = "Enter text to transform...",
            minLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolInputField(
                value = keyword,
                onValueChange = {
                    keyword = it
                    runCipher()
                },
                label = "Key Matrix Keyword",
                placeholder = "e.g. MONARCHY",
                modifier = Modifier.weight(0.7f),
                minLines = 1
            )
            ToolInputField(
                value = fillerChar,
                onValueChange = {
                    fillerChar = it.take(1)
                    runCipher()
                },
                label = "Filler",
                placeholder = "X",
                modifier = Modifier.weight(0.3f),
                minLines = 1
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "MONARCHY" to "INSTRUMENTS",
                "WHEATSTONE" to "SECRET MISSION",
                "GUIDANCE" to "COMMUNICATION"
            ).forEach { (kw, sample) ->
                FilterChip(
                    selected = keyword == kw,
                    onClick = {
                        keyword = kw
                        text = sample
                        runCipher()
                    },
                    label = { Text("Preset: $kw") }
                )
            }
        }

        Button(onClick = { runCipher() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (operation == "ENCRYPT") "Encrypt Digraphs" else "Decrypt Digraphs")
        }
    }
}

// 3. Great-Circle Distance & Bearing Navigator UI (Tool 198)
@Composable
fun GreatCircleDistanceUI(onResultUpdated: (String, String?) -> Unit) {
    var lat1 by remember { mutableDoubleStateOf(37.774929) }  // SFO
    var lon1 by remember { mutableDoubleStateOf(-122.419416) }
    var lat2 by remember { mutableDoubleStateOf(40.712776) }  // NYC
    var lon2 by remember { mutableDoubleStateOf(-74.005974) }
    var model by remember { mutableStateOf("WGS84_ELLIPSOID") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { GreatCircleDistanceTool() }

    fun runNav() {
        coroutineScope.launch {
            val input = GreatCircleInput(lat1, lon1, lat2, lon2, model)
            when (val result = tool.execute(input)) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(lat1, lon1, lat2, lon2, model) { runNav() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = model == "WGS84_ELLIPSOID",
                onClick = { model = "WGS84_ELLIPSOID" },
                label = { Text("WGS-84 Ellipsoid (Vincenty)") }
            )
            FilterChip(
                selected = model == "SPHERICAL_HAVERSINE",
                onClick = { model = "SPHERICAL_HAVERSINE" },
                label = { Text("Spherical (Haversine)") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = lat1 == 37.774929 && lat2 == 40.712776,
                onClick = {
                    lat1 = 37.774929; lon1 = -122.419416; lat2 = 40.712776; lon2 = -74.005974
                },
                label = { Text("SFO -> NYC") }
            )
            FilterChip(
                selected = lat1 == 51.507351 && lat2 == 35.676192,
                onClick = {
                    lat1 = 51.507351; lon1 = -0.127758; lat2 = 35.676192; lon2 = 139.650311
                },
                label = { Text("London -> Tokyo") }
            )
            FilterChip(
                selected = lat1 == -33.868820 && lat2 == 34.052235,
                onClick = {
                    lat1 = -33.868820; lon1 = 151.209296; lat2 = 34.052235; lon2 = -118.243683
                },
                label = { Text("Sydney -> LAX") }
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = lat1.toString(),
                onValueChange = { it.toDoubleOrNull()?.let { v -> lat1 = v } },
                label = "Origin Lat (°)",
                placeholder = "37.7749",
                modifier = Modifier.weight(1f),
                minLines = 1
            )
            ToolInputField(
                value = lon1.toString(),
                onValueChange = { it.toDoubleOrNull()?.let { v -> lon1 = v } },
                label = "Origin Lon (°)",
                placeholder = "-122.4194",
                modifier = Modifier.weight(1f),
                minLines = 1
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = lat2.toString(),
                onValueChange = { it.toDoubleOrNull()?.let { v -> lat2 = v } },
                label = "Dest Lat (°)",
                placeholder = "40.7128",
                modifier = Modifier.weight(1f),
                minLines = 1
            )
            ToolInputField(
                value = lon2.toString(),
                onValueChange = { it.toDoubleOrNull()?.let { v -> lon2 = v } },
                label = "Dest Lon (°)",
                placeholder = "-74.0060",
                modifier = Modifier.weight(1f),
                minLines = 1
            )
        }

        Button(onClick = { runNav() }, modifier = Modifier.fillMaxWidth()) {
            Text("Calculate Distance & Bearing")
        }
    }
}

// 4. Resistor Circuit Network & Voltage Divider UI (Tool 199)
@Composable
fun ResistorEquivalentCircuitUI(onResultUpdated: (String, String?) -> Unit) {
    var topology by remember { mutableStateOf("SERIES") }
    var resistorValues by remember { mutableStateOf("100, 220, 470") }
    var supplyVoltage by remember { mutableDoubleStateOf(12.0) }
    var loadResistance by remember { mutableStateOf("1000") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { ResistorEquivalentCircuitTool() }

    fun runCircuit() {
        coroutineScope.launch {
            val rLoad = loadResistance.toDoubleOrNull()
            val input = ResistorCircuitInput(topology, resistorValues, supplyVoltage, rLoad)
            when (val result = tool.execute(input)) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(topology, supplyVoltage) { runCircuit() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("SERIES" to "Series (Req = ΣR)", "PARALLEL" to "Parallel (1/Req = Σ1/R)", "VOLTAGE_DIVIDER" to "Voltage Divider").forEach { (top, label) ->
                FilterChip(
                    selected = topology == top,
                    onClick = { topology = top },
                    label = { Text(label) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = resistorValues == "100, 220, 470",
                onClick = { resistorValues = "100, 220, 470"; runCircuit() },
                label = { Text("100Ω, 220Ω, 470Ω") }
            )
            FilterChip(
                selected = resistorValues == "1k, 2.2k, 4.7k",
                onClick = { resistorValues = "1k, 2.2k, 4.7k"; runCircuit() },
                label = { Text("1k, 2.2k, 4.7k") }
            )
            FilterChip(
                selected = resistorValues == "10k, 10k",
                onClick = { resistorValues = "10k, 10k"; topology = "VOLTAGE_DIVIDER"; runCircuit() },
                label = { Text("Divider 50% (10k, 10k)") }
            )
        }

        ToolInputField(
            value = resistorValues,
            onValueChange = {
                resistorValues = it
                runCircuit()
            },
            label = "Resistors in Circuit (Ohms, k, M)",
            placeholder = "e.g. 100, 220, 4.7k, 10k",
            minLines = 1
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = supplyVoltage.toString(),
                onValueChange = { it.toDoubleOrNull()?.let { v -> supplyVoltage = v } },
                label = "Supply Voltage (V)",
                placeholder = "12.0",
                modifier = Modifier.weight(1f),
                minLines = 1
            )
            if (topology == "VOLTAGE_DIVIDER") {
                ToolInputField(
                    value = loadResistance,
                    onValueChange = {
                        loadResistance = it
                        runCircuit()
                    },
                    label = "Load RL (Ohms)",
                    placeholder = "1000",
                    modifier = Modifier.weight(1f),
                    minLines = 1
                )
            }
        }

        Button(onClick = { runCircuit() }, modifier = Modifier.fillMaxWidth()) {
            Text("Solve Resistor Circuit")
        }
    }
}

// 5. SVG Path Data Inspector & Android Vector Converter UI (Tool 200 - The Bicentennial Crowning Tool)
@Composable
fun SvgPathDataInspectorUI(onResultUpdated: (String, String?) -> Unit) {
    var pathData by remember { mutableStateOf("M 10 80 Q 52.5 10, 95 80 T 180 80 Z") }
    var viewportWidth by remember { mutableDoubleStateOf(200.0) }
    var viewportHeight by remember { mutableDoubleStateOf(200.0) }
    var fillColorHex by remember { mutableStateOf("#3F51B5") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { SvgPathDataInspectorTool() }

    fun runSvg() {
        coroutineScope.launch {
            val input = SvgPathInput(pathData, viewportWidth, viewportHeight, fillColorHex)
            when (val result = tool.execute(input)) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(viewportWidth, viewportHeight, fillColorHex) { runSvg() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = pathData.startsWith("M 10 80 Q"),
                onClick = {
                    pathData = "M 10 80 Q 52.5 10, 95 80 T 180 80 Z"
                    viewportWidth = 200.0; viewportHeight = 200.0
                    runSvg()
                },
                label = { Text("Quad Bézier Wave") }
            )
            FilterChip(
                selected = pathData.startsWith("M 10 10 H 90"),
                onClick = {
                    pathData = "M 10 10 H 90 V 90 H 10 L 10 10 Z"
                    viewportWidth = 100.0; viewportHeight = 100.0
                    runSvg()
                },
                label = { Text("Box (H/V/L/Z)") }
            )
            FilterChip(
                selected = pathData.startsWith("M 50 15 C"),
                onClick = {
                    pathData = "M 50 15 C 30 -5, 0 10, 0 40 C 0 70, 50 95, 50 95 C 50 95, 100 70, 100 40 C 100 10, 70 -5, 50 15 Z"
                    viewportWidth = 100.0; viewportHeight = 100.0
                    fillColorHex = "#E91E63"
                    runSvg()
                },
                label = { Text("Heart Shape (Cubic)") }
            )
            FilterChip(
                selected = pathData.startsWith("M 50 0 L 61 35"),
                onClick = {
                    pathData = "M 50 0 L 61 35 L 98 35 L 68 57 L 79 91 L 50 70 L 21 91 L 32 57 L 2 35 L 39 35 Z"
                    viewportWidth = 100.0; viewportHeight = 100.0
                    fillColorHex = "#FFC107"
                    runSvg()
                },
                label = { Text("5-Point Star") }
            )
        }

        ToolInputField(
            value = pathData,
            onValueChange = {
                pathData = it
                runSvg()
            },
            label = "SVG Path Data (d='...')",
            placeholder = "M 10 80 Q 52.5 10, 95 80 T 180 80 Z",
            minLines = 4
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = viewportWidth.toString(),
                onValueChange = { it.toDoubleOrNull()?.let { v -> viewportWidth = v } },
                label = "Viewport Width",
                placeholder = "100.0",
                modifier = Modifier.weight(1f),
                minLines = 1
            )
            ToolInputField(
                value = viewportHeight.toString(),
                onValueChange = { it.toDoubleOrNull()?.let { v -> viewportHeight = v } },
                label = "Viewport Height",
                placeholder = "100.0",
                modifier = Modifier.weight(1f),
                minLines = 1
            )
            ToolInputField(
                value = fillColorHex,
                onValueChange = {
                    fillColorHex = it
                    runSvg()
                },
                label = "Fill Color",
                placeholder = "#3F51B5",
                modifier = Modifier.weight(1f),
                minLines = 1
            )
        }

        Button(onClick = { runSvg() }, modifier = Modifier.fillMaxWidth()) {
            Text("Inspect Geometry & Generate Android Vector")
        }
    }
}
