package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5SecurityToolsTest {

    private val hmacTool = HmacGeneratorTool()
    private val rsaTool = RsaKeyPairTool()

    @Test
    fun testHmac_sha256StandardVector() = runTest {
        // Standard test: payload "The quick brown fox jumps over the lazy dog", key "key"
        // Known HMAC-SHA256: f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8
        val result = hmacTool.execute(
            HmacInput(
                message = "The quick brown fox jumps over the lazy dog",
                secretKey = "key",
                algorithm = HmacAlgorithm.HMAC_SHA256,
                keyEncoding = KeyEncoding.UTF8_TEXT
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8", data.hexSignature.lowercase())
    }

    @Test
    fun testHmac_sha512Keyed() = runTest {
        val result = hmacTool.execute(
            HmacInput(
                message = "Offline Authentication",
                secretKey = "offline-secret-key",
                algorithm = HmacAlgorithm.HMAC_SHA512
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(128, data.hexSignature.length) // 512 bits / 4 bits per hex char = 128
        assertTrue(data.base64Signature.isNotEmpty())
    }

    @Test
    fun testHmac_hexKeyFormat() = runTest {
        val result = hmacTool.execute(
            HmacInput(
                message = "Test data",
                secretKey = "48656c6c6f", // "Hello" in hex
                algorithm = HmacAlgorithm.HMAC_SHA256,
                keyEncoding = KeyEncoding.HEX_STRING
            )
        )
        assertTrue(result is ToolResult.Success)
    }

    @Test
    fun testHmac_invalidHexKey() = runTest {
        val result = hmacTool.execute(
            HmacInput(
                message = "Test data",
                secretKey = "ZZZZ", // Not hex
                algorithm = HmacAlgorithm.HMAC_SHA256,
                keyEncoding = KeyEncoding.HEX_STRING
            )
        )
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testRsaKeyPair_generation1024() = runTest {
        val result = rsaTool.execute(RsaKeyPairInput(keySize = RsaKeySize.RSA_1024))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(1024, data.keySizeBits)
        assertTrue(data.publicKeyPem.contains("BEGIN PUBLIC KEY"))
        assertTrue(data.publicKeyPem.contains("END PUBLIC KEY"))
        assertTrue(data.privateKeyPem.contains("BEGIN PRIVATE KEY"))
        assertTrue(data.privateKeyPem.contains("END PRIVATE KEY"))
    }
}
