package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.engine.pipeline.PipelineEngine
import com.offline.toolbox.engine.pipeline.PipelineExecutionResult
import com.offline.toolbox.engine.pipeline.PipelineStep

data class PipelineChainInput(
    val initialText: String,
    val recipeId: String? = null,
    val customSteps: List<PipelineStep> = emptyList()
)

data class PipelineChainOutput(
    val initialInput: String,
    val finalOutput: String,
    val stepCount: Int,
    val isSuccess: Boolean,
    val executionTimeMs: Long,
    val stepSummary: List<String>,
    val fullResult: PipelineExecutionResult
)

class PipelineChainingTool : Tool<PipelineChainInput, PipelineChainOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "pipeline_chaining",
        name = "Multi-Tool Pipeline Chainer",
        description = "Chains multiple offline text, crypto, and developer transformations into an atomic sequential pipeline with an audit trail.",
        category = ToolCategory.DEVELOPER,
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        )
    )

    override suspend fun execute(input: PipelineChainInput): ToolResult<PipelineChainOutput> {
        val steps = if (!input.recipeId.isNullOrBlank()) {
            val recipe = PipelineEngine.builtInRecipes.find { it.id == input.recipeId }
                ?: return ToolResult.Failure("Unknown pipeline recipe ID: ${input.recipeId}")
            recipe.steps
        } else if (input.customSteps.isNotEmpty()) {
            input.customSteps
        } else {
            // Default to Sanitize & Sluggify
            PipelineEngine.builtInRecipes.first().steps
        }

        val execResult = PipelineEngine.execute(input.initialText, steps)
        val summaries = execResult.stepResults.map { step ->
            val status = if (step.isSuccess) "✓" else "✗"
            "$status Step ${step.stepIndex} [${step.toolName}]: ${step.outputData.take(40)}"
        }

        return if (execResult.isSuccess) {
            ToolResult.Success(
                PipelineChainOutput(
                    initialInput = execResult.initialInput,
                    finalOutput = execResult.finalOutput,
                    stepCount = execResult.stepResults.size,
                    isSuccess = true,
                    executionTimeMs = execResult.totalDurationMs,
                    stepSummary = summaries,
                    fullResult = execResult
                )
            )
        } else {
            val firstErr = execResult.stepResults.find { !it.isSuccess }?.errorMessage ?: "Pipeline execution failed"
            ToolResult.Failure("Pipeline error: $firstErr")
        }
    }
}
