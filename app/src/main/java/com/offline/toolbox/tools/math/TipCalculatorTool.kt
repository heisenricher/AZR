package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale
import kotlin.math.ceil

data class TipInput(
    val billAmount: Double,
    val tipPercentage: Double = 15.0,
    val splitCount: Int = 1,
    val roundUpTotal: Boolean = false
)

data class TipOutput(
    val tipAmount: Double,
    val totalBillWithTip: Double,
    val tipPerPerson: Double,
    val totalPerPerson: Double,
    val effectiveTipPercent: Double,
    val breakdownText: String,
    val summary: String
)

class TipCalculatorTool : Tool<TipInput, TipOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "tip_calculator",
        name = "Tip & Bill Splitter",
        description = "Calculate restaurant tips, bill splitting per person, and optional rounding up.",
        category = ToolCategory.MATH,
        tags = listOf("tip", "bill", "split", "restaurant", "percentage", "gratuity", "calculator"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Receipt"
    )

    override suspend fun execute(input: TipInput): ToolResult<TipOutput> {
        val startTime = System.currentTimeMillis()
        val bill = input.billAmount.coerceAtLeast(0.0)
        val people = input.splitCount.coerceAtLeast(1)

        val rawTip = bill * (input.tipPercentage / 100.0)
        var total = bill + rawTip

        if (input.roundUpTotal) {
            total = ceil(total)
        }

        val actualTip = total - bill
        val effectiveTipPct = if (bill > 0.0) (actualTip / bill) * 100.0 else input.tipPercentage

        val totalPerPerson = total / people
        val tipPerPerson = actualTip / people

        val breakdown = buildString {
            appendLine("Bill Amount:         \$${format2(bill)}")
            appendLine("Tip (${format1(input.tipPercentage)}%):           \$${format2(actualTip)}")
            appendLine("Total Bill:          \$${format2(total)}")
            appendLine("--------------------------------")
            if (people > 1) {
                appendLine("Split ($people people):")
                appendLine("  Per Person Total:  \$${format2(totalPerPerson)}")
                appendLine("  Per Person Tip:    \$${format2(tipPerPerson)}")
            } else {
                appendLine("Total to Pay:        \$${format2(total)}")
            }
        }

        val summary = "\$${format2(totalPerPerson)} / person (Total: \$${format2(total)})"

        return ToolResult.Success(
            data = TipOutput(
                tipAmount = round2(actualTip),
                totalBillWithTip = round2(total),
                tipPerPerson = round2(tipPerPerson),
                totalPerPerson = round2(totalPerPerson),
                effectiveTipPercent = round1(effectiveTipPct),
                breakdownText = breakdown.trimEnd(),
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun format2(v: Double): String = String.format(Locale.US, "%.2f", v)
    private fun format1(v: Double): String = String.format(Locale.US, "%.1f", v)
    private fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0
    private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0
}
