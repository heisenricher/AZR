package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.MessageDigest
import java.util.Locale

data class Bip39WordDetail(
    val position: Int,
    val word: String,
    val index11Bit: Int,
    val binary11Bit: String
)

data class Bip39ValidatorInput(
    // Standard test vector from BIP-39 specification
    val mnemonicPhrase: String = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
)

data class Bip39ValidatorOutput(
    val isValidChecksum: Boolean,
    val wordCount: Int,
    val entropyBitsCount: Int,
    val checksumBitsCount: Int,
    val entropyHex: String,
    val calculatedChecksumBits: String,
    val expectedChecksumBits: String,
    val wordDetails: List<Bip39WordDetail>,
    val formattedReport: String,
    val summary: String
)

class Bip39MnemonicEntropyTool : Tool<Bip39ValidatorInput, Bip39ValidatorOutput> {
    companion object {
        // Standard BIP-39 2048 English wordlist embedded in memory
        val BIP39_WORDS: List<String> by lazy {
            Bip39Wordlist.WORDS
        }
        val WORD_INDEX_MAP: Map<String, Int> by lazy {
            BIP39_WORDS.mapIndexed { idx, w -> w to idx }.toMap()
        }
    }

    override val metadata: ToolMetadata = ToolMetadata(
        id = "bip39_mnemonic_entropy_tool",
        name = "BIP-39 Mnemonic Seed & Entropy Validator",
        description = "Inspect and validate BIP-0039 cryptocurrency seed phrases, extract 11-bit word indexes, and verify SHA-256 entropy checksums.",
        category = ToolCategory.SECURITY,
        tags = listOf("bip39", "mnemonic", "seed phrase", "crypto", "bitcoin", "ethereum", "entropy", "checksum", "wallet"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Key"
    )

    override suspend fun execute(input: Bip39ValidatorInput): ToolResult<Bip39ValidatorOutput> {
        val startTime = System.currentTimeMillis()
        val words = input.mnemonicPhrase.trim().lowercase(Locale.US).split(Regex("\\s+")).filter { it.isNotBlank() }

        if (words.isEmpty()) {
            return ToolResult.Failure("Mnemonic seed phrase cannot be empty.")
        }

        val wordCount = words.size
        if (wordCount !in listOf(12, 15, 18, 21, 24)) {
            return ToolResult.Failure("Invalid BIP-39 word count ($wordCount). Standard mnemonics must contain exactly 12, 15, 18, 21, or 24 words.")
        }

        val totalBits = wordCount * 11
        val checksumBits = totalBits / 33 // 12 -> 4, 15 -> 5, 18 -> 6, 21 -> 7, 24 -> 8
        val entropyBits = totalBits - checksumBits

        val wordDetails = mutableListOf<Bip39WordDetail>()
        val bitString = StringBuilder()

        for ((idx, w) in words.withIndex()) {
            val wordIndex = WORD_INDEX_MAP[w]
                ?: return ToolResult.Failure("Word #${idx + 1} ('$w') is not in the official BIP-39 English dictionary.")

            val bin11 = String.format(Locale.US, "%11s", Integer.toBinaryString(wordIndex)).replace(' ', '0')
            bitString.append(bin11)
            wordDetails.add(Bip39WordDetail(idx + 1, w, wordIndex, bin11))
        }

        val fullBits = bitString.toString()
        val entropyBitString = fullBits.substring(0, entropyBits)
        val embeddedChecksumBitString = fullBits.substring(entropyBits)

        // Convert entropy bit string to ByteArray
        val entropyBytes = ByteArray(entropyBits / 8)
        for (i in entropyBytes.indices) {
            val byteStr = entropyBitString.substring(i * 8, (i + 1) * 8)
            entropyBytes[i] = byteStr.toInt(2).toByte()
        }

        // Calculate SHA-256 of entropy bytes
        val hash = MessageDigest.getInstance("SHA-256").digest(entropyBytes)
        val firstByteBin = String.format(Locale.US, "%8s", Integer.toBinaryString(hash[0].toInt() and 0xFF)).replace(' ', '0')
        val calculatedChecksumBitString = firstByteBin.substring(0, checksumBits)

        val isValidChecksum = embeddedChecksumBitString == calculatedChecksumBitString
        val entropyHex = entropyBytes.joinToString("") { String.format(Locale.US, "%02x", it) }

        val report = buildString {
            appendLine("BIP-0039 MNEMONIC SEED & CHECKSUM VERIFICATION")
            appendLine("--------------------------------------------------")
            appendLine("Validation Status:    ${if (isValidChecksum) "VALID BIP-39 SEED" else "INVALID CHECKSUM"}")
            appendLine("Word Count:           $wordCount words")
            appendLine("Entropy Size:         $entropyBits bits (${entropyBits / 8} bytes)")
            appendLine("Checksum Size:        $checksumBits bits")
            appendLine("--------------------------------------------------")
            appendLine("Raw Entropy (Hex):")
            appendLine(entropyHex)
            appendLine("Embedded Checksum:    $embeddedChecksumBitString")
            appendLine("Calculated Checksum:  $calculatedChecksumBitString")
            appendLine("Checksum Match:       ${if (isValidChecksum) "YES (Exact SHA-256 match)" else "NO (Mnemonic is corrupt or mistyped)"}")
            appendLine("--------------------------------------------------")
            appendLine("WORD INDEX BREAKDOWN:")
            wordDetails.forEach { wd ->
                appendLine(" #${String.format(Locale.US, "%02d", wd.position)}: ${String.format(Locale.US, "%-10s", wd.word)} Index: ${String.format(Locale.US, "%4d", wd.index11Bit)} (${wd.binary11Bit})")
            }
        }

        val output = Bip39ValidatorOutput(
            isValidChecksum = isValidChecksum,
            wordCount = wordCount,
            entropyBitsCount = entropyBits,
            checksumBitsCount = checksumBits,
            entropyHex = entropyHex,
            calculatedChecksumBits = calculatedChecksumBitString,
            expectedChecksumBits = embeddedChecksumBitString,
            wordDetails = wordDetails,
            formattedReport = report,
            summary = "BIP-39 ($wordCount words): ${if (isValidChecksum) "Valid Checksum" else "Invalid Checksum"}"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Validated BIP-39 mnemonic ($wordCount words)"
        )
    }
}
