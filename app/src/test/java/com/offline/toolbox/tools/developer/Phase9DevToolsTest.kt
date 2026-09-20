package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9DevToolsTest {

    private val curlTool = CurlCommandParserTool()
    private val asnTool = BgpAsnLookupTool()
    private val gitIgnoreTool = GitIgnoreGeneratorTool()
    private val cronDiffTool = CrontabScheduleDiffTool()

    @Test
    fun testCurlParser_postWithHeadersAndBody() = runTest {
        val cmd = """curl -X POST "https://api.example.com/items" -H "Authorization: Bearer token123" -H "Content-Type: application/json" -d '{"name":"Widget"}'"""
        val res = curlTool.execute(CurlParserInput(curlCommand = cmd))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("POST", data.method)
        assertEquals("https://api.example.com/items", data.url)
        assertEquals("Bearer token123", data.headers["Authorization"])
        assertTrue(data.kotlinSnippet.contains("HttpURLConnection"))
        assertTrue(data.pythonSnippet.contains("requests.post"))
        assertTrue(data.javascriptSnippet.contains("fetch("))
    }

    @Test
    fun testCurlParser_invalidCommandFails() = runTest {
        val res = curlTool.execute(CurlParserInput(curlCommand = "not-a-curl-command http://foo"))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testBgpAsnLookup_knownAndPrivateRanges() = runTest {
        val resGoogle = asnTool.execute(BgpAsnInput(asnQuery = "15169"))
        assertTrue(resGoogle is ToolResult.Success)
        val dataGoogle = (resGoogle as ToolResult.Success).data
        assertEquals(15169L, dataGoogle.asNumber)
        assertEquals("AS15169", dataGoogle.asPlain)
        assertEquals("0.15169", dataGoogle.asDot)
        assertTrue(dataGoogle.matchedOrgName?.contains("Google") == true)
        assertFalse(dataGoogle.isPrivateOrReserved)

        val resPrivate = asnTool.execute(BgpAsnInput(asnQuery = "64512"))
        assertTrue(resPrivate is ToolResult.Success)
        val dataPrivate = (resPrivate as ToolResult.Success).data
        assertTrue(dataPrivate.isPrivateOrReserved)
        assertEquals("RFC 6996 16-Bit Private Use ASN", dataPrivate.rangeClassification)

        val resAsDot = asnTool.execute(BgpAsnInput(asnQuery = "0.13335"))
        assertTrue(resAsDot is ToolResult.Success)
        val dataAsDot = (resAsDot as ToolResult.Success).data
        assertEquals(13335L, dataAsDot.asNumber)
        assertTrue(dataAsDot.matchedOrgName?.contains("Cloudflare") == true)
    }

    @Test
    fun testBgpAsnLookup_invalidFails() = runTest {
        val res = asnTool.execute(BgpAsnInput(asnQuery = "99999999999")) // exceeds 32-bit
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testGitIgnoreGenerator_generatesMultiStackTemplate() = runTest {
        val res = gitIgnoreTool.execute(GitIgnoreInput(
            selectedPresets = listOf(GitIgnorePreset.ANDROID, GitIgnorePreset.GRADLE, GitIgnorePreset.NODE)
        ))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.generatedGitIgnore.contains(".gradle/"))
        assertTrue(data.generatedGitIgnore.contains("node_modules/"))
        assertEquals(3, data.presetCount)
        assertTrue(data.totalRules > 5)
    }

    @Test
    fun testCrontabDiff_overlapDetection() = runTest {
        // */15 and 0 * * * *
        val res = cronDiffTool.execute(CrontabDiffInput(
            cronA = "*/15 * * * *",
            cronB = "0 * * * *"
        ))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.upcomingSimultaneousRuns.isNotEmpty())
        assertTrue(data.overlapRelationship.isNotEmpty())
    }

    @Test
    fun testCrontabDiff_invalidCronFails() = runTest {
        val res = cronDiffTool.execute(CrontabDiffInput(
            cronA = "invalid cron syntax",
            cronB = "0 0 * * *"
        ))
        assertTrue(res is ToolResult.Failure)
    }
}
