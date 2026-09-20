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
import com.offline.toolbox.tools.color.ApcaContrastInput
import com.offline.toolbox.tools.color.WcagApcaContrastTool
import com.offline.toolbox.tools.data.HexDumpInput
import com.offline.toolbox.tools.data.HexDumpInputMode
import com.offline.toolbox.tools.data.HexDumpViewerTool
import com.offline.toolbox.tools.data.XmlJsonDirection
import com.offline.toolbox.tools.data.XmlJsonInput
import com.offline.toolbox.tools.data.XmlToJsonConverterTool
import com.offline.toolbox.tools.developer.DnsParserInput
import com.offline.toolbox.tools.developer.DnsRecordParserTool
import com.offline.toolbox.tools.developer.DockerComposeInput
import com.offline.toolbox.tools.developer.DockerComposeValidatorTool
import com.offline.toolbox.tools.developer.SqlDialect
import com.offline.toolbox.tools.developer.SqlDialectConverterTool
import com.offline.toolbox.tools.developer.SqlDialectInput
import com.offline.toolbox.tools.developer.SubnetSupernetCalculatorTool
import com.offline.toolbox.tools.developer.SubnetSupernetInput
import com.offline.toolbox.tools.math.ActivityLevel
import com.offline.toolbox.tools.math.BiologicalGender
import com.offline.toolbox.tools.math.BmrInput
import com.offline.toolbox.tools.math.BmrTdeeCalculatorTool
import com.offline.toolbox.tools.math.PolynomialDegree
import com.offline.toolbox.tools.math.PolynomialInput
import com.offline.toolbox.tools.math.PolynomialRootSolverTool
import com.offline.toolbox.tools.math.VectorInput
import com.offline.toolbox.tools.math.VectorMathCalculatorTool
import com.offline.toolbox.tools.media.AudioFrequencyIntervalTool
import com.offline.toolbox.tools.media.AudioIntervalInput
import com.offline.toolbox.tools.media.MusicalInterval
import com.offline.toolbox.tools.security.ShamirInput
import com.offline.toolbox.tools.security.ShamirMode
import com.offline.toolbox.tools.security.ShamirSecretSharingTool
import com.offline.toolbox.tools.security.SteganographyTextTool
import com.offline.toolbox.tools.security.StegoInput
import com.offline.toolbox.tools.security.StegoMode
import com.offline.toolbox.tools.text.MarkdownTableFormatterTool
import com.offline.toolbox.tools.text.StringSimilarityInput
import com.offline.toolbox.tools.text.StringSimilarityTool
import com.offline.toolbox.tools.text.TableAlignment
import com.offline.toolbox.tools.text.TableFormatterInput
import kotlinx.coroutines.launch

@Composable
fun SubnetSupernetCalculatorUI(onResultUpdated: (String, String?) -> Unit) {
    var subnetsText by remember {
        mutableStateOf("192.168.0.0/24\n192.168.1.0/24\n192.168.2.0/24\n192.168.3.0/24")
    }
    val tool = remember { SubnetSupernetCalculatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(SubnetSupernetInput(subnetList = subnetsText))
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
            value = subnetsText,
            onValueChange = { subnetsText = it; run() },
            label = "IPv4 Subnet List (One per line e.g. 192.168.1.0/24)"
        )
    }
}

@Composable
fun DnsRecordParserUI(onResultUpdated: (String, String?) -> Unit) {
    var zoneText by remember {
        mutableStateOf("""
            example.com.        3600    IN  A       93.184.216.34
            example.com.        3600    IN  AAAA    2606:2800:220:1:248:1893:25c8:1946
            example.com.        86400   IN  NS      ns1.example.com.
            mail.example.com.   3600    IN  CNAME   example.com.
            example.com.        3600    IN  MX      10 mail.example.com.
            example.com.        300     IN  TXT     "v=spf1 -all"
        """.trimIndent())
    }
    val tool = remember { DnsRecordParserTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(DnsParserInput(zoneContent = zoneText))
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
            value = zoneText,
            onValueChange = { zoneText = it; run() },
            label = "DNS Zone / Resource Records Text"
        )
    }
}

