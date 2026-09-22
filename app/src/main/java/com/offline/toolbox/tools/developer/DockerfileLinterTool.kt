package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class DockerInstruction(val opcode: String, val argument: String, val lineNumber: Int)

data class DockerfileLinterInput(
    val dockerfileContent: String = """
        FROM alpine:3.20 AS base
        WORKDIR /app
        RUN apk add --no-cache ca-certificates tzdata
        
        FROM base AS build
        COPY package.json package-lock.json ./
        RUN npm ci --only=production
        COPY . .
        
        FROM node:20-alpine
        WORKDIR /app
        USER node
        COPY --from=build --chown=node:node /app /app
        EXPOSE 3000
        HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:3000/health || exit 1
        CMD ["node", "server.js"]
    """.trimIndent()
)

data class DockerfileLinterOutput(
    val stageCount: Int,
    val totalInstructions: Int,
    val baseImages: List<String>,
    val exposedPorts: List<String>,
    val hasNonRootUser: Boolean,
    val hasHealthcheck: Boolean,
    val qualityScore: Int,
    val warnings: List<String>,
    val formattedReport: String,
    val summary: String
)

class DockerfileLinterTool : Tool<DockerfileLinterInput, DockerfileLinterOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "dockerfile_linter_tool",
        name = "Dockerfile Linter & Container Security Auditor",
        description = "Analyze Dockerfiles for multi-stage builds, root user exposure, image pinning, package manager cleanup, and layer caching optimization.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("docker", "dockerfile", "container", "linter", "devops", "security", "best practices", "alpine", "layers"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Box"
    )

    override suspend fun execute(input: DockerfileLinterInput): ToolResult<DockerfileLinterOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.dockerfileContent.trim()

        if (raw.isBlank()) {
            return ToolResult.Failure("Dockerfile content cannot be empty.")
        }

        val lines = raw.lines()
        val instructions = mutableListOf<DockerInstruction>()
        val warnings = mutableListOf<String>()

        var currentLineBuilder = StringBuilder()
        var startLine = 1

        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = line.trim()

            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (currentLineBuilder.isEmpty()) startLine = lineNum

            if (trimmed.endsWith("\\")) {
                currentLineBuilder.append(trimmed.removeSuffix("\\").trim()).append(" ")
            } else {
                currentLineBuilder.append(trimmed)
                val fullInst = currentLineBuilder.toString().trim()
                currentLineBuilder = StringBuilder()

                val spaceIdx = fullInst.indexOfFirst { it.isWhitespace() }
                if (spaceIdx != -1) {
                    val opcode = fullInst.substring(0, spaceIdx).uppercase(Locale.US)
                    val arg = fullInst.substring(spaceIdx + 1).trim()
                    instructions.add(DockerInstruction(opcode, arg, startLine))
                } else {
                    instructions.add(DockerInstruction(fullInst.uppercase(Locale.US), "", startLine))
                }
            }
        }

        if (instructions.isEmpty() || instructions.none { it.opcode == "FROM" }) {
            return ToolResult.Failure("Invalid Dockerfile: Missing required 'FROM' instruction.")
        }

        val baseImages = mutableListOf<String>()
        val exposedPorts = mutableListOf<String>()
        var cmdCount = 0
        var entrypointCount = 0
        var hasNonRootUser = false
        var hasHealthcheck = false

        for (inst in instructions) {
            when (inst.opcode) {
                "FROM" -> {
                    val img = inst.argument.split(Regex("\\s+")).first()
                    baseImages.add(img)
                    if (!img.contains(":") || img.endsWith(":latest")) {
                        warnings.add("Line ${inst.lineNumber}: Base image '$img' uses ':latest' or unpinned tag. Pin exact SHA digest or version tag.")
                    }
                }
                "EXPOSE" -> exposedPorts.addAll(inst.argument.split(Regex("\\s+")))
                "USER" -> {
                    if (!inst.argument.equals("root", ignoreCase = true) && inst.argument != "0") {
                        hasNonRootUser = true
                    } else {
                        warnings.add("Line ${inst.lineNumber}: Explicit USER root instruction detected.")
                    }
                }
                "HEALTHCHECK" -> hasHealthcheck = true
                "CMD" -> cmdCount++
                "ENTRYPOINT" -> entrypointCount++
                "ADD" -> {
                    if (!inst.argument.startsWith("http://") && !inst.argument.startsWith("https://") && !inst.argument.endsWith(".tar.gz") && !inst.argument.endsWith(".tar")) {
                        warnings.add("Line ${inst.lineNumber}: Prefer 'COPY' over 'ADD' for local files unless auto-extracting tar archives.")
                    }
                }
                "RUN" -> {
                    val arg = inst.argument
                    if (arg.contains("apt-get update") && !arg.contains("apt-get install")) {
                        warnings.add("Line ${inst.lineNumber}: 'apt-get update' should be chained in the same RUN line with 'apt-get install' to prevent layer cache staleness.")
                    }
                    if (arg.contains("apt-get install") && !arg.contains("rm -rf /var/lib/apt/lists")) {
                        warnings.add("Line ${inst.lineNumber}: apt-get cache not cleared. Add '&& rm -rf /var/lib/apt/lists/*' to reduce image size.")
                    }
                    if (arg.contains("sudo")) {
                        warnings.add("Line ${inst.lineNumber}: Avoid using 'sudo' in container build scripts.")
                    }
                }
            }
        }

        if (cmdCount > 1) {
            warnings.add("Multiple 'CMD' instructions found ($cmdCount). Only the last CMD takes effect.")
        }
        if (entrypointCount > 1) {
            warnings.add("Multiple 'ENTRYPOINT' instructions found ($entrypointCount). Only the last ENTRYPOINT takes effect.")
        }
        if (!hasNonRootUser) {
            warnings.add("Security: No non-root USER defined. Application will execute with root privileges.")
        }
        if (!hasHealthcheck) {
            warnings.add("Reliability: No HEALTHCHECK instruction defined.")
        }

        val stageCount = baseImages.size
        var qualityScore = 100 - (warnings.size * 12)
        if (stageCount > 1) qualityScore += 10 // bonus for multi-stage
        qualityScore = qualityScore.coerceIn(10, 100)

        val report = buildString {
            appendLine("DOCKERFILE LINTER & SECURITY AUDIT REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Build Stages:        $stageCount (${if (stageCount > 1) "Multi-Stage Build" else "Single Stage"})")
            appendLine("Total Instructions:  ${instructions.size}")
            appendLine("Base Images:         ${baseImages.joinToString(", ")}")
            appendLine("Exposed Ports:       ${if (exposedPorts.isNotEmpty()) exposedPorts.joinToString(", ") else "None"}")
            appendLine("Non-Root User:       ${if (hasNonRootUser) "YES (Hardened)" else "NO (Root default)"}")
            appendLine("Container Healthcheck: ${if (hasHealthcheck) "Configured" else "None"}")
            appendLine("Container Hygiene Score: $qualityScore / 100")
            appendLine("--------------------------------------------------")
            if (warnings.isNotEmpty()) {
                appendLine("LINT FINDINGS & SECURITY WARNINGS:")
                warnings.forEach { appendLine("  [!] $it") }
            } else {
                appendLine("LINT FINDINGS: No security or layer optimization warnings found! Excellent Dockerfile.")
            }
        }

        return ToolResult.Success(
            data = DockerfileLinterOutput(
                stageCount = stageCount,
                totalInstructions = instructions.size,
                baseImages = baseImages,
                exposedPorts = exposedPorts,
                hasNonRootUser = hasNonRootUser,
                hasHealthcheck = hasHealthcheck,
                qualityScore = qualityScore,
                warnings = warnings,
                formattedReport = report,
                summary = "Dockerfile: $qualityScore/100 ($stageCount stages, ${warnings.size} warnings)"
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Linted Dockerfile ($qualityScore/100)"
        )
    }
}
