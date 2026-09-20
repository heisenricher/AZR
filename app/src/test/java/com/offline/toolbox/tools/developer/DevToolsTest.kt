package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevToolsTest {

    // --- JwtDecoderTool ---
    @Test
    fun testJwtDecoder_emptyFails() = runBlocking {
        val tool = JwtDecoderTool()
        val result = tool.execute("")
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testJwtDecoder_invalidSegmentsFails() = runBlocking {
        val tool = JwtDecoderTool()
        val result = tool.execute("invalid.token")
        // "invalid.token" has 2 parts, but might fail base64/json parsing or succeed if minimal
        val singlePart = tool.execute("onlyonepart")
        assertTrue(singlePart is ToolResult.Failure)
    }

    @Test
    fun testJwtDecoder_validJwt() = runBlocking {
        val tool = JwtDecoderTool()
        // Standard test JWT: {"alg":"HS256","typ":"JWT"}.{"sub":"1234567890","name":"John Doe","iat":1516239022}.signature
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val result = tool.execute(jwt)
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("HS256", data.claims.algorithm)
        assertEquals("JWT", data.claims.tokenType)
        assertEquals("1234567890", data.claims.subject)
    }

    // --- UrlEncoderTool ---
    @Test
    fun testUrlEncoder_encodeAndDecode() = runBlocking {
        val tool = UrlEncoderTool()
        val original = "hello world & foo=bar/baz"
        val encResult = tool.execute(UrlEncoderInput(original, UrlOperation.ENCODE))
        assertTrue(encResult is ToolResult.Success)
        val encoded = (encResult as ToolResult.Success).data
        assertTrue(encoded.contains("%26"))
        assertTrue(encoded.contains("%3D") || encoded.contains("="))

        val decResult = tool.execute(UrlEncoderInput(encoded, UrlOperation.DECODE))
        assertTrue(decResult is ToolResult.Success)
        assertEquals(original, (decResult as ToolResult.Success).data)
    }

    @Test
    fun testUrlEncoder_extractQueryParams() = runBlocking {
        val tool = UrlEncoderTool()
        val url = "https://example.com/search?q=android+toolbox&page=2&sort=desc"
        val result = tool.execute(UrlEncoderInput(url, UrlOperation.EXTRACT_QUERY_PARAMS))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.contains("q"))
        assertTrue(data.contains("android toolbox"))
        assertTrue(data.contains("page = 2"))
    }

    // --- HtmlEntityTool ---
    @Test
    fun testHtmlEntity_encodeAndDecode() = runBlocking {
        val tool = HtmlEntityTool()
        val raw = "<div class=\"test\">Tom & Jerry 'fun'</div>"
        val encResult = tool.execute(HtmlEntityInput(raw, HtmlEntityMode.ENCODE))
        assertTrue(encResult is ToolResult.Success)
        val encoded = (encResult as ToolResult.Success).data
        assertTrue(encoded.contains("&lt;div"))
        assertTrue(encoded.contains("&amp;"))

        val decResult = tool.execute(HtmlEntityInput(encoded, HtmlEntityMode.DECODE))
        assertTrue(decResult is ToolResult.Success)
        assertEquals(raw, (decResult as ToolResult.Success).data)
    }

    // --- UnixTimestampTool ---
    @Test
    fun testUnixTimestamp_epochZero() = runBlocking {
        val tool = UnixTimestampTool()
        val result = tool.execute("0")
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(0L, data.epochSeconds)
        assertEquals(0L, data.epochMilliseconds)
        assertTrue(data.utcDateTime.startsWith("1970-01-01"))
    }

    @Test
    fun testUnixTimestamp_now() = runBlocking {
        val tool = UnixTimestampTool()
        val result = tool.execute("now")
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.epochSeconds > 1_700_000_000L)
    }

    // --- NumberBaseConverterTool ---
    @Test
    fun testNumberBaseConverter_decimalConversions() = runBlocking {
        val tool = NumberBaseConverterTool()
        val result = tool.execute(NumberBaseInput("255", NumberBase.DECIMAL))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("11111111", data.binary)
        assertEquals("377", data.octal)
        assertEquals("255", data.decimal)
        assertEquals("FF", data.hexadecimal)
    }

    @Test
    fun testNumberBaseConverter_hexConversions() = runBlocking {
        val tool = NumberBaseConverterTool()
        val result = tool.execute(NumberBaseInput("1A", NumberBase.HEXADECIMAL))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("26", data.decimal)
        assertEquals("11010", data.binary)
    }

    @Test
    fun testNumberBaseConverter_invalidDigitsFails() = runBlocking {
        val tool = NumberBaseConverterTool()
        val result = tool.execute(NumberBaseInput("102", NumberBase.BINARY))
        assertTrue(result is ToolResult.Failure)
    }

    // --- RegexTesterTool ---
    @Test
    fun testRegexTester_matchingAndReplacement() = runBlocking {
        val tool = RegexTesterTool()
        val input = RegexTesterInput(
            regexPattern = "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b",
            testText = "Contact us at support@example.com or sales@offline.org for info.",
            ignoreCase = true,
            replacement = "[REDACTED_EMAIL]"
        )
        val result = tool.execute(input)
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.isMatch)
        assertEquals(2, data.matchCount)
        assertEquals(
            "Contact us at [REDACTED_EMAIL] or [REDACTED_EMAIL] for info.",
            data.replacedText
        )
    }

    @Test
    fun testRegexTester_invalidPatternFails() = runBlocking {
        val tool = RegexTesterTool()
        val result = tool.execute(RegexTesterInput("[unclosed-group", "test"))
        assertTrue(result is ToolResult.Failure)
    }
}
