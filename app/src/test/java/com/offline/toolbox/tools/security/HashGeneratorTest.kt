package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HashGeneratorTest {

    private lateinit var tool: HashGeneratorTool

    @Before
    fun setUp() {
        tool = HashGeneratorTool()
    }

    @Test
    fun testSha256StandardVector() = runBlocking {
        // Known NIST SHA-256 of empty string is e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val res = tool.execute(HashInput("", HashAlgorithm.SHA_256, uppercase = false))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", data.hash)
        assertEquals(256, data.bitLength)
    }

    @Test
    fun testMd5StandardVector() = runBlocking {
        // Known MD5 of "hello" is 5d41402abc4b2a76b9719d911017c592
        val res = tool.execute(HashInput("hello", HashAlgorithm.MD5, uppercase = false))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("5d41402abc4b2a76b9719d911017c592", data.hash)
    }

    @Test
    fun testUppercaseToggle() = runBlocking {
        val res = tool.execute(HashInput("hello", HashAlgorithm.MD5, uppercase = true))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("5D41402ABC4B2A76B9719D911017C592", data.hash)
    }
}
