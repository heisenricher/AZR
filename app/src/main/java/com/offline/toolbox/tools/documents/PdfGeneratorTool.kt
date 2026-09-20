package com.offline.toolbox.tools.documents

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

enum class PdfPageFormat(val widthPt: Int, val heightPt: Int, val label: String) {
    A4(595, 842, "A4 (210 x 297 mm)"),
    LETTER(612, 792, "US Letter (8.5 x 11 in)")
}

enum class PdfFontType(val regular: String, val bold: String, val label: String) {
    HELVETICA("Helvetica", "Helvetica-Bold", "Sans-Serif (Helvetica)"),
    TIMES("Times-Roman", "Times-Bold", "Serif (Times)"),
    COURIER("Courier", "Courier-Bold", "Monospace (Courier)")
}

data class PdfGeneratorInput(
    val title: String = "Untitled Document",
    val author: String = "Offline Toolbox",
    val content: String = "",
    val pageFormat: PdfPageFormat = PdfPageFormat.A4,
    val fontType: PdfFontType = PdfFontType.HELVETICA,
    val fontSize: Int = 11,
    val includePageNumbers: Boolean = true
)

data class PdfGeneratorOutput(
    val pdfBase64: String,
    val pageCount: Int,
    val totalBytes: Int,
    val formattedSummary: String,
    val summary: String
)

