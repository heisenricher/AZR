package com.offline.toolbox.engine.pipeline

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.developer.PipelineChainInput
import com.offline.toolbox.tools.developer.PipelineChainingTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PipelineEngineTest {

    private val pipelineTool = PipelineChainingTool()

    @Test
    fun testPipeline_cleanAndSluggifyRecipe() = runTest {
        val result = pipelineTool.execute(
            PipelineChainInput(
                initialText = "   Hello World! Android Offline Utilities 2026   ",
                recipeId = "clean_slug"
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.isSuccess)
        assertEquals(3, data.stepCount)
        assertEquals("hello-world-android-offline-utilities-2026", data.finalOutput)
        assertEquals(3, data.fullResult.stepResults.size)
        assertTrue(data.fullResult.stepResults.all { it.isSuccess })
    }

    @Test
    fun testPipeline_hackerObfuscateRecipe() = runTest {
        val result = pipelineTool.execute(
            PipelineChainInput(
                initialText = "secret",
                recipeId = "hacker_obfuscate"
            )
        )
        assertTrue(result is ToolResult.Success)
        val data = (result as ToolResult.Success).data
        assertTrue(data.isSuccess)
        assertEquals(3, data.stepCount)
        // Check that intermediate steps are populated
        assertEquals("leetspeak", data.fullResult.stepResults[0].toolId)
        assertEquals("rot13_cipher", data.fullResult.stepResults[1].toolId)
        assertEquals("base64", data.fullResult.stepResults[2].toolId)
    }

    @Test
    fun testPipeline_customMultiStepChain() = runTest {
        // Custom chain: Text -> Lower Case -> ROT13 -> Base64
        val customSteps = listOf(
            PipelineStep("text_case_converter", "Lower Case", mapOf("mode" to "LOWERCASE")),
            PipelineStep("rot13_cipher", "ROT13"),
            PipelineStep("base64", "Base64", mapOf("mode" to "ENCODE"))
        )
        val result = PipelineEngine.execute("HELLO", customSteps)
        assertTrue(result.isSuccess)
        assertEquals(3, result.stepResults.size)
        assertEquals("hello", result.stepResults[0].outputData)
        assertEquals("uryyb", result.stepResults[1].outputData) // ROT13 of "hello" is "uryyb"
        assertEquals("dXJ5eWI=", result.stepResults[2].outputData) // Base64 of "uryyb"
        assertEquals("dXJ5eWI=", result.finalOutput)
    }

    @Test
    fun testPipeline_errorPropagation() = runTest {
        val customSteps = listOf(
            PipelineStep("unknown_tool_xyz", "Broken Step")
        )
        val result = PipelineEngine.execute("test", customSteps)
        assertTrue(!result.isSuccess)
        assertNotNull(result.stepResults.first().errorMessage)
    }
}