@Composable
fun DockerComposeValidatorUI(onResultUpdated: (String, String?) -> Unit) {
    var yamlText by remember {
        mutableStateOf("""
            version: '3.8'
            services:
              web:
                image: nginx:alpine
                ports:
                  - "80:80"
                  - "443:443"
                depends_on:
                  - api
              api:
                image: myapp:latest
                ports:
                  - "8080:8080"
                depends_on:
                  - db
              db:
                image: postgres:15-alpine
                environment:
                  POSTGRES_DB: app
                  POSTGRES_PASSWORD: secret
        """.trimIndent())
    }
    val tool = remember { DockerComposeValidatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(DockerComposeInput(yamlContent = yamlText))
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
            value = yamlText,
            onValueChange = { yamlText = it; run() },
            label = "Docker Compose YAML Content"
        )
    }
}

@Composable
fun SqlDialectConverterUI(onResultUpdated: (String, String?) -> Unit) {
    var sqlText by remember {
        mutableStateOf("""
            CREATE TABLE users (
                id SERIAL PRIMARY KEY,
                username VARCHAR(50) NOT NULL,
                is_active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """.trimIndent())
    }
    var sourceDialect by remember { mutableStateOf(SqlDialect.POSTGRESQL) }
    var targetDialect by remember { mutableStateOf(SqlDialect.MYSQL) }
    val tool = remember { SqlDialectConverterTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(SqlDialectInput(
                sqlContent = sqlText,
                sourceDialect = sourceDialect,
                targetDialect = targetDialect
            ))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(sourceDialect, targetDialect) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Source Dialect:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SqlDialect.values().forEach { d ->
                FilterChip(
                    selected = sourceDialect == d,
                    onClick = { sourceDialect = d },
                    label = { Text(d.displayName) }
                )
            }
        }
        Text("Target Dialect:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SqlDialect.values().forEach { d ->
                FilterChip(
                    selected = targetDialect == d,
                    onClick = { targetDialect = d },
                    label = { Text(d.displayName) }
                )
            }
        }
        ToolInputField(
            value = sqlText,
            onValueChange = { sqlText = it; run() },
            label = "SQL Query / Schema Input"
        )
    }
}

@Composable
fun ShamirSecretSharingUI(onResultUpdated: (String, String?) -> Unit) {
    var mode by remember { mutableStateOf(ShamirMode.SPLIT) }
    var secretText by remember { mutableStateOf("Master Recovery Key 2026") }
    var thresholdK by remember { mutableIntStateOf(3) }
    var totalN by remember { mutableIntStateOf(5) }
    var sharesInput by remember { mutableStateOf("") }
    val tool = remember { ShamirSecretSharingTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(ShamirInput(
                mode = mode,
                secretText = if (mode == ShamirMode.SPLIT) secretText else sharesInput,
                thresholdK = thresholdK,
                totalSharesN = totalN
            ))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(mode, thresholdK, totalN) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == ShamirMode.SPLIT,
                onClick = { mode = ShamirMode.SPLIT },
                label = { Text("Split Secret") }
            )
            FilterChip(
                selected = mode == ShamirMode.COMBINE,
                onClick = { mode = ShamirMode.COMBINE },
                label = { Text("Combine Shares") }
            )
        }

        if (mode == ShamirMode.SPLIT) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Threshold (K): $thresholdK", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(2, 3, 4, 5).forEach { k ->
                            FilterChip(
                                selected = thresholdK == k,
                                onClick = { thresholdK = k; if (totalN < k) totalN = k },
                                label = { Text("$k") }
                            )
                        }
                    }
                }
                Column {
                    Text("Total Shares (N): $totalN", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(3, 5, 7, 10).forEach { n ->
                            FilterChip(
                                selected = totalN == n,
                                onClick = { totalN = n; if (thresholdK > n) thresholdK = n },
                                label = { Text("$n") }
                            )
                        }
                    }
                }
            }
            ToolInputField(
                value = secretText,
                onValueChange = { secretText = it; run() },
                label = "Confidential Secret to Split"
            )
        } else {
            ToolInputField(
                value = sharesInput,
                onValueChange = { sharesInput = it; run() },
                label = "Enter Cryptographic Shares (One per line e.g. 1-6a8f...)"
            )
        }
    }
}

