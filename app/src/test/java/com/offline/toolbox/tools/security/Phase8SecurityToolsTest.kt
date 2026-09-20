package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase8SecurityToolsTest {

    private val shamirTool = ShamirSecretSharingTool()
    private val stegoTool = SteganographyTextTool()

    @Test
    fun testShamir_splitAndReconstructThreshold() = runTest {
        val secret = "Ultra-Secret-Vault-Key-99"
        val splitRes = shamirTool.execute(ShamirInput(
            mode = ShamirMode.SPLIT,
            secretText = secret,
            thresholdK = 3,
            totalSharesN = 5
        ))
        assertTrue(splitRes is ToolResult.Success)
        val splitData = (splitRes as ToolResult.Success).data
        assertEquals(5, splitData.generatedShares.size)

        // Select any 3 shares (e.g. shares 1, 3, 5)
        val selectedShares = listOf(
            splitData.generatedShares[0],
            splitData.generatedShares[2],
            splitData.generatedShares[4]
        )

        val combineRes = shamirTool.execute(ShamirInput(
            mode = ShamirMode.COMBINE,
            sharesToCombine = selectedShares
        ))
        assertTrue(combineRes is ToolResult.Success)
        val combineData = (combineRes as ToolResult.Success).data
        assertEquals(secret, combineData.reconstructedSecret)
    }

    @Test
    fun testShamir_insufficientSharesFails() = runTest {
        val res = shamirTool.execute(ShamirInput(
            mode = ShamirMode.COMBINE,
            sharesToCombine = listOf("1-ab12") // only 1 share
        ))
        assertTrue(res is ToolResult.Failure)
    }

    @Test
    fun testSteganography_encodeAndDecode() = runTest {
        val cover = "The product launch is on schedule for Q3."
        val secret = "Codename Valkyrie"

        val encodeRes = stegoTool.execute(StegoInput(
            mode = StegoMode.ENCODE,
            coverText = cover,
            hiddenMessage = secret
        ))
        assertTrue(encodeRes is ToolResult.Success)
        val stegoText = (encodeRes as ToolResult.Success).data.resultingText

        val decodeRes = stegoTool.execute(StegoInput(
            mode = StegoMode.DECODE,
            stegoPayloadToDecode = stegoText
        ))
        assertTrue(decodeRes is ToolResult.Success)
        val decodedSecret = (decodeRes as ToolResult.Success).data.resultingText
        assertEquals(secret, decodedSecret)
    }

    @Test
    fun testSteganography_noPayloadFails() = runTest {
        val res = stegoTool.execute(StegoInput(
            mode = StegoMode.DECODE,
            stegoPayloadToDecode = "Normal text without any hidden zero width characters."
        ))
        assertTrue(res is ToolResult.Failure)
    }
}
