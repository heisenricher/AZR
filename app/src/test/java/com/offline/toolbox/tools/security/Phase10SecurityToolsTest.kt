package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10SecurityToolsTest {

    private val argon2Tool = Argon2ParameterCalculatorTool()
    private val railFenceTool = RailFenceCipherTool()

    @Test
    fun testArgon2Calculator_rfcDefaultProfile() = runTest {
        val res = argon2Tool.execute(
            Argon2CalculatorInput(
                variant = Argon2Variant.ARGON2ID,
                profile = Argon2Profile.RFC_RECOMMENDED_FIRST
            )
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(65536, data.memoryKiB)
        assertEquals(64.0, data.memoryMiB, 0.001)
        assertEquals(3, data.iterations)
        assertEquals(4, data.parallelism)
        assertTrue(data.rfcComplianceStatus.contains("RFC 9106 Compliant"))
        assertTrue(data.asicGpuResistanceScore.contains("Very High"))
    }

    @Test
    fun testArgon2Calculator_mobileProfile() = runTest {
        val res = argon2Tool.execute(
            Argon2CalculatorInput(
                variant = Argon2Variant.ARGON2ID,
                profile = Argon2Profile.MOBILE_OPTIMIZED
            )
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(19456, data.memoryKiB)
        assertEquals(19.0, data.memoryMiB, 0.001)
        assertEquals(2, data.iterations)
        assertEquals(2, data.parallelism)
        assertTrue(data.rfcComplianceStatus.contains("RFC 9106 Compliant"))
    }

    @Test
    fun testRailFenceCipher_encryptAndDecryptRoundtrip() = runTest {
        val plain = "WE ARE DISCOVERED FLEE AT ONCE"
        val encRes = railFenceTool.execute(
            RailFenceInput(text = plain, rails = 3, mode = RailFenceMode.ENCRYPT)
        )
        assertTrue(encRes is ToolResult.Success)
        val cipher = (encRes as ToolResult.Success).data.resultText
        assertTrue(cipher != plain)

        val decRes = railFenceTool.execute(
            RailFenceInput(text = cipher, rails = 3, mode = RailFenceMode.DECRYPT)
        )
        assertTrue(decRes is ToolResult.Success)
        val decrypted = (decRes as ToolResult.Success).data.resultText
        assertEquals(plain, decrypted)
    }

    @Test
    fun testRailFenceCipher_fourRailsRoundtrip() = runTest {
        val plain = "DEFENDTHEEASTWALLOFTHECASTLE"
        val encRes = railFenceTool.execute(
            RailFenceInput(text = plain, rails = 4, mode = RailFenceMode.ENCRYPT, preserveSpaces = false)
        )
        assertTrue(encRes is ToolResult.Success)
        val cipher = (encRes as ToolResult.Success).data.resultText

        val decRes = railFenceTool.execute(
            RailFenceInput(text = cipher, rails = 4, mode = RailFenceMode.DECRYPT, preserveSpaces = false)
        )
        assertTrue(decRes is ToolResult.Success)
        val decrypted = (decRes as ToolResult.Success).data.resultText
        assertEquals(plain, decrypted)
    }

    @Test
    fun testRailFenceCipher_emptyInputFails() = runTest {
        val res = railFenceTool.execute(RailFenceInput(text = "   "))
        assertTrue(res is ToolResult.Failure)
    }
}