@Composable
fun SteganographyTextUI(onResultUpdated: (String, String?) -> Unit) {
    var mode by remember { mutableStateOf(StegoMode.ENCODE) }
    var coverText by remember {
        mutableStateOf("The weekly team sync will take place in Conference Room B tomorrow at 10 AM.")
    }
    var secretText by remember { mutableStateOf("Project Phoenix Launch Alpha") }
    var payloadText by remember { mutableStateOf("") }
    val tool = remember { SteganographyTextTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(StegoInput(
                mode = mode,
                coverText = coverText,
                hiddenMessage = secretText,
                stegoPayloadToDecode = payloadText
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == StegoMode.ENCODE,
                onClick = { mode = StegoMode.ENCODE },
                label = { Text("Encode (Hide)") }
            )
            FilterChip(
                selected = mode == StegoMode.DECODE,
                onClick = { mode = StegoMode.DECODE },
                label = { Text("Decode (Reveal)") }
            )
        }

        if (mode == StegoMode.ENCODE) {
            ToolInputField(
                value = coverText,
                onValueChange = { coverText = it; run() },
                label = "Innocent Cover Text"
            )
            ToolInputField(
                value = secretText,
                onValueChange = { secretText = it; run() },
                label = "Secret Message to Hide Invisibly"
            )
        } else {
            ToolInputField(
                value = payloadText,
                onValueChange = { payloadText = it; run() },
                label = "Text Containing Hidden Steganography Payload"
            )
        }
    }
}

@Composable
fun XmlToJsonConverterUI(onResultUpdated: (String, String?) -> Unit) {
    var direction by remember { mutableStateOf(XmlJsonDirection.XML_TO_JSON) }
    var content by remember {
        mutableStateOf("""
            <user id="101" status="active">
                <profile>
                    <name>Alex Vance</name>
                    <role>Scientist</role>
                </profile>
                <skills>
                    <skill>Physics</skill>
                    <skill>Robotics</skill>
                </skills>
            </user>
        """.trimIndent())
    }
    val tool = remember { XmlToJsonConverterTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(XmlJsonInput(content = content, direction = direction))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(direction) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = direction == XmlJsonDirection.XML_TO_JSON,
                onClick = { direction = XmlJsonDirection.XML_TO_JSON },
                label = { Text("XML → JSON") }
            )
            FilterChip(
                selected = direction == XmlJsonDirection.JSON_TO_XML,
                onClick = { direction = XmlJsonDirection.JSON_TO_XML },
                label = { Text("JSON → XML") }
            )
        }
        ToolInputField(
            value = content,
            onValueChange = { content = it; run() },
            label = if (direction == XmlJsonDirection.XML_TO_JSON) "XML Document Input" else "JSON Object Input"
        )
    }
}

@Composable
fun HexDumpViewerUI(onResultUpdated: (String, String?) -> Unit) {
    var content by remember { mutableStateOf("AZR Offline Android Utility Toolbox 2026\nSovereign Suite") }
    var inputMode by remember { mutableStateOf(HexDumpInputMode.PLAIN_TEXT) }
    var bytesPerLine by remember { mutableIntStateOf(16) }
    val tool = remember { HexDumpViewerTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(HexDumpInput(
                content = content,
                inputMode = inputMode,
                bytesPerLine = bytesPerLine
            ))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(inputMode, bytesPerLine) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = inputMode == HexDumpInputMode.PLAIN_TEXT,
                onClick = { inputMode = HexDumpInputMode.PLAIN_TEXT },
                label = { Text("Plain Text") }
            )
            FilterChip(
                selected = inputMode == HexDumpInputMode.HEX_STRING,
                onClick = { inputMode = HexDumpInputMode.HEX_STRING },
                label = { Text("Raw Hex String") }
            )
        }
        ToolInputField(
            value = content,
            onValueChange = { content = it; run() },
            label = if (inputMode == HexDumpInputMode.PLAIN_TEXT) "Text Payload" else "Hex Bytes (e.g. 415a5220)"
        )
    }
}

