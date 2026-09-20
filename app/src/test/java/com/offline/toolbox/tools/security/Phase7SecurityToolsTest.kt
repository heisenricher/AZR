package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7SecurityToolsTest {

    private val totpTool = TotpGeneratorTool()
    private val dicewareTool = PassphraseDicewareTool()

    @Test
    fun testTotp_rfc6238Standard() = runTest {
        // Known test secret "JBSWY3DPEHPK3PXP" at a fixed epoch
        val result = totpTool.execute(TotpInput(
            base32Secret = "JBSWY3DPEHPK3PXP",
            timeStepSeconds = 30,
            digits = 6,
            customEpochSeconds = 1700000000L
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(6, data.digits)
        assertEquals(6, data.currentCode.length)
        assertTrue(data.currentCode.all { it.isDigit() })
        assertEquals(6, data.nextCode.length)
        assertEquals(30, data.remainingSeconds + (1700000000L % 30).toInt())
    }

    @Test
    fun testTotp_invalidBase32Fails() = runTest {
        val result = totpTool.execute(TotpInput(
            base32Secret = "INVALID_CHARS_89!!"
        ))
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun testDiceware_generatePassphrase() = runTest {
        val result = dicewareTool.execute(DicewareInput(
            wordCount = 5,
            delimiter = "-",
            capitalizeWords = true,
            appendNumber = true
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals(5, data.words.size)
        assertTrue(data.entropyBits >= 40.0)
        assertTrue(data.passphrase.contains("-"))
        assertTrue(data.passphrase.any { it.isDigit() })
    }

    @Test
    fun testDiceware_entropyIncreasesWithWords() = runTest {
        val r4 = (dicewareTool.execute(DicewareInput(wordCount = 4)) as ToolResult.Success).data
        val r7 = (dicewareTool.execute(DicewareInput(wordCount = 7)) as ToolResult.Success).data
        assertTrue(r7.entropyBits > r4.entropyBits)
    }
}
