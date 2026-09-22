package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10DevToolsTest {

    private val harTool = HarAnalyzerTool()
    private val prometheusTool = PrometheusMetricParserTool()
    private val sshKeyTool = SshKeyFingerprintTool()
    private val jwkTool = JsonWebKeyTool()

    @Test
    fun testHarAnalyzer_parsesEntriesAndStats() = runTest {
        val sampleHar = """
        {
          "log": {
            "version": "1.2",
            "entries": [
              {
                "time": 50.0,
                "request": { "method": "GET", "url": "https://api.offline.com/health" },
                "response": { "status": 200, "content": { "size": 256 } },
                "timings": { "dns": 5.0, "connect": 10.0, "send": 1.0, "wait": 30.0, "receive": 4.0 }
              },
              {
                "time": 250.0,
                "request": { "method": "POST", "url": "https://api.offline.com/login" },
                "response": { "status": 401, "content": { "size": 64 } },
                "timings": { "dns": 0.0, "connect": 0.0, "send": 2.0, "wait": 240.0, "receive": 8.0 }
              }
            ]
          }
        }
        """.trimIndent()

        val res = harTool.execute(HarAnalyzerInput(harContent = sampleHar))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(2, data.totalRequests)
        assertEquals(320L, data.totalTransferredBytes)
        assertEquals(1, data.status2xxCount)
        assertEquals(1, data.status4xxCount)
        assertEquals("POST", data.slowestRequests.first().method)
        assertEquals("https://api.offline.com/login", data.slowestRequests.first().url)
    }

    @Test
    fun testHarAnalyzer_invalidHarFails() = runTest {
        val res = harTool.execute(HarAnalyzerInput(harContent = "{ not-json }"))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testPrometheusMetricParser_validMetrics() = runTest {
        val sampleExposition = """
        # HELP http_requests_total Total HTTP requests
        # TYPE http_requests_total counter
        http_requests_total{method="GET",handler="/index"} 100
        http_requests_total{method="POST",handler="/submit"} 42
        # HELP process_resident_memory_bytes Resident memory
        # TYPE process_resident_memory_bytes gauge
        process_resident_memory_bytes 52428800
        """.trimIndent()

        val res = prometheusTool.execute(PrometheusMetricInput(expositionText = sampleExposition))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(3, data.totalSamples)
        assertEquals(2, data.uniqueMetricNames)
        assertEquals(2, data.typeBreakdown["counter"])
        assertEquals(1, data.typeBreakdown["gauge"])
        assertTrue(data.parsedSamples.any { it.name == "http_requests_total" })
        assertTrue(data.parsedSamples.any { it.name == "process_resident_memory_bytes" })
    }

    @Test
    fun testPrometheusMetricParser_emptyFails() = runTest {
        val res = prometheusTool.execute(PrometheusMetricInput(expositionText = "   "))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testSshKeyFingerprint_ed25519Key() = runTest {
        val ed25519Key = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl dev@offline.toolbox"
        val res = sshKeyTool.execute(SshKeyInput(publicKeyString = ed25519Key))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("ssh-ed25519", data.keyType)
        assertEquals(256, data.bitLength)
        assertEquals("dev@offline.toolbox", data.comment)
        assertTrue(data.sha256Fingerprint.startsWith("SHA256:"))
        assertTrue(data.md5Fingerprint.startsWith("MD5:"))
    }

    @Test
    fun testSshKeyFingerprint_invalidKeyFails() = runTest {
        val res = sshKeyTool.execute(SshKeyInput(publicKeyString = "invalid-ssh-key"))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testJsonWebKey_rsaKeyThumbprint() = runTest {
        val rsaJwk = """
        {
          "kty": "RSA",
          "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fTr4Cqhe07dXg-9_11Qz0Y31_79GzK-j80e791e84",
          "e": "AQAB",
          "kid": "key-2026-rsa"
        }
        """.trimIndent()

        val res = jwkTool.execute(JsonWebKeyInput(jwkJson = rsaJwk))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("RSA", data.keyType)
        assertEquals("key-2026-rsa", data.keyId)
        assertTrue(data.thumbprintSha256.isNotEmpty())
        assertTrue(data.thumbprintSha256Hex.length == 64)
        assertTrue(data.canonicalJson.startsWith("{\"e\":\"AQAB\",\"kty\":\"RSA\""))
    }

    @Test
    fun testJsonWebKey_ecKeyThumbprint() = runTest {
        val ecJwk = """
        {
          "kty": "EC",
          "crv": "P-256",
          "x": "f83OJ3D2xFMTbKEHfgkUXOcWgGPLOmpcvEAChN9GMjc",
          "y": "x_daQauBhQ0tZxFlGQM2x1BgGhDAOGx6ychIt69R_es"
        }
        """.trimIndent()

        val res = jwkTool.execute(JsonWebKeyInput(jwkJson = ecJwk))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("EC", data.keyType)
        assertTrue(data.thumbprintSha256.isNotEmpty())
        assertTrue(data.canonicalJson.startsWith("{\"crv\":\"P-256\",\"kty\":\"EC\""))
    }
}
