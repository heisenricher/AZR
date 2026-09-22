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

    @Test
    fun testPhase9ApexZenithRegistryAndAliases() {
        val all = ToolRegistry.getAllTools()
        assertTrue("Total registered tools should be at least 135 (currently ${all.size})", all.size >= 135)

        val curlResults = LocalSearchEngine.search("curl")
        assertTrue("Searching 'curl' should find curl parser tool", curlResults.any { it.tool.metadata.id == "curl_command_parser_tool" })

        val asnResults = LocalSearchEngine.search("asn")
        assertTrue("Searching 'asn' should find BGP ASN tool", asnResults.any { it.tool.metadata.id == "bgp_asn_lookup_tool" })

        val hkdfResults = LocalSearchEngine.search("hkdf")
        assertTrue("Searching 'hkdf' should find HKDF tool", hkdfResults.any { it.tool.metadata.id == "hkdf_key_derivation_tool" })

        val refinanceResults = LocalSearchEngine.search("refinance")
        assertTrue("Searching 'refinance' should find Loan Refinance tool", refinanceResults.any { it.tool.metadata.id == "loan_refinance_comparator_tool" })
    }

    @Test
    fun testPhase10CenturionMilestoneRegistryAndAliases() {
        val all = ToolRegistry.getAllTools()
        assertTrue("Total registered tools should be at least 150 (currently ${all.size})", all.size >= 150)

        val harResults = LocalSearchEngine.search("har")
        assertTrue("Searching 'har' should find HAR analyzer", harResults.any { it.tool.metadata.id == "har_analyzer_tool" })

        val promResults = LocalSearchEngine.search("prometheus")
        assertTrue("Searching 'prometheus' should find Prometheus parser", promResults.any { it.tool.metadata.id == "prometheus_metric_parser_tool" })

        val argonResults = LocalSearchEngine.search("argon2")
        assertTrue("Searching 'argon2' should find Argon2 calculator", argonResults.any { it.tool.metadata.id == "argon2_parameter_calculator_tool" })

        val geoResults = LocalSearchEngine.search("geojson")
        assertTrue("Searching 'geojson' should find GeoJSON validator", geoResults.any { it.tool.metadata.id == "geo_json_validator_tool" })

        val bsResults = LocalSearchEngine.search("black scholes")
        assertTrue("Searching 'black scholes' should find Black-Scholes tool", bsResults.any { it.tool.metadata.id == "black_scholes_option_pricer_tool" })

        val soundexResults = LocalSearchEngine.search("soundex")
        assertTrue("Searching 'soundex' should find Soundex comparator", soundexResults.any { it.tool.metadata.id == "soundex_metaphone_tool" })

        val binauralResults = LocalSearchEngine.search("binaural")
        assertTrue("Searching 'binaural' should find Binaural beats analyzer", binauralResults.any { it.tool.metadata.id == "beats_binaural_acoustic_tool" })
    }

    @Test
    fun testPhase11SesquicentennialRegistryAndAliases() {
        val all = ToolRegistry.getAllTools()
        assertTrue("Total registered tools should be at least 165 (currently ${all.size})", all.size >= 165)

        val nginxResults = LocalSearchEngine.search("nginx")
        assertTrue("Searching 'nginx' should find Nginx validator", nginxResults.any { it.tool.metadata.id == "nginx_config_validator_tool" })

        val pragmaResults = LocalSearchEngine.search("pragma")
        assertTrue("Searching 'pragma' should find SQLite PRAGMA inspector", pragmaResults.any { it.tool.metadata.id == "sqlite_pragma_inspector_tool" })

        val totpResults = LocalSearchEngine.search("otpauth")
        assertTrue("Searching 'otpauth' should find TOTP URI builder", totpResults.any { it.tool.metadata.id == "hmac_totp_uri_builder_tool" })

        val umaskResults = LocalSearchEngine.search("umask")
        assertTrue("Searching 'umask' should find Unix umask calculator", umaskResults.any { it.tool.metadata.id == "unix_umask_calculator_tool" })

        val vernamResults = LocalSearchEngine.search("vernam")
        assertTrue("Searching 'vernam' should find Vernam OTP cipher", vernamResults.any { it.tool.metadata.id == "vernam_one_time_pad_tool" })

        val bip39Results = LocalSearchEngine.search("bip39")
        assertTrue("Searching 'bip39' should find BIP-39 mnemonic tool", bip39Results.any { it.tool.metadata.id == "bip39_mnemonic_entropy_tool" })

        val wktResults = LocalSearchEngine.search("wkt")
        assertTrue("Searching 'wkt' should find WKT geometry parser", wktResults.any { it.tool.metadata.id == "wkt_geometry_parser_tool" })

        val bencodeResults = LocalSearchEngine.search("bencode")
        assertTrue("Searching 'bencode' should find Bencode parser", bencodeResults.any { it.tool.metadata.id == "bencode_parser_tool" })

        val mcResults = LocalSearchEngine.search("monte carlo")
        assertTrue("Searching 'monte carlo' should find Monte Carlo Pi tool", mcResults.any { it.tool.metadata.id == "monte_carlo_pi_simulator_tool" })

        val ytmResults = LocalSearchEngine.search("ytm")
        assertTrue("Searching 'ytm' should find Bond YTM solver", ytmResults.any { it.tool.metadata.id == "bond_yield_to_maturity_tool" })

        val kinResults = LocalSearchEngine.search("kinematics")
        assertTrue("Searching 'kinematics' should find Kinematics trajectory tool", kinResults.any { it.tool.metadata.id == "kinematics_trajectory_tool" })

        val rleResults = LocalSearchEngine.search("rle")
        assertTrue("Searching 'rle' should find RLE compression tool", rleResults.any { it.tool.metadata.id == "run_length_encoding_tool" })

        val levResults = LocalSearchEngine.search("levenshtein matrix")
        assertTrue("Searching 'levenshtein matrix' should find Levenshtein visualizer", levResults.any { it.tool.metadata.id == "levenshtein_matrix_visualizer_tool" })

        val rt60Results = LocalSearchEngine.search("rt60")
        assertTrue("Searching 'rt60' should find RT60 acoustic tool", rt60Results.any { it.tool.metadata.id == "reverb_rt60_acoustic_tool" })

        val deltaEResults = LocalSearchEngine.search("ciede2000")
        assertTrue("Searching 'ciede2000' should find CIEDE2000 color tool", deltaEResults.any { it.tool.metadata.id == "color_delta_e_2000_tool" })
    }

    @Test
    fun testPhase12ToolAliasesAndTotalCount() {
        val all = ToolRegistry.getAllTools()
        assertTrue("Total registered tools should be at least 180 (currently ${all.size})", all.size >= 180)

        val systemdResults = LocalSearchEngine.search("systemd")
        assertTrue("Searching 'systemd' should find systemd unit validator", systemdResults.any { it.tool.metadata.id == "systemd_service_unit_validator_tool" })

        val dockerResults = LocalSearchEngine.search("dockerfile")
        assertTrue("Searching 'dockerfile' should find Dockerfile linter", dockerResults.any { it.tool.metadata.id == "dockerfile_linter_tool" })

        val htaccessResults = LocalSearchEngine.search("htaccess")
        assertTrue("Searching 'htaccess' should find htaccess validator", htaccessResults.any { it.tool.metadata.id == "apache_htaccess_validator_tool" })

        val k8sResults = LocalSearchEngine.search("k8s")
        assertTrue("Searching 'k8s' should find K8s inspector", k8sResults.any { it.tool.metadata.id == "kube_yaml_resource_inspector_tool" })

        val polyResults = LocalSearchEngine.search("polybius")
        assertTrue("Searching 'polybius' should find Polybius cipher", polyResults.any { it.tool.metadata.id == "polybius_square_cipher_tool" })

        val affineResults = LocalSearchEngine.search("affine")
        assertTrue("Searching 'affine' should find Affine cipher", affineResults.any { it.tool.metadata.id == "affine_cipher_tool" })

        val geoResults = LocalSearchEngine.search("geohash")
        assertTrue("Searching 'geohash' should find GeoHash codec", geoResults.any { it.tool.metadata.id == "geohash_codec_tool" })

        val cborResults = LocalSearchEngine.search("cbor")
        assertTrue("Searching 'cbor' should find CBOR inspector", cborResults.any { it.tool.metadata.id == "cbor_hex_inspector_tool" })

        val annuityResults = LocalSearchEngine.search("annuity")
        assertTrue("Searching 'annuity' should find Annuity calculator", annuityResults.any { it.tool.metadata.id == "annuity_calculator_tool" })

        val heatResults = LocalSearchEngine.search("heat index")
        assertTrue("Searching 'heat index' should find Heat Index tool", heatResults.any { it.tool.metadata.id == "heat_index_wind_chill_tool" })

        val rocketResults = LocalSearchEngine.search("rocket")
        assertTrue("Searching 'rocket' should find Rocket propulsion tool", rocketResults.any { it.tool.metadata.id == "tsiolkovsky_rocket_equation_tool" })

        val stemResults = LocalSearchEngine.search("stemmer")
        assertTrue("Searching 'stemmer' should find Porter stemmer", stemResults.any { it.tool.metadata.id == "porter_stemmer_tool" })

        val caesarResults = LocalSearchEngine.search("caesar brute")
        assertTrue("Searching 'caesar brute' should find Caesar breaker", caesarResults.any { it.tool.metadata.id == "caesar_brute_force_breaker_tool" })

        val snrResults = LocalSearchEngine.search("snr")
        assertTrue("Searching 'snr' should find Audio SNR tool", snrResults.any { it.tool.metadata.id == "snr_audio_calculator_tool" })

        val blendResults = LocalSearchEngine.search("alpha blend")
        assertTrue("Searching 'alpha blend' should find Color alpha blender", blendResults.any { it.tool.metadata.id == "color_hex_alpha_blender_tool" })
    }
}
