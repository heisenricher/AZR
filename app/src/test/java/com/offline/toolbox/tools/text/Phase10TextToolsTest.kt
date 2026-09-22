package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10TextToolsTest {

    private val phoneticTool = SoundexMetaphoneTool()
    private val ngramTool = TextStatisticsNgramTool()

    @Test
    fun testSoundexMetaphone_smithVsSmythe() = runTest {
        val res = phoneticTool.execute(
            SoundexMetaphoneInput(primaryWord = "Smith", comparisonWord = "Smythe")
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("S530", data.soundex1)
        assertEquals("S530", data.soundex2)
        assertTrue(data.soundexMatch)
        assertEquals(data.metaphone1, data.metaphone2)
        assertTrue(data.metaphoneMatch)
        assertTrue(data.overallPhoneticSimilarity.contains("EXACT PHONETIC MATCH"))
    }

    @Test
    fun testSoundexMetaphone_robertVsRupert() = runTest {
        val res = phoneticTool.execute(
            SoundexMetaphoneInput(primaryWord = "Robert", comparisonWord = "Rupert")
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("R163", data.soundex1)
        assertEquals("R163", data.soundex2)
        assertTrue(data.soundexMatch)
    }

    @Test
    fun testNgramStatistics_frequencyAndTtr() = runTest {
        val prose = "the cat in the hat saw another cat in the box"
        val res = ngramTool.execute(
            TextStatisticsNgramInput(text = prose, caseSensitive = false, filterStopwords = false)
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(11, data.totalWordCount)
        assertEquals(7, data.uniqueWordCount) // the, cat, in, hat, saw, another, box
        assertEquals(7.0 / 11.0, data.typeTokenRatio, 0.01)
        // "the" appears 3 times, "cat" appears 2 times, "in" appears 2 times
        assertEquals("the", data.topUnigrams.first().ngram)
        assertEquals(3, data.topUnigrams.first().count)
        // bigram "in the" appears 2 times
        assertTrue(data.topBigrams.any { it.ngram == "in the" && it.count == 2 })
    }

    @Test
    fun testNgramStatistics_emptyFails() = runTest {
        val res = ngramTool.execute(TextStatisticsNgramInput(text = "    "))
        assertTrue(res is ToolResult.Failure)
    }
}
