package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase13SecurityToolsTest {

    private val bifidTool = BifidCipherTool()
    private val scryptTool = ScryptCostEstimatorTool()

    // 1. Bifid Cipher Tests
    @Test
    fun testBifidCipher_roundTripEncryptDecrypt() = runTest {
        val originalText = "DEFEND THE EAST WALL"
        val keyword = "BIFID"
        val period = 5

        val encResult = bifidTool.execute(
            BifidCipherInput(
                operation = "ENCRYPT",
                text = originalText,
                period = period,
                keyword = keyword
            )
        )
        assertTrue(encResult is ToolResult.Success)
        val ciphertext = (encResult as ToolResult.Success).data.resultText
        assertTrue(ciphertext.isNotEmpty())

        val decResult = bifidTool.execute(
            BifidCipherInput(
                operation = "DECRYPT",
                text = ciphertext,
                period = period,
                keyword = keyword
            )
        )
        assertTrue(decResult is ToolResult.Success)
        val decrypted = (decResult as ToolResult.Success).data.resultText
        // Bifid preserves whitespace and casing in place
        assertEquals("DEFEND THE EAST WALL", decrypted)
    }

    @Test
    fun testBifidCipher_emptyTextFails() = runTest {
        val res = bifidTool.execute(BifidCipherInput(text = "   "))
        assertTrue(res is ToolResult.Failure)
    }

    // 2. scrypt Cost Estimator Tests
    @Test
    fun testScryptCost_exactCalculation() = runTest {
        val input = ScryptCostInput(
            nCostFactor = 16384L,
            rBlockSize = 8,
            pParallelism = 1,
            keyLengthBytes = 32
        )
        val res = scryptTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // 128 * 8 * 16384 = 16,777,216 bytes = 16.0 MiB
        assertEquals(16777216L, data.memorySizeBytes)
        assertEquals(16.0, data.memorySizeMib, 0.001)
        assertTrue(data.isPowerOfTwo)
        assertTrue(data.targetUseProfile.contains("Interactive Mobile"))
    }

    @Test
    fun testScryptCost_nonPowerOfTwoFails() = runTest {
        val input = ScryptCostInput(
            nCostFactor = 1000L,
            rBlockSize = 8,
            pParallelism = 1
        )
        val res = scryptTool.execute(input)
        assertTrue(res is ToolResult.Failure)
    }
}
