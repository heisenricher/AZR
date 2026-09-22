package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12SecurityToolsTest {

    private val polybiusTool = PolybiusSquareCipherTool()
    private val affineTool = AffineCipherTool()

    @Test
    fun testPolybiusSquare_encodeAndDecode() = runTest {
        val plain = "HELLO"
        val encodeRes = polybiusTool.execute(
            PolybiusInput(operation = "ENCODE", text = plain, keyword = "")
        )
        assertTrue(encodeRes is ToolResult.Success)
        val encoded = (encodeRes as ToolResult.Success).data.resultText
        assertTrue(encoded.isNotEmpty())

        val decodeRes = polybiusTool.execute(
            PolybiusInput(operation = "DECODE", text = encoded, keyword = "")
        )
        assertTrue(decodeRes is ToolResult.Success)
        val decoded = (decodeRes as ToolResult.Success).data.resultText
        assertEquals("HELLO", decoded)
    }

    @Test
    fun testPolybiusSquare_withKeyword() = runTest {
        val res = polybiusTool.execute(
            PolybiusInput(operation = "ENCODE", text = "TEST", keyword = "SECRET")
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("SECRET", data.keywordUsed)
        assertEquals(listOf('S', 'E', 'C', 'R', 'T'), data.gridMatrix[0])
    }

    @Test
    fun testAffineCipher_encryptAndDecrypt() = runTest {
        val plain = "AFFINE CIPHER"
        val a = 5
        val b = 8

        val encRes = affineTool.execute(
            AffineCipherInput(operation = "ENCRYPT", text = plain, a = a, b = b)
        )
        assertTrue(encRes is ToolResult.Success)
        val cipher = (encRes as ToolResult.Success).data.resultText

        val decRes = affineTool.execute(
            AffineCipherInput(operation = "DECRYPT", text = cipher, a = a, b = b)
        )
        assertTrue(decRes is ToolResult.Success)
        val recovered = (decRes as ToolResult.Success).data.resultText
        assertEquals(plain, recovered)
    }

    @Test
    fun testAffineCipher_rejectsNonCoprimeKey() = runTest {
        val res = affineTool.execute(
            AffineCipherInput(operation = "ENCRYPT", text = "HELLO", a = 4, b = 7)
        )
        assertTrue(res is ToolResult.Failure)
        assertTrue((res as ToolResult.Failure).message.contains("coprime"))
    }

    @Test
    fun testAffineCipher_bruteForceSolver() = runTest {
        // Encrypt with a=7, b=3
        val plain = "CRYPTOGRAPHY IS THE FOUNDATION OF PRIVACY"
        val encRes = affineTool.execute(
            AffineCipherInput(operation = "ENCRYPT", text = plain, a = 7, b = 3)
        )
        val cipher = (encRes as ToolResult.Success).data.resultText

        val bruteRes = affineTool.execute(
            AffineCipherInput(operation = "BRUTE_FORCE", text = cipher)
        )
        assertTrue(bruteRes is ToolResult.Success)
        val data = (bruteRes as ToolResult.Success).data
        assertEquals(plain, data.resultText)
        assertEquals(7, data.a)
        assertEquals(3, data.b)
    }
}
