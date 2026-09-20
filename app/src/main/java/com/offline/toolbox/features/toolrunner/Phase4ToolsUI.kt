package com.offline.toolbox.features.toolrunner

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.offline.toolbox.core.designsystem.components.ToolInputField
import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.data.CsvFilterOperator
import com.offline.toolbox.tools.data.CsvFilterSortInput
import com.offline.toolbox.tools.data.CsvFilterSortTool
import com.offline.toolbox.tools.data.CsvSortOrder
import com.offline.toolbox.tools.developer.Base32Alphabet
import com.offline.toolbox.tools.developer.Base32Input
import com.offline.toolbox.tools.developer.Base32Mode
import com.offline.toolbox.tools.developer.Base32Tool
import com.offline.toolbox.tools.developer.HtmlToMarkdownInput
import com.offline.toolbox.tools.developer.HtmlToMarkdownTool
import com.offline.toolbox.tools.documents.PdfFontType
import com.offline.toolbox.tools.documents.PdfGeneratorInput
import com.offline.toolbox.tools.documents.PdfGeneratorTool
import com.offline.toolbox.tools.documents.PdfPageFormat
import com.offline.toolbox.tools.file.ChecksumAlgorithm
import com.offline.toolbox.tools.file.FileChecksumInput
import com.offline.toolbox.tools.file.FileChecksumTool
import com.offline.toolbox.tools.file.ZipArchiveInput
import com.offline.toolbox.tools.file.ZipArchiveTool
import com.offline.toolbox.tools.file.ZipOperation
import com.offline.toolbox.tools.math.LoanEmiCalculatorTool
import com.offline.toolbox.tools.math.LoanEmiInput
import com.offline.toolbox.tools.math.TenureUnit
import com.offline.toolbox.tools.media.ExifInspectorInput
import com.offline.toolbox.tools.media.ExifInspectorTool
import com.offline.toolbox.tools.media.QrErrorCorrection
import com.offline.toolbox.tools.media.QrMatrixGeneratorTool
import com.offline.toolbox.tools.media.QrMatrixInput
import com.offline.toolbox.tools.text.AsciiArtBannerInput
import com.offline.toolbox.tools.text.AsciiArtBannerTool
import com.offline.toolbox.tools.text.AsciiBannerStyle
import com.offline.toolbox.tools.text.MorseCodeInput
import com.offline.toolbox.tools.text.MorseCodeTool
import com.offline.toolbox.tools.text.MorseDirection
import kotlinx.coroutines.launch

// ----------------------------------------------------
// 1. ZIP ARCHIVE & SECURITY INSPECTOR UI
// ----------------------------------------------------
@Composable
fun ZipArchiveToolUI(
    tool: ZipArchiveTool,
    onOutputChange: (String) -> Unit
) {
    var operation by remember { mutableStateOf(ZipOperation.CREATE_ARCHIVE) }
    var fileName by remember { mutableStateOf("notes.txt") }
    var fileContent by remember { mutableStateOf("Confidential Offline Notes\nStored locally without network sync.") }
    var zipBase64Input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun runZip() {
        scope.launch {
            val input = if (operation == ZipOperation.CREATE_ARCHIVE) {
                ZipArchiveInput(
                    operation = ZipOperation.CREATE_ARCHIVE,
                    filesToArchive = mapOf(fileName to fileContent)
                )
            } else {
                ZipArchiveInput(
                    operation = operation,
                    zipBase64 = zipBase64Input
                )
            }

            when (val res = tool.execute(input)) {
                is ToolResult.Success -> {
                    val out = res.data
                    val fullOutput = buildString {
                        appendLine(out.formattedReport)
                        if (out.generatedZipBase64 != null) {
                            appendLine()
                            appendLine("GENERATED ZIP (Base64 Encoded):")
                            appendLine(out.generatedZipBase64)
                        }
                    }
                    onOutputChange(fullOutput.trim())
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}\n${res.userGuidance ?: ""}")
            }
        }
    }

    LaunchedEffect(Unit) { runZip() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = operation == ZipOperation.CREATE_ARCHIVE,
                onClick = { operation = ZipOperation.CREATE_ARCHIVE },
                label = { Text("Create ZIP") }
            )
            FilterChip(
                selected = operation == ZipOperation.SAFE_EXTRACT_CHECK,
                onClick = { operation = ZipOperation.SAFE_EXTRACT_CHECK },
                label = { Text("Zip Slip Security Check") }
            )
        }

        if (operation == ZipOperation.CREATE_ARCHIVE) {
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("File Name in Archive") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            ToolInputField(
                value = fileContent,
                onValueChange = { fileContent = it },
                label = "File Content Text",
                minLines = 4
            )
        } else {
            ToolInputField(
                value = zipBase64Input,
                onValueChange = { zipBase64Input = it },
                label = "Paste Base64 ZIP Archive",
                placeholder = "Base64 encoded ZIP byte stream...",
                minLines = 4
            )
        }

        Button(onClick = { runZip() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (operation == ZipOperation.CREATE_ARCHIVE) "Generate Secure ZIP" else "Inspect Archive & Validate Paths")
        }
    }
}

