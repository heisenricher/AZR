package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PasswordGeneratorTest {

    private lateinit var tool: PasswordGeneratorTool

    @Before
    fun setUp() {
        tool = PasswordGeneratorTool()
    }

    @Test
    fun testRandomPasswordLengthAndEntropy() = runBlocking {
        val config = PasswordConfig(
            mode = PasswordMode.RANDOM_CHARS,
            length = 24,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true
        )
        val res = tool.execute(config)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(24, data.password.length)
        assertTrue(data.entropyBits > 100.0) // 24 chars of ~90 pool is ~150 bits
        assertEquals("Very Strong", data.strengthRating)
    }

    @Test
    fun testPassphraseWordCount() = runBlocking {
        val config = PasswordConfig(
            mode = PasswordMode.MEMORABLE_PASSPHRASE,
            passphraseWords = 5,
            passphraseSeparator = "-",
            appendNumberToPassphrase = false
        )
        val res = tool.execute(config)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        val words = data.password.split("-")
        assertEquals(5, words.size)
    }
}
