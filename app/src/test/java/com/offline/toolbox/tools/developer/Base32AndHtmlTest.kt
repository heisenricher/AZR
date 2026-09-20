package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Base32AndHtmlTest {

    private val base32Tool = Base32Tool()
    private val htmlTool = HtmlToMarkdownTool()

    @Test
    fun testBase32Rfc4648StandardVectors() = runBlocking {
        // RFC 4648 Section 10 standard test vectors
        val vectors = mapOf(
            "f" to "MY======",
            "fo" to "MZXQ====",
            "foo" to "MZXW6===",
            "foob" to "MZXW6YQ=",
            "foobar" to "MZXW6YTBOI======"
        )

        for ((input, expected) in vectors) {
            val encRes = base32Tool.execute(
                Base32Input(
                    text = input,
                    mode = Base32Mode.ENCODE,
                    alphabet = Base32Alphabet.RFC4648,
                    pad = true
                )
            )
            assertTrue("Encoding '$input' failed", encRes is ToolResult.Success)
            assertEquals(expected, (encRes as ToolResult.Success).data.result)

            // Verify round-trip decoding
            val decRes = base32Tool.execute(
                Base32Input(
                    text = expected,
                    mode = Base32Mode.DECODE,
                    alphabet = Base32Alphabet.RFC4648
                )
            )
            assertTrue("Decoding '$expected' failed", decRes is ToolResult.Success)
            assertEquals(input, (decRes as ToolResult.Success).data.result)
        }
    }

    @Test
    fun testBase32HexAlphabet() = runBlocking {
        val res = base32Tool.execute(
            Base32Input(
                text = "foobar",
                mode = Base32Mode.ENCODE,
                alphabet = Base32Alphabet.HEX,
                pad = true
            )
        )
        assertTrue(res is ToolResult.Success)
        val encoded = (res as ToolResult.Success).data.result
        // In Base32Hex: M (12) -> C, Z (25) -> P, X (23) -> N, W (22) -> M, 6 (30) -> U, Y (24) -> O, T (19) -> J, etc.
        assertEquals("CPNMUOJ1E8======", encoded)
    }

    @Test
    fun testHtmlToMarkdownConversion() = runBlocking {
        val html = """
            <h1>Title of Article</h1>
            <p>This is a paragraph with <strong>bold</strong> and <em>italic</em> text.</p>
            <ul>
              <li>Item One</li>
              <li>Item Two</li>
            </ul>
            <pre><code>val offline = true</code></pre>
            <p><a href="https://example.com">Visit Site</a></p>
            <script>alert('evil');</script>
        """.trimIndent()

        val res = htmlTool.execute(HtmlToMarkdownInput(html = html))
        assertTrue(res is ToolResult.Success)
        val md = (res as ToolResult.Success).data.markdown

        assertTrue("Should convert h1 to #", md.contains("# Title of Article"))
        assertTrue("Should convert strong to **bold**", md.contains("**bold**"))
        assertTrue("Should convert em to *italic*", md.contains("*italic*"))
        assertTrue("Should convert li to - Item One", md.contains("- Item One"))
        assertTrue("Should convert code to ```", md.contains("```") && md.contains("val offline = true"))
        assertTrue("Should convert link to [Visit Site](https://example.com)", md.contains("[Visit Site](https://example.com)"))
        assertFalse("Should strip script tags", md.contains("alert('evil')"))
        assertFalse("Should strip script tags", md.contains("<script>"))
    }
}
