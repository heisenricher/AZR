package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase11DevToolsTest {

    private val nginxTool = NginxConfigValidatorTool()
    private val pragmaTool = SqlitePragmaInspectorTool()
    private val totpUriTool = HmacTotpUriBuilderTool()
    private val umaskTool = UnixUmaskCalculatorTool()

    @Test
    fun testNginxConfigValidator_validConfig() = runTest {
        val config = """
            server {
                listen 80;
                server_name example.com;
                return 301 https://${'$'}host${'$'}request_uri;
            }
            server {
                listen 443 ssl;
                server_name example.com;
                ssl_certificate /etc/ssl/cert.pem;
                ssl_certificate_key /etc/ssl/key.pem;
                location / {
                    proxy_pass http://127.0.0.1:8080;
                }
            }
        """.trimIndent()

        val res = nginxTool.execute(NginxValidatorInput(config))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.isValid)
        assertEquals(2, data.serverBlocksCount)
        assertEquals(1, data.locationBlocksCount)
        assertTrue(data.detectedListenPorts.any { it.contains("443") })
    }

    @Test
    fun testNginxConfigValidator_unmatchedBrace() = runTest {
        val badConfig = "server { listen 80; server_name localhost;"
        val res = nginxTool.execute(NginxValidatorInput(badConfig))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertFalse(data.isValid)
        assertTrue(data.syntaxErrors.any { it.contains("Unbalanced") || it.contains("brace") })
    }

    @Test
    fun testSqlitePragmaInspector_generatesRecommendations() = runTest {
        val input = SqlitePragmaInput(
            profile = SqliteTuningProfile.ANDROID_ROOM_RECOMMENDED
        )
        val res = pragmaTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.directives.isNotEmpty())
        assertTrue(data.generatedSqlScript.contains("PRAGMA journal_mode = WAL;"))
        assertTrue(data.generatedSqlScript.contains("PRAGMA synchronous = NORMAL;"))
        assertTrue(data.generatedSqlScript.contains("PRAGMA foreign_keys = ON;"))
    }

    @Test
    fun testHmacTotpUriBuilder_validKeyUri() = runTest {
        val input = HmacTotpUriInput(
            mode = TotpUriOperationMode.BUILD_KEY_URI,
            accountName = "alice@corp.com",
            issuer = "SecureGate",
            base32Secret = "JBSWY3DPEHPK3PXP",
            algorithm = "SHA1",
            digits = 6,
            periodSeconds = 30
        )
        val res = totpUriTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.isValidBase32)
        assertTrue(data.fullUri.startsWith("otpauth://totp/SecureGate:alice%40corp.com"))
        assertTrue(data.fullUri.contains("secret=JBSWY3DPEHPK3PXP"))
        assertEquals(80, data.secretBitLength) // 10 bytes * 8 = 80 bits
    }

    @Test
    fun testHmacTotpUriBuilder_invalidBase32Secret() = runTest {
        val input = HmacTotpUriInput(base32Secret = "INVALID_SECRET_189!")
        val res = totpUriTool.execute(input)
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testUnixUmaskCalculator_022Standard() = runTest {
        val res = umaskTool.execute(UnixUmaskInput("022"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("0644", data.fileOctal)
        assertEquals("rw-r--r--", data.fileSymbolic)
        assertEquals("0755", data.directoryOctal)
        assertEquals("rwxr-xr-x", data.directorySymbolic)
    }

    @Test
    fun testUnixUmaskCalculator_077Secure() = runTest {
        val res = umaskTool.execute(UnixUmaskInput("077"))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals("0600", data.fileOctal)
        assertEquals("rw-------", data.fileSymbolic)
        assertEquals("0700", data.directoryOctal)
        assertEquals("rwx------", data.directorySymbolic)
        assertTrue(data.securityLevel.contains("Secure") || data.securityAuditNotes.any { it.contains("Secure") } || data.securityLevel.contains("Privacy"))
    }

    @Test
    fun testUnixUmaskCalculator_invalidOctal() = runTest {
        val res = umaskTool.execute(UnixUmaskInput("999"))
        assertTrue(res is ToolResult.Failure)
    }
}
