package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.xml.sax.InputSource

data class XmlFormatterInput(
    val xml: String,
    val indentSpaces: Int = 2,
    val minify: Boolean = false
)

data class XmlFormatterOutput(
    val formattedXml: String,
    val lineCount: Int,
    val summary: String
)

class XmlFormatterTool : Tool<XmlFormatterInput, XmlFormatterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "xml_formatter",
        name = "XML Formatter & Validator",
        description = "Format, beautify, minify, and validate XML documents offline with custom indentation.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("xml", "format", "beautify", "minify", "validate", "html", "markup", "indent"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_PASTE,
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Code"
    )

    override suspend fun execute(input: XmlFormatterInput): ToolResult<XmlFormatterOutput> {
        val startTime = System.currentTimeMillis()
        val text = input.xml.trim()

        if (text.isEmpty()) {
            return ToolResult.Failure(
                message = "Input XML is empty.",
                userGuidance = "Paste an XML document or snippet (e.g. '<root><item>value</item></root>')."
            )
        }

        return try {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = true
            // Secure against XXE
            try {
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false)
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            } catch (_: Exception) {}

            val db = dbf.newDocumentBuilder()
            val doc = db.parse(InputSource(StringReader(text)))
            doc.normalize()

            val tf = TransformerFactory.newInstance()
            val transformer = tf.newTransformer()

            if (input.minify) {
                transformer.setOutputProperty(OutputKeys.INDENT, "no")
            } else {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", input.indentSpaces.coerceIn(1, 8).toString())
            }

            val writer = StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(writer))
            val result = writer.toString().trim()

            val lineCount = result.lines().size
            val summary = if (input.minify) "Minified XML ($lineCount line)" else "Formatted XML ($lineCount lines)"

            ToolResult.Success(
                data = XmlFormatterOutput(
                    formattedXml = result,
                    lineCount = lineCount,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure(
                message = "Invalid XML: ${e.localizedMessage ?: e.message}",
                userGuidance = "Ensure all opening tags have matching closing tags and attributes are properly quoted.",
                cause = e
            )
        }
    }
}
