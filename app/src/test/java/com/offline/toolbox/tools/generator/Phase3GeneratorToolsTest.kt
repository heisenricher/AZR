package com.offline.toolbox.tools.generator

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3GeneratorToolsTest {

    // --- QrPayloadBuilderTool ---
    @Test
    fun testQrPayload_wifi() = runBlocking {
        val tool = QrPayloadBuilderTool()
        val result = tool.execute(
            QrPayloadInput(
                type = QrPayloadType.WIFI,
                wifiSsid = "Coffee_Shop",
                wifiPassword = "Password!123",
                wifiAuthType = "WPA"
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("WIFI:T:WPA;S:Coffee_Shop;P:Password!123;H:false;;", data.payloadString)
    }

    @Test
    fun testQrPayload_vCard() = runBlocking {
        val tool = QrPayloadBuilderTool()
        val result = tool.execute(
            QrPayloadInput(
                type = QrPayloadType.VCARD,
                contactName = "Alice Smith",
                contactPhone = "+1-555-0100",
                contactEmail = "alice@example.com",
                contactOrg = "Tech Co"
            )
        )
        assertTrue(result is ToolResult.Success)
        val payload = (result as ToolResult.Success).data.payloadString
        assertTrue(payload.startsWith("BEGIN:VCARD"))
        assertTrue(payload.contains("FN:Alice Smith"))
        assertTrue(payload.contains("TEL;TYPE=CELL:+1-555-0100"))
        assertTrue(payload.contains("EMAIL;TYPE=INTERNET:alice@example.com"))
        assertTrue(payload.endsWith("END:VCARD"))
    }

    @Test
    fun testQrPayload_emailAndSms() = runBlocking {
        val tool = QrPayloadBuilderTool()
        val emailRes = tool.execute(
            QrPayloadInput(
                type = QrPayloadType.EMAIL,
                emailTo = "support@test.org",
                emailSubject = "Help Request",
                emailBody = "Issue details"
            )
        )
        assertTrue(emailRes is ToolResult.Success)
        assertTrue((emailRes as ToolResult.Success).data.payloadString.startsWith("mailto:support@test.org?subject=Help%20Request"))

        val smsRes = tool.execute(
            QrPayloadInput(
                type = QrPayloadType.SMS,
                smsPhone = "+15551234",
                smsMessage = "Hello World"
            )
        )
        assertTrue(smsRes is ToolResult.Success)
        assertEquals("SMSTO:+15551234:Hello World", (smsRes as ToolResult.Success).data.payloadString)
    }

    @Test
    fun testQrPayload_geo() = runBlocking {
        val tool = QrPayloadBuilderTool()
        val result = tool.execute(
            QrPayloadInput(
                type = QrPayloadType.GEO,
                latitude = 40.7128,
                longitude = -74.0060
            )
        )
        assertTrue(result is ToolResult.Success)
        assertEquals("geo:40.7128,-74.006", (result as ToolResult.Success).data.payloadString)
    }
}
