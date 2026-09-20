package com.offline.toolbox.engine.search

import com.offline.toolbox.engine.registry.ToolRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSearchEngineTest {

    @Test
    fun testEmptyQueryReturnsAll() {
        val all = ToolRegistry.getAllTools()
        val results = LocalSearchEngine.search("", all)
        assertTrue(results.size == all.size)
    }

    @Test
    fun testExactToolNameSearch() {
        val results = LocalSearchEngine.search("JSON Formatter & Validator")
        assertFalse(results.isEmpty())
        assertTrue(results[0].tool.metadata.id == "json_formatter")
    }

    @Test
    fun testAliasSearchB64() {
        val results = LocalSearchEngine.search("b64")
        assertFalse(results.isEmpty())
        assertTrue(results.any { it.tool.metadata.id == "base64_converter" })
    }

    @Test
    fun testFuzzyMisspellingSearch() {
        // "passwrd" should fuzzy match "Password"
        val results = LocalSearchEngine.search("passwrd")
        assertFalse(results.isEmpty())
        assertTrue(results.any { it.tool.metadata.id == "password_generator" })
    }

    @Test
    fun testCategoryKeywordSearch() {
        val results = LocalSearchEngine.search("security")
        assertFalse(results.isEmpty())
        assertTrue(results.any { it.tool.metadata.id == "hash_generator" })
        assertTrue(results.any { it.tool.metadata.id == "password_generator" })
    }

    @Test
    fun testPhase4ToolAliasesAndTotalCount() {
        val all = ToolRegistry.getAllTools()
        assertTrue("Total registered tools should be at least 60 (currently ${all.size})", all.size >= 60)

        val emiResults = LocalSearchEngine.search("emi")
        assertTrue("Searching 'emi' should find loan EMI calculator", emiResults.any { it.tool.metadata.id == "loan_emi_calculator_tool" })

        val zipResults = LocalSearchEngine.search("unzip")
        assertTrue("Searching 'unzip' should find ZIP archive tool", zipResults.any { it.tool.metadata.id == "zip_archive_tool" })

        val morseResults = LocalSearchEngine.search("sos")
        assertTrue("Searching 'sos' should find Morse code tool", morseResults.any { it.tool.metadata.id == "morse_code_tool" })

        val pdfResults = LocalSearchEngine.search("pdf")
        assertTrue("Searching 'pdf' should find PDF generator tool", pdfResults.any { it.tool.metadata.id == "pdf_generator_tool" })
    }
}
