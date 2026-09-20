package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.SecureRandom
import kotlin.math.log2

enum class PasswordMode {
    RANDOM_CHARS,
    MEMORABLE_PASSPHRASE
}

data class PasswordConfig(
    val mode: PasswordMode = PasswordMode.RANDOM_CHARS,
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeAmbiguous: Boolean = false,
    val passphraseWords: Int = 4,
    val passphraseSeparator: String = "-",
    val capitalizePassphrase: Boolean = true,
    val appendNumberToPassphrase: Boolean = true
)

data class PasswordOutput(
    val password: String,
    val entropyBits: Double,
    val strengthRating: String,
    val characterPoolSize: Int
)

class PasswordGeneratorTool : Tool<PasswordConfig, PasswordOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "password_generator",
        name = "Password & Passphrase Generator",
        description = "Generate cryptographically secure passwords and memorable passphrases with entropy evaluation.",
        category = ToolCategory.SECURITY,
        tags = listOf("password", "generator", "passphrase", "security", "entropy", "random", "crypto", "diceware"),
        inputType = ToolDataType.NONE,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Lock"
    )

    private val secureRandom = SecureRandom()

    private val upperChars = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val upperCharsWithAmbiguous = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val lowerChars = "abcdefghijkmnopqrstuvwxyz"
    private val lowerCharsWithAmbiguous = "abcdefghijklmnopqrstuvwxyz"
    private val numberChars = "23456789"
    private val numberCharsWithAmbiguous = "0123456789"
    private val symbolChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    // Offline curated Diceware wordlist (curated clean English nouns/adjectives)
    private val wordlist = listOf(
        "anchor", "badger", "cactus", "dragon", "falcon", "galaxy", "harbor", "island",
        "jungle", "knight", "leopard", "meadow", "nebula", "orange", "planet", "quiver",
        "rocket", "shadow", "timber", "umbrella", "velvet", "whisper", "zenith", "beacon",
        "canyon", "dolphin", "echo", "feather", "glacier", "horizon", "igloo", "jaguar",
        "kettle", "lantern", "magnet", "needle", "orbit", "puzzle", "quartz", "river",
        "sapphire", "thunder", "utopia", "vortex", "walrus", "yonder", "breeze", "crystal",
        "desert", "eclipse", "forest", "geyser", "haven", "island", "journey", "karma",
        "lagoon", "mountain", "oasis", "panther", "quest", "rainbow", "summit", "tempest",
        "valley", "wind", "yellow", "aurora", "blizzard", "comet", "diamond", "ember"
    )

    override suspend fun execute(input: PasswordConfig): ToolResult<PasswordOutput> {
        val startTime = System.currentTimeMillis()

        return if (input.mode == PasswordMode.RANDOM_CHARS) {
            generateRandomPassword(input, startTime)
        } else {
            generatePassphrase(input, startTime)
        }
    }

    private fun generateRandomPassword(input: PasswordConfig, startTime: Long): ToolResult<PasswordOutput> {
        val pool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        val uppers = if (input.excludeAmbiguous) upperChars else upperCharsWithAmbiguous
        val lowers = if (input.excludeAmbiguous) lowerChars else lowerCharsWithAmbiguous
        val numbers = if (input.excludeAmbiguous) numberChars else numberCharsWithAmbiguous
        val symbols = symbolChars

        if (input.includeUppercase) {
            pool.append(uppers)
            guaranteedChars.add(uppers[secureRandom.nextInt(uppers.length)])
        }
        if (input.includeLowercase) {
            pool.append(lowers)
            guaranteedChars.add(lowers[secureRandom.nextInt(lowers.length)])
        }
        if (input.includeNumbers) {
            pool.append(numbers)
            guaranteedChars.add(numbers[secureRandom.nextInt(numbers.length)])
        }
        if (input.includeSymbols) {
            pool.append(symbols)
            guaranteedChars.add(symbols[secureRandom.nextInt(symbols.length)])
        }

        if (pool.isEmpty()) {
            return ToolResult.Failure(
                message = "No character sets selected.",
                userGuidance = "Enable at least one character type (uppercase, lowercase, numbers, or symbols)."
            )
        }

        val poolString = pool.toString()
        val poolSize = poolString.length
        val length = input.length.coerceIn(4, 128)

        val resultChars = ArrayList<Char>(length)
        resultChars.addAll(guaranteedChars.take(length))

        while (resultChars.size < length) {
            resultChars.add(poolString[secureRandom.nextInt(poolSize)])
        }

        // Cryptographically shuffle guaranteed characters
        for (i in resultChars.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = resultChars[i]
            resultChars[i] = resultChars[j]
            resultChars[j] = temp
        }

        val password = resultChars.joinToString("")
        val entropy = length * log2(poolSize.toDouble())

        return ToolResult.Success(
            data = PasswordOutput(
                password = password,
                entropyBits = ((entropy * 10).toInt()) / 10.0,
                strengthRating = getStrengthLabel(entropy),
                characterPoolSize = poolSize
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "${getStrengthLabel(entropy)} (${length} chars)"
        )
    }

    private fun generatePassphrase(input: PasswordConfig, startTime: Long): ToolResult<PasswordOutput> {
        val wordCount = input.passphraseWords.coerceIn(3, 12)
        val selectedWords = mutableListOf<String>()

        for (i in 0 until wordCount) {
            val word = wordlist[secureRandom.nextInt(wordlist.size)]
            val formatted = if (input.capitalizePassphrase) word.replaceFirstChar { it.uppercaseChar() } else word
            selectedWords.add(formatted)
        }

        if (input.appendNumberToPassphrase) {
            val number = secureRandom.nextInt(100)
            selectedWords[selectedWords.size - 1] = "${selectedWords.last()}$number"
        }

        val passphrase = selectedWords.joinToString(input.passphraseSeparator)
        // Wordlist entropy: wordCount * log2(poolSize)
        val entropy = wordCount * log2(wordlist.size.toDouble()) + if (input.appendNumberToPassphrase) log2(100.0) else 0.0

        return ToolResult.Success(
            data = PasswordOutput(
                password = passphrase,
                entropyBits = ((entropy * 10).toInt()) / 10.0,
                strengthRating = getStrengthLabel(entropy),
                characterPoolSize = wordlist.size
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "${getStrengthLabel(entropy)} (${wordCount} words)"
        )
    }

    private fun getStrengthLabel(entropyBits: Double): String = when {
        entropyBits < 30.0 -> "Very Weak"
        entropyBits < 45.0 -> "Weak"
        entropyBits < 65.0 -> "Reasonable"
        entropyBits < 90.0 -> "Strong"
        else -> "Very Strong"
    }
}
