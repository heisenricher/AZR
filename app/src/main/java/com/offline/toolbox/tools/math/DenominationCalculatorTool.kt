package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.roundToLong

enum class CurrencyPreset(val symbol: String, val denominations: List<Double>) {
    USD("$", listOf(100.0, 50.0, 20.0, 10.0, 5.0, 2.0, 1.0, 0.25, 0.10, 0.05, 0.01)),
    EUR("€", listOf(500.0, 200.0, 100.0, 50.0, 20.0, 10.0, 5.0, 2.0, 1.0, 0.50, 0.20, 0.10, 0.05, 0.02, 0.01)),
    GBP("£", listOf(50.0, 20.0, 10.0, 5.0, 2.0, 1.0, 0.50, 0.20, 0.10, 0.05, 0.02, 0.01)),
    INR("₹", listOf(500.0, 200.0, 100.0, 50.0, 20.0, 10.0, 5.0, 2.0, 1.0))
}

data class DenominationItem(
    val denomination: Double,
    val count: Int,
    val subtotal: Double
)

data class DenominationInput(
    val amount: Double = 387.65,
    val currency: CurrencyPreset = CurrencyPreset.USD
)

data class DenominationOutput(
    val totalAmount: Double,
    val currencySymbol: String,
    val totalNotesAndCoins: Int,
    val items: List<DenominationItem>,
    val formattedReport: String,
    val summary: String
)

class DenominationCalculatorTool : Tool<DenominationInput, DenominationOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "denomination_calculator_tool",
        name = "Cash Denomination & Register Splitter",
        description = "Breakdown total cash amounts into optimal note and coin denominations across USD, EUR, GBP, and INR.",
        category = ToolCategory.MATH,
        tags = listOf("cash", "denomination", "currency", "money", "register", "change", "notes", "coins"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Payments"
    )

    override suspend fun execute(input: DenominationInput): ToolResult<DenominationOutput> {
        val startTime = System.currentTimeMillis()
        val total = input.amount

        if (total < 0) {
            return ToolResult.Failure("Amount cannot be negative.")
        }

        // Calculate in cents/paise (integer) to prevent floating-point precision loss
        var remainingCents = (total * 100.0).roundToLong()
        val items = mutableListOf<DenominationItem>()
        var totalPieces = 0

        for (denom in input.currency.denominations) {
            val denomCents = (denom * 100.0).roundToLong()
            if (denomCents <= 0) continue

            val count = (remainingCents / denomCents).toInt()
            if (count > 0) {
                val subtotal = count * denom
                items.add(DenominationItem(denom, count, subtotal))
                totalPieces += count
                remainingCents %= denomCents
            }
        }

        val sym = input.currency.symbol
        val report = buildString {
            appendLine("CASH DENOMINATION BREAKDOWN (${input.currency.name})")
            appendLine("--------------------------------------------------")
            appendLine("Total Amount:         $sym${String.format(Locale.US, "%.2f", total)}")
            appendLine("Total Physical Items: $totalPieces piece(s)")
            appendLine()
            appendLine("Denomination Details:")
            items.forEach { item ->
                val denomLabel = if (item.denomination >= 1.0) {
                    "$sym${item.denomination.toInt()}"
                } else {
                    "${(item.denomination * 100).toInt()}c"
                }
                appendLine("• $denomLabel".padEnd(10) + "× ${item.count}".padEnd(8) + "= $sym${String.format(Locale.US, "%.2f", item.subtotal)}")
            }
        }

        val summary = "$sym${String.format(Locale.US, "%.2f", total)} = $totalPieces piece(s)"

        return ToolResult.Success(
            data = DenominationOutput(
                totalAmount = total,
                currencySymbol = sym,
                totalNotesAndCoins = totalPieces,
                items = items,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
