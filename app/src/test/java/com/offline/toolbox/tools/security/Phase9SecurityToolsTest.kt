package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase9SecurityToolsTest {

    private val hkdfTool = HkdfKeyDerivationTool()
    private val vigenereTool = VigenereCipherTool()

    @Test
    fun testHkdf_sha256KeyDerivation() = runTest {
        val res = hkdfTool.execute(HkdfInput(
            ikm = "input-master-key-material",
            salt = "cryptographic-salt",
            info = "context-info-1",
            outputKeyLengthBytes = 32,
            algorithm = HkdfHashAlgorithm.SHA256
        ))

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(32, data.keyLengthBytes)
        assertEquals(64, data.derivedKeyHex.length) // 32 bytes = 64 hex characters
        assertEquals(64, data.pseudorandomKeyHex.length)
        assertEquals(HkdfHashAlgorithm.SHA256, data.algorithm)
    }

    @Test
    fun testHkdf_sha512KeyDerivation() = runTest {
        val res = hkdfTool.execute(HkdfInput(
            ikm = "master-super-secret-key-material",
            salt = "app-salt-value",
            info = "session-encryption-context",
            outputKeyLengthBytes = 64,
            algorithm = HkdfHashAlgorithm.SHA512
        ))

        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(64, data.keyLengthBytes)
        assertEquals(128, data.derivedKeyHex.length) // 64 bytes = 128 hex chars
    }

    @Test
    fun testHkdf_emptyIkmFails() = runTest {
        val res = hkdfTool.execute(HkdfInput(
            ikm = ""
        ))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testVigenere_encryptionAndDecryptionCycle() = runTest {
        val original = "ATTACK AT DAWN ON THE NORTHEAST FORTRESS"
        val key = "LEMON"

        val encRes = vigenereTool.execute(VigenereInput(
            text = original,
            key = key,
            mode = VigenereMode.ENCRYPT
        ))
        assertTrue(encRes is ToolResult.Success)
        val cipherText = (encRes as ToolResult.Success).data.result
        // Ensure ciphertext is altered
        assertTrue(cipherText != original)

        val decRes = vigenereTool.execute(VigenereInput(
            text = cipherText,
            key = key,
            mode = VigenereMode.DECRYPT
        ))
        assertTrue(decRes is ToolResult.Success)
        val decryptedText = (decRes as ToolResult.Success).data.result
        assertEquals(original, decryptedText)
    }

    @Test
    fun testVigenere_indexOfCoincidence() = runTest {
        val sample = "The project team delivered an outstanding offline utility toolbox with extreme performance and privacy guarantees."
        val res = vigenereTool.execute(VigenereInput(
            text = sample,
            key = "SECRET",
            mode = VigenereMode.ENCRYPT
        ))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.indexOfCoincidence > 0.0)
    }

    @Test
    fun testVigenere_emptyKeyFails() = runTest {
        val res = vigenereTool.execute(VigenereInput(
            text = "Hello",
            key = "",
            mode = VigenereMode.ENCRYPT
        ))
        assertTrue(res is ToolResult.Failure)
    }
}
