package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase6SecurityToolsTest {

    private val cipherTool = SymmetricCipherTool()
    private val bcryptTool = BcryptWorkFactorTool()

    @Test
    fun testSymmetricCipher_aes256GcmRoundTrip() = runTest {
        val secretMessage = "Confidential Payload 100% Offline"
        val password = "SuperSecurePassword123!"

        // Encrypt
        val encryptResult = cipherTool.execute(SymmetricCipherInput(
            text = secretMessage,
            passphrase = password,
            mode = SymmetricMode.ENCRYPT,
            algorithm = SymmetricAlgorithm.AES_256_GCM
        ))
        assertTrue(encryptResult is ToolResult.Success)
        val encryptedData = (encryptResult as ToolResult.Success).data
        assertTrue(encryptedData.isSuccess)
        assertNotEquals(secretMessage, encryptedData.result)

        // Decrypt
        val decryptResult = cipherTool.execute(SymmetricCipherInput(
            text = encryptedData.result,
            passphrase = password,
            mode = SymmetricMode.DECRYPT,
            algorithm = SymmetricAlgorithm.AES_256_GCM
        ))
        assertTrue(decryptResult is ToolResult.Success)
        val decryptedData = (decryptResult as ToolResult.Success).data
        assertTrue(decryptedData.isSuccess)
        assertEquals(secretMessage, decryptedData.result)
    }

    @Test
    fun testSymmetricCipher_aes256CbcRoundTrip() = runTest {
        val message = "Testing CBC mode operation"
        val password = "SecretCbcKey@456"

        val encResult = cipherTool.execute(SymmetricCipherInput(
            text = message,
            passphrase = password,
            mode = SymmetricMode.ENCRYPT,
            algorithm = SymmetricAlgorithm.AES_256_CBC
        ))
        assertTrue(encResult is ToolResult.Success)
        val ciphertext = (encResult as ToolResult.Success).data.result

        val decResult = cipherTool.execute(SymmetricCipherInput(
            text = ciphertext,
            passphrase = password,
            mode = SymmetricMode.DECRYPT,
            algorithm = SymmetricAlgorithm.AES_256_CBC
        ))
        assertTrue(decResult is ToolResult.Success)
        assertEquals(message, (decResult as ToolResult.Success).data.result)
    }

    @Test
    fun testSymmetricCipher_wrongPasswordFails() = runTest {
        val message = "Secret Content"
        val encResult = cipherTool.execute(SymmetricCipherInput(
            text = message,
            passphrase = "CorrectPassword123",
            mode = SymmetricMode.ENCRYPT,
            algorithm = SymmetricAlgorithm.AES_256_GCM
        ))
        val ciphertext = (encResult as ToolResult.Success).data.result

        val decResult = cipherTool.execute(SymmetricCipherInput(
            text = ciphertext,
            passphrase = "WrongPassword999",
            mode = SymmetricMode.DECRYPT,
            algorithm = SymmetricAlgorithm.AES_256_GCM
        ))
        assertTrue(decResult is ToolResult.Failure)
    }

    @Test
    fun testBcryptWorkFactor_parseValidHash() = runTest {
        val hash = "\$2a\$12\$e8kZ1VvI5yJ7wE5O8h.bOed9WJ8a4lK1U2m3N4o5P6q7R8s9T0u1v"
        val result = bcryptTool.execute(BcryptInput(hash))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("Bcrypt 2a (OpenBSD / Standard)", data.algorithm)
        assertEquals(12, data.costFactor)
        assertEquals(4096L, data.totalRounds)
        assertEquals("STRONG (Recommended 2026)", data.securityLevel)
        assertTrue(data.saltPart.isNotEmpty())
        assertTrue(data.checksumPart.isNotEmpty())
    }

    @Test
    fun testBcryptWorkFactor_numericCost() = runTest {
        val result = bcryptTool.execute(BcryptInput("10"))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(10, data.costFactor)
        assertEquals(1024L, data.totalRounds)
    }

    @Test
    fun testBcryptWorkFactor_invalidHash() = runTest {
        val result = bcryptTool.execute(BcryptInput("invalid_hash_string"))
        assertTrue(result is ToolResult.Failure)
    }
}
