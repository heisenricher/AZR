package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5DevToolsTest {

    private val subnetTool = SubnetCalculatorTool()
    private val macTool = MacAddressTool()
    private val cronTool = CronExpressionTool()

    @Test
    fun testSubnetCalculator_slash24() = runTest {
        val result = subnetTool.execute(SubnetInput("192.168.1.100", 24))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("192.168.1.0", data.networkAddress)
        assertEquals("192.168.1.255", data.broadcastAddress)
        assertEquals("255.255.255.0", data.subnetMask)
        assertEquals("0.0.0.255", data.wildcardMask)
        assertEquals("192.168.1.1", data.firstUsableIp)
        assertEquals("192.168.1.254", data.lastUsableIp)
        assertEquals(256L, data.totalHosts)
        assertEquals(254L, data.usableHosts)
        assertEquals("Class C", data.ipClass)
        assertTrue(data.isPrivateIp)
    }

    @Test
    fun testSubnetCalculator_slash30() = runTest {
        val result = subnetTool.execute(SubnetInput("10.0.0.5", 30))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("10.0.0.4", data.networkAddress)
        assertEquals("10.0.0.7", data.broadcastAddress)
        assertEquals(4L, data.totalHosts)
        assertEquals(2L, data.usableHosts)
    }

    @Test
    fun testSubnetCalculator_invalidIp() = runTest {
        val result = subnetTool.execute(SubnetInput("999.168.1.1", 24))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testMacAddress_formatsAndUnicast() = runTest {
        val result = macTool.execute(MacAddressInput(macAddress = "00-1A-2B-3C-4D-5E"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("00:1A:2B:3C:4D:5E", data.colonFormat)
        assertEquals("00-1A-2B-3C-4D-5E", data.hyphenFormat)
        assertEquals("001a.2b3c.4d5e", data.ciscoFormat)
        assertEquals("001A2B3C4D5E", data.rawHex)
        assertFalse("Should be unicast", data.isMulticast)
    }

    @Test
    fun testMacAddress_multicastAndVendor() = runTest {
        val result = macTool.execute(MacAddressInput(macAddress = "01:00:5E:00:00:01"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue("Should be multicast", data.isMulticast)
        assertTrue(data.vendorOui != null)
    }

    @Test
    fun testMacAddress_raspberryPiVendor() = runTest {
        val result = macTool.execute(MacAddressInput(macAddress = "B8:27:EB:12:34:56"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("Raspberry Pi Foundation", data.vendorOui)
        assertFalse(data.isMulticast)
        assertFalse(data.isLocallyAdministered)
    }

    @Test
    fun testCronExpression_every15Min() = runTest {
        val result = cronTool.execute(CronInput("*/15 * * * *"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.humanDescription.contains("15 minute", ignoreCase = true) || data.humanDescription.contains("At every", ignoreCase = true))
        assertEquals(5, data.nextRuns.size)
    }

    @Test
    fun testCronExpression_weekdaysAt9Am() = runTest {
        val result = cronTool.execute(CronInput("0 9 * * 1-5"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.hourExplanation.contains("9"))
    }

    @Test
    fun testCronExpression_invalidFields() = runTest {
        val result = cronTool.execute(CronInput("0 9 *"))
        assertTrue(result is ToolResult.Failure)
    }
}
