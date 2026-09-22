package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class KubeYamlInput(
    val yamlContent: String = """
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: offline-api-service
          namespace: production
          labels:
            app: offline-api
        spec:
          replicas: 3
          selector:
            matchLabels:
              app: offline-api
          template:
            metadata:
              labels:
                app: offline-api
            spec:
              securityContext:
                runAsNonRoot: true
                runAsUser: 10001
              containers:
              - name: api
                image: offline-api:1.4.2
                ports:
                - containerPort: 8080
                resources:
                  requests:
                    memory: "128Mi"
                    cpu: "250m"
                  limits:
                    memory: "512Mi"
                    cpu: "1000m"
                securityContext:
                  readOnlyRootFilesystem: true
                  allowPrivilegeEscalation: false
                livenessProbe:
                  httpGet:
                    path: /healthz
                    port: 8080
                  initialDelaySeconds: 15
                readinessProbe:
                  httpGet:
                    path: /ready
                    port: 8080
                  initialDelaySeconds: 5
    """.trimIndent()
)

data class KubeYamlOutput(
    val apiVersion: String,
    val kind: String,
    val resourceName: String,
    val namespace: String,
    val containerCount: Int,
    val hasResourceLimits: Boolean,
    val hasHealthProbes: Boolean,
    val hasSecurityContext: Boolean,
    val readinessScore: Int,
    val warnings: List<String>,
    val formattedReport: String,
    val summary: String
)