class PdfGeneratorTool : Tool<PdfGeneratorInput, PdfGeneratorOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "pdf_generator_tool",
        name = "Offline PDF Document Builder",
        description = "Generate clean, multi-page PDF documents from text, notes, and code with pagination.",
        category = ToolCategory.FILE,
        tags = listOf("pdf", "document", "export", "print", "generator", "writer", "page", "file"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "PictureAsPdf"
    )

    override suspend fun execute(input: PdfGeneratorInput): ToolResult<PdfGeneratorOutput> {
        val startTime = System.currentTimeMillis()

        if (input.content.isBlank() && input.title.isBlank()) {
            return ToolResult.Failure(
                message = "Document content is empty.",
                userGuidance = "Provide a title or content text to generate a PDF document."
            )
        }

        return try {
            val pdfBytes = buildPdf(input)
            val base64 = Base64.getEncoder().encodeToString(pdfBytes)
            val approxPageEstimate = calculateEstimatedPages(input)

            val summary = "Generated PDF (${pdfBytes.size} bytes, ~$approxPageEstimate pages)"
            val formatted = buildString {
                appendLine("PDF DOCUMENT GENERATED SUCCESSFULLY")
                appendLine("Title:       ${input.title}")
                appendLine("Author:      ${input.author}")
                appendLine("Page Format: ${input.pageFormat.label}")
                appendLine("Font Family: ${input.fontType.label}")
                appendLine("File Size:   ${pdfBytes.size} bytes")
                appendLine("Total Pages: $approxPageEstimate")
                appendLine("Base64 Length: ${base64.length} chars")
            }

            ToolResult.Success(
                data = PdfGeneratorOutput(
                    pdfBase64 = base64,
                    pageCount = approxPageEstimate,
                    totalBytes = pdfBytes.size,
                    formattedSummary = formatted,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("Failed to build PDF: ${e.message}", cause = e)
        }
    }

    private fun calculateEstimatedPages(input: PdfGeneratorInput): Int {
        val linesPerPage = ((input.pageFormat.heightPt - 140) / (input.fontSize * 1.4)).toInt().coerceAtLeast(10)
        val wrappedLines = wrapText(input.content, maxCharsPerLine(input))
        return (wrappedLines.size / linesPerPage) + 1
    }

    private fun maxCharsPerLine(input: PdfGeneratorInput): Int {
        val printableWidth = input.pageFormat.widthPt - 100 // 50pt margins each side
        val charWidthEstimate = input.fontSize * 0.55
        return (printableWidth / charWidthEstimate).toInt().coerceAtLeast(30)
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (p in paragraphs) {
            if (p.isBlank()) {
                result.add("")
                continue
            }
            val words = p.split(Regex("\\s+"))
            var currentLine = StringBuilder()
            for (word in words) {
                if (currentLine.isEmpty()) {
                    currentLine.append(word)
                } else if (currentLine.length + 1 + word.length <= maxChars) {
                    currentLine.append(" ").append(word)
                } else {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }
        return result
    }

    private fun buildPdf(input: PdfGeneratorInput): ByteArray {
        val width = input.pageFormat.widthPt
        val height = input.pageFormat.heightPt
        val margin = 50
        val fontSize = input.fontSize
        val lineHeight = (fontSize * 1.4).toInt()

        val allLines = wrapText(input.content, maxCharsPerLine(input))
        val maxLinesFirstPage = ((height - 200) / lineHeight).coerceAtLeast(5)
        val maxLinesSubsequent = ((height - 140) / lineHeight).coerceAtLeast(10)

        val pages = mutableListOf<List<String>>()
        var cursor = 0
        var isFirst = true

        while (cursor < allLines.size || pages.isEmpty()) {
            val limit = if (isFirst) maxLinesFirstPage else maxLinesSubsequent
            val takeCount = limit.coerceAtMost(allLines.size - cursor)
            val slice = if (cursor < allLines.size) allLines.subList(cursor, cursor + takeCount) else emptyList()
            pages.add(slice)
            cursor += takeCount
            isFirst = false
            if (cursor >= allLines.size) break
        }

        val totalPages = pages.size
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val objects = mutableListOf<String>()

        // Obj 1: Catalog
        objects.add("<< /Type /Catalog /Pages 2 0 R >>")

        // Obj 2: Pages placeholder (will be filled after building page objects)
        // Font objects: Obj 3 (Regular), Obj 4 (Bold)
        val regFontName = input.fontType.regular
        val boldFontName = input.fontType.bold

        val pageObjIds = mutableListOf<Int>()
        val pageContentObjIds = mutableListOf<Int>()

        var nextObjId = 5
        for (i in 0 until totalPages) {
            val pageId = nextObjId++
            val contentId = nextObjId++
            pageObjIds.add(pageId)
            pageContentObjIds.add(contentId)
        }

        val pagesObj = "<< /Type /Pages /Kids [${pageObjIds.joinToString(" ") { "$it 0 R" }}] /Count $totalPages >>"

        val fontRegObj = "<< /Type /Font /Subtype /Type1 /BaseFont /$regFontName /Encoding /WinAnsiEncoding >>"
        val fontBoldObj = "<< /Type /Font /Subtype /Type1 /BaseFont /$boldFontName /Encoding /WinAnsiEncoding >>"

        val fullObjects = mutableListOf<String>()
        fullObjects.add(objects[0]) // 1: Catalog
        fullObjects.add(pagesObj)   // 2: Pages
        fullObjects.add(fontRegObj) // 3: Font Regular
        fullObjects.add(fontBoldObj)// 4: Font Bold

        for (pIndex in 0 until totalPages) {
            val contentId = pageContentObjIds[pIndex]

            // Page Object
            val pageObj = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $width $height] /Contents $contentId 0 R /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> >>"
            fullObjects.add(pageObj)

            // Content Stream Object
            val pageLines = pages[pIndex]
            val streamBuilder = StringBuilder()

            // Header on first page
            if (pIndex == 0) {
                // Title (Bold 18pt)
                streamBuilder.append("BT /F2 18 Tf $margin ${height - 65} Td (${escapePdfText(input.title)}) Tj ET\n")
                // Metadata Author & Date (9pt)
                streamBuilder.append("BT /F1 9 Tf $margin ${height - 82} Td (${escapePdfText("By: ${input.author}  |  $dateString")}) Tj ET\n")
                // Divider line
                streamBuilder.append("0.5 w $margin ${height - 92} m ${width - margin} ${height - 92} l S\n")
            } else {
                // Running top header for subsequent pages
                streamBuilder.append("BT /F1 8 Tf $margin ${height - 35} Td (${escapePdfText(input.title)}) Tj ET\n")
                streamBuilder.append("0.2 w $margin ${height - 40} m ${width - margin} ${height - 40} l S\n")
            }

            // Body text
            val startY = if (pIndex == 0) height - 120 else height - 60
            var currentY = startY
            for (line in pageLines) {
                if (line.isNotEmpty()) {
                    val safeLine = escapePdfText(line)
                    streamBuilder.append("BT /F1 $fontSize Tf $margin $currentY Td ($safeLine) Tj ET\n")
                }
                currentY -= lineHeight
            }

            // Footer / Page numbers
            if (input.includePageNumbers) {
                val pageText = "Page ${pIndex + 1} of $totalPages"
                streamBuilder.append("BT /F1 8 Tf ${width / 2 - 20} 30 Td ($pageText) Tj ET\n")
            }

            val streamBytes = streamBuilder.toString().toByteArray(StandardCharsets.ISO_8859_1)
            val streamObj = "<< /Length ${streamBytes.size} >>\nstream\n${streamBuilder}endstream"
            fullObjects.add(streamObj)
        }

        val baos = ByteArrayOutputStream()
        baos.write("%PDF-1.4\n".toByteArray(StandardCharsets.ISO_8859_1))
        baos.write(byteArrayOf(0x25, 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), 0x0A))

        val xrefOffsets = mutableListOf<Long>()
        xrefOffsets.add(0L) // Object 0 is always 0

        for (i in fullObjects.indices) {
            xrefOffsets.add(baos.size().toLong())
            val objNum = i + 1
            baos.write("$objNum 0 obj\n${fullObjects[i]}\nendobj\n".toByteArray(StandardCharsets.ISO_8859_1))
        }

        val startXref = baos.size()
        baos.write("xref\n0 ${fullObjects.size + 1}\n".toByteArray(StandardCharsets.ISO_8859_1))
        baos.write("0000000000 65535 f \n".toByteArray(StandardCharsets.ISO_8859_1))

        for (i in 1..fullObjects.size) {
            val offset = xrefOffsets[i]
            val entry = "%010d 00000 n \n".format(Locale.US, offset)
            baos.write(entry.toByteArray(StandardCharsets.ISO_8859_1))
        }

        baos.write("trailer\n<< /Size ${fullObjects.size + 1} /Root 1 0 R >>\n".toByteArray(StandardCharsets.ISO_8859_1))
        baos.write("startxref\n$startXref\n%%EOF\n".toByteArray(StandardCharsets.ISO_8859_1))

        return baos.toByteArray()
    }

    private fun escapePdfText(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '(' -> sb.append("\\(")
                ')' -> sb.append("\\)")
                '\r' -> {}
                '\t' -> sb.append("    ")
                else -> {
                    val code = ch.code
                    if (code in 32..126 || code in 160..255) {
                        sb.append(ch)
                    } else if (code < 32) {
                        // ignore unprintable control characters
                    } else {
                        // Replace unicode out-of-range character with closest ascii or ?
                        sb.append('?')
                    }
                }
            }
        }
        return sb.toString()
    }
}
