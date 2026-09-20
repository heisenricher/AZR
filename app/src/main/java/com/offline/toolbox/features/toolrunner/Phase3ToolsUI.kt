package com.offline.toolbox.features.toolrunner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import com.offline.toolbox.tools.converter.AreaConverterInput
import com.offline.toolbox.tools.converter.AreaConverterTool
import com.offline.toolbox.tools.converter.AreaUnit
import com.offline.toolbox.tools.converter.VolumeConverterInput
import com.offline.toolbox.tools.converter.VolumeConverterTool
import com.offline.toolbox.tools.converter.VolumeUnit
import com.offline.toolbox.tools.developer.MarkdownPreviewTool
import com.offline.toolbox.tools.developer.SqlFormatterInput
import com.offline.toolbox.tools.developer.SqlFormatterTool
import com.offline.toolbox.tools.developer.XmlFormatterInput
import com.offline.toolbox.tools.developer.XmlFormatterTool
import com.offline.toolbox.tools.generator.QrPayloadBuilderTool
import com.offline.toolbox.tools.generator.QrPayloadInput
import com.offline.toolbox.tools.generator.QrPayloadType
import com.offline.toolbox.tools.math.CompoundFrequency
import com.offline.toolbox.tools.math.CompoundInterestInput
import com.offline.toolbox.tools.math.CompoundInterestTool
import com.offline.toolbox.tools.math.DiscountCalculatorTool
import com.offline.toolbox.tools.math.DiscountInput
import com.offline.toolbox.tools.text.Rot13CipherTool
import com.offline.toolbox.tools.text.Rot13Input
import com.offline.toolbox.tools.text.SlugGeneratorTool
import com.offline.toolbox.tools.text.SlugInput
import com.offline.toolbox.tools.text.StringInspectorTool
import kotlinx.coroutines.launch

