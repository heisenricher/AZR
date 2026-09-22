package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase13TextToolsTest {

    private val jaroWinklerTool = JaroWinklerDistanceTool()
    private val atbashTool = AtbashCipherTool()

    // 1. Jaro-Winkler Distance Tests
    @Test
    fun testJaroWinkler_marthaVsMarhta() = runTest {
        val input = JaroWinklerInput(
            string1 = "martha",
            string2 = "marhta",
            caseSensitive = false,
            prefixScalingFactor = 0.1
        )
        val res = jaroWinklerTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // Standard Winkler similarity for martha/marhta is ~0.961
        assertEquals(0.961, data.jaroWinklerSimilarity, 0.01)
        assertEquals(3, data.commonPrefixLength) // "mar"
        assertTrue(data.similarityTier.contains("Very High"))
    }

    @Test
    fun testJaroWinkler_identicalAndDisjointStrings() = runTest {
        val exactRes = jaroWinklerTool.execute(JaroWinklerInput(string1 = "kotlin", string2 = "kotlin"))
        assertTrue(exactRes is ToolResult.Success)
        assertEquals(1.0, (exactRes as ToolResult.Success).data.jaroWinklerSimilarity, 0.001)

        val disjointRes = jaroWinklerTool.execute(JaroWinklerInput(string1 = "abc", string2 = "xyz"))
        assertTrue(disjointRes is ToolResult.Success)
        assertEquals(0.0, (disjointRes as ToolResult.Success).data.jaroWinklerSimilarity, 0.001)
    }

    // 2. Atbash Cipher Tests
    @Test
    fun testAtbashCipher_reciprocalEncryption() = runTest {
        val plaintext = "Hello World 123"
        // Encrypt with reverseDigits = true
        val encRes = atbashTool.execute(AtbashInput(text = plaintext, reverseDigits = true))
        assertTrue(encRes is ToolResult.Success)
        val ciphertext = (encRes as ToolResult.Success).data.transformedText

        // H -> S, e -> v, l -> o, o -> l; W -> D, o -> l, r -> i, l -> o, d -> w; 1 -> 8, 2 -> 7, 3 -> 6
        assertEquals("Svool Dliow 876", ciphertext)

        // Decrypt with same function (reciprocal)
        val decRes = atbashTool.execute(AtbashInput(text = ciphertext, reverseDigits = true))
        assertTrue(decRes is ToolResult.Success)
        assertEquals(plaintext, (decRes as ToolResult.Success).data.transformedText)
    }

    @Test
    fun testAtbashCipher_emptyInputFails() = runTest {
        val res = atbashTool.execute(AtbashInput(text = "   "))
        assertTrue(res is ToolResult.Failure)
    }
}