@Composable
fun PolynomialRootSolverUI(onResultUpdated: (String, String?) -> Unit) {
    var degree by remember { mutableStateOf(PolynomialDegree.QUADRATIC) }
    var a by remember { mutableDoubleStateOf(1.0) }
    var b by remember { mutableDoubleStateOf(-5.0) }
    var c by remember { mutableDoubleStateOf(6.0) }
    var d by remember { mutableDoubleStateOf(0.0) }
    val tool = remember { PolynomialRootSolverTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(PolynomialInput(degree = degree, a = a, b = b, c = c, d = d))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(degree, a, b, c, d) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = degree == PolynomialDegree.QUADRATIC,
                onClick = { degree = PolynomialDegree.QUADRATIC },
                label = { Text("Quadratic (ax² + bx + c = 0)") }
            )
            FilterChip(
                selected = degree == PolynomialDegree.CUBIC,
                onClick = { degree = PolynomialDegree.CUBIC },
                label = { Text("Cubic (ax³ + bx² + cx + d = 0)") }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = a.toString(),
                onValueChange = { a = it.toDoubleOrNull() ?: a; run() },
                label = "Coeff a",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = b.toString(),
                onValueChange = { b = it.toDoubleOrNull() ?: b; run() },
                label = "Coeff b",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = c.toString(),
                onValueChange = { c = it.toDoubleOrNull() ?: c; run() },
                label = "Coeff c",
                modifier = Modifier.weight(1f)
            )
            if (degree == PolynomialDegree.CUBIC) {
                ToolInputField(
                    value = d.toString(),
                    onValueChange = { d = it.toDoubleOrNull() ?: d; run() },
                    label = "Coeff d",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VectorMathCalculatorUI(onResultUpdated: (String, String?) -> Unit) {
    var ax by remember { mutableDoubleStateOf(1.0) }
    var ay by remember { mutableDoubleStateOf(2.0) }
    var az by remember { mutableDoubleStateOf(3.0) }
    var bx by remember { mutableDoubleStateOf(4.0) }
    var by by remember { mutableDoubleStateOf(5.0) }
    var bz by remember { mutableDoubleStateOf(6.0) }
    val tool = remember { VectorMathCalculatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(VectorInput(ax = ax, ay = ay, az = az, bx = bx, by = by, bz = bz))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(ax, ay, az, bx, by, bz) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Vector A:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(value = ax.toString(), onValueChange = { ax = it.toDoubleOrNull() ?: ax; run() }, label = "Ax", modifier = Modifier.weight(1f))
            ToolInputField(value = ay.toString(), onValueChange = { ay = it.toDoubleOrNull() ?: ay; run() }, label = "Ay", modifier = Modifier.weight(1f))
            ToolInputField(value = az.toString(), onValueChange = { az = it.toDoubleOrNull() ?: az; run() }, label = "Az", modifier = Modifier.weight(1f))
        }
        Text("Vector B:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(value = bx.toString(), onValueChange = { bx = it.toDoubleOrNull() ?: bx; run() }, label = "Bx", modifier = Modifier.weight(1f))
            ToolInputField(value = by.toString(), onValueChange = { by = it.toDoubleOrNull() ?: by; run() }, label = "By", modifier = Modifier.weight(1f))
            ToolInputField(value = bz.toString(), onValueChange = { bz = it.toDoubleOrNull() ?: bz; run() }, label = "Bz", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun BmrTdeeCalculatorUI(onResultUpdated: (String, String?) -> Unit) {
    var weight by remember { mutableDoubleStateOf(75.0) }
    var height by remember { mutableDoubleStateOf(178.0) }
    var age by remember { mutableIntStateOf(28) }
    var gender by remember { mutableStateOf(BiologicalGender.MALE) }
    var activity by remember { mutableStateOf(ActivityLevel.MODERATELY_ACTIVE) }
    val tool = remember { BmrTdeeCalculatorTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(BmrInput(
                weightKg = weight,
                heightCm = height,
                ageYears = age,
                gender = gender,
                activityLevel = activity
            ))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(gender, activity) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = gender == BiologicalGender.MALE,
                onClick = { gender = BiologicalGender.MALE },
                label = { Text("Male (+5)") }
            )
            FilterChip(
                selected = gender == BiologicalGender.FEMALE,
                onClick = { gender = BiologicalGender.FEMALE },
                label = { Text("Female (-161)") }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = weight.toString(),
                onValueChange = { weight = it.toDoubleOrNull() ?: weight; run() },
                label = "Weight (kg)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = height.toString(),
                onValueChange = { height = it.toDoubleOrNull() ?: height; run() },
                label = "Height (cm)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = age.toString(),
                onValueChange = { age = it.toIntOrNull() ?: age; run() },
                label = "Age (y)",
                modifier = Modifier.weight(1f)
            )
        }
        Text("Activity Level:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityLevel.values().forEach { lvl ->
                FilterChip(
                    selected = activity == lvl,
                    onClick = { activity = lvl },
                    label = { Text("${lvl.name} (${lvl.multiplier}x)") }
                )
            }
        }
    }
}

@Composable
fun MarkdownTableFormatterUI(onResultUpdated: (String, String?) -> Unit) {
    var tableText by remember {
        mutableStateOf("""
            Name | Role | Salary | Department
            Alice | Senior Architect | $160,000 | Engineering
            Bob | Product Designer | $125,000 | Design
            Charlie | SecOps Lead | $150,000 | Security
        """.trimIndent())
    }
    var alignment by remember { mutableStateOf(TableAlignment.AUTO) }
    val tool = remember { MarkdownTableFormatterTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(TableFormatterInput(content = tableText, alignment = alignment))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(alignment) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TableAlignment.values().forEach { a ->
                FilterChip(
                    selected = alignment == a,
                    onClick = { alignment = a },
                    label = { Text(a.name) }
                )
            }
        }
        ToolInputField(
            value = tableText,
            onValueChange = { tableText = it; run() },
            label = "Raw Table Data (Pipes, TSV, or CSV)"
        )
    }
}

@Composable
fun StringSimilarityUI(onResultUpdated: (String, String?) -> Unit) {
    var textA by remember { mutableStateOf("kitten") }
    var textB by remember { mutableStateOf("sitting") }
    val tool = remember { StringSimilarityTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(StringSimilarityInput(stringA = textA, stringB = textB))
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
            value = textA,
            onValueChange = { textA = it; run() },
            label = "String A"
        )
        ToolInputField(
            value = textB,
            onValueChange = { textB = it; run() },
            label = "String B"
        )
    }
}

@Composable
fun WcagApcaContrastUI(onResultUpdated: (String, String?) -> Unit) {
    var textHex by remember { mutableStateOf("#0F172A") }
    var bgHex by remember { mutableStateOf("#F8FAFC") }
    val tool = remember { WcagApcaContrastTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(ApcaContrastInput(textColorHex = textHex, backgroundColorHex = bgHex))
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
            ToolInputField(
                value = textHex,
                onValueChange = { textHex = it; run() },
                label = "Text Hex (#0F172A)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = bgHex,
                onValueChange = { bgHex = it; run() },
                label = "Background Hex (#F8FAFC)",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AudioFrequencyIntervalUI(onResultUpdated: (String, String?) -> Unit) {
    var freq by remember { mutableDoubleStateOf(440.0) }
    var interval by remember { mutableStateOf(MusicalInterval.PERFECT_FIFTH) }
    val tool = remember { AudioFrequencyIntervalTool() }
    val scope = rememberCoroutineScope()

    fun run() {
        scope.launch {
            val res = tool.execute(AudioIntervalInput(baseFrequencyHz = freq, interval = interval))
            if (res is ToolResult.Success) {
                onResultUpdated(res.data.formattedReport, res.data.summary)
            } else if (res is ToolResult.Failure) {
                onResultUpdated("Error: ${res.message}", null)
            }
        }
    }

    LaunchedEffect(interval) { run() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolInputField(
            value = freq.toString(),
            onValueChange = { freq = it.toDoubleOrNull() ?: freq; run() },
            label = "Base Pitch Frequency (Hz) e.g. 440.0"
        )
        Text("Musical Interval:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MusicalInterval.values().forEach { i ->
                FilterChip(
                    selected = interval == i,
                    onClick = { interval = i },
                    label = { Text(i.displayName) }
                )
            }
        }
    }
}
