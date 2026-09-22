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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offline.toolbox.core.designsystem.components.ToolInputField
import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.color.ColorShadeTintGeneratorTool
import com.offline.toolbox.tools.color.ColorShadeTintInput
import com.offline.toolbox.tools.data.OpenLocationCodeTool
import com.offline.toolbox.tools.data.PlusCodeInput
import com.offline.toolbox.tools.data.TsvAggregateInput
import com.offline.toolbox.tools.data.TsvFilterAggregateTool
import com.offline.toolbox.tools.developer.EnvFileLinterInput
import com.offline.toolbox.tools.developer.EnvFileSecurityLinterTool
import com.offline.toolbox.tools.developer.SemverRangeInput
import com.offline.toolbox.tools.developer.SemverRangeSolverTool
import com.offline.toolbox.tools.developer.SqlIndexAdvisorInput
import com.offline.toolbox.tools.developer.SqlIndexAdvisorTool
import com.offline.toolbox.tools.developer.TerraformHclInput
import com.offline.toolbox.tools.developer.TerraformHclValidatorTool
import com.offline.toolbox.tools.math.DewPointInput
import com.offline.toolbox.tools.math.DewPointRelativeHumidityTool
import com.offline.toolbox.tools.math.KeplerOrbitalInput
import com.offline.toolbox.tools.math.KeplerOrbitalPeriodTool
import com.offline.toolbox.tools.math.OhmsLawInput
import com.offline.toolbox.tools.math.OhmsLawPowerTool
import com.offline.toolbox.tools.media.LufsLoudnessInput
import com.offline.toolbox.tools.media.LufsLoudnessMeterTool
import com.offline.toolbox.tools.security.BifidCipherInput
import com.offline.toolbox.tools.security.BifidCipherTool
import com.offline.toolbox.tools.security.ScryptCostEstimatorTool
import com.offline.toolbox.tools.security.ScryptCostInput
import com.offline.toolbox.tools.text.AtbashCipherTool
import com.offline.toolbox.tools.text.AtbashInput
import com.offline.toolbox.tools.text.JaroWinklerDistanceTool
import com.offline.toolbox.tools.text.JaroWinklerInput
import kotlinx.coroutines.launch

