package com.offline.toolbox.tools.generator

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.SecureRandom

enum class ChoiceMode(val label: String) {
    CUSTOM_LIST("Pick from Custom List"),
    COIN_FLIP("Flip a Coin"),
    DICE_ROLL("Roll Dice")
}

data class RandomChoiceInput(
    val mode: ChoiceMode = ChoiceMode.CUSTOM_LIST,
    val itemsText: String = "Option A, Option B, Option C",
    val diceSides: Int = 6,
    val diceCount: Int = 1
)

data class RandomChoiceOutput(
    val chosenResult: String,
    val details: String,
    val summary: String
)

class RandomChoiceTool : Tool<RandomChoiceInput, RandomChoiceOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "random_choice_picker",
        name = "Random Picker & Dice Roller",
        description = "Pick randomly from a custom list, flip coins, or roll polyhedral dice (D4 to D100).",
        category = ToolCategory.DEVELOPER,
        tags = listOf("random", "picker", "choice", "dice", "coin", "flip", "decision", "lottery", "selector"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Casino"
    )

    private val random = SecureRandom()

    override suspend fun execute(input: RandomChoiceInput): ToolResult<RandomChoiceOutput> {
        val startTime = System.currentTimeMillis()

        return when (input.mode) {
            ChoiceMode.CUSTOM_LIST -> {
                val items = input.itemsText.split(Regex("[,;\\n]+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (items.isEmpty()) {
                    return ToolResult.Failure(
                        message = "Custom list is empty.",
                        userGuidance = "Enter items separated by commas or newlines (e.g. 'Pizza, Sushi, Burger, Tacos')."
                    )
                }

                val selected = items[random.nextInt(items.size)]
                val summary = "Selected: $selected (out of ${items.size} options)"

                ToolResult.Success(
                    data = RandomChoiceOutput(
                        chosenResult = selected,
                        details = "Options considered:\n• " + items.joinToString("\n• "),
                        summary = summary
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            }
            ChoiceMode.COIN_FLIP -> {
                val isHeads = random.nextBoolean()
                val result = if (isHeads) "HEADS" else "TAILS"
                val summary = "Coin Flip: $result"

                ToolResult.Success(
                    data = RandomChoiceOutput(
                        chosenResult = result,
                        details = "Flipped a fair 50/50 coin.",
                        summary = summary
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            }
            ChoiceMode.DICE_ROLL -> {
                val sides = input.diceSides.coerceIn(2, 1000)
                val count = input.diceCount.coerceIn(1, 20)
                val rolls = (1..count).map { 1 + random.nextInt(sides) }
                val total = rolls.sum()

                val rollsStr = rolls.joinToString(" + ")
                val chosen = if (count > 1) "$total ($rollsStr)" else "$total"
                val summary = "Rolled ${count}d$sides → $chosen"

                ToolResult.Success(
                    data = RandomChoiceOutput(
                        chosenResult = chosen,
                        details = "Dice type: d$sides • Quantity: $count • Individual rolls: ${rolls.joinToString(", ")}",
                        summary = summary
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    summary = summary
                )
            }
        }
    }
}
