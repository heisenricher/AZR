package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityToolsTest {

    // --- HashCheckerTool ---
    @Test
    fun testHashChecker_matchAndMismatch() = runBlocking {
        val tool = HashCheckerTool()
        val text = "hello world"
        // SHA-256 of "hello world"
        val expectedSha256 = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"

        val matchRes = tool.execute(HashCheckerInput(text, expectedSha256))
        assertTrue(matchRes is ToolResult.Success)
        val matchData = (matchRes as ToolResult.Success).data
        assertTrue(matchData.matches)
        assertEquals("SHA-256", matchData.algorithmUsed)

        val mismatchRes = tool.execute(HashCheckerInput(text, "0000000000000000000000000000000000000000000000000000000000000000"))
        assertTrue(mismatchRes is ToolResult.Success)
        assertFalse((mismatchRes as ToolResult.Success).data.matches)
    }

    @Test
    fun testHashChecker_emptyFails() = runBlocking {
        val tool = HashCheckerTool()
        val result = tool.execute(HashCheckerInput("content", ""))
        assertTrue(result is ToolResult.Failure)
    }

    // --- PasswordStrengthTool ---
    @Test
    fun testPasswordStrength_emptyAndWeak() = runBlocking {
        val tool = PasswordStrengthTool()
        val emptyRes = tool.execute("")
        assertTrue(emptyRes is ToolResult.Success)
        assertEquals(0, (emptyRes as ToolResult.Success).data.score)

        val weakRes = tool.execute("123456")
        assertTrue(weakRes is ToolResult.Success)
        val weakData = (weakRes as ToolResult.Success).data
        assertTrue(weakData.score <= 1)
        assertFalse(weakData.hasUpper)
        assertFalse(weakData.hasSymbol)
    }

    @Test
    fun testPasswordStrength_strong() = runBlocking {
        val tool = PasswordStrengthTool()
        val strongRes = tool.execute("K9#mQ!9xL2@vP\$8zW*")
        assertTrue(strongRes is ToolResult.Success)
        val strongData = (strongRes as ToolResult.Success).data
        assertTrue(strongData.score >= 3)
        assertTrue(strongData.hasUpper)
        assertTrue(strongData.hasLower)
        assertTrue(strongData.hasNumber)
        assertTrue(strongData.hasSymbol)
    }

    // --- UuidGeneratorTool ---
    @Test
    fun testUuidGenerator_options() = runBlocking {
        val tool = UuidGeneratorTool()
        val countRes = tool.execute(UuidConfig(count = 3))
        assertTrue(countRes is ToolResult.Success)
        val lines = (countRes as ToolResult.Success).data.lines()
        assertEquals(3, lines.size)

        val noHyphenRes = tool.execute(UuidConfig(count = 1, includeHyphens = false, uppercase = true))
        assertTrue(noHyphenRes is ToolResult.Success)
        val uuidStr = (noHyphenRes as ToolResult.Success).data
        assertEquals(32, uuidStr.length)
        assertFalse(uuidStr.contains("-"))
        assertEquals(uuidStr.uppercase(), uuidStr)
    }
}
