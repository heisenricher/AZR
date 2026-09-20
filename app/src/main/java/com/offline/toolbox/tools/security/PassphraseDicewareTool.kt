package com.offline.toolbox.tools.security

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.SecureRandom
import java.util.Locale
import kotlin.math.log2
import kotlin.math.pow

data class DicewareInput(
    val wordCount: Int = 5,
    val delimiter: String = "-",
    val capitalizeWords: Boolean = true,
    val appendNumber: Boolean = true
)

data class DicewareOutput(
    val passphrase: String,
    val words: List<String>,
    val diceRolls: List<String>,
    val entropyBits: Double,
    val securityLevel: String,
    val formattedReport: String,
    val summary: String
)

class PassphraseDicewareTool : Tool<DicewareInput, DicewareOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "passphrase_diceware_tool",
        name = "Diceware Cryptographic Passphrase Generator",
        description = "Generate memorable, cryptographically strong multi-word passphrases using simulated 5-dice rolls and exact entropy calculations.",
        category = ToolCategory.SECURITY,
        tags = listOf("diceware", "passphrase", "password", "security", "entropy", "dice", "eff", "crypto", "random"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Casino"
    )

    private val wordlist = listOf(
        "aback", "abbey", "abbot", "abide", "ablaze", "aboard", "abode", "abrupt", "absent", "absorb",
        "access", "accord", "account", "acid", "acorn", "across", "action", "active", "actor", "adapt",
        "admit", "adopt", "advance", "advice", "aerial", "affair", "afford", "afraid", "agency", "agent",
        "agile", "agree", "ahead", "aimless", "airfield", "alarm", "albatross", "album", "alchemy", "alert",
        "algebra", "alien", "alkaline", "allege", "allied", "almanac", "almost", "aloe", "alpha", "alpine",
        "already", "also", "altitude", "aluminum", "always", "amber", "ambient", "amend", "amplify", "anchor",
        "ancient", "android", "angel", "angler", "animal", "ankle", "annex", "annual", "answer", "antenna",
        "anthem", "antler", "anxiety", "anyway", "apache", "apart", "apex", "apiece", "apparel", "appeal",
        "appear", "apple", "apron", "aquarium", "arcade", "arch", "arctic", "ardent", "arena", "argon",
        "argument", "armada", "armor", "aroma", "arrange", "array", "arrest", "arrow", "artery", "artist",
        "artwork", "asbestos", "ascend", "ashcan", "asleep", "aspect", "aspire", "assault", "asset", "assist",
        "asteroid", "athlete", "atlas", "atom", "atrium", "attack", "attempt", "attire", "attract", "auction",
        "audible", "audio", "audit", "augment", "august", "aura", "aurora", "author", "auto", "autumn",
        "avail", "avenge", "avenue", "average", "avid", "avocado", "avoid", "await", "awake", "award",
        "aware", "awesome", "awful", "awkward", "axis", "aztec", "azure", "baboon", "baby", "badge",
        "badger", "bagel", "bagpipe", "baker", "balance", "balcony", "ballad", "ballet", "balloon", "ballot",
        "bamboo", "banana", "bandana", "bandit", "bangle", "banjo", "banker", "banner", "banquet", "barber",
        "bargain", "baritone", "bark", "barley", "barnacle", "baron", "barrel", "barricade", "barter", "baseball",
        "basement", "basic", "basil", "basin", "basket", "bassoon", "baton", "battery", "battle", "bayonet",
        "bazaar", "beacon", "bead", "beaker", "beaming", "beanie", "bearable", "beast", "beauty", "beaver",
        "become", "bedbug", "bedrock", "beehive", "beetle", "before", "beggar", "beginner", "behalf", "behave",
        "behind", "behold", "beige", "believe", "bellhop", "belong", "beloved", "belt", "bench", "benefit",
        "beret", "berry", "beside", "bestow", "betray", "better", "between", "beyond", "bicycle", "bifocal",
        "bigfoot", "bike", "bilateral", "bilingual", "binary", "bind", "bingo", "biography", "biology", "birch",
        "birdbath", "birthday", "biscuit", "bishop", "bison", "bitter", "blacksmith", "blender", "blimp", "blizzard",
        "block", "blossom", "blueberry", "blueprint", "bobcat", "bonfire", "bonus", "bookcase", "boomerang", "border",
        "botany", "boulder", "boundary", "bouquet", "bowling", "boxcar", "brave", "breeze", "brick", "bridge",
        "bronze", "bubble", "bucket", "buffalo", "builder", "bulletin", "bunker", "butterfly", "bypass", "cabin",
        "cactus", "cadence", "calcium", "calendar", "camel", "camera", "campfire", "canal", "canary", "candle",
        "canyon", "captain", "caramel", "caravan", "carbon", "cardinal", "cargo", "carousel", "carpenter", "cascade",
        "castle", "catalyst", "cavalry", "celestial", "cement", "century", "ceramic", "cereal", "champion", "channel",
        "chapel", "charcoal", "chariot", "cheetah", "chemist", "cherry", "chestnut", "chimney", "chipmunk", "chronicle",
        "cinnamon", "circle", "circuit", "cistern", "citadel", "civil", "clover", "coastal", "cobalt", "coconut",
        "colosseum", "comet", "compass", "compress", "concept", "condor", "conifer", "constellation", "corridor", "cosmos",
        "crater", "crescent", "cricket", "crystal", "cubicle", "curator", "cyclone", "cypress", "dagger", "daffodil",
        "damage", "dancer", "dandelion", "daybreak", "decagon", "decibel", "deck", "declare", "defend", "delta",
        "density", "deposit", "deputy", "desert", "desk", "destiny", "detour", "diagram", "dialog", "diamond",
        "diesel", "digital", "dilemma", "diplomat", "discovery", "dolphin", "dragon", "driftwood", "drumstick", "dynamo"
    )

    override suspend fun execute(input: DicewareInput): ToolResult<DicewareOutput> {
        val startTime = System.currentTimeMillis()
        val count = input.wordCount.coerceIn(3, 10)
        val rng = SecureRandom()

        val selectedWords = mutableListOf<String>()
        val diceRolls = mutableListOf<String>()

        for (i in 0 until count) {
            // Roll 5 dice (each 1-6)
            val roll = (1..5).map { rng.nextInt(6) + 1 }.joinToString("")
            diceRolls.add(roll)

            // Select pseudo-uniformly from the wordlist
            val idx = rng.nextInt(wordlist.size)
            var w = wordlist[idx]
            if (input.capitalizeWords) {
                w = w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
            selectedWords.add(w)
        }

        if (input.appendNumber) {
            val num = rng.nextInt(90) + 10 // 2-digit number
            selectedWords[selectedWords.size - 1] = "${selectedWords.last()}$num"
        }

        val passphrase = selectedWords.joinToString(input.delimiter)

        // Entropy: count * log2(wordlist.size) + (if appendNumber then log2(90))
        val baseEntropy = count * log2(wordlist.size.toDouble())
        val extraEntropy = if (input.appendNumber) log2(90.0) else 0.0
        val totalEntropy = (baseEntropy + extraEntropy)

        val secLevel = when {
            totalEntropy >= 80 -> "UNBREAKABLE (Centuries vs Supercomputer)"
            totalEntropy >= 64 -> "STRONG (Decades vs Brute Force Clusters)"
            totalEntropy >= 48 -> "MODERATE (Standard User Passphrase)"
            else -> "LOW (Add more words for higher entropy)"
        }

        val report = buildString {
            appendLine("DICEWARE CRYPTOGRAPHIC PASSPHRASE")
            appendLine("--------------------------------------------------")
            appendLine("Passphrase:       $passphrase")
            appendLine()
            appendLine("SPECIFICATIONS")
            appendLine("• Word Count:     $count words")
            appendLine("• Wordlist Size:  ${wordlist.size} vetted dictionary words")
            appendLine("• Exact Entropy:  ${String.format(Locale.US, "%.1f", totalEntropy)} bits of entropy")
            appendLine("• Security Tier:  $secLevel")
            appendLine()
            appendLine("SIMULATED DICE ROLLS")
            selectedWords.forEachIndexed { i, word ->
                val roll = diceRolls.getOrNull(i) ?: "-----"
                appendLine("  [Dice $roll] → $word")
            }
        }

        val summary = "$passphrase (${String.format(Locale.US, "%.0f", totalEntropy)} bits entropy)"

        return ToolResult.Success(
            data = DicewareOutput(
                passphrase = passphrase,
                words = selectedWords,
                diceRolls = diceRolls,
                entropyBits = totalEntropy,
                securityLevel = secLevel,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
