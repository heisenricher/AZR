package com.offline.toolbox.tools.generator

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class QrPayloadType(val label: String) {
    WIFI("Wi-Fi Network"),
    VCARD("Contact Card (vCard)"),
    EMAIL("Email (mailto:)"),
    SMS("SMS Text Message"),
    GEO("Geo Location (GPS)")
}

data class QrPayloadInput(
    val type: QrPayloadType = QrPayloadType.WIFI,
    // WiFi fields
    val wifiSsid: String = "MyHomeNetwork",
    val wifiPassword: String = "SecretPassword123",
    val wifiAuthType: String = "WPA", // WPA, WEP, nopass
    val wifiHidden: Boolean = false,
    // vCard fields
    val contactName: String = "Jane Doe",
    val contactPhone: String = "+1-555-0199",
    val contactEmail: String = "jane.doe@example.com",
    val contactOrg: String = "Acme Corp",
    // Email fields
    val emailTo: String = "support@example.com",
    val emailSubject: String = "Inquiry",
    val emailBody: String = "Hello, I have a question...",
    // SMS fields
    val smsPhone: String = "+1-555-0199",
    val smsMessage: String = "Hello!",
    // Geo fields
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194
)

data class QrPayloadOutput(
    val payloadString: String,
    val typeLabel: String,
    val byteLength: Int,
    val summary: String
)

class QrPayloadBuilderTool : Tool<QrPayloadInput, QrPayloadOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "qr_payload_builder",
        name = "QR Code Payload Builder",
        description = "Generate standard offline QR code payloads for Wi-Fi networks, vCard contacts, SMS, Email, and GPS coordinates.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("qr", "payload", "wifi", "vcard", "contact", "sms", "email", "barcode", "geo", "generator"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "QrCode"
    )

    override suspend fun execute(input: QrPayloadInput): ToolResult<QrPayloadOutput> {
        val startTime = System.currentTimeMillis()

        val payload = when (input.type) {
            QrPayloadType.WIFI -> {
                val escape = { s: String -> s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:") }
                "WIFI:T:${input.wifiAuthType};S:${escape(input.wifiSsid)};P:${escape(input.wifiPassword)};H:${input.wifiHidden};;"
            }
            QrPayloadType.VCARD -> {
                buildString {
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    appendLine("FN:${input.contactName}")
                    if (input.contactOrg.isNotBlank()) appendLine("ORG:${input.contactOrg}")
                    if (input.contactPhone.isNotBlank()) appendLine("TEL;TYPE=CELL:${input.contactPhone}")
                    if (input.contactEmail.isNotBlank()) appendLine("EMAIL;TYPE=INTERNET:${input.contactEmail}")
                    append("END:VCARD")
                }
            }
            QrPayloadType.EMAIL -> {
                val encSub = URLEncoder.encode(input.emailSubject, StandardCharsets.UTF_8.toString()).replace("+", "%20")
                val encBody = URLEncoder.encode(input.emailBody, StandardCharsets.UTF_8.toString()).replace("+", "%20")
                "mailto:${input.emailTo}?subject=$encSub&body=$encBody"
            }
            QrPayloadType.SMS -> {
                "SMSTO:${input.smsPhone}:${input.smsMessage}"
            }
            QrPayloadType.GEO -> {
                "geo:${input.latitude},${input.longitude}"
            }
        }

        val bytes = payload.toByteArray(StandardCharsets.UTF_8).size
        val summary = "Generated ${input.type.label} payload ($bytes bytes)"

        return ToolResult.Success(
            data = QrPayloadOutput(
                payloadString = payload,
                typeLabel = input.type.label,
                byteLength = bytes,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
