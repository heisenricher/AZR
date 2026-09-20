package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7DevToolsTest {

    private val uaTool = UserAgentParserTool()
    private val semverTool = SemVerComparatorTool()
    private val chmodTool = ChmodPermissionsCalculatorTool()
    private val httpTool = HttpHeaderInspectorTool()

    @Test
    fun testUserAgent_androidPixel() = runTest {
        val ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.119 Mobile Safari/537.36"
        val result = uaTool.execute(UserAgentInput(ua))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("Google Chrome", data.browserName)
        assertEquals("Android", data.operatingSystem)
        assertEquals("14", data.osVersion)
        assertEquals("Mobile / Phone", data.deviceCategory)
        assertFalse(data.isBotOrCrawler)
    }

    @Test
    fun testUserAgent_googlebot() = runTest {
        val ua = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
        val result = uaTool.execute(UserAgentInput(ua))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.isBotOrCrawler)
    }

    @Test
    fun testSemVer_compareAndBump() = runTest {
        val result = semverTool.execute(SemVerInput(
            versionA = "1.4.2",
            versionB = "1.5.0",
            constraint = "^1.4.0"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.comparisonResult.contains("LESS than"))
        assertEquals("Minor Change (Feature)", data.diffType)
        assertTrue(data.satisfiesConstraint)
        assertEquals("1.4.3", data.nextPatch)
        assertEquals("1.5.0", data.nextMinor)
        assertEquals("2.0.0", data.nextMajor)
    }

    @Test
    fun testSemVer_invalidFormat() = runTest {
        val result = semverTool.execute(SemVerInput(versionA = "invalid.semver"))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testChmod_octal755() = runTest {
        val result = chmodTool.execute(ChmodInput("755"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("755", data.octal3Digit)
        assertEquals("-rwxr-xr-x", data.symbolicNotation)
        assertTrue(data.ownerRead && data.ownerWrite && data.ownerExecute)
        assertTrue(data.groupRead && !data.groupWrite && data.groupExecute)
        assertTrue(data.othersRead && !data.othersWrite && data.othersExecute)
        assertFalse(data.suid)
    }

    @Test
    fun testChmod_symbolicToOctal() = runTest {
        val result = chmodTool.execute(ChmodInput("rw-r--r--"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("644", data.octal3Digit)
    }

    @Test
    fun testHttpHeader_securityAudit() = runTest {
        val raw = """
            HTTP/1.1 200 OK
            Content-Type: text/html
            Strict-Transport-Security: max-age=31536000; includeSubDomains
            X-Frame-Options: DENY
            X-Content-Type-Options: nosniff
            Content-Security-Policy: default-src 'self'
            Referrer-Policy: no-referrer
        """.trimIndent()
        val result = httpTool.execute(HttpHeaderInput(raw))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(200, data.statusCode)
        assertTrue(data.securityScore >= 80)
        assertTrue(data.securityAudits.any { it.headerName == "Strict-Transport-Security" && it.status == "PASS" })
    }
}
