package com.offline.toolbox.tools.math

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.util.Locale

data class DiscountInput(
    val originalPrice: Double,
    val primaryDiscountPercent: Double,
    val additionalDiscountPercent: Double = 0.0,
    val salesTaxPercent: Double = 0.0
)

data class DiscountOutput(
    val finalPrice: Double,
    val totalSavings: Double,
    val effectiveDiscountPercent: Double,
    val taxAmount: Double,
    val breakdownText: String,
    val summary: String
)

class DiscountCalculatorTool : Tool<DiscountInput, DiscountOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "discount_calculator",
        name = "Discount & Sales Tax Calculator",
        description = "Calculate sale price with stacked coupons, additional percent off, sales tax, and total savings.",
        category = ToolCategory.MATH,
        tags = listOf("discount", "sale", "price", "coupon", "tax", "shopping", "savings", "percent off"),
        inputType = ToolDataType.NUMBER,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "LocalOffer"
    )

    override suspend fun execute(input: DiscountInput): ToolResult<DiscountOutput> {
        val startTime = System.currentTimeMillis()
        val original = input.originalPrice.coerceAtLeast(0.0)

        // 1. Primary discount
        val d1 = input.primaryDiscountPercent.coerceIn(0.0, 100.0)
        val afterD1 = original * (1.0 - d1 / 100.0)

        // 2. Secondary stacked discount
        val d2 = input.additionalDiscountPercent.coerceIn(0.0, 100.0)
        val discountedPreTax = afterD1 * (1.0 - d2 / 100.0)

        // 3. Sales tax
        val taxPct = input.salesTaxPercent.coerceAtLeast(0.0)
        val taxAmount = discountedPreTax * (taxPct / 100.0)
        val finalPrice = discountedPreTax + taxAmount

        val totalSavedPreTax = original - discountedPreTax
        val effectiveDiscountPct = if (original > 0.0) (totalSavedPreTax / original) * 100.0 else 0.0

        val breakdown = buildString {
            appendLine("Original Price:        \$${"%,.2f".format(Locale.US, original)}")
            appendLine("Primary Discount:      ${d1}% (-\$${"%,.2f".format(Locale.US, original * (d1 / 100.0))})")
            if (d2 > 0.0) {
                appendLine("Additional Discount:   ${d2}% (-\$${"%,.2f".format(Locale.US, afterD1 * (d2 / 100.0))})")
            }
            appendLine("Price Before Tax:      \$${"%,.2f".format(Locale.US, discountedPreTax)}")
            if (taxPct > 0.0) {
                appendLine("Sales Tax (${taxPct}%):    +\$${"%,.2f".format(Locale.US, taxAmount)}")
            }
            appendLine("--------------------------------")
            appendLine("Final Payable Price:   \$${"%,.2f".format(Locale.US, finalPrice)}")
            appendLine("Total Savings:         \$${"%,.2f".format(Locale.US, totalSavedPreTax)} (${"%.1f".format(effectiveDiscountPct)}% off)")
        }

        val summary = "Pay \$${"%,.2f".format(Locale.US, finalPrice)} • Save \$${"%,.2f".format(Locale.US, totalSavedPreTax)}"

        return ToolResult.Success(
            data = DiscountOutput(
                finalPrice = finalPrice,
                totalSavings = totalSavedPreTax,
                effectiveDiscountPercent = effectiveDiscountPct,
                taxAmount = taxAmount,
                breakdownText = breakdown,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
