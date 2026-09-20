package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

enum class XmlJsonDirection {
    XML_TO_JSON,
    JSON_TO_XML
}

data class XmlJsonInput(
    val content: String = """
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
    """.trimIndent(),
    val direction: XmlJsonDirection = XmlJsonDirection.XML_TO_JSON
)

data class XmlJsonOutput(
    val convertedContent: String,
    val direction: XmlJsonDirection,
    val elementCount: Int,
    val formattedReport: String,
    val summary: String
)

class XmlToJsonConverterTool : Tool<XmlJsonInput, XmlJsonOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "xml_to_json_converter_tool",
        name = "XML <-> JSON Bi-Directional Converter",
        description = "Convert XML documents to structured JSON and JSON objects to valid XML with attribute and hierarchy preservation.",
        category = ToolCategory.DATA,
        tags = listOf("xml", "json", "convert", "hierarchy", "tree", "data", "tags", "attributes"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Transform"
    )

    override suspend fun execute(input: XmlJsonInput): ToolResult<XmlJsonOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.content.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Input data cannot be empty.")
        }

        return if (input.direction == XmlJsonDirection.XML_TO_JSON) {
            try {
                val dbf = DocumentBuilderFactory.newInstance()
                // Disable external entities for safety
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                val db = dbf.newDocumentBuilder()
                val doc = db.parse(InputSource(StringReader(raw)))
                doc.documentElement.normalize()

                val rootObj = JSONObject()
                val rootElement = doc.documentElement
                rootObj.put(rootElement.tagName, parseElement(rootElement))

                val formattedJson = rootObj.toString(2)
                val report = buildString {
                    appendLine("XML → JSON CONVERSION SUCCESS")
                    appendLine("--------------------------------------------------")
                    appendLine("Root Tag:    <${rootElement.tagName}>")
                    appendLine("Result Format: JSON (2-space indent)")
                    appendLine()
                    appendLine("Converted JSON:")
                    appendLine(formattedJson)
                }

                val summary = "Converted <${rootElement.tagName}> XML to JSON"

                ToolResult.Success(
                    data = XmlJsonOutput(
                        convertedContent = formattedJson,
                        direction = input.direction,
                        elementCount = 1,
                        formattedReport = report,
                        summary = summary
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            } catch (e: Exception) {
                ToolResult.Failure("Failed to parse XML: ${e.message}")
            }
        } else {
            // JSON TO XML
            try {
                val rootJson = JSONObject(raw)
                val sb = StringBuilder()
                sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")

                fun jsonToXml(obj: Any?, tagName: String, indent: String) {
                    when (obj) {
                        is JSONObject -> {
                            sb.appendLine("$indent<$tagName>")
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                jsonToXml(obj.get(k), k, "$indent  ")
                            }
                            sb.appendLine("$indent</$tagName>")
                        }
                        is JSONArray -> {
                            for (i in 0 until obj.length()) {
                                jsonToXml(obj.get(i), tagName, indent)
                            }
                        }
                        else -> {
                            val escaped = obj.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                            sb.appendLine("$indent<$tagName>$escaped</$tagName>")
                        }
                    }
                }

                val rootKeys = rootJson.keys()
                while (rootKeys.hasNext()) {
                    val k = rootKeys.next()
                    jsonToXml(rootJson.get(k), k, "")
                }

                val xmlResult = sb.toString().trim()
                val report = buildString {
                    appendLine("JSON → XML CONVERSION SUCCESS")
                    appendLine("--------------------------------------------------")
                    appendLine("Direction:   JSON to XML")
                    appendLine()
                    appendLine("Converted XML:")
                    appendLine(xmlResult)
                }

                val summary = "Converted JSON to XML (${rootJson.length()} root keys)"

                ToolResult.Success(
                    data = XmlJsonOutput(
                        convertedContent = xmlResult,
                        direction = input.direction,
                        elementCount = rootJson.length(),
                        formattedReport = report,
                        summary = summary
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            } catch (e: Exception) {
                ToolResult.Failure("Failed to parse JSON: ${e.message}")
            }
        }
    }

    private fun parseElement(element: Element): Any {
        val obj = JSONObject()

        // Attributes
        val attrs = element.attributes
        for (i in 0 until attrs.length) {
            val attr = attrs.item(i)
            obj.put("@${attr.nodeName}", attr.nodeValue)
        }

        // Child nodes
        val children = element.childNodes
        val childGroups = mutableMapOf<String, MutableList<Any>>()
        val textBuilder = StringBuilder()

        for (i in 0 until children.length) {
            val child = children.item(i)
            when (child.nodeType) {
                Node.ELEMENT_NODE -> {
                    val childElem = child as Element
                    val childObj = parseElement(childElem)
                    childGroups.getOrPut(childElem.tagName) { mutableListOf() }.add(childObj)
                }
                Node.TEXT_NODE -> {
                    val txt = child.textContent.trim()
                    if (txt.isNotEmpty()) textBuilder.append(txt)
                }
            }
        }

        if (childGroups.isEmpty() && attrs.length == 0) {
            val txt = textBuilder.toString()
            return when {
                txt.equals("true", ignoreCase = true) -> true
                txt.equals("false", ignoreCase = true) -> false
                txt.toIntOrNull() != null -> txt.toInt()
                txt.toDoubleOrNull() != null -> txt.toDouble()
                else -> txt
            }
        }

        for ((tag, list) in childGroups) {
            if (list.size == 1) {
                obj.put(tag, list[0])
            } else {
                val arr = JSONArray()
                list.forEach { arr.put(it) }
                obj.put(tag, arr)
            }
        }

        val directText = textBuilder.toString()
        if (directText.isNotEmpty()) {
            obj.put("#text", directText)
        }

        return obj
    }
}