// 1. SQL Query Index Advisor UI
@Composable
fun SqlIndexAdvisorUI(onResultUpdated: (String, String?) -> Unit) {
    var queryText by remember {
        mutableStateOf(
            """SELECT u.id, u.email, o.order_date, o.total_amount
FROM users u
INNER JOIN orders o ON u.id = o.user_id
WHERE u.status = 'active'
  AND o.order_date >= '2026-01-01'
ORDER BY o.order_date DESC;"""
        )
    }
    var dialect by remember { mutableStateOf("SQLITE") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { SqlIndexAdvisorTool() }

    fun runAdvisor() {
        coroutineScope.launch {
            when (val result = tool.execute(SqlIndexAdvisorInput(queryText, dialect))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(dialect) { runAdvisor() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("SQLITE" to "SQLite", "POSTGRES" to "PostgreSQL", "MYSQL" to "MySQL").forEach { (d, label) ->
                FilterChip(
                    selected = dialect == d,
                    onClick = { dialect = d },
                    label = { Text(label) }
                )
            }
        }

        ToolInputField(
            value = queryText,
            onValueChange = { queryText = it },
            label = "SQL Query",
            placeholder = "Enter SELECT query...",
            minLines = 6,
            maxLines = 12
        )

        Button(
            onClick = { runAdvisor() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Analyze Query & Generate Index DDL")
        }
    }
}

// 2. Terraform HCL Syntax & Security Linter UI
@Composable
fun TerraformHclValidatorUI(onResultUpdated: (String, String?) -> Unit) {
    var hclText by remember {
        mutableStateOf(
            """terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

resource "aws_s3_bucket" "secure_bucket" {
  bucket = "offline-storage-bucket"
}

output "bucket_id" {
  value = aws_s3_bucket.secure_bucket.id
}"""
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { TerraformHclValidatorTool() }

    fun runLint() {
        coroutineScope.launch {
            when (val result = tool.execute(TerraformHclInput(hclText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runLint() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = hclText,
            onValueChange = { hclText = it },
            label = "Terraform HCL Manifest",
            placeholder = "Paste HCL configuration...",
            minLines = 8,
            maxLines = 14
        )

        Button(
            onClick = { runLint() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Validate HCL Syntax & Security Posture")
        }
    }
}

// 3. SemVer Range & Version Solver UI
@Composable
fun SemverRangeSolverUI(onResultUpdated: (String, String?) -> Unit) {
    var rangeExpr by remember { mutableStateOf("^1.2.0 || >=2.1.0 <3.0.0") }
    var candidates by remember {
        mutableStateOf("1.1.0, 1.2.0, 1.2.4, 1.3.0, 2.0.0, 2.1.0, 2.4.9, 3.0.0, 3.1.2")
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { SemverRangeSolverTool() }

    fun runResolve() {
        coroutineScope.launch {
            when (val result = tool.execute(SemverRangeInput(rangeExpr, candidates))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runResolve() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = rangeExpr,
            onValueChange = { rangeExpr = it },
            label = "SemVer Range Expression",
            placeholder = "e.g. ^1.2.3, ~2.0.0, >=1.0 <2.5, ||"
        )

        ToolInputField(
            value = candidates,
            onValueChange = { candidates = it },
            label = "Candidate Release Versions",
            placeholder = "Comma-separated versions...",
            minLines = 3,
            maxLines = 6
        )

        Button(
            onClick = { runResolve() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resolve Optimal Compatible Version")
        }
    }
}

// 4. Dotenv (.env) Security Linter UI
@Composable
fun EnvFileSecurityLinterUI(onResultUpdated: (String, String?) -> Unit) {
    var envText by remember {
        mutableStateOf(
            """PORT=8080
NODE_ENV=production
DATABASE_URL=postgres://app_user:s3cr3tP@ssw0rd@db.internal:5432/prod_db
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
DEBUG=false
PORT=3000"""
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { EnvFileSecurityLinterTool() }

    fun runAudit() {
        coroutineScope.launch {
            when (val result = tool.execute(EnvFileLinterInput(envText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runAudit() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = envText,
            onValueChange = { envText = it },
            label = "Dotenv (.env) File Content",
            placeholder = "Paste environment configuration...",
            minLines = 6,
            maxLines = 12
        )

        Button(
            onClick = { runAudit() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Audit Secrets & Generate Sanitized Example")
        }
    }
}

// 5. Bifid Delastelle Cipher UI
@Composable
fun BifidCipherUI(onResultUpdated: (String, String?) -> Unit) {
    var op by remember { mutableStateOf("ENCRYPT") }
    var text by remember { mutableStateOf("DEFEND THE EAST WALL") }
    var period by remember { mutableIntStateOf(5) }
    var keyword by remember { mutableStateOf("BIFID") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { BifidCipherTool() }

    fun runCipher() {
        coroutineScope.launch {
            when (val result = tool.execute(BifidCipherInput(op, text, period, keyword))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(op) { runCipher() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = op == "ENCRYPT",
                onClick = { op = "ENCRYPT" },
                label = { Text("Encrypt Bifid") }
            )
            FilterChip(
                selected = op == "DECRYPT",
                onClick = { op = "DECRYPT" },
                label = { Text("Decrypt Bifid") }
            )
        }

        ToolInputField(
            value = text,
            onValueChange = { text = it },
            label = if (op == "ENCRYPT") "Plaintext" else "Ciphertext",
            placeholder = "Enter text...",
            minLines = 2,
            maxLines = 4
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = period.toString(),
                onValueChange = { period = it.toIntOrNull()?.coerceIn(1, 20) ?: period },
                label = "Period Block Size (P)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = keyword,
                onValueChange = { keyword = it },
                label = "Grid Keyword",
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { runCipher() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (op == "ENCRYPT") "Fractionate & Encrypt with Bifid" else "Reconstruct & Decrypt with Bifid")
        }
    }
}

// 6. scrypt Memory Cost Estimator UI
@Composable
fun ScryptCostEstimatorUI(onResultUpdated: (String, String?) -> Unit) {
    var nValue by remember { mutableLongStateOf(16384L) }
    var rValue by remember { mutableIntStateOf(8) }
    var pValue by remember { mutableIntStateOf(1) }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { ScryptCostEstimatorTool() }

    fun runCost() {
        coroutineScope.launch {
            when (val result = tool.execute(ScryptCostInput(nValue, rValue, pValue, 32))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(nValue, rValue, pValue) { runCost() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Standard Profiles (Cost Factor N):", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                16384L to "Mobile (16 MiB)",
                32768L to "Web (32 MiB)",
                65536L to "Server (64 MiB)",
                1048576L to "Vault (1 GiB)"
            ).forEach { (n, label) ->
                FilterChip(
                    selected = nValue == n,
                    onClick = { nValue = n },
                    label = { Text(label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = rValue.toString(),
                onValueChange = { rValue = it.toIntOrNull()?.coerceIn(1, 64) ?: rValue },
                label = "Block Size r (default 8)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = pValue.toString(),
                onValueChange = { pValue = it.toIntOrNull()?.coerceIn(1, 32) ?: pValue },
                label = "Parallelism p (default 1)",
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { runCost() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate scrypt RAM Footprint & Security")
        }
    }
}

// 7. Google Plus Codes (Open Location Code) UI
@Composable
fun OpenLocationCodeUI(onResultUpdated: (String, String?) -> Unit) {
    var op by remember { mutableStateOf("ENCODE") }
    var latText by remember { mutableStateOf("37.774929") }
    var lonText by remember { mutableStateOf("-122.419416") }
    var plusCodeText by remember { mutableStateOf("849VQGW8+X6") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { OpenLocationCodeTool() }

    fun runCode() {
        val lat = latText.toDoubleOrNull() ?: 37.774929
        val lon = lonText.toDoubleOrNull() ?: -122.419416
        coroutineScope.launch {
            when (val result = tool.execute(PlusCodeInput(op, lat, lon, plusCodeText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(op) { runCode() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = op == "ENCODE",
                onClick = { op = "ENCODE" },
                label = { Text("Encode Lat/Lon") }
            )
            FilterChip(
                selected = op == "DECODE",
                onClick = { op = "DECODE" },
                label = { Text("Decode Plus Code") }
            )
        }

        if (op == "ENCODE") {
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
        } else {
            ToolInputField(
                value = plusCodeText,
                onValueChange = { plusCodeText = it },
                label = "Plus Code (e.g. 849VQGW8+X6)",
                placeholder = "Enter 8-11 character code..."
            )
        }

        Button(
            onClick = { runCode() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (op == "ENCODE") "Generate Google Plus Code" else "Decode to Bounding Box & Center")
        }
    }
}

// 8. TSV Filter & Column Aggregator UI
@Composable
fun TsvFilterAggregateUI(onResultUpdated: (String, String?) -> Unit) {
    var tsvText by remember {
        mutableStateOf(
            """category	item	quantity	price
Electronics	Laptop	5	999.50
Furniture	Chair	12	89.00
Electronics	Mouse	25	29.99
Furniture	Desk	4	250.00
Electronics	Monitor	8	199.99
Office	Paper	50	5.50
Office	Pen	100	1.25"""
        )
    }
    var aggCol by remember { mutableStateOf("price") }
    var groupCol by remember { mutableStateOf("category") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { TsvFilterAggregateTool() }

    fun runAggregate() {
        coroutineScope.launch {
            when (val result = tool.execute(TsvAggregateInput(tsvText, aggregateColumn = aggCol, groupByColumn = groupCol))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runAggregate() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = tsvText,
            onValueChange = { tsvText = it },
            label = "TSV Content (Tab Delimited)",
            placeholder = "Paste TSV text...",
            minLines = 5,
            maxLines = 10
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = aggCol,
                onValueChange = { aggCol = it },
                label = "Aggregate Numeric Column",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = groupCol,
                onValueChange = { groupCol = it },
                label = "Group By Column (Optional)",
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { runAggregate() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compute Stream Statistics & Pivot Summary")
        }
    }
}

// 9. Ohm's Law & Joule's Power Solver UI
@Composable
fun OhmsLawPowerUI(onResultUpdated: (String, String?) -> Unit) {
    var vText by remember { mutableStateOf("12.0") }
    var iText by remember { mutableStateOf("") }
    var rText by remember { mutableStateOf("4.0") }
    var pText by remember { mutableStateOf("") }
    var bandsText by remember { mutableStateOf("Brown, Black, Red, Gold") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { OhmsLawPowerTool() }

    fun runSolve() {
        val v = vText.toDoubleOrNull()
        val i = iText.toDoubleOrNull()
        val r = rText.toDoubleOrNull()
        val p = pText.toDoubleOrNull()

        coroutineScope.launch {
            when (val result = tool.execute(OhmsLawInput(v, i, r, p, bandsText))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(Unit) { runSolve() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Provide any two known parameters:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = vText,
                onValueChange = { vText = it },
                label = "Voltage V (Volts)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = iText,
                onValueChange = { iText = it },
                label = "Current I (Amps)",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = rText,
                onValueChange = { rText = it },
                label = "Resistance R (Ohms Ω)",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = pText,
                onValueChange = { pText = it },
                label = "Power P (Watts)",
                modifier = Modifier.weight(1f)
            )
        }

        ToolInputField(
            value = bandsText,
            onValueChange = { bandsText = it },
            label = "Resistor Color Bands (4 or 5 bands)",
            placeholder = "e.g. Brown, Black, Red, Gold"
        )

        Button(
            onClick = { runSolve() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solve Ohm's Law & Circuit Metrics")
        }
    }
}

// 10. Dew Point & Vapor Pressure Calculator UI
@Composable
fun DewPointRelativeHumidityUI(onResultUpdated: (String, String?) -> Unit) {
    var tempText by remember { mutableStateOf("25.0") }
    var unit by remember { mutableStateOf("CELSIUS") }
    var rhText by remember { mutableStateOf("60.0") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { DewPointRelativeHumidityTool() }

    fun runCalc() {
        val t = tempText.toDoubleOrNull() ?: 25.0
        val rh = rhText.toDoubleOrNull() ?: 60.0

        coroutineScope.launch {
            when (val result = tool.execute(DewPointInput(t, unit, rh))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(unit) { runCalc() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = tempText,
                onValueChange = { tempText = it },
                label = "Temperature (${if (unit == "CELSIUS") "°C" else "°F"})",
                modifier = Modifier.weight(1.5f)
            )
            FilterChip(
                selected = unit == "CELSIUS",
                onClick = { unit = if (unit == "CELSIUS") "FAHRENHEIT" else "CELSIUS" },
                label = { Text(unit.take(1)) },
                modifier = Modifier.weight(0.5f)
            )
        }

        ToolInputField(
            value = rhText,
            onValueChange = { rhText = it },
            label = "Relative Humidity (%)"
        )

        Button(
            onClick = { runCalc() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compute Dew Point & Vapor Pressure")
        }
    }
}

// 11. Kepler's 3rd Law & Orbital Mechanics UI
@Composable
fun KeplerOrbitalPeriodUI(onResultUpdated: (String, String?) -> Unit) {
    var mode by remember { mutableStateOf("PERIOD_FROM_ALTITUDE") }
    var body by remember { mutableStateOf("EARTH") }
    var altText by remember { mutableStateOf("420.0") }
    var periodText by remember { mutableStateOf("23.934") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { KeplerOrbitalPeriodTool() }

    fun runOrbit() {
        val alt = altText.toDoubleOrNull() ?: 420.0
        val p = periodText.toDoubleOrNull() ?: 24.0

        coroutineScope.launch {
            when (val result = tool.execute(KeplerOrbitalInput(mode, body, alt, p))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(mode, body) { runOrbit() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("EARTH" to "Earth", "SUN" to "Sun", "MOON" to "Moon", "MARS" to "Mars").forEach { (b, label) ->
                FilterChip(
                    selected = body == b,
                    onClick = { body = b },
                    label = { Text(label) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == "PERIOD_FROM_ALTITUDE",
                onClick = { mode = "PERIOD_FROM_ALTITUDE" },
                label = { Text("Period from Altitude") }
            )
            FilterChip(
                selected = mode == "ALTITUDE_FROM_PERIOD",
                onClick = { mode = "ALTITUDE_FROM_PERIOD" },
                label = { Text("Altitude from Period") }
            )
        }

        if (mode == "PERIOD_FROM_ALTITUDE") {
            ToolInputField(
                value = altText,
                onValueChange = { altText = it },
                label = "Orbital Altitude Above Surface (km)"
            )
        } else {
            ToolInputField(
                value = periodText,
                onValueChange = { periodText = it },
                label = "Target Orbital Period (Hours)"
            )
        }

        Button(
            onClick = { runOrbit() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compute Orbital Velocity & Period")
        }
    }
}

// 12. Jaro-Winkler Similarity & Distance UI
@Composable
fun JaroWinklerDistanceUI(onResultUpdated: (String, String?) -> Unit) {
    var str1 by remember { mutableStateOf("martha") }
    var str2 by remember { mutableStateOf("marhta") }
    var caseSensitive by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { JaroWinklerDistanceTool() }

    fun runSimilarity() {
        coroutineScope.launch {
            when (val result = tool.execute(JaroWinklerInput(str1, str2, caseSensitive))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(caseSensitive) { runSimilarity() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = str1,
                onValueChange = { str1 = it },
                label = "String A",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = str2,
                onValueChange = { str2 = it },
                label = "String B",
                modifier = Modifier.weight(1f)
            )
        }

        FilterChip(
            selected = caseSensitive,
            onClick = { caseSensitive = !caseSensitive },
            label = { Text("Case Sensitive Comparison") }
        )

        Button(
            onClick = { runSimilarity() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compute Jaro-Winkler Metric & Transpositions")
        }
    }
}

// 13. Atbash Reciprocal Substitution Cipher UI
@Composable
fun AtbashCipherUI(onResultUpdated: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG") }
    var reverseDigits by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { AtbashCipherTool() }

    fun runAtbash() {
        coroutineScope.launch {
            when (val result = tool.execute(AtbashInput(text, reverseDigits))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(reverseDigits) { runAtbash() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = text,
            onValueChange = { text = it },
            label = "Input Text (A ↔ Z Reciprocal)",
            placeholder = "Enter text to encode/decode...",
            minLines = 3,
            maxLines = 6
        )

        FilterChip(
            selected = reverseDigits,
            onClick = { reverseDigits = !reverseDigits },
            label = { Text("Also Reflect Digits (0 ↔ 9)") }
        )

        Button(
            onClick = { runAtbash() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Transform via Symmetric Atbash")
        }
    }
}

// 14. Broadcast Audio LUFS & True Peak Normalizer UI
@Composable
fun LufsLoudnessMeterUI(onResultUpdated: (String, String?) -> Unit) {
    var lufsText by remember { mutableStateOf("-11.5") }
    var tpText by remember { mutableStateOf("-0.3") }
    var lraText by remember { mutableStateOf("6.5") }
    var platform by remember { mutableStateOf("SPOTIFY") }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { LufsLoudnessMeterTool() }

    fun runLoudness() {
        val lufs = lufsText.toDoubleOrNull() ?: -14.0
        val tp = tpText.toDoubleOrNull() ?: -1.0
        val lra = lraText.toDoubleOrNull() ?: 7.0

        coroutineScope.launch {
            when (val result = tool.execute(LufsLoudnessInput(lufs, tp, lra, platform))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(platform) { runLoudness() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "SPOTIFY" to "Spotify (-14)",
                "APPLE" to "Apple (-16)",
                "YOUTUBE" to "YouTube (-14)",
                "EBU" to "EBU R128 (-23)"
            ).forEach { (p, label) ->
                FilterChip(
                    selected = platform == p,
                    onClick = { platform = p },
                    label = { Text(label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolInputField(
                value = lufsText,
                onValueChange = { lufsText = it },
                label = "Integrated LUFS",
                modifier = Modifier.weight(1f)
            )
            ToolInputField(
                value = tpText,
                onValueChange = { tpText = it },
                label = "True Peak (dBTP)",
                modifier = Modifier.weight(1f)
            )
        }

        ToolInputField(
            value = lraText,
            onValueChange = { lraText = it },
            label = "Loudness Range LRA (LU)"
        )

        Button(
            onClick = { runLoudness() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Predict Normalization Gain & Clipping Risk")
        }
    }
}

// 15. Monochromatic Color Shade & Tint Scale Generator UI
@Composable
fun ColorShadeTintGeneratorUI(onResultUpdated: (String, String?) -> Unit) {
    var baseHex by remember { mutableStateOf("#3F51B5") }
    var steps by remember { mutableIntStateOf(10) }

    val coroutineScope = rememberCoroutineScope()
    val tool = remember { ColorShadeTintGeneratorTool() }

    fun runPalette() {
        coroutineScope.launch {
            when (val result = tool.execute(ColorShadeTintInput(baseHex, steps))) {
                is ToolResult.Success -> onResultUpdated(result.data.formattedReport, result.data.summary)
                is ToolResult.Failure -> onResultUpdated("Error: ${result.message}", null)
            }
        }
    }

    LaunchedEffect(steps) { runPalette() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = baseHex,
            onValueChange = { baseHex = it },
            label = "Base Color (HEX)",
            placeholder = "e.g. #3F51B5"
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(5 to "5 Steps", 10 to "10 Steps", 15 to "15 Steps", 20 to "20 Steps").forEach { (s, label) ->
                FilterChip(
                    selected = steps == s,
                    onClick = { steps = s },
                    label = { Text(label) }
                )
            }
        }

        Button(
            onClick = { runPalette() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate Stepped Shade & Tint Palette")
        }
    }
}
