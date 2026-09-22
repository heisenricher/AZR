package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase13DataToolsTest {

    private val olcTool = OpenLocationCodeTool()
    private val tsvTool = TsvFilterAggregateTool()

    // 1. Open Location Code (Plus Codes) Tests
    @Test
    fun testOpenLocationCode_encodeAndDecode() = runTest {
        // Encode SF coordinates: 37.774929, -122.419416
        val encResult = olcTool.execute(
            PlusCodeInput(
                operation = "ENCODE",
                latitude = 37.774929,
                longitude = -122.419416
            )
        )
        assertTrue(encResult is ToolResult.Success)
        val code = (encResult as ToolResult.Success).data.code
        assertTrue(code.contains("+"))
        assertEquals(11, code.length) // 8 chars + '+' + 2 chars

        // Decode the Plus Code back to bounding box
        val decResult = olcTool.execute(
            PlusCodeInput(
                operation = "DECODE",
                plusCode = code
            )
        )
        assertTrue(decResult is ToolResult.Success)
        val box = (decResult as ToolResult.Success).data.boundingBox
        assertEquals(37.774929, box.centerLatitude, 0.01)
        assertEquals(-122.419416, box.centerLongitude, 0.01)
    }

    @Test
    fun testOpenLocationCode_invalidCodeFails() = runTest {
        val res = olcTool.execute(
            PlusCodeInput(
                operation = "DECODE",
                plusCode = "INVALID_PLUS_CODE_ZZ"
            )
        )
        assertTrue(res is ToolResult.Failure)
    }

    // 2. TSV Filter and Aggregate Tests
    @Test
    fun testTsvFilterAggregate_sumMeanAndGroupBy() = runTest {
        val tsvData = """
            category	item	quantity	price
            Electronics	Laptop	5	1000.00
            Furniture	Chair	10	100.00
            Electronics	Mouse	20	50.00
            Furniture	Desk	2	300.00
        """.trimIndent()

        val input = TsvAggregateInput(
            tsvContent = tsvData,
            filterColumn = "",
            filterOperator = "ALL",
            aggregateColumn = "price",
            groupByColumn = "category"
        )

        val res = tsvTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(4, data.filteredRowCount)
        assertEquals(1450.0, data.overallSum, 0.001)
        assertEquals(362.5, data.overallMean, 0.001)
        assertEquals(50.0, data.overallMin, 0.001)
        assertEquals(1000.0, data.overallMax, 0.001)

        val elecGroup = data.groupSummaries.firstOrNull { it.groupKey == "Electronics" }
        assertTrue(elecGroup != null)
        assertEquals(2, elecGroup!!.rowCount)
        assertEquals(1050.0, elecGroup.sum, 0.001)
    }

    @Test
    fun testTsvFilterAggregate_withNumericFilter() = runTest {
        val tsvData = """
            product	stock	cost
            WidgetA	5	10.0
            WidgetB	25	50.0
            WidgetC	100	15.0
        """.trimIndent()

        val input = TsvAggregateInput(
            tsvContent = tsvData,
            filterColumn = "stock",
            filterOperator = "GREATER_THAN",
            filterValue = "10",
            aggregateColumn = "cost"
        )

        val res = tsvTool.execute(input)
        assertTrue(res is ToolResult.Success)
        val data = (res as ToolResult.Success).data
        assertEquals(2, data.filteredRowCount) // WidgetB and WidgetC
        assertEquals(65.0, data.overallSum, 0.001)
    }

    @Test
    fun testTsvFilterAggregate_emptyDataFails() = runTest {
        val res = tsvTool.execute(TsvAggregateInput(tsvContent = ""))
        assertTrue(res is ToolResult.Failure)
    }
}