// ----------------------------------------------------
// 2. FILE CHECKSUM & INTEGRITY UI
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileChecksumToolUI(
    tool: FileChecksumTool,
    onOutputChange: (String) -> Unit
) {
    var content by remember { mutableStateOf("Offline Toolbox Data Integrity Verification String 2026") }
    var isBase64 by remember { mutableStateOf(false) }
    var algorithm by remember { mutableStateOf(ChecksumAlgorithm.ALL) }
    var expectedChecksum by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runChecksum() {
        scope.launch {
            val res = tool.execute(
                FileChecksumInput(
                    content = content,
                    isBase64 = isBase64,
                    algorithm = algorithm,
                    expectedChecksum = expectedChecksum.ifBlank { null }
                )
            )
            when (res) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}\n${res.userGuidance ?: ""}")
            }
        }
    }

    LaunchedEffect(Unit) { runChecksum() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = content,
            onValueChange = { content = it; runChecksum() },
            label = "Input Text or Base64 Payload",
            minLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Treat as Base64 Binary File Payload", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = isBase64, onCheckedChange = { isBase64 = it; runChecksum() })
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = algorithm.algorithmName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Algorithm Scope") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ChecksumAlgorithm.entries.forEach { algo ->
                    DropdownMenuItem(
                        text = { Text(algo.algorithmName) },
                        onClick = {
                            algorithm = algo
                            expanded = false
                            runChecksum()
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = expectedChecksum,
            onValueChange = { expectedChecksum = it; runChecksum() },
            label = { Text("Expected Checksum to Verify (Optional)") },
            placeholder = { Text("Paste hash to check match...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

// ----------------------------------------------------
// 3. EXIF METADATA & PRIVACY INSPECTOR UI
// ----------------------------------------------------
@Composable
fun ExifInspectorToolUI(
    tool: ExifInspectorTool,
    onOutputChange: (String) -> Unit
) {
    var imageBase64 by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun runExif(data: String) {
        scope.launch {
            if (data.isBlank()) {
                onOutputChange("Paste a Base64-encoded image string to inspect EXIF metadata.")
                return@launch
            }
            when (val res = tool.execute(ExifInspectorInput(data))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Audit Failed: ${res.message}")
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = imageBase64,
            onValueChange = { imageBase64 = it; runExif(it) },
            label = "Image Base64 Payload",
            placeholder = "data:image/jpeg;base64,... or raw base64",
            minLines = 4
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { runExif(imageBase64) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Inspect EXIF & GPS")
            }
            OutlinedButton(
                onClick = {
                    // Sample minimal JPEG with dummy EXIF header
                    imageBase64 = "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
                    runExif(imageBase64)
                }
            ) {
                Text("Sample JPEG")
            }
        }
    }
}

// ----------------------------------------------------
// 4. OFFLINE PDF DOCUMENT BUILDER UI
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfGeneratorToolUI(
    tool: PdfGeneratorTool,
    onOutputChange: (String) -> Unit
) {
    var title by remember { mutableStateOf("Offline Toolbox Report") }
    var author by remember { mutableStateOf("Android User") }
    var content by remember {
        mutableStateOf(
            "Privacy-First Offline Architecture\n\n" +
            "This document was generated completely on-device without internet connectivity.\n" +
            "Zero telemetry, zero cloud dependencies, and zero permissions required.\n\n" +
            "Key Advantages:\n" +
            "1. High Speed Local Processing\n" +
            "2. Complete Data Confidentiality\n" +
            "3. Autonomous Offline Operability"
        )
    }
    var format by remember { mutableStateOf(PdfPageFormat.A4) }
    var fontType by remember { mutableStateOf(PdfFontType.HELVETICA) }
    var fontSize by remember { mutableIntStateOf(11) }
    var includePageNumbers by remember { mutableStateOf(true) }
    var formatExpanded by remember { mutableStateOf(false) }
    var fontExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runPdf() {
        scope.launch {
            val res = tool.execute(
                PdfGeneratorInput(
                    title = title,
                    author = author,
                    content = content,
                    pageFormat = format,
                    fontType = fontType,
                    fontSize = fontSize,
                    includePageNumbers = includePageNumbers
                )
            )
            when (res) {
                is ToolResult.Success -> {
                    val out = res.data
                    val report = buildString {
                        appendLine(out.formattedSummary)
                        appendLine("--------------------------------")
                        appendLine("BASE64 PDF STRING (Ready to Save or Share):")
                        appendLine(out.pdfBase64.take(200) + "... [${out.pdfBase64.length} chars total]")
                    }
                    onOutputChange(report)
                }
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runPdf() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; runPdf() },
            label = { Text("Document Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = author,
            onValueChange = { author = it; runPdf() },
            label = { Text("Author / Organization") },
            modifier = Modifier.fillMaxWidth()
        )

        ToolInputField(
            value = content,
            onValueChange = { content = it; runPdf() },
            label = "Document Body Text",
            minLines = 5
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = formatExpanded,
                onExpandedChange = { formatExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = format.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Page Format") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                    PdfPageFormat.entries.forEach { f ->
                        DropdownMenuItem(text = { Text(f.label) }, onClick = { format = f; formatExpanded = false; runPdf() })
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = fontExpanded,
                onExpandedChange = { fontExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = fontType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Font Style") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = fontExpanded, onDismissRequest = { fontExpanded = false }) {
                    PdfFontType.entries.forEach { f ->
                        DropdownMenuItem(text = { Text(f.label) }, onClick = { fontType = f; fontExpanded = false; runPdf() })
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include Page Numbers in Footer", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = includePageNumbers, onCheckedChange = { includePageNumbers = it; runPdf() })
        }
    }
}

// ----------------------------------------------------
// 5. QR CODE MATRIX GENERATOR UI
// ----------------------------------------------------
@Composable
fun QrMatrixGeneratorToolUI(
    tool: QrMatrixGeneratorTool,
    onOutputChange: (String) -> Unit
) {
    var payload by remember { mutableStateOf("https://github.com/offline-toolbox") }
    var ecLevel by remember { mutableStateOf(QrErrorCorrection.M) }
    var asciiPreview by remember { mutableStateOf("") }
    var currentMatrix by remember { mutableStateOf<List<List<Boolean>>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun runQr() {
        scope.launch {
            if (payload.isBlank()) {
                onOutputChange("Enter text or URL to generate QR matrix.")
                currentMatrix = emptyList()
                return@launch
            }
            when (val res = tool.execute(QrMatrixInput(payload, ecLevel))) {
                is ToolResult.Success -> {
                    val out = res.data
                    asciiPreview = out.asciiArt
                    currentMatrix = out.matrix
                    val report = buildString {
                        appendLine("QR MATRIX GENERATED: Version ${out.version} (${out.dimension}x${out.dimension})")
                        appendLine("Recovery Level: ${ecLevel.label}")
                        appendLine("--------------------------------")
                        appendLine(out.asciiArt)
                    }
                    onOutputChange(report)
                }
                is ToolResult.Failure -> {
                    onOutputChange("Error: ${res.message}\n${res.userGuidance ?: ""}")
                    currentMatrix = emptyList()
                }
            }
        }
    }

    LaunchedEffect(Unit) { runQr() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = payload,
            onValueChange = { payload = it; runQr() },
            label = "QR Content / URL / Text",
            minLines = 2
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QrErrorCorrection.entries.forEach { ec ->
                FilterChip(
                    selected = ecLevel == ec,
                    onClick = { ecLevel = ec; runQr() },
                    label = { Text(ec.name) }
                )
            }
        }

        // Graphical Visual Preview of QR Modules
        if (currentMatrix.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.padding(vertical = 4.dp).align(Alignment.CenterHorizontally)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val dim = currentMatrix.size
                    val cellSize = 6.dp
                    for (r in 0 until dim) {
                        Row {
                            for (c in 0 until dim) {
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .background(if (currentMatrix[r][c]) Color.Black else Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. LOAN EMI & AMORTIZATION UI
// ----------------------------------------------------
@Composable
fun LoanEmiCalculatorToolUI(
    tool: LoanEmiCalculatorTool,
    onOutputChange: (String) -> Unit
) {
    var principalStr by remember { mutableStateOf("250000") }
    var rateStr by remember { mutableStateOf("8.75") }
    var tenureStr by remember { mutableStateOf("5") }
    var tenureUnit by remember { mutableStateOf(TenureUnit.YEARS) }
    var prepaymentStr by remember { mutableStateOf("0") }
    val scope = rememberCoroutineScope()

    fun runEmi() {
        scope.launch {
            val p = principalStr.toDoubleOrNull() ?: 0.0
            val r = rateStr.toDoubleOrNull() ?: 0.0
            val t = tenureStr.toIntOrNull() ?: 0
            val prepay = prepaymentStr.toDoubleOrNull() ?: 0.0

            if (p <= 0 || r < 0 || t <= 0) {
                onOutputChange("Please enter valid positive numbers for Principal, Interest Rate, and Tenure.")
                return@launch
            }

            when (val res = tool.execute(LoanEmiInput(p, r, t, tenureUnit, prepay))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Calculation Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runEmi() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = principalStr,
            onValueChange = { principalStr = it; runEmi() },
            label = { Text("Loan Amount (Principal)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rateStr,
                onValueChange = { rateStr = it; runEmi() },
                label = { Text("Interest Rate (%/yr)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = tenureStr,
                onValueChange = { tenureStr = it; runEmi() },
                label = { Text("Tenure") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = tenureUnit == TenureUnit.YEARS,
                onClick = { tenureUnit = TenureUnit.YEARS; runEmi() },
                label = { Text("Tenure in Years") }
            )
            FilterChip(
                selected = tenureUnit == TenureUnit.MONTHS,
                onClick = { tenureUnit = TenureUnit.MONTHS; runEmi() },
                label = { Text("Tenure in Months") }
            )
        }

        OutlinedTextField(
            value = prepaymentStr,
            onValueChange = { prepaymentStr = it; runEmi() },
            label = { Text("Extra Monthly Prepayment (Optional)") },
            placeholder = { Text("e.g. 5000 to save interest & months") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

// ----------------------------------------------------
// 7. MORSE CODE TRANSLATOR UI
// ----------------------------------------------------
@Composable
fun MorseCodeToolUI(
    tool: MorseCodeTool,
    onOutputChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("SOS SAVE OUR PRIVACY") }
    var direction by remember { mutableStateOf(MorseDirection.AUTO_DETECT) }
    val scope = rememberCoroutineScope()

    fun runMorse() {
        scope.launch {
            if (text.isBlank()) {
                onOutputChange("Type text or Morse code (using dots . and dashes -).")
                return@launch
            }
            when (val res = tool.execute(MorseCodeInput(text, direction))) {
                is ToolResult.Success -> {
                    val out = res.data
                    val report = buildString {
                        appendLine("RESULT:")
                        appendLine(out.convertedText)
                        appendLine()
                        appendLine("STATISTICS:")
                        appendLine("Direction:   ${out.detectedDirection.label}")
                        if (out.totalUnits > 0) {
                            appendLine("Dots / Dashes: ${out.dotCount} dots, ${out.dashCount} dashes")
                            appendLine(out.timingPatternSummary)
                        }
                    }
                    onOutputChange(report.trim())
                }
                is ToolResult.Failure -> onOutputChange("Translation failed: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runMorse() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = text,
            onValueChange = { text = it; runMorse() },
            label = "Input Text or Morse Code",
            minLines = 3
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MorseDirection.entries.forEach { dir ->
                FilterChip(
                    selected = direction == dir,
                    onClick = { direction = dir; runMorse() },
                    label = { Text(if (dir == MorseDirection.AUTO_DETECT) "Auto" else dir.label.substringBefore(" ")) }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 8. BASE32 ENCODER / DECODER UI
// ----------------------------------------------------
@Composable
fun Base32ToolUI(
    tool: Base32Tool,
    onOutputChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("OfflineToolbox2026") }
    var mode by remember { mutableStateOf(Base32Mode.ENCODE) }
    var alphabet by remember { mutableStateOf(Base32Alphabet.RFC4648) }
    var pad by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun runBase32() {
        scope.launch {
            when (val res = tool.execute(Base32Input(text, mode, alphabet, pad))) {
                is ToolResult.Success -> onOutputChange(res.data.formattedReport)
                is ToolResult.Failure -> onOutputChange("Base32 Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runBase32() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = text,
            onValueChange = { text = it; runBase32() },
            label = if (mode == Base32Mode.ENCODE) "Raw Text to Encode" else "Base32 String to Decode",
            minLines = 3
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Base32Mode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m; runBase32() },
                    label = { Text(m.label.substringBefore(" ")) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include Standard '=' Padding", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = pad, onCheckedChange = { pad = it; runBase32() })
        }
    }
}

// ----------------------------------------------------
// 9. HTML TO MARKDOWN UI
// ----------------------------------------------------
@Composable
fun HtmlToMarkdownToolUI(
    tool: HtmlToMarkdownTool,
    onOutputChange: (String) -> Unit
) {
    var html by remember {
        mutableStateOf(
            "<h1>Offline Toolbox</h1>\n" +
            "<p>Welcome to <strong>100% offline</strong> utility suite.</p>\n" +
            "<ul>\n" +
            "  <li>Fast execution</li>\n" +
            "  <li>Zero telemetry</li>\n" +
            "</ul>\n" +
            "<pre><code>val offline = true</code></pre>\n" +
            "<a href=\"https://offline.tools\">Learn More</a>"
        )
    }
    val scope = rememberCoroutineScope()

    fun runConvert() {
        scope.launch {
            when (val res = tool.execute(HtmlToMarkdownInput(html))) {
                is ToolResult.Success -> onOutputChange(res.data.markdown)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runConvert() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = html,
            onValueChange = { html = it; runConvert() },
            label = "HTML Source Markup",
            minLines = 6
        )

        Button(onClick = { runConvert() }, modifier = Modifier.fillMaxWidth()) {
            Text("Convert to Markdown")
        }
    }
}

// ----------------------------------------------------
// 10. ASCII ART BANNER & BOX UI
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsciiArtBannerToolUI(
    tool: AsciiArtBannerTool,
    onOutputChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("OFFLINE") }
    var style by remember { mutableStateOf(AsciiBannerStyle.BLOCK_FONT) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runBanner() {
        scope.launch {
            when (val res = tool.execute(AsciiArtBannerInput(text, style))) {
                is ToolResult.Success -> onOutputChange(res.data.banner)
                is ToolResult.Failure -> onOutputChange("Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runBanner() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; runBanner() },
            label = { Text("Banner Text") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = style.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Banner Style") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AsciiBannerStyle.entries.forEach { s ->
                    DropdownMenuItem(text = { Text(s.label) }, onClick = { style = s; expanded = false; runBanner() })
                }
            }
        }
    }
}

// ----------------------------------------------------
// 11. CSV FILTER, QUERY & SORT UI
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvFilterSortToolUI(
    tool: CsvFilterSortTool,
    onOutputChange: (String) -> Unit
) {
    var csv by remember {
        mutableStateOf(
            "Name,Role,Score,City\n" +
            "Alice,Architect,98,New York\n" +
            "Bob,Developer,85,Berlin\n" +
            "Charlie,Security,94,London\n" +
            "David,Developer,78,Tokyo\n" +
            "Eve,Architect,91,Paris"
        )
    }
    var filterCol by remember { mutableStateOf("Score") }
    var filterOp by remember { mutableStateOf(CsvFilterOperator.GREATER_THAN) }
    var filterVal by remember { mutableStateOf("80") }
    var sortCol by remember { mutableStateOf("Score") }
    var sortOrder by remember { mutableStateOf(CsvSortOrder.DESCENDING_NUMERIC) }
    var deduplicate by remember { mutableStateOf(false) }
    var projectCols by remember { mutableStateOf("") }
    var opExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runCsv() {
        scope.launch {
            val res = tool.execute(
                CsvFilterSortInput(
                    csvData = csv,
                    filterColumn = filterCol,
                    filterOperator = filterOp,
                    filterValue = filterVal,
                    sortColumn = sortCol,
                    sortOrder = sortOrder,
                    deduplicateRows = deduplicate,
                    projectColumns = projectCols
                )
            )
            when (res) {
                is ToolResult.Success -> {
                    val out = res.data
                    val report = buildString {
                        appendLine("SUMMARY: ${out.summary}")
                        appendLine("--------------------------------")
                        appendLine("TABLE PREVIEW:")
                        appendLine(out.tablePreview)
                        appendLine("--------------------------------")
                        appendLine("OUTPUT CSV:")
                        appendLine(out.processedCsv)
                    }
                    onOutputChange(report)
                }
                is ToolResult.Failure -> onOutputChange("CSV Query Error: ${res.message}")
            }
        }
    }

    LaunchedEffect(Unit) { runCsv() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolInputField(
            value = csv,
            onValueChange = { csv = it; runCsv() },
            label = "Source CSV with Header Row",
            minLines = 4
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = filterCol,
                onValueChange = { filterCol = it; runCsv() },
                label = { Text("Filter Column") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = opExpanded,
                onExpandedChange = { opExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = filterOp.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Condition") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = opExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = opExpanded, onDismissRequest = { opExpanded = false }) {
                    CsvFilterOperator.entries.forEach { op ->
                        DropdownMenuItem(text = { Text(op.label) }, onClick = { filterOp = op; opExpanded = false; runCsv() })
                    }
                }
            }
        }

        OutlinedTextField(
            value = filterVal,
            onValueChange = { filterVal = it; runCsv() },
            label = { Text("Filter Target Value") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = sortCol,
                onValueChange = { sortCol = it; runCsv() },
                label = { Text("Sort Column") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = sortExpanded,
                onExpandedChange = { sortExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = sortOrder.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sort Order") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    CsvSortOrder.entries.forEach { ord ->
                        DropdownMenuItem(text = { Text(ord.label) }, onClick = { sortOrder = ord; sortExpanded = false; runCsv() })
                    }
                }
            }
        }
    }
}
