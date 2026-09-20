package com.offline.toolbox.features.toolrunner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offline.toolbox.core.designsystem.components.ToolInputField
import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.converter.DataStorageConverterTool
import com.offline.toolbox.tools.converter.DataStorageInput
import com.offline.toolbox.tools.converter.LengthConverterTool
import com.offline.toolbox.tools.converter.LengthConverterInput
import com.offline.toolbox.tools.converter.LengthUnit
import com.offline.toolbox.tools.converter.RomanMode
import com.offline.toolbox.tools.converter.RomanNumeralConverterTool
import com.offline.toolbox.tools.converter.RomanNumeralInput
import com.offline.toolbox.tools.converter.SpeedConverterTool
import com.offline.toolbox.tools.converter.SpeedConverterInput
import com.offline.toolbox.tools.converter.SpeedUnit
import com.offline.toolbox.tools.converter.StorageUnit
import com.offline.toolbox.tools.converter.TemperatureConverterTool
import com.offline.toolbox.tools.converter.TemperatureConverterInput
import com.offline.toolbox.tools.converter.TemperatureUnit
import com.offline.toolbox.tools.converter.WeightConverterTool
import com.offline.toolbox.tools.converter.WeightConverterInput
import com.offline.toolbox.tools.converter.WeightUnit
import com.offline.toolbox.tools.data.CsvDelimiter
import com.offline.toolbox.tools.data.CsvToJsonInput
import com.offline.toolbox.tools.data.CsvToJsonTool
import com.offline.toolbox.tools.data.JsonToCsvTool
import com.offline.toolbox.tools.datetime.CountdownCalculatorTool
import com.offline.toolbox.tools.datetime.DateAddSubtractInput
import com.offline.toolbox.tools.datetime.DateAddSubtractTool
import com.offline.toolbox.tools.datetime.DateOperation
import com.offline.toolbox.tools.datetime.DateUnit
import com.offline.toolbox.tools.developer.HtmlEntityInput
import com.offline.toolbox.tools.developer.HtmlEntityMode
import com.offline.toolbox.tools.developer.HtmlEntityTool
import com.offline.toolbox.tools.developer.JwtDecoderTool
import com.offline.toolbox.tools.developer.NumberBase
import com.offline.toolbox.tools.developer.NumberBaseConverterTool
import com.offline.toolbox.tools.developer.NumberBaseInput
import com.offline.toolbox.tools.developer.RegexTesterInput
import com.offline.toolbox.tools.developer.RegexTesterTool
import com.offline.toolbox.tools.developer.UnixTimestampTool
import com.offline.toolbox.tools.developer.UrlEncoderInput
import com.offline.toolbox.tools.developer.UrlEncoderTool
import com.offline.toolbox.tools.developer.UrlOperation
import com.offline.toolbox.tools.generator.ChoiceMode
import com.offline.toolbox.tools.generator.RandomChoiceInput
import com.offline.toolbox.tools.generator.RandomChoiceTool
import com.offline.toolbox.tools.generator.RandomNumberConfig
import com.offline.toolbox.tools.generator.RandomNumberTool
import com.offline.toolbox.tools.math.AverageCalculatorTool
import com.offline.toolbox.tools.math.BmiCalculatorTool
import com.offline.toolbox.tools.math.BmiInput
import com.offline.toolbox.tools.math.BmiUnitSystem
import com.offline.toolbox.tools.math.FractionCalculatorTool
import com.offline.toolbox.tools.math.FractionInput
import com.offline.toolbox.tools.math.FractionOperator
import com.offline.toolbox.tools.math.TipCalculatorTool
import com.offline.toolbox.tools.math.TipInput
import com.offline.toolbox.tools.security.HashAlgorithm
import com.offline.toolbox.tools.security.HashCheckerInput
import com.offline.toolbox.tools.security.HashCheckerTool
import com.offline.toolbox.tools.security.PasswordStrengthTool
import com.offline.toolbox.tools.security.UuidConfig
import com.offline.toolbox.tools.security.UuidGeneratorTool
import com.offline.toolbox.tools.text.FindAndReplaceInput
import com.offline.toolbox.tools.text.FindAndReplaceTool
import com.offline.toolbox.tools.text.LineOperation
import com.offline.toolbox.tools.text.LineOperationsInput
import com.offline.toolbox.tools.text.LineOperationsTool
import com.offline.toolbox.tools.text.LoremIpsumGeneratorTool
import com.offline.toolbox.tools.text.LoremIpsumInput
import com.offline.toolbox.tools.text.LoremUnit
import com.offline.toolbox.tools.text.TextCleanerInput
import com.offline.toolbox.tools.text.TextCleanerOptions
import com.offline.toolbox.tools.text.TextCleanerTool
import com.offline.toolbox.tools.text.TextDiffInput
import com.offline.toolbox.tools.text.TextDiffTool
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun TextCleanerUI(
    tool: TextCleanerTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var trimLines by remember { mutableStateOf(true) }
    var removeExtraSpaces by remember { mutableStateOf(true) }
    var removeEmptyLines by remember { mutableStateOf(true) }
    var stripHtml by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runClean() {
        scope.launch {
            val opts = TextCleanerOptions(
                removeExtraSpaces = removeExtraSpaces,
                trimLines = trimLines,
                removeEmptyLines = removeEmptyLines,
                stripHtmlTags = stripHtml
            )
            when (val res = tool.execute(TextCleanerInput(text, opts))) {
                is ToolResult.Success -> onResult(res.data.cleanedText, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    ToolInputField(value = text, onValueChange = { text = it; runClean() }, placeholder = "Paste text to clean whitespace and lines...")
    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Trim Lines")
        Switch(checked = trimLines, onCheckedChange = { trimLines = it; runClean() })
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Remove Duplicate Spaces")
        Switch(checked = removeExtraSpaces, onCheckedChange = { removeExtraSpaces = it; runClean() })
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Remove Blank Lines")
        Switch(checked = removeEmptyLines, onCheckedChange = { removeEmptyLines = it; runClean() })
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Strip HTML Tags")
        Switch(checked = stripHtml, onCheckedChange = { stripHtml = it; runClean() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineOperationsUI(
    tool: LineOperationsTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedOp by remember { mutableStateOf(LineOperation.SORT_AZ) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runOp(op: LineOperation) {
        scope.launch {
            when (val res = tool.execute(LineOperationsInput(text, op))) {
                is ToolResult.Success -> onResult(res.data, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    ToolInputField(value = text, onValueChange = { text = it; runOp(selectedOp) }, placeholder = "Enter multi-line text to sort, deduplicate, or number...")
    Spacer(modifier = Modifier.height(10.dp))

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedOp.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Line Operation") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LineOperation.values().forEach { op ->
                DropdownMenuItem(
                    text = { Text(op.displayName) },
                    onClick = { selectedOp = op; expanded = false; runOp(op) }
                )
            }
        }
    }
}

@Composable
fun FindAndReplaceUI(
    tool: FindAndReplaceTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var findQuery by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runReplace() {
        scope.launch {
            val input = FindAndReplaceInput(text, findQuery, replacement, caseSensitive, wholeWord, useRegex)
            when (val res = tool.execute(input)) {
                is ToolResult.Success -> onResult(res.data.resultText, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    ToolInputField(value = text, onValueChange = { text = it }, placeholder = "Source text to search in...")
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = findQuery, onValueChange = { findQuery = it; runReplace() }, label = { Text("Find Text / Pattern") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(value = replacement, onValueChange = { replacement = it; runReplace() }, label = { Text("Replace With") }, modifier = Modifier.fillMaxWidth())

    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Match Case")
        Switch(checked = caseSensitive, onCheckedChange = { caseSensitive = it; runReplace() })
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Whole Word Only")
        Switch(checked = wholeWord, onCheckedChange = { wholeWord = it; runReplace() })
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Regular Expression")
        Switch(checked = useRegex, onCheckedChange = { useRegex = it; runReplace() })
    }
}

@Composable
fun LoremIpsumUI(
    tool: LoremIpsumGeneratorTool,
    onResult: (String, String?, Long) -> Unit
) {
    var unit by remember { mutableStateOf(LoremUnit.PARAGRAPHS) }
    var count by remember { mutableIntStateOf(3) }
    val scope = rememberCoroutineScope()

    fun generate() {
        scope.launch {
            when (val res = tool.execute(LoremIpsumInput(unit, count))) {
                is ToolResult.Success -> onResult(res.data, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { generate() }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LoremUnit.values().forEach { u ->
            FilterChip(
                selected = unit == u,
                onClick = { unit = u; generate() },
                label = { Text(u.label) }
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text("Quantity: $count ${unit.label.lowercase()}")
    Slider(value = count.toFloat(), onValueChange = { count = it.toInt() }, onValueChangeFinished = { generate() }, valueRange = 1f..20f, steps = 19)
    Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) { Text("Generate Lorem Ipsum") }
}

@Composable
fun TextDiffUI(
    tool: TextDiffTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit
) {
    var original by remember { mutableStateOf(initialText) }
    var modified by remember { mutableStateOf(initialText) }
    val scope = rememberCoroutineScope()

    fun runDiff() {
        scope.launch {
            when (val res = tool.execute(TextDiffInput(original, modified))) {
                is ToolResult.Success -> onResult(res.data.formattedDiff, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    ToolInputField(value = original, onValueChange = { original = it; runDiff() }, label = "Original Text", placeholder = "Paste original version...", minLines = 3)
    Spacer(modifier = Modifier.height(8.dp))
    ToolInputField(value = modified, onValueChange = { modified = it; runDiff() }, label = "Modified Text", placeholder = "Paste modified version...", minLines = 3)
}

@Composable
fun JwtDecoderUI(
    tool: JwtDecoderTool,
    initialToken: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var token by remember { mutableStateOf(initialToken.ifEmpty { "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c" }) }
    val scope = rememberCoroutineScope()

    fun runJwt() {
        scope.launch {
            when (val res = tool.execute(token)) {
                is ToolResult.Success -> {
                    val out = buildString {
                        appendLine("STATUS: ${res.data.summary}")
                        appendLine()
                        appendLine("HEADER:")
                        appendLine(res.data.formattedHeaderJson)
                        appendLine()
                        appendLine("PAYLOAD:")
                        appendLine(res.data.formattedPayloadJson)
                        appendLine()
                        appendLine("SIGNATURE: ${res.data.signatureHex}")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runJwt() }
    ToolInputField(value = token, onValueChange = { token = it; runJwt() }, label = "JWT Token", placeholder = "Paste JWT here...", minLines = 3)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlEncoderUI(
    tool: UrlEncoderTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var op by remember { mutableStateOf(UrlOperation.ENCODE) }
    val scope = rememberCoroutineScope()

    fun runUrl(selectedOp: UrlOperation) {
        scope.launch {
            when (val res = tool.execute(UrlEncoderInput(text, selectedOp))) {
                is ToolResult.Success -> onResult(res.data, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, null)
            }
        }
    }

    ToolInputField(value = text, onValueChange = { text = it; runUrl(op) }, placeholder = "Enter URL or parameters to encode/decode...")
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UrlOperation.values().forEach { o ->
            FilterChip(selected = op == o, onClick = { op = o; runUrl(o) }, label = { Text(o.displayName) })
        }
    }
}

@Composable
fun HtmlEntityUI(
    tool: HtmlEntityTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var mode by remember { mutableStateOf(HtmlEntityMode.ENCODE) }
    val scope = rememberCoroutineScope()

    fun runHtml(m: HtmlEntityMode) {
        scope.launch {
            when (val res = tool.execute(HtmlEntityInput(text, m))) {
                is ToolResult.Success -> onResult(res.data, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    ToolInputField(value = text, onValueChange = { text = it; runHtml(mode) }, placeholder = "Enter text to encode/decode HTML entities...")
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HtmlEntityMode.values().forEach { m ->
            FilterChip(selected = mode == m, onClick = { mode = m; runHtml(m) }, label = { Text(m.label) })
        }
    }
}

@Composable
fun UnixTimestampUI(
    tool: UnixTimestampTool,
    initialTimestamp: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialTimestamp.ifEmpty { "now" }) }
    val scope = rememberCoroutineScope()

    fun runTs(str: String) {
        scope.launch {
            when (val res = tool.execute(str)) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Epoch (Seconds):       ${d.epochSeconds}")
                        appendLine("Epoch (Milliseconds):  ${d.epochMilliseconds}")
                        appendLine("UTC Date/Time:         ${d.utcDateTime}")
                        appendLine("Local Date/Time:       ${d.localDateTime}")
                        appendLine("Relative Time:         ${d.relativeTime}")
                        appendLine("Day of Week:           ${d.dayOfWeek} (Day ${d.dayOfYear} of year)")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runTs(text) }
    ToolInputField(value = text, onValueChange = { text = it; runTs(it) }, label = "Timestamp / ISO String / 'now'", placeholder = "e.g. 1725840000 or now", minLines = 1)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = { text = "now"; runTs("now") }, modifier = Modifier.fillMaxWidth()) { Text("Get Current Timestamp ('now')") }
}

@Composable
fun NumberBaseConverterUI(
    tool: NumberBaseConverterTool,
    initialValue: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var value by remember { mutableStateOf(initialValue.ifEmpty { "255" }) }
    var base by remember { mutableStateOf(NumberBase.DECIMAL) }
    val scope = rememberCoroutineScope()

    fun runBase(v: String, b: NumberBase) {
        scope.launch {
            when (val res = tool.execute(NumberBaseInput(v, b))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Binary (Base 2):       ${d.binaryFormatted}")
                        appendLine("Octal (Base 8):        ${d.octal}")
                        appendLine("Decimal (Base 10):     ${d.decimal}")
                        appendLine("Hexadecimal (Base 16): 0x${d.hexadecimal}")
                        if (d.asciiChar != null) {
                            appendLine("ASCII Character:       '${d.asciiChar}'")
                        }
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runBase(value, base) }
    ToolInputField(value = value, onValueChange = { value = it; runBase(it, base) }, label = "Number Value", placeholder = "Enter number...", minLines = 1)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberBase.values().forEach { b ->
            FilterChip(selected = base == b, onClick = { base = b; runBase(value, b) }, label = { Text(b.displayName.substringBefore(" ")) })
        }
    }
}

@Composable
fun RegexTesterUI(
    tool: RegexTesterTool,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var pattern by remember { mutableStateOf("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") }
    var testText by remember { mutableStateOf("Contact us at support@example.com or admin@offline.org.") }
    var ignoreCase by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun runRegex() {
        scope.launch {
            when (val res = tool.execute(RegexTesterInput(pattern, testText, ignoreCase))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Match Status: ${d.summary}")
                        if (d.matches.isNotEmpty()) {
                            appendLine()
                            appendLine("Matches Found:")
                            d.matches.forEach { m ->
                                appendLine("${m.index}. \"${m.value}\" at ${m.range}")
                            }
                        }
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runRegex() }
    OutlinedTextField(value = pattern, onValueChange = { pattern = it; runRegex() }, label = { Text("Regex Pattern") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    ToolInputField(value = testText, onValueChange = { testText = it; runRegex() }, label = "Test String", placeholder = "String to evaluate against regex...")
}

@Composable
fun LengthConverterUI(
    tool: LengthConverterTool,
    onResult: (String, String?, Long) -> Unit
) {
    var valueStr by remember { mutableStateOf("10") }
    var fromUnit by remember { mutableStateOf(LengthUnit.METER) }
    var toUnit by remember { mutableStateOf(LengthUnit.FOOT) }
    val scope = rememberCoroutineScope()

    fun runConvert() {
        val v = valueStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(LengthConverterInput(v, fromUnit, toUnit))) {
                is ToolResult.Success -> {
                    val out = buildString {
                        appendLine("Result: ${res.data.resultFormatted}")
                        appendLine("--------------------------------")
                        res.data.allConversions.forEach { (u, valConverted) ->
                            appendLine("${u.displayName} (${u.symbol}): $valConverted")
                        }
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runConvert() }
    OutlinedTextField(value = valueStr, onValueChange = { valueStr = it; runConvert() }, label = { Text("Value") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Text("From Unit:")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(LengthUnit.METER, LengthUnit.KILOMETER, LengthUnit.FOOT, LengthUnit.INCH, LengthUnit.MILE).forEach { u ->
            FilterChip(selected = fromUnit == u, onClick = { fromUnit = u; runConvert() }, label = { Text(u.symbol) })
        }
    }
    Text("To Unit:")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(LengthUnit.METER, LengthUnit.KILOMETER, LengthUnit.FOOT, LengthUnit.INCH, LengthUnit.MILE).forEach { u ->
            FilterChip(selected = toUnit == u, onClick = { toUnit = u; runConvert() }, label = { Text(u.symbol) })
        }
    }
}

@Composable
fun WeightConverterUI(
    tool: WeightConverterTool,
    onResult: (String, String?, Long) -> Unit
) {
    var valueStr by remember { mutableStateOf("5") }
    var fromUnit by remember { mutableStateOf(WeightUnit.KILOGRAM) }
    var toUnit by remember { mutableStateOf(WeightUnit.POUND) }
    val scope = rememberCoroutineScope()

    fun runConvert() {
        val v = valueStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(WeightConverterInput(v, fromUnit, toUnit))) {
                is ToolResult.Success -> {
                    val out = buildString {
                        appendLine("Result: ${res.data.resultFormatted}")
                        appendLine("--------------------------------")
                        res.data.allConversions.forEach { (u, valConverted) ->
                            appendLine("${u.displayName} (${u.symbol}): $valConverted")
                        }
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runConvert() }
    OutlinedTextField(value = valueStr, onValueChange = { valueStr = it; runConvert() }, label = { Text("Weight Value") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Text("From Unit:")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(WeightUnit.KILOGRAM, WeightUnit.POUND, WeightUnit.GRAM, WeightUnit.OUNCE).forEach { u ->
            FilterChip(selected = fromUnit == u, onClick = { fromUnit = u; runConvert() }, label = { Text(u.symbol) })
        }
    }
    Text("To Unit:")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(WeightUnit.KILOGRAM, WeightUnit.POUND, WeightUnit.GRAM, WeightUnit.OUNCE).forEach { u ->
            FilterChip(selected = toUnit == u, onClick = { toUnit = u; runConvert() }, label = { Text(u.symbol) })
        }
    }
}

@Composable
fun TemperatureConverterUI(
    tool: TemperatureConverterTool,
    onResult: (String, String?, Long) -> Unit
) {
    var valueStr by remember { mutableStateOf("25") }
    var fromUnit by remember { mutableStateOf(TemperatureUnit.CELSIUS) }
    var toUnit by remember { mutableStateOf(TemperatureUnit.FAHRENHEIT) }
    val scope = rememberCoroutineScope()

    fun runConvert() {
        val v = valueStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(TemperatureConverterInput(v, fromUnit, toUnit))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Converted:   ${d.resultFormatted}")
                        appendLine()
                        appendLine("Celsius:     ${d.celsius} °C")
                        appendLine("Fahrenheit:  ${d.fahrenheit} °F")
                        appendLine("Kelvin:      ${d.kelvin} K")
                        appendLine("Rankine:     ${d.rankine} °R")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runConvert() }
    OutlinedTextField(value = valueStr, onValueChange = { valueStr = it; runConvert() }, label = { Text("Temperature") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TemperatureUnit.values().forEach { u ->
            FilterChip(selected = fromUnit == u, onClick = { fromUnit = u; runConvert() }, label = { Text("From ${u.symbol}") })
        }
    }
}

@Composable
fun DataStorageConverterUI(
    tool: DataStorageConverterTool,
    onResult: (String, String?, Long) -> Unit
) {
    var valueStr by remember { mutableStateOf("1024") }
    var fromUnit by remember { mutableStateOf(StorageUnit.MEGABYTE) }
    var toUnit by remember { mutableStateOf(StorageUnit.GIGABYTE) }
    var useBinary by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun runConvert() {
        val v = valueStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(DataStorageInput(v, fromUnit, toUnit, useBinary))) {
                is ToolResult.Success -> {
                    val out = buildString {
                        appendLine("Result: ${res.data.resultFormatted}")
                        appendLine("Total Bits: ${res.data.totalBits.toLong()}")
                        appendLine("--------------------------------")
                        res.data.allConversions.forEach { (u, valConverted) ->
                            appendLine("${u.displayName} (${u.symbol}): $valConverted")
                        }
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runConvert() }
    OutlinedTextField(value = valueStr, onValueChange = { valueStr = it; runConvert() }, label = { Text("Storage Size") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Use Binary Base 1024 (KiB/MiB)")
        Switch(checked = useBinary, onCheckedChange = { useBinary = it; runConvert() })
    }
}

@Composable
fun SpeedConverterUI(
    tool: SpeedConverterTool,
    onResult: (String, String?, Long) -> Unit
) {
    var valueStr by remember { mutableStateOf("100") }
    var fromUnit by remember { mutableStateOf(SpeedUnit.KILOMETERS_PER_HOUR) }
    var toUnit by remember { mutableStateOf(SpeedUnit.MILES_PER_HOUR) }
    val scope = rememberCoroutineScope()

    fun runConvert() {
        val v = valueStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(SpeedConverterInput(v, fromUnit, toUnit))) {
                is ToolResult.Success -> {
                    val out = buildString {
                        appendLine("Result: ${res.data.resultFormatted}")
                        appendLine("--------------------------------")
                        res.data.allConversions.forEach { (u, valConverted) ->
                            appendLine("${u.displayName} (${u.symbol}): $valConverted")
                        }
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runConvert() }
    OutlinedTextField(value = valueStr, onValueChange = { valueStr = it; runConvert() }, label = { Text("Speed Value") }, modifier = Modifier.fillMaxWidth())
}

@Composable
fun RomanNumeralUI(
    tool: RomanNumeralConverterTool,
    initialValue: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialValue.ifEmpty { "2026" }) }
    val scope = rememberCoroutineScope()

    fun runRoman(inputStr: String) {
        scope.launch {
            when (val res = tool.execute(RomanNumeralInput(inputStr, RomanMode.AUTO_DETECT))) {
                is ToolResult.Success -> onResult(res.data, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runRoman(text) }
    ToolInputField(value = text, onValueChange = { text = it; runRoman(it) }, label = "Number (1-3999) or Roman (MMXXVI)", placeholder = "e.g. 2026 or MMXXVI", minLines = 1)
}

@Composable
fun TipCalculatorUI(
    tool: TipCalculatorTool,
    onResult: (String, String?, Long) -> Unit
) {
    var billStr by remember { mutableStateOf("75.00") }
    var tipPct by remember { mutableDoubleStateOf(18.0) }
    var splitPeople by remember { mutableIntStateOf(2) }
    var roundUp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runTip() {
        val b = billStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(TipInput(b, tipPct, splitPeople, roundUp))) {
                is ToolResult.Success -> onResult(res.data.breakdownText, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runTip() }
    OutlinedTextField(value = billStr, onValueChange = { billStr = it; runTip() }, label = { Text("Bill Amount ($)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Text("Tip Percentage: ${tipPct.toInt()}%")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(10.0, 15.0, 18.0, 20.0, 25.0).forEach { p ->
            FilterChip(selected = tipPct == p, onClick = { tipPct = p; runTip() }, label = { Text("${p.toInt()}%") })
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text("Split between: $splitPeople people")
    Slider(value = splitPeople.toFloat(), onValueChange = { splitPeople = it.toInt(); runTip() }, valueRange = 1f..10f, steps = 8)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Round Up Total")
        Switch(checked = roundUp, onCheckedChange = { roundUp = it; runTip() })
    }
}

@Composable
fun BmiCalculatorUI(
    tool: BmiCalculatorTool,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var weightStr by remember { mutableStateOf("70") }
    var heightStr by remember { mutableStateOf("175") }
    var system by remember { mutableStateOf(BmiUnitSystem.METRIC) }
    val scope = rememberCoroutineScope()

    fun runBmi() {
        val w = weightStr.toDoubleOrNull() ?: 0.0
        val h = heightStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(BmiInput(w, h, system))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("BMI Score:       ${d.bmiScore}")
                        appendLine("WHO Category:    ${d.category}")
                        appendLine("Healthy Range:   ${d.healthyWeightMin} - ${d.healthyWeightMax} ${d.weightUnit}")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runBmi() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = system == BmiUnitSystem.METRIC, onClick = { system = BmiUnitSystem.METRIC; runBmi() }, label = { Text("Metric (kg, cm)") })
        FilterChip(selected = system == BmiUnitSystem.IMPERIAL, onClick = { system = BmiUnitSystem.IMPERIAL; runBmi() }, label = { Text("Imperial (lbs, in)") })
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = weightStr, onValueChange = { weightStr = it; runBmi() }, label = { Text("Weight (${if (system == BmiUnitSystem.METRIC) "kg" else "lbs"})") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(value = heightStr, onValueChange = { heightStr = it; runBmi() }, label = { Text("Height (${if (system == BmiUnitSystem.METRIC) "cm" else "inches"})") }, modifier = Modifier.fillMaxWidth())
}

@Composable
fun AverageCalculatorUI(
    tool: AverageCalculatorTool,
    initialNumbers: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var text by remember { mutableStateOf(initialNumbers.ifEmpty { "12, 18, 25, 34, 18, 42, 50" }) }
    val scope = rememberCoroutineScope()

    fun runAvg(str: String) {
        scope.launch {
            when (val res = tool.execute(str)) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Count:               ${d.count}")
                        appendLine("Sum:                 ${d.sum}")
                        appendLine("Mean (Average):      ${d.mean}")
                        appendLine("Median:              ${d.median}")
                        if (d.mode.isNotEmpty()) {
                            appendLine("Mode:                ${d.mode.joinToString(", ")}")
                        }
                        appendLine("Min:                 ${d.min}")
                        appendLine("Max:                 ${d.max}")
                        appendLine("Range:               ${d.range}")
                        appendLine("Standard Deviation:  ${d.standardDeviation}")
                        appendLine("Variance:            ${d.variance}")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runAvg(text) }
    ToolInputField(value = text, onValueChange = { text = it; runAvg(it) }, label = "Numbers (separated by comma, space, or line)", placeholder = "e.g. 10, 20, 30...")
}

@Composable
fun FractionCalculatorUI(
    tool: FractionCalculatorTool,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var num1 by remember { mutableStateOf("1") }
    var den1 by remember { mutableStateOf("2") }
    var op by remember { mutableStateOf(FractionOperator.ADD) }
    var num2 by remember { mutableStateOf("3") }
    var den2 by remember { mutableStateOf("4") }
    val scope = rememberCoroutineScope()

    fun runFraction() {
        val n1 = num1.toLongOrNull() ?: 1L
        val d1 = den1.toLongOrNull() ?: 1L
        val n2 = num2.toLongOrNull() ?: 1L
        val d2 = den2.toLongOrNull() ?: 1L
        scope.launch {
            when (val res = tool.execute(FractionInput(n1, d1, op, n2, d2))) {
                is ToolResult.Success -> onResult(res.data.calculationSteps, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runFraction() }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = num1, onValueChange = { num1 = it; runFraction() }, label = { Text("Num 1") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = den1, onValueChange = { den1 = it; runFraction() }, label = { Text("Den 1") }, modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FractionOperator.values().forEach { o ->
            FilterChip(selected = op == o, onClick = { op = o; runFraction() }, label = { Text(o.symbol) })
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = num2, onValueChange = { num2 = it; runFraction() }, label = { Text("Num 2") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = den2, onValueChange = { den2 = it; runFraction() }, label = { Text("Den 2") }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun DateAddSubtractUI(
    tool: DateAddSubtractTool,
    onResult: (String, String?, Long) -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var op by remember { mutableStateOf(DateOperation.ADD) }
    var amountStr by remember { mutableStateOf("30") }
    var unit by remember { mutableStateOf(DateUnit.DAYS) }
    val scope = rememberCoroutineScope()

    fun runDate() {
        val amt = amountStr.toIntOrNull() ?: 0
        scope.launch {
            when (val res = tool.execute(DateAddSubtractInput(startDate, op, amt, unit))) {
                is ToolResult.Success -> onResult(res.data.formattedResult, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runDate() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = op == DateOperation.ADD, onClick = { op = DateOperation.ADD; runDate() }, label = { Text("Add (+)") })
        FilterChip(selected = op == DateOperation.SUBTRACT, onClick = { op = DateOperation.SUBTRACT; runDate() }, label = { Text("Subtract (-)") })
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it; runDate() }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(DateUnit.DAYS, DateUnit.WEEKS, DateUnit.MONTHS, DateUnit.YEARS, DateUnit.BUSINESS_DAYS).forEach { u ->
            FilterChip(selected = unit == u, onClick = { unit = u; runDate() }, label = { Text(u.label.substringBefore(" ")) })
        }
    }
}

@Composable
fun CountdownCalculatorUI(
    tool: CountdownCalculatorTool,
    onResult: (String, String?, Long) -> Unit
) {
    var targetDate by remember { mutableStateOf(LocalDateTime.now().plusDays(100)) }
    val scope = rememberCoroutineScope()

    fun runCountdown() {
        scope.launch {
            when (val res = tool.execute(targetDate)) {
                is ToolResult.Success -> {
                    val out = buildString {
                        appendLine("Target:       $targetDate")
                        appendLine("Status:       ${res.data.summary}")
                        appendLine("Total Days:   ${res.data.totalDays}")
                        appendLine("Total Hours:  ${res.data.totalHours}")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runCountdown() }
    Text("Target Date: $targetDate")
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { targetDate = targetDate.plusDays(30); runCountdown() }) { Text("+30 Days") }
        OutlinedButton(onClick = { targetDate = targetDate.plusDays(365); runCountdown() }) { Text("+1 Year (New Year)") }
    }
}

@Composable
fun HashCheckerUI(
    tool: HashCheckerTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var content by remember { mutableStateOf(initialText) }
    var expectedHash by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun runCheck() {
        scope.launch {
            when (val res = tool.execute(HashCheckerInput(content, expectedHash))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("STATUS: ${res.data.summary}")
                        appendLine("Detected Algorithm: ${d.algorithmUsed}")
                        appendLine("Calculated Hash:    ${d.calculatedHash}")
                        appendLine("Expected Hash:      ${d.expectedHashCleaned}")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    ToolInputField(value = content, onValueChange = { content = it; runCheck() }, label = "Content to verify", placeholder = "Enter content...")
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = expectedHash, onValueChange = { expectedHash = it; runCheck() }, label = { Text("Expected Checksum (MD5, SHA-256, etc.)") }, modifier = Modifier.fillMaxWidth())
}

@Composable
fun PasswordStrengthUI(
    tool: PasswordStrengthTool,
    initialPassword: String,
    onResult: (String, String?, Long) -> Unit
) {
    var password by remember { mutableStateOf(initialPassword) }
    val scope = rememberCoroutineScope()

    fun runAudit(p: String) {
        scope.launch {
            when (val res = tool.execute(p)) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Strength:           ${d.strengthLevel} (Score: ${d.score}/4)")
                        appendLine("Entropy:            ${d.entropyBits} bits")
                        appendLine("Estimated Crack:    ${d.estimatedCrackTime}")
                        appendLine("Length:             ${d.length} characters")
                        appendLine("Uppercase (A-Z):    ${if (d.hasUpper) "Yes" else "No"}")
                        appendLine("Lowercase (a-z):    ${if (d.hasLower) "Yes" else "No"}")
                        appendLine("Numbers (0-9):      ${if (d.hasNumber) "Yes" else "No"}")
                        appendLine("Symbols (!@#$):     ${if (d.hasSymbol) "Yes" else "No"}")
                        if (d.suggestions.isNotEmpty()) {
                            appendLine()
                            appendLine("Recommendations:")
                            d.suggestions.forEach { appendLine("• $it") }
                        }
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    ToolInputField(value = password, onValueChange = { password = it; runAudit(it) }, label = "Password to Audit", placeholder = "Enter password to inspect strength...")
}

@Composable
fun UuidGeneratorUI(
    tool: UuidGeneratorTool,
    onResult: (String, String?, Long) -> Unit
) {
    var count by remember { mutableIntStateOf(5) }
    var uppercase by remember { mutableStateOf(false) }
    var hyphens by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun generate() {
        scope.launch {
            when (val res = tool.execute(UuidConfig(count, uppercase, hyphens))) {
                is ToolResult.Success -> onResult(res.data, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { generate() }
    Text("Quantity: $count UUID(s)")
    Slider(value = count.toFloat(), onValueChange = { count = it.toInt() }, onValueChangeFinished = { generate() }, valueRange = 1f..20f, steps = 19)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Uppercase Hex")
        Switch(checked = uppercase, onCheckedChange = { uppercase = it; generate() })
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Include Hyphens")
        Switch(checked = hyphens, onCheckedChange = { hyphens = it; generate() })
    }
    Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) { Text("Generate New UUIDs") }
}

@Composable
fun CsvToJsonUI(
    tool: CsvToJsonTool,
    initialCsv: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var csvText by remember { mutableStateOf(initialCsv.ifEmpty { "id,name,role,active\n1,Alice,Engineer,true\n2,Bob,Designer,false\n3,Charlie,Product,true" }) }
    val scope = rememberCoroutineScope()

    fun runCsv() {
        scope.launch {
            when (val res = tool.execute(CsvToJsonInput(csvText))) {
                is ToolResult.Success -> onResult(res.data.jsonString, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runCsv() }
    ToolInputField(value = csvText, onValueChange = { csvText = it; runCsv() }, label = "CSV / TSV Text", placeholder = "Paste CSV rows here...", minLines = 4)
}

@Composable
fun JsonToCsvUI(
    tool: JsonToCsvTool,
    initialJson: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var jsonText by remember { mutableStateOf(initialJson.ifEmpty { "[\n  {\"id\": 1, \"name\": \"Alice\", \"role\": \"Engineer\"},\n  {\"id\": 2, \"name\": \"Bob\", \"role\": \"Designer\"}\n]" }) }
    val scope = rememberCoroutineScope()

    fun runJson() {
        scope.launch {
            when (val res = tool.execute(jsonText)) {
                is ToolResult.Success -> onResult(res.data, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runJson() }
    ToolInputField(value = jsonText, onValueChange = { jsonText = it; runJson() }, label = "JSON Array of Objects", placeholder = "Paste JSON array...", minLines = 4)
}

@Composable
fun RandomNumberUI(
    tool: RandomNumberTool,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var minStr by remember { mutableStateOf("1") }
    var maxStr by remember { mutableStateOf("100") }
    var count by remember { mutableIntStateOf(5) }
    var uniqueOnly by remember { mutableStateOf(false) }
    var sortResults by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun runRand() {
        val min = minStr.toLongOrNull() ?: 1L
        val max = maxStr.toLongOrNull() ?: 100L
        scope.launch {
            when (val res = tool.execute(RandomNumberConfig(min, max, count, uniqueOnly, sortResults))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Numbers:  ${d.numbersFormatted}")
                        appendLine("Count:    ${d.count}")
                        appendLine("Sum:      ${d.sum}")
                        appendLine("Average:  ${d.average}")
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runRand() }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = minStr, onValueChange = { minStr = it; runRand() }, label = { Text("Min") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = maxStr, onValueChange = { maxStr = it; runRand() }, label = { Text("Max") }, modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text("Quantity: $count numbers")
    Slider(value = count.toFloat(), onValueChange = { count = it.toInt(); runRand() }, valueRange = 1f..50f, steps = 49)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Unique Numbers Only")
        Switch(checked = uniqueOnly, onCheckedChange = { uniqueOnly = it; runRand() })
    }
    Button(onClick = { runRand() }, modifier = Modifier.fillMaxWidth()) { Text("Generate New Numbers") }
}

@Composable
fun RandomChoiceUI(
    tool: RandomChoiceTool,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var mode by remember { mutableStateOf(ChoiceMode.CUSTOM_LIST) }
    var items by remember { mutableStateOf("Pizza, Sushi, Burgers, Tacos, Salad") }
    var diceSides by remember { mutableIntStateOf(6) }
    var diceCount by remember { mutableIntStateOf(2) }
    val scope = rememberCoroutineScope()

    fun runChoice() {
        scope.launch {
            when (val res = tool.execute(RandomChoiceInput(mode, items, diceSides, diceCount))) {
                is ToolResult.Success -> {
                    val out = "${res.data.chosenResult}\n\n${res.data.details}"
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runChoice() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceMode.values().forEach { m ->
            FilterChip(selected = mode == m, onClick = { mode = m; runChoice() }, label = { Text(m.label) })
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    when (mode) {
        ChoiceMode.CUSTOM_LIST -> {
            ToolInputField(value = items, onValueChange = { items = it; runChoice() }, label = "Options List (Comma or Line Separated)", placeholder = "e.g. Option 1, Option 2...")
            Button(onClick = { runChoice() }, modifier = Modifier.fillMaxWidth()) { Text("Pick Random Option") }
        }
        ChoiceMode.COIN_FLIP -> {
            Button(onClick = { runChoice() }, modifier = Modifier.fillMaxWidth()) { Text("Flip Coin (Heads or Tails)") }
        }
        ChoiceMode.DICE_ROLL -> {
            Text("Dice Sides (d$diceSides)")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(4, 6, 8, 10, 12, 20, 100).forEach { s ->
                    FilterChip(selected = diceSides == s, onClick = { diceSides = s; runChoice() }, label = { Text("d$s") })
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Dice Count: $diceCount")
            Slider(value = diceCount.toFloat(), onValueChange = { diceCount = it.toInt(); runChoice() }, valueRange = 1f..6f, steps = 5)
            Button(onClick = { runChoice() }, modifier = Modifier.fillMaxWidth()) { Text("Roll Dice") }
        }
    }
}
