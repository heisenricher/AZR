package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12DevToolsTest {

    private val systemdTool = SystemdServiceUnitValidatorTool()
    private val dockerfileTool = DockerfileLinterTool()
    private val htaccessTool = ApacheHtaccessValidatorTool()
    private val kubeTool = KubeYamlResourceInspectorTool()

    @Test
    fun testSystemdValidator_validUnit() = runTest {
        val unit = """
            [Unit]
            Description=Offline Background Worker
            After=network.target

            [Service]
            Type=simple
            ExecStart=/usr/bin/worker
            Restart=always
            User=nobody
            NoNewPrivileges=true
            ProtectSystem=strict

            [Install]
            WantedBy=multi-user.target
        """.trimIndent()

        val res = systemdTool.execute(SystemdUnitInput(unit))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.isValid)
        assertTrue(data.securityScore > 50)
        assertFalse(data.runsAsRoot)
        assertEquals("simple", data.serviceType)
    }

    @Test
    fun testSystemdValidator_missingExecStart() = runTest {
        val badUnit = """
            [Unit]
            Description=Incomplete Unit

            [Service]
            Restart=always
        """.trimIndent()

        val res = systemdTool.execute(SystemdUnitInput(badUnit))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertFalse(data.isValid)
        assertTrue(data.issues.any { it.contains("ExecStart") })
    }

    @Test
    fun testDockerfileLinter_bestPractices() = runTest {
        val dockerfile = """
            FROM alpine:3.20
            RUN apk add --no-cache curl
            WORKDIR /app
            COPY . .
            USER guest
            EXPOSE 8080
            HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/ || exit 1
            CMD ["./start.sh"]
        """.trimIndent()

        val res = dockerfileTool.execute(DockerfileLinterInput(dockerfile))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.hasNonRootUser)
        assertTrue(data.hasHealthcheck)
        assertTrue(data.qualityScore >= 70)
    }

    @Test
    fun testDockerfileLinter_flagsLatestAndRoot() = runTest {
        val insecureDockerfile = """
            FROM ubuntu:latest
            RUN apt-get update && apt-get install -y nginx
            CMD ["nginx", "-g", "daemon off;"]
        """.trimIndent()

        val res = dockerfileTool.execute(DockerfileLinterInput(insecureDockerfile))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertFalse(data.hasNonRootUser)
        assertTrue(data.warnings.any { it.contains("root") || it.contains("USER") })
        assertTrue(data.warnings.any { it.contains("latest") || it.contains("tag") })
    }

    @Test
    fun testApacheHtaccessValidator_rulesAndSecurity() = runTest {
        val htaccess = """
            Options -Indexes
            ServerSignature Off

            RewriteEngine On
            RewriteBase /
            RewriteCond %{HTTPS} off
            RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
        """.trimIndent()

        val res = htaccessTool.execute(ApacheHtaccessInput(htaccess))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.isValid)
        assertTrue(data.hasIndexesDisabled)
        assertTrue(data.hasHttpsRedirect)
        assertEquals(1, data.rewriteRuleCount)
    }

    @Test
    fun testKubeYamlInspector_checksResourcesAndSecurity() = runTest {
        val manifest = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: secure-deployment
            spec:
              template:
                spec:
                  securityContext:
                    runAsNonRoot: true
                    readOnlyRootFilesystem: true
                  containers:
                  - name: web
                    image: nginx:1.27-alpine
                    resources:
                      requests:
                        cpu: 100m
                        memory: 128Mi
                      limits:
                        cpu: 500m
                        memory: 512Mi
                    livenessProbe:
                      httpGet:
                        path: /health
                        port: 80
        """.trimIndent()

        val res = kubeTool.execute(KubeYamlInput(manifest))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.hasResourceLimits)
        assertTrue(data.hasHealthProbes)
        assertTrue(data.hasSecurityContext)
        assertTrue(data.readinessScore >= 70)
    }
}
