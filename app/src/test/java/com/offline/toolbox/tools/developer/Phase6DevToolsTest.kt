package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase6DevToolsTest {

    private val ipv6Tool = IPv6SubnetCalculatorTool()
    private val portTool = PortLookupTool()

    @Test
    fun testIPv6_expandAndCompress() = runTest {
        val result = ipv6Tool.execute(IPv6SubnetInput(
            ipv6Address = "2001:db8::1",
            prefixLength = 64
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("2001:0db8:0000:0000:0000:0000:0000:0001", data.expandedAddress)
        assertEquals("2001:db8::1", data.compressedAddress)
        assertEquals(64, data.prefixLength)
        assertTrue(data.formattedReport.contains("Network Prefix"))
    }

    @Test
    fun testIPv6_loopbackAndScope() = runTest {
        val result = ipv6Tool.execute(IPv6SubnetInput(
            ipv6Address = "::1",
            prefixLength = 128
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("Loopback (::1)", data.addressType)
        assertEquals("::1", data.compressedAddress)
        assertEquals("0000:0000:0000:0000:0000:0000:0000:0001", data.expandedAddress)
    }

    @Test
    fun testIPv6_linkLocal() = runTest {
        val result = ipv6Tool.execute(IPv6SubnetInput(
            ipv6Address = "fe80::1",
            prefixLength = 64
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue("Should be link-local", data.isLinkLocal)
        assertFalse("Should not be global unicast", data.isGlobalUnicast)
    }

    @Test
    fun testIPv6_invalidAddress() = runTest {
        val result = ipv6Tool.execute(IPv6SubnetInput(
            ipv6Address = "invalid:::ipv6:address",
            prefixLength = 64
        ))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testPortLookup_port443Https() = runTest {
        val result = portTool.execute(PortLookupInput(query = "443"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.matchedCount >= 1)
        val https = data.ports.first { it.portNumber == 443 }
        assertEquals("HTTPS", https.serviceName)
        assertEquals("TCP", https.protocol)
    }

    @Test
    fun testPortLookup_sshServiceLookup() = runTest {
        val result = portTool.execute(PortLookupInput(query = "ssh"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.matchedCount >= 1)
        assertTrue(data.ports.any { it.portNumber == 22 })
    }

    @Test
    fun testPortLookup_emptyQueryFails() = runTest {
        val result = portTool.execute(PortLookupInput(query = ""))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testPortLookup_unknownPort() = runTest {
        val result = portTool.execute(PortLookupInput(query = "59999"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(0, data.matchedCount)
        assertTrue(data.formattedReport.contains("No standard port found"))
    }
}