class KubeYamlResourceInspectorTool : Tool<KubeYamlInput, KubeYamlOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "kube_yaml_resource_inspector_tool",
        name = "Kubernetes Resource & Security Manifest Inspector",
        description = "Inspect Kubernetes manifests (Deployments, Pods, Services), audit CPU/memory limits, liveness/readiness probes, and pod security contexts.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("kubernetes", "k8s", "yaml", "manifest", "deployment", "pod", "security context", "devops", "cloud native"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Layers"
    )

    override suspend fun execute(input: KubeYamlInput): ToolResult<KubeYamlOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.yamlContent.trim()

        if (raw.isBlank()) {
            return ToolResult.Failure("Kubernetes YAML manifest cannot be empty.")
        }

        val lines = raw.lines()
        var apiVersion = ""
        var kind = ""
        var resourceName = ""
        var namespace = "default"
        var inMetadata = false
        val containerNames = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        var hasLimits = false
        var hasRequests = false
        var hasLiveness = false
        var hasReadiness = false
        var hasNonRoot = false
        var hasReadOnlyFs = false
        var isPrivileged = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (trimmed.startsWith("apiVersion:")) {
                apiVersion = trimmed.removePrefix("apiVersion:").trim().trim('"', '\'')
            } else if (trimmed.startsWith("kind:")) {
                kind = trimmed.removePrefix("kind:").trim().trim('"', '\'')
            } else if (trimmed.startsWith("metadata:")) {
                inMetadata = true
            } else if (inMetadata && !line.startsWith(" ") && !line.startsWith("\t")) {
                inMetadata = false
            }

            if (inMetadata) {
                if (trimmed.startsWith("name:")) {
                    resourceName = trimmed.removePrefix("name:").trim().trim('"', '\'')
                } else if (trimmed.startsWith("namespace:")) {
                    namespace = trimmed.removePrefix("namespace:").trim().trim('"', '\'')
                }
            }

            if (trimmed.startsWith("- name:") && (lines.any { it.contains("containers:") })) {
                val cName = trimmed.removePrefix("- name:").trim()
                if (cName.isNotEmpty()) containerNames.add(cName)
            }

            if (trimmed.startsWith("image:")) {
                val img = trimmed.removePrefix("image:").trim().trim('"', '\'')
                if (img.endsWith(":latest") || !img.contains(":")) {
                    warnings.add("Container image '$img' uses ':latest' or unpinned tag.")
                }
            }

            if (trimmed.contains("limits:")) hasLimits = true
            if (trimmed.contains("requests:")) hasRequests = true
            if (trimmed.contains("livenessProbe:")) hasLiveness = true
            if (trimmed.contains("readinessProbe:")) hasReadiness = true
            if (trimmed.contains("runAsNonRoot: true")) hasNonRoot = true
            if (trimmed.contains("readOnlyRootFilesystem: true")) hasReadOnlyFs = true
            if (trimmed.contains("privileged: true")) isPrivileged = true
        }

        if (kind.isEmpty()) {
            return ToolResult.Failure("Invalid manifest: Missing required 'kind:' field.")
        }
        if (resourceName.isEmpty()) {
            resourceName = "unnamed-${kind.lowercase(Locale.US)}"
        }

        val isWorkload = kind in listOf("Deployment", "Pod", "StatefulSet", "DaemonSet", "Job", "CronJob")

        if (isWorkload) {
            if (!hasLimits) warnings.add("Reliability: Container CPU/Memory limits not defined. Risk of node out-of-memory starvation.")
            if (!hasRequests) warnings.add("Scheduling: Container CPU/Memory requests not defined.")
            if (!hasLiveness) warnings.add("Resilience: Missing 'livenessProbe' for container health monitoring.")
            if (!hasReadiness) warnings.add("Traffic Routing: Missing 'readinessProbe' before routing live service traffic.")
            if (!hasNonRoot) warnings.add("Security: 'runAsNonRoot: true' not enabled in pod/container securityContext.")
            if (!hasReadOnlyFs) warnings.add("Security: 'readOnlyRootFilesystem: true' not configured.")
            if (isPrivileged) warnings.add("CRITICAL RISK: Container has 'privileged: true' (root-equivalent node access).")
        }

        var score = 100 - (warnings.size * 12)
        if (isPrivileged) score -= 40
        score = score.coerceIn(10, 100)

        val report = buildString {
            appendLine("KUBERNETES MANIFEST INSPECTION REPORT")
            appendLine("--------------------------------------------------")
            appendLine("API Version:         $apiVersion")
            appendLine("Resource Kind:       $kind")
            appendLine("Resource Name:       $resourceName")
            appendLine("Namespace:           $namespace")
            if (isWorkload) {
                appendLine("Containers:          ${if (containerNames.isNotEmpty()) containerNames.joinToString(", ") else "1 (default)"}")
                appendLine("Resource Limits:     ${if (hasLimits) "CONFIGURED" else "MISSING"}")
                appendLine("Health Probes:       ${if (hasLiveness && hasReadiness) "Full (Liveness + Readiness)" else if (hasLiveness || hasReadiness) "Partial" else "NONE"}")
                appendLine("Security Context:    ${if (hasNonRoot && hasReadOnlyFs) "Hardened (Non-root, RO-FS)" else "Permissive"}")
            }
            appendLine("Production Score:    $score / 100")
            appendLine("--------------------------------------------------")
            if (warnings.isNotEmpty()) {
                appendLine("SECURITY & BEST PRACTICE AUDIT FINDINGS:")
                warnings.forEach { appendLine("  ⚠ $it") }
            } else {
                appendLine("AUDIT VERDICT: Production-ready manifest adhering to security and resilience best practices.")
            }
        }

        return ToolResult.Success(
            data = KubeYamlOutput(
                apiVersion = apiVersion,
                kind = kind,
                resourceName = resourceName,
                namespace = namespace,
                containerCount = containerNames.size.coerceAtLeast(1),
                hasResourceLimits = hasLimits,
                hasHealthProbes = hasLiveness || hasReadiness,
                hasSecurityContext = hasNonRoot || hasReadOnlyFs,
                readinessScore = score,
                warnings = warnings,
                formattedReport = report,
                summary = "K8s $kind '$resourceName' ($score/100, ${warnings.size} warnings)"
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Inspected K8s $kind '$resourceName'"
        )
    }
}
