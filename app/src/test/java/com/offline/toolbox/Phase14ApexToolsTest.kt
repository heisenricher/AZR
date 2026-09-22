package com.offline.toolbox

import com.offline.toolbox.core.model.ToolResult
import com.offline.toolbox.tools.data.GreatCircleDistanceTool
import com.offline.toolbox.tools.data.GreatCircleInput
import com.offline.toolbox.tools.developer.GitCommitLinterInput
import com.offline.toolbox.tools.developer.GitCommitMessageLinterTool
import com.offline.toolbox.tools.math.ResistorCircuitInput
import com.offline.toolbox.tools.math.ResistorEquivalentCircuitTool
import com.offline.toolbox.tools.media.SvgPathDataInspectorTool
import com.offline.toolbox.tools.media.SvgPathInput
import com.offline.toolbox.tools.security.PlayfairCipherInput
import com.offline.toolbox.tools.security.PlayfairCipherTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase14ApexToolsTest {

    private val gitCommitTool = GitCommitMessageLinterTool()
    private val playfairTool = PlayfairCipherTool()
    private val greatCircleTool = GreatCircleDistanceTool()
    private val resistorTool = ResistorEquivalentCircuitTool()
    private val svgTool = SvgPathDataInspectorTool()

    // 1. Conventional Commits 1.0.0 Linter Tests (Tool 196)
    @Test
    fun testGitCommitLinter_validConventionalCommit() = runTest {
        val msg = """
            feat(auth)!: add biometric fingerprint authentication

            Implement Android BiometricPrompt API for fast local biometric unlock.
            Replaces legacy pin-only authentication pipeline.

            BREAKING CHANGE: Minimum supported biometric hardware level is BIOMETRIC_STRONG.
            Fixes: #1042
        """.trimIndent()

        val res = gitCommitTool.execute(GitCommitLinterInput(msg))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.isValid)
        assertEquals("feat", data.commitType)
        assertEquals("auth", data.scope)
        assertTrue(data.isBreakingChange)
        assertEquals("add biometric fingerprint authentication", data.subject)
        assertTrue(data.footers.any { it.contains("BREAKING CHANGE") })
        assertTrue(data.footers.any { it.contains("Fixes: #1042") })
        assertTrue(data.qualityScore >= 80)
    }

    @Test
    fun testGitCommitLinter_flagsPastTenseAndMissingBlankLine() = runTest {
        val badMsg = """
            feat: added biometric login
            Direct body without blank line
        """.trimIndent()

        val res = gitCommitTool.execute(GitCommitLinterInput(badMsg, enforceImperativeMood = true))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertFalse(data.isValid) // missing blank line is an error
        assertTrue(data.issues.any { it.rule == "MISSING_BLANK_LINE" })
        assertTrue(data.issues.any { it.rule == "IMPERATIVE_MOOD" })
    }

    @Test
    fun testGitCommitLinter_emptyInputFails() = runTest {
        val res = gitCommitTool.execute(GitCommitLinterInput("   "))
        assertTrue(res is ToolResult.Failure)
    }

    // 2. Wheatstone-Playfair Digraph Substitution Cipher Tests (Tool 197)
    @Test
    fun testPlayfairCipher_roundTripEncryptDecrypt() = runTest {
        val originalText = "INSTRUMENTS"
        val keyword = "MONARCHY"

        val encRes = playfairTool.execute(
            PlayfairCipherInput(operation = "ENCRYPT", text = originalText, keyword = keyword)
        )
        assertTrue(encRes is ToolResult.Success)
        val ciphertext = (encRes as ToolResult.Success).data.resultText
        assertTrue(ciphertext.isNotEmpty())
        assertEquals(12, ciphertext.length) // INSTRUMENTS is 11 chars -> padded to 12 chars

        val decRes = playfairTool.execute(
            PlayfairCipherInput(operation = "DECRYPT", text = ciphertext, keyword = keyword)
        )
        assertTrue(decRes is ToolResult.Success)
        val decrypted = (decRes as ToolResult.Success).data.resultText
        // Padded with X at end
        assertEquals("INSTRUMENTSX", decrypted)
    }

    @Test
    fun testPlayfairCipher_doubleLetterFillerInsertion() = runTest {
        // "BALLOON" has consecutive 'L' and 'O'
        val res = playfairTool.execute(
            PlayfairCipherInput(operation = "ENCRYPT", text = "BALLOON", keyword = "PLAYFAIR")
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // 'LL' splits into 'LX', 'L...'
        assertTrue(data.digraphPairs.any { it.original == "LX" })
    }

    @Test
    fun testPlayfairCipher_emptyInputFails() = runTest {
        val res = playfairTool.execute(PlayfairCipherInput(text = "   "))
        assertTrue(res is ToolResult.Failure)
    }

    // 3. Great-Circle Geodesic Distance & Bearing Navigator Tests (Tool 198)
    @Test
    fun testGreatCircle_sfoToNycVincentyAndHaversine() = runTest {
        val input = GreatCircleInput(
            lat1 = 37.774929,  // SFO
            lon1 = -122.419416,
            lat2 = 40.712776,  // NYC
            lon2 = -74.005974,
            calculationModel = "WGS84_ELLIPSOID"
        )
        val res = greatCircleTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // SFO to NYC geodesic distance is approximately 4,135 km (2,570 miles)
        assertEquals(4135.0, data.distanceKm, 25.0)
        assertEquals(2570.0, data.distanceMiles, 20.0)
        assertTrue(data.distanceNauticalMiles > 2000.0)

        // Bearing from SFO to NYC is northeast (ENE / NE around 66°)
        assertEquals(66.0, data.initialBearingDeg, 5.0)
        assertTrue(data.initialCompassHeading.contains("E") || data.initialCompassHeading.contains("NE"))

        // Midpoint should be around Nebraska/Kansas (~41°N, ~99°W)
        assertTrue(data.midpoint.latitude in 39.0..43.0)
        assertTrue(data.midpoint.longitude in -103.0..-96.0)
    }

    @Test
    fun testGreatCircle_invalidCoordinatesFail() = runTest {
        val res = greatCircleTool.execute(GreatCircleInput(lat1 = 95.0))
        assertTrue(res is ToolResult.Failure)
    }

    // 4. Resistor Circuit Network & Voltage Divider Tests (Tool 199)
    @Test
    fun testResistorCircuit_seriesAndParallelCalculations() = runTest {
        // Series: 100 + 220 + 470 = 790 ohms
        val seriesRes = resistorTool.execute(
            ResistorCircuitInput(topology = "SERIES", resistorValues = "100, 220, 470", supplyVoltageVolts = 12.0)
        )
        assertTrue(seriesRes is ToolResult.Success)
        val seriesData = (seriesRes as ToolResult.Success).data
        assertEquals(790.0, seriesData.equivalentResistanceOhms, 0.001)
        assertEquals(3, seriesData.branches.size)
        // I = 12 / 790 = 0.015189 A = 15.19 mA
        assertEquals(0.01519, seriesData.totalCurrentAmperes, 0.0005)

        // Parallel: 100 || 100 = 50 ohms
        val parallelRes = resistorTool.execute(
            ResistorCircuitInput(topology = "PARALLEL", resistorValues = "100, 100", supplyVoltageVolts = 10.0)
        )
        assertTrue(parallelRes is ToolResult.Success)
        val parallelData = (parallelRes as ToolResult.Success).data
        assertEquals(50.0, parallelData.equivalentResistanceOhms, 0.001)
        assertEquals(0.2, parallelData.totalCurrentAmperes, 0.001) // 10V / 50 ohms = 0.2 A
    }

    @Test
    fun testResistorCircuit_voltageDividerWithLoad() = runTest {
        // Divider: R1 = 10k, R2 = 10k, Vin = 12V
        val res = resistorTool.execute(
            ResistorCircuitInput(
                topology = "VOLTAGE_DIVIDER",
                resistorValues = "10k, 10k",
                supplyVoltageVolts = 12.0,
                loadResistanceOhms = 10000.0 // 10k load
            )
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        // Open circuit Vout = 12 * (10k / 20k) = 6.0 V
        assertEquals(6.0, data.dividerVoutNoLoad ?: 0.0, 0.01)
        // Loaded Vout: R2 || RL = 5k. Vout = 12 * (5k / 15k) = 4.0 V
        assertEquals(4.0, data.dividerVoutWithLoad ?: 0.0, 0.01)
    }

    @Test
    fun testResistorCircuit_emptyValuesFail() = runTest {
        val res = resistorTool.execute(ResistorCircuitInput(resistorValues = "   "))
        assertTrue(res is ToolResult.Failure)
    }

    // 5. SVG Path Data Inspector & Android Vector Converter Tests (Tool 200)
    @Test
    fun testSvgPathInspector_bezierWaveAndBoundingBox() = runTest {
        val path = "M 10 80 Q 52.5 10, 95 80 T 180 80 Z"
        val res = svgTool.execute(SvgPathInput(pathData = path, viewportWidth = 200.0, viewportHeight = 200.0))
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(4, data.totalCommands) // M, Q, T, Z
        assertTrue(data.isClosed)
        assertEquals(10.0, data.boundingBox.minX, 0.1)
        assertEquals(180.0, data.boundingBox.maxX, 0.1)
        assertTrue(data.boundingBox.width > 150.0)

        // Vector XML verification
        assertTrue(data.androidVectorDrawableXml.contains("<vector xmlns:android="))
        assertTrue(data.androidVectorDrawableXml.contains("android:viewportWidth=\"200.0\""))
        assertTrue(data.androidVectorDrawableXml.contains("android:pathData=\"$path\""))
    }

    @Test
    fun testSvgPathInspector_emptyPathFails() = runTest {
        val res = svgTool.execute(SvgPathInput(pathData = "   "))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testResistorCircuit_zeroSupplyVoltageAndDroopSafeguard() = runTest {
        val res = resistorTool.execute(
            ResistorCircuitInput(
                topology = "VOLTAGE_DIVIDER",
                resistorValues = "10k, 10k",
                supplyVoltageVolts = 0.0,
                loadResistanceOhms = 10000.0
            )
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(0.0, data.dividerVoutNoLoad ?: -1.0, 0.001)
        assertEquals(0.0, data.dividerVoutWithLoad ?: -1.0, 0.001)
        assertFalse("Report should not contain NaN", data.formattedReport.contains("NaN"))
        assertFalse("Report should not contain Infinity", data.formattedReport.contains("Infinity"))
    }

    @Test
    fun testGreatCircle_coincidentPoints() = runTest {
        val res = greatCircleTool.execute(
            GreatCircleInput(lat1 = 45.0, lon1 = 9.0, lat2 = 45.0, lon2 = 9.0)
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(0.0, data.distanceKm, 0.001)
        assertEquals(0.0, data.distanceMiles, 0.001)
    }

    @Test
    fun testPlayfairCipher_keywordWithJ() = runTest {
        val res = playfairTool.execute(
            PlayfairCipherInput(operation = "ENCRYPT", text = "JUSTICE", keyword = "JUPITER")
        )
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertTrue(data.resultText.isNotEmpty())
        assertFalse("Result should not contain J", data.resultText.contains("J"))
    }
}
