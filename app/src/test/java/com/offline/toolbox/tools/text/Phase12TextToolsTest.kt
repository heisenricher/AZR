package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12TextToolsTest {

    private val stemmerTool = PorterStemmerTool()
    private val caesarBreakerTool = CaesarBruteForceBreakerTool()

    @Test
    fun testPorterStemmer_reducesInflections() = runTest {
        val input = StemmerInput(
            text = "The engineer is connecting multiple distributed connections and debugging systems."
        )
        val res = stemmerTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        val stemmed = data.stemmedText.lowercase()
        assertTrue(stemmed.contains("connect"))
        assertTrue(stemmed.contains("system"))
        assertTrue(data.uniqueStems <= data.uniqueWords)
    }

    @Test
    fun testPorterStemmer_classicWordTests() {
        assertEquals("caress", stemmerTool.stemWord("caresses"))
        assertEquals("poni", stemmerTool.stemWord("ponies"))
        assertEquals("ti", stemmerTool.stemWord("ties"))
        assertEquals("caress", stemmerTool.stemWord("caress"))
        assertEquals("cat", stemmerTool.stemWord("cats"))
        assertEquals("feed", stemmerTool.stemWord("feed"))
        assertEquals("agre", stemmerTool.stemWord("agreed"))
        assertEquals("happi", stemmerTool.stemWord("happy"))
    }

    @Test
    fun testCaesarBreaker_recoversRot3() = runTest {
        // "Hello World! This is an encrypted secret." rotated by 3:
        // H->K, e->h, l->o, l->o, o->r
        val cipher = "Khoor Zruog! Wklv lv dq hqfubswhg vhfuhw."
        val res = caesarBreakerTool.execute(CaesarBreakerInput(ciphertext = cipher))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(3, data.bestShift)
        assertTrue(data.bestDecryptedText.contains("Hello World"))
        assertEquals(26, data.totalCandidatesTested)
    }

    @Test
    fun testCaesarBreaker_recoversRot13() = runTest {
        // Plain: "Security and cryptography are essential for modern software systems"
        // Rot13: "Frphevgl naq pelcgbtencul ner rffragvny sbe zbqrea fbsgjner flfgrzf"
        val cipher = "Frphevgl naq pelcgbtencul ner rffragvny sbe zbqrea fbsgjner flfgrzf"
        val res = caesarBreakerTool.execute(CaesarBreakerInput(ciphertext = cipher))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(13, data.bestShift)
        assertTrue(data.bestDecryptedText.lowercase().contains("security"))
        assertTrue(data.bestDecryptedText.lowercase().contains("cryptography"))
    }
}
