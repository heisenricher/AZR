package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase11SecurityToolsTest {

    private val vernamTool = VernamOneTimePadTool()
    private val bip39Tool = Bip39MnemonicEntropyTool()

    @Test
    fun testVernamOneTimePad_encryptAndDecryptRoundtrip() = runTest {
        val original = "TOP SECRET TRANSMISSION 42"
        val encRes = vernamTool.execute(VernamOtpInput(content = original, mode = OtpOperationMode.ENCRYPT_GENERATE_KEY))
        assertTrue(encRes is ToolResult.Success)
        val encData = (encRes as ToolResult.Success).data
        val padHex = encData.padHex
        val cipherHex = encData.ciphertextHex

        // Decrypt using generated key
        val decRes = vernamTool.execute(VernamOtpInput(content = cipherHex, keyOrPadHex = padHex, mode = OtpOperationMode.DECRYPT_WITH_KEY))
        assertTrue(decRes is ToolResult.Success)
        val decData = (decRes as ToolResult.Success).data
        assertEquals(original, decData.resultTextOrHex)
    }

    @Test
    fun testVernamOneTimePad_shortKeyFails() = runTest {
        val res = vernamTool.execute(VernamOtpInput(content = "Hello World", keyOrPadHex = "00", mode = OtpOperationMode.ENCRYPT_WITH_KEY))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testBip39Validator_officialBip39VectorValid() = runTest {
        val vector = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val res = bip39Tool.execute(Bip39ValidatorInput(vector))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.isValidChecksum)
        assertEquals(12, data.wordCount)
        assertEquals(128, data.entropyBitsCount)
        assertEquals(4, data.checksumBitsCount)
        assertEquals("00000000000000000000000000000000", data.entropyHex)
    }

    @Test
    fun testBip39Validator_invalidChecksumDetected() = runTest {
        val badVector = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon"
        val res = bip39Tool.execute(Bip39ValidatorInput(badVector))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertFalse(data.isValidChecksum)
    }

    @Test
    fun testBip39Validator_invalidWordCount() = runTest {
        val res = bip39Tool.execute(Bip39ValidatorInput("abandon abandon abandon"))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testBip39Validator_unknownWord() = runTest {
        val res = bip39Tool.execute(Bip39ValidatorInput("abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon xyzxyzxyz"))
        assertTrue(res is ToolResult.Failure)
    }
}