@Composable
fun SlugGeneratorUI(
    tool: SlugGeneratorTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit
) {
    var text by remember { mutableStateOf(initialText.ifEmpty { "Hello World! Welcome to Offline Toolbox 2026." }) }
    var separator by remember { mutableStateOf("-") }
    var lowercase by remember { mutableStateOf(true) }
    var removeAccents by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun runSlug() {
        scope.launch {
            when (val res = tool.execute(SlugInput(text, separator, lowercase, removeAccents))) {
                is ToolResult.Success -> onResult(res.data.slug, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runSlug() }
    ToolInputField(value = text, onValueChange = { text = it; runSlug() }, label = "Source Text", placeholder = "Enter text to convert to URL slug...")
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("-", "_", ".").forEach { s ->
            FilterChip(selected = separator == s, onClick = { separator = s; runSlug() }, label = { Text("Separator: '$s'") })
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Lowercase")
        Switch(checked = lowercase, onCheckedChange = { lowercase = it; runSlug() })
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Remove Accents / Diacritics")
        Switch(checked = removeAccents, onCheckedChange = { removeAccents = it; runSlug() })
    }
}

@Composable
fun StringInspectorUI(
    tool: StringInspectorTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit
) {
    var text by remember { mutableStateOf(initialText.ifEmpty { "Offline Toolbox 🛠️\nFast, secure & private!" }) }
    val scope = rememberCoroutineScope()

    fun runInspect(t: String) {
        scope.launch {
            when (val res = tool.execute(t)) {
                is ToolResult.Success -> onResult(res.data.detailsFormatted, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runInspect(text) }
    ToolInputField(value = text, onValueChange = { text = it; runInspect(it) }, label = "String to Inspect", placeholder = "Type or paste text to analyze characters...", minLines = 3)
}

@Composable
fun Rot13CipherUI(
    tool: Rot13CipherTool,
    initialText: String,
    onResult: (String, String?, Long) -> Unit
) {
    var text by remember { mutableStateOf(initialText.ifEmpty { "Hello World! Uryyb Jbeyq!" }) }
    var shift by remember { mutableIntStateOf(13) }
    var rotateDigits by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runRot() {
        scope.launch {
            when (val res = tool.execute(Rot13Input(text, shift, rotateDigits))) {
                is ToolResult.Success -> onResult(res.data.transformedText, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runRot() }
    ToolInputField(value = text, onValueChange = { text = it; runRot() }, label = "Text to Encrypt / Decrypt", placeholder = "Enter text...")
    Spacer(modifier = Modifier.height(8.dp))
    Text("Rotation Shift: $shift ${if (shift == 13) "(Standard ROT-13)" else ""}")
    Slider(value = shift.toFloat(), onValueChange = { shift = it.toInt(); runRot() }, valueRange = 1f..25f, steps = 23)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Rotate Numeric Digits (0-9)")
        Switch(checked = rotateDigits, onCheckedChange = { rotateDigits = it; runRot() })
    }
}

@Composable
fun SqlFormatterUI(
    tool: SqlFormatterTool,
    initialSql: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var sql by remember { mutableStateOf(initialSql.ifEmpty { "select u.id, u.name, count(o.id) as orders_count from users u left join orders o on u.id = o.user_id where u.active = 1 and u.country in ('US', 'CA') group by u.id, u.name order by orders_count desc limit 50" }) }
    var uppercase by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun runFormat() {
        scope.launch {
            when (val res = tool.execute(SqlFormatterInput(sql, uppercaseKeywords = uppercase))) {
                is ToolResult.Success -> onResult(res.data.formattedSql, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runFormat() }
    ToolInputField(value = sql, onValueChange = { sql = it; runFormat() }, label = "SQL Query", placeholder = "Paste SQL query...", minLines = 4)
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Uppercase SQL Keywords")
        Switch(checked = uppercase, onCheckedChange = { uppercase = it; runFormat() })
    }
}

@Composable
fun XmlFormatterUI(
    tool: XmlFormatterTool,
    initialXml: String,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var xml by remember { mutableStateOf(initialXml.ifEmpty { "<catalog><book id=\"1\"><title>Offline Android</title><author>Senior Architect</author><price>29.99</price></book><book id=\"2\"><title>Kotlin Coroutines</title><price>39.99</price></book></catalog>" }) }
    var minify by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runXml() {
        scope.launch {
            when (val res = tool.execute(XmlFormatterInput(xml, indentSpaces = 2, minify = minify))) {
                is ToolResult.Success -> onResult(res.data.formattedXml, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runXml() }
    ToolInputField(value = xml, onValueChange = { xml = it; runXml() }, label = "XML Document", placeholder = "Paste XML...", minLines = 4)
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Minify XML")
        Switch(checked = minify, onCheckedChange = { minify = it; runXml() })
    }
}

@Composable
fun MarkdownPreviewUI(
    tool: MarkdownPreviewTool,
    initialMd: String,
    onResult: (String, String?, Long) -> Unit
) {
    var md by remember { mutableStateOf(initialMd.ifEmpty { "# Offline Toolbox\n\nA 100% offline, privacy-first utility application.\n\n## Features\n- **Zero** Internet permissions\n- 50+ local tools\n- Downstream chaining\n\n```kotlin\nval offline = true\n```\n\nLearn more at [GitHub](https://github.com)" }) }
    val scope = rememberCoroutineScope()

    fun runMd() {
        scope.launch {
            when (val res = tool.execute(md)) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        if (d.headingsOutline.isNotEmpty()) {
                            appendLine("TABLE OF CONTENTS / OUTLINE:")
                            d.headingsOutline.forEach { appendLine(it) }
                            appendLine("--------------------------------")
                        }
                        appendLine("DOCUMENT METRICS:")
                        appendLine("Headings:     ${d.stats.headingCount}")
                        appendLine("Words:        ${d.stats.wordCount}")
                        appendLine("Links:        ${d.stats.linkCount}")
                        appendLine("Code Blocks:  ${d.stats.codeBlockCount}")
                        appendLine("Images:       ${d.stats.imageCount}")
                        appendLine("Est. Read:    ~${"%.1f".format(d.stats.estimatedReadTimeMinutes)} min")
                        appendLine("--------------------------------")
                        appendLine("PLAIN TEXT PREVIEW:")
                        appendLine(d.plainText)
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runMd() }
    ToolInputField(value = md, onValueChange = { md = it; runMd() }, label = "Markdown Content", placeholder = "Paste markdown...", minLines = 5)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompoundInterestUI(
    tool: CompoundInterestTool,
    onResult: (String, String?, Long) -> Unit,
    onError: (String, String?) -> Unit
) {
    var principalStr by remember { mutableStateOf("10000") }
    var rateStr by remember { mutableStateOf("7.5") }
    var yearsStr by remember { mutableStateOf("10") }
    var pmtStr by remember { mutableStateOf("200") }
    var freq by remember { mutableStateOf(CompoundFrequency.MONTHLY) }
    var freqExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runCalc() {
        val p = principalStr.toDoubleOrNull() ?: 0.0
        val r = rateStr.toDoubleOrNull() ?: 0.0
        val y = yearsStr.toDoubleOrNull() ?: 1.0
        val pmt = pmtStr.toDoubleOrNull() ?: 0.0

        scope.launch {
            when (val res = tool.execute(CompoundInterestInput(p, r, y, freq, pmt))) {
                is ToolResult.Success -> onResult(res.data.formattedSummary, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> onError(res.message, res.userGuidance)
            }
        }
    }

    LaunchedEffect(Unit) { runCalc() }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = principalStr, onValueChange = { principalStr = it; runCalc() }, label = { Text("Principal ($)") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = rateStr, onValueChange = { rateStr = it; runCalc() }, label = { Text("Annual Rate (%)") }, modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = yearsStr, onValueChange = { yearsStr = it; runCalc() }, label = { Text("Years") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = pmtStr, onValueChange = { pmtStr = it; runCalc() }, label = { Text("Monthly Add ($)") }, modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(6.dp))

    ExposedDropdownMenuBox(expanded = freqExpanded, onExpandedChange = { freqExpanded = it }) {
        OutlinedTextField(
            value = freq.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Compounding Frequency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
            CompoundFrequency.values().forEach { f ->
                DropdownMenuItem(text = { Text(f.label) }, onClick = { freq = f; freqExpanded = false; runCalc() })
            }
        }
    }
}

@Composable
fun DiscountCalculatorUI(
    tool: DiscountCalculatorTool,
    onResult: (String, String?, Long) -> Unit
) {
    var priceStr by remember { mutableStateOf("120.00") }
    var discountStr by remember { mutableStateOf("25") }
    var extraDiscountStr by remember { mutableStateOf("10") }
    var taxStr by remember { mutableStateOf("8.5") }
    val scope = rememberCoroutineScope()

    fun runDiscount() {
        val p = priceStr.toDoubleOrNull() ?: 0.0
        val d = discountStr.toDoubleOrNull() ?: 0.0
        val extra = extraDiscountStr.toDoubleOrNull() ?: 0.0
        val tax = taxStr.toDoubleOrNull() ?: 0.0

        scope.launch {
            when (val res = tool.execute(DiscountInput(p, d, extra, tax))) {
                is ToolResult.Success -> onResult(res.data.breakdownText, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runDiscount() }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = priceStr, onValueChange = { priceStr = it; runDiscount() }, label = { Text("Original Price ($)") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = discountStr, onValueChange = { discountStr = it; runDiscount() }, label = { Text("Discount (%)") }, modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = extraDiscountStr, onValueChange = { extraDiscountStr = it; runDiscount() }, label = { Text("Extra Coupon (%)") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = taxStr, onValueChange = { taxStr = it; runDiscount() }, label = { Text("Sales Tax (%)") }, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaConverterUI(
    tool: AreaConverterTool,
    onResult: (String, String?, Long) -> Unit
) {
    var valueStr by remember { mutableStateOf("100") }
    var fromUnit by remember { mutableStateOf(AreaUnit.SQUARE_METER) }
    var toUnit by remember { mutableStateOf(AreaUnit.SQUARE_FOOT) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runConvert() {
        val v = valueStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(AreaConverterInput(v, fromUnit, toUnit))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Target Result: ${d.resultFormatted}")
                        appendLine("--------------------------------")
                        appendLine("All Area Equivalents:")
                        d.allConversions.forEach { (u, converted) ->
                            appendLine("• %-18s: %.4f %s".format(u.displayName, converted, u.symbol))
                        }
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runConvert() }

    OutlinedTextField(value = valueStr, onValueChange = { valueStr = it; runConvert() }, label = { Text("Area Value") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(expanded = fromExpanded, onExpandedChange = { fromExpanded = it }, modifier = Modifier.weight(1f)) {
            OutlinedTextField(value = fromUnit.displayName, onValueChange = {}, readOnly = true, label = { Text("From") }, modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable))
            ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                AreaUnit.values().forEach { u ->
                    DropdownMenuItem(text = { Text(u.displayName) }, onClick = { fromUnit = u; fromExpanded = false; runConvert() })
                }
            }
        }
        ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = it }, modifier = Modifier.weight(1f)) {
            OutlinedTextField(value = toUnit.displayName, onValueChange = {}, readOnly = true, label = { Text("To") }, modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable))
            ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                AreaUnit.values().forEach { u ->
                    DropdownMenuItem(text = { Text(u.displayName) }, onClick = { toUnit = u; toExpanded = false; runConvert() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeConverterUI(
    tool: VolumeConverterTool,
    onResult: (String, String?, Long) -> Unit
) {
    var valueStr by remember { mutableStateOf("1") }
    var fromUnit by remember { mutableStateOf(VolumeUnit.GALLON_US) }
    var toUnit by remember { mutableStateOf(VolumeUnit.LITER) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runConvert() {
        val v = valueStr.toDoubleOrNull() ?: 0.0
        scope.launch {
            when (val res = tool.execute(VolumeConverterInput(v, fromUnit, toUnit))) {
                is ToolResult.Success -> {
                    val d = res.data
                    val out = buildString {
                        appendLine("Target Result: ${d.resultFormatted}")
                        appendLine("--------------------------------")
                        appendLine("All Volume Equivalents:")
                        d.allConversions.forEach { (u, converted) ->
                            appendLine("• %-20s: %.4f %s".format(u.displayName, converted, u.symbol))
                        }
                    }
                    onResult(out, res.summary, res.executionTimeMs)
                }
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runConvert() }

    OutlinedTextField(value = valueStr, onValueChange = { valueStr = it; runConvert() }, label = { Text("Volume Value") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(expanded = fromExpanded, onExpandedChange = { fromExpanded = it }, modifier = Modifier.weight(1f)) {
            OutlinedTextField(value = fromUnit.displayName, onValueChange = {}, readOnly = true, label = { Text("From") }, modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable))
            ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                VolumeUnit.values().forEach { u ->
                    DropdownMenuItem(text = { Text(u.displayName) }, onClick = { fromUnit = u; fromExpanded = false; runConvert() })
                }
            }
        }
        ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = it }, modifier = Modifier.weight(1f)) {
            OutlinedTextField(value = toUnit.displayName, onValueChange = {}, readOnly = true, label = { Text("To") }, modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable))
            ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                VolumeUnit.values().forEach { u ->
                    DropdownMenuItem(text = { Text(u.displayName) }, onClick = { toUnit = u; toExpanded = false; runConvert() })
                }
            }
        }
    }
}

@Composable
fun QrPayloadBuilderUI(
    tool: QrPayloadBuilderTool,
    onResult: (String, String?, Long) -> Unit
) {
    var type by remember { mutableStateOf(QrPayloadType.WIFI) }
    var wifiSsid by remember { mutableStateOf("MyCoffeeShop_5G") }
    var wifiPassword by remember { mutableStateOf("Espresso2026") }
    var contactName by remember { mutableStateOf("Jane Doe") }
    var contactPhone by remember { mutableStateOf("+1-555-0199") }
    var contactEmail by remember { mutableStateOf("jane.doe@example.com") }
    var emailTo by remember { mutableStateOf("contact@domain.com") }
    var emailSub by remember { mutableStateOf("Product Feedback") }
    var smsPhone by remember { mutableStateOf("+1-555-0199") }
    var smsMsg by remember { mutableStateOf("Arrived safely!") }
    val scope = rememberCoroutineScope()

    fun runBuild() {
        val input = QrPayloadInput(
            type = type,
            wifiSsid = wifiSsid,
            wifiPassword = wifiPassword,
            contactName = contactName,
            contactPhone = contactPhone,
            contactEmail = contactEmail,
            emailTo = emailTo,
            emailSubject = emailSub,
            smsPhone = smsPhone,
            smsMessage = smsMsg
        )
        scope.launch {
            when (val res = tool.execute(input)) {
                is ToolResult.Success -> onResult(res.data.payloadString, res.summary, res.executionTimeMs)
                is ToolResult.Failure -> {}
            }
        }
    }

    LaunchedEffect(Unit) { runBuild() }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        QrPayloadType.values().forEach { t ->
            FilterChip(selected = type == t, onClick = { type = t; runBuild() }, label = { Text(t.label.substringBefore(" ")) })
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    when (type) {
        QrPayloadType.WIFI -> {
            OutlinedTextField(value = wifiSsid, onValueChange = { wifiSsid = it; runBuild() }, label = { Text("Wi-Fi Network Name (SSID)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(value = wifiPassword, onValueChange = { wifiPassword = it; runBuild() }, label = { Text("Wi-Fi Password") }, modifier = Modifier.fillMaxWidth())
        }
        QrPayloadType.VCARD -> {
            OutlinedTextField(value = contactName, onValueChange = { contactName = it; runBuild() }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it; runBuild() }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it; runBuild() }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
        }
        QrPayloadType.EMAIL -> {
            OutlinedTextField(value = emailTo, onValueChange = { emailTo = it; runBuild() }, label = { Text("Recipient Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(value = emailSub, onValueChange = { emailSub = it; runBuild() }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
        }
        QrPayloadType.SMS -> {
            OutlinedTextField(value = smsPhone, onValueChange = { smsPhone = it; runBuild() }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(value = smsMsg, onValueChange = { smsMsg = it; runBuild() }, label = { Text("Pre-filled Message") }, modifier = Modifier.fillMaxWidth())
        }
        QrPayloadType.GEO -> {
            Text("Default Geo Coordinates: 37.7749, -122.4194 (San Francisco)")
        }
    }
}
