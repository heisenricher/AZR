package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7TextToolsTest {

    private val caseInspectorTool = TextCaseInspectorTool()
    private val natoTool = NatoPhoneticTool()

    @Test
    fun testCaseInspector_camelCase() = runTest {
        val result = caseInspectorTool.execute(TextCaseInspectorInput(
            identifier = "offlineUtilityToolbox"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.detectedCaseStyle.contains("camelCase"))
        assertEquals(3, data.tokenCount)
        assertEquals(listOf("offline", "Utility", "Toolbox"), data.extractedTokens)
        assertEquals("offline_utility_toolbox", data.suggestedSnakeCase)
        assertEquals("OfflineUtilityToolbox", data.suggestedPascalCase)
        assertEquals("OFFLINE_UTILITY_TOOLBOX", data.suggestedConstantCase)
    }

    @Test
    fun testCaseInspector_screamingSnakeCase() = runTest {
        val result = caseInspectorTool.execute(TextCaseInspectorInput(
            identifier = "MAX_BUFFER_SIZE"
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.detectedCaseStyle.contains("CONSTANT_CASE"))
        assertEquals(3, data.tokenCount)
        assertEquals("maxBufferSize", data.suggestedCamelCase)
    }

    @Test
    fun testNato_textToPhonetic() = runTest {
        val result = natoTool.execute(NatoInput(
            text = "SOS 123",
            mode = NatoMode.TEXT_TO_PHONETIC
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.result.contains("Sierra Oscar Sierra"))
        assertTrue(data.result.contains("One Two Three"))
    }

    @Test
    fun testNato_phoneticToText() = runTest {
        val result = natoTool.execute(NatoInput(
            text = "Alfa Zulu Romeo",
            mode = NatoMode.PHONETIC_TO_TEXT
        ))
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertEquals("AZR", data.result)
    }

    @Test
    fun testNato_emptyInputFails() = runTest {
        val result = natoTool.execute(NatoInput(text = "   "))
        assertTrue(result is ToolResult.Failure)
    }
}
