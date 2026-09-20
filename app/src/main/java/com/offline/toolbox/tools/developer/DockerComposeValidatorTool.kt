package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

data class DockerServiceInfo(
    val name: String,
    val image: String?,
    val hostPorts: List<Int>,
    val containerPorts: List<Int>,
    val dependsOn: List<String>,
    val environmentKeys: List<String>
)

data class DockerComposeInput(
    val yamlContent: String = """
        version: '3.8'
        services:
          web:
            image: nginx:alpine
            ports:
              - "80:80"
              - "443:443"
            depends_on:
              - api
          api:
            image: myapp:latest
            ports:
              - "8080:8080"
            depends_on:
              - db
          db:
            image: postgres:15-alpine
            environment:
              POSTGRES_DB: app
              POSTGRES_PASSWORD: secret
            volumes:
              - db_data:/var/lib/postgresql/data
        volumes:
          db_data:
    """.trimIndent()
)

data class DockerComposeOutput(
    val serviceCount: Int,
    val services: List<DockerServiceInfo>,
    val portConflicts: List<String>,
    val missingDependencies: List<String>,
    val warnings: List<String>,
    val formattedReport: String,
    val summary: String
)

class DockerComposeValidatorTool : Tool<DockerComposeInput, DockerComposeOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "docker_compose_validator_tool",
        name = "Docker Compose YAML & Port Conflict Validator",
        description = "Validate docker-compose specifications offline, detect overlapping host port bindings, and check service dependencies.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("docker", "compose", "yaml", "container", "ports", "service", "devops", "conflict", "validator"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Layers"
    )

    override suspend fun execute(input: DockerComposeInput): ToolResult<DockerComposeOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.yamlContent.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Docker compose YAML cannot be empty.")
        }

        val lines = raw.lines()
        val services = mutableListOf<DockerServiceInfo>()
        val warnings = mutableListOf<String>()

        var inServices = false
        var currentServiceName: String? = null
        var currentImage: String? = null
        val currentHostPorts = mutableListOf<Int>()
        val currentContainerPorts = mutableListOf<Int>()
        val currentDependsOn = mutableListOf<String>()
        val currentEnvs = mutableListOf<String>()

        var subSection: String? = null

        fun flushCurrentService() {
            val sName = currentServiceName ?: return
            services.add(
                DockerServiceInfo(
                    name = sName,
                    image = currentImage,
                    hostPorts = currentHostPorts.toList(),
                    containerPorts = currentContainerPorts.toList(),
                    dependsOn = currentDependsOn.toList(),
                    environmentKeys = currentEnvs.toList()
                )
            )
            currentServiceName = null
            currentImage = null
            currentHostPorts.clear()
            currentContainerPorts.clear()
            currentDependsOn.clear()
            currentEnvs.clear()
            subSection = null
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val indent = line.takeWhile { it == ' ' }.length

            if (indent == 0) {
                flushCurrentService()
                inServices = trimmed.startsWith("services:")
                continue
            }

            if (!inServices) continue

            // Service name level (indent around 2 or 4)
            if (indent in 2..4 && trimmed.endsWith(":") && !trimmed.startsWith("-")) {
                val candidateName = trimmed.removeSuffix(":").trim()
                if (candidateName !in listOf("ports", "environment", "volumes", "depends_on", "build", "networks")) {
                    flushCurrentService()
                    currentServiceName = candidateName
                    continue
                }
            }

            if (currentServiceName == null) continue

            if (trimmed.startsWith("image:")) {
                currentImage = trimmed.removePrefix("image:").trim().trim('"', '\'')
                subSection = null
            } else if (trimmed.startsWith("ports:")) {
                subSection = "ports"
            } else if (trimmed.startsWith("depends_on:")) {
                subSection = "depends_on"
            } else if (trimmed.startsWith("environment:")) {
                subSection = "environment"
            } else if (trimmed.startsWith("volumes:")) {
                subSection = "volumes"
            } else if (trimmed.startsWith("-") && subSection != null) {
                val item = trimmed.removePrefix("-").trim().trim('"', '\'')
                when (subSection) {
                    "ports" -> {
                        val parts = item.split(":")
                        if (parts.size >= 2) {
                            val host = parts[0].toIntOrNull()
                            val cont = parts[1].toIntOrNull()
                            if (host != null) currentHostPorts.add(host)
                            if (cont != null) currentContainerPorts.add(cont)
                        }
                    }
                    "depends_on" -> {
                        currentDependsOn.add(item)
                    }
                    "environment" -> {
                        val key = item.substringBefore("=").trim()
                        currentEnvs.add(key)
                    }
                }
            } else if (subSection == "environment" && trimmed.contains(":")) {
                val key = trimmed.substringBefore(":").trim()
                currentEnvs.add(key)
            }
        }
        flushCurrentService()

        if (services.isEmpty()) {
            return ToolResult.Failure("No services detected in docker-compose YAML. Check indentation under 'services:'.")
        }

        // Port conflict detection
        val portMap = mutableMapOf<Int, MutableList<String>>()
        for (s in services) {
            for (p in s.hostPorts) {
                portMap.getOrPut(p) { mutableListOf() }.add(s.name)
            }
        }

        val portConflicts = mutableListOf<String>()
        for ((port, svcList) in portMap) {
            if (svcList.size > 1) {
                portConflicts.add("Host port $port is bound by multiple services: ${svcList.joinToString(", ")}")
            }
        }

        // Missing dependencies
        val existingServiceNames = services.map { it.name }.toSet()
        val missingDeps = mutableListOf<String>()
        for (s in services) {
            for (dep in s.dependsOn) {
                if (dep !in existingServiceNames) {
                    missingDeps.add("Service '${s.name}' depends on unknown service '$dep'")
                }
            }
        }

        val report = buildString {
            appendLine("DOCKER COMPOSE CONFIGURATION AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Services Detected:   ${services.size}")
            appendLine("Port Conflicts:      ${portConflicts.size}")
            appendLine("Missing Depends:     ${missingDeps.size}")
            appendLine()
            if (portConflicts.isNotEmpty()) {
                appendLine("PORT CONFLICTS:")
                portConflicts.forEach { appendLine("• CRITICAL: $it") }
                appendLine()
            }
            if (missingDeps.isNotEmpty()) {
                appendLine("DEPENDENCY ISSUES:")
                missingDeps.forEach { appendLine("• WARNING: $it") }
                appendLine()
            }
            appendLine("SERVICES OVERVIEW:")
            services.forEach { s ->
                val portsStr = if (s.hostPorts.isNotEmpty()) s.hostPorts.joinToString(", ") { "$it" } else "None"
                val depsStr = if (s.dependsOn.isNotEmpty()) s.dependsOn.joinToString(", ") else "None"
                appendLine("• ${s.name}:")
                appendLine("    Image:        ${s.image ?: "Custom / Build"}")
                appendLine("    Host Ports:   $portsStr")
                appendLine("    Depends On:   $depsStr")
            }
        }

        val summary = "${services.size} service(s) analyzed | ${portConflicts.size} conflict(s)"

        return ToolResult.Success(
            data = DockerComposeOutput(
                serviceCount = services.size,
                services = services,
                portConflicts = portConflicts,
                missingDependencies = missingDeps,
                warnings = warnings,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
