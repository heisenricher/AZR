package com.offline.toolbox.engine.search

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.engine.registry.ToolRegistry
import java.util.Locale
import kotlin.math.min

data class SearchResultItem(
    val tool: Tool<*, *>,
    val relevanceScore: Int,
    val matchedField: String
)

object LocalSearchEngine {

    private val aliasDictionary = mapOf(
        "b64" to listOf("base64"),
        "base 64" to listOf("base64"),
        "sha" to listOf("hash", "sha256", "sha512"),
        "md5" to listOf("hash"),
        "pwd" to listOf("password"),
        "pass" to listOf("password", "passphrase"),
        "diceware" to listOf("password", "passphrase"),
        "stats" to listOf("word counter", "statistics"),
        "calc" to listOf("percentage", "calculator", "fraction"),
        "math" to listOf("percentage", "calculator", "average", "fraction"),
        "days" to listOf("date difference", "add / subtract date", "countdown"),
        "calendar" to listOf("date difference", "add / subtract date"),
        "age" to listOf("date difference"),
        "hex" to listOf("color", "number base"),
        "rgb" to listOf("color"),
        "contrast" to listOf("color"),
        "wcag" to listOf("color"),
        "beautify" to listOf("json"),
        "lint" to listOf("json"),
        "minify" to listOf("json"),
        "jwt" to listOf("jwt decoder", "token"),
        "token" to listOf("jwt decoder"),
        "url" to listOf("url encoder"),
        "uri" to listOf("url encoder"),
        "html" to listOf("html entity"),
        "entities" to listOf("html entity"),
        "epoch" to listOf("unix timestamp"),
        "timestamp" to listOf("unix timestamp"),
        "binary" to listOf("number base"),
        "octal" to listOf("number base"),
        "regex" to listOf("regex tester"),
        "lorem" to listOf("lorem ipsum"),
        "placeholder" to listOf("lorem ipsum"),
        "diff" to listOf("text diff"),
        "compare" to listOf("text diff"),
        "clean" to listOf("text cleaner"),
        "trim" to listOf("text cleaner"),
        "sort" to listOf("line operations"),
        "dedup" to listOf("line operations"),
        "replace" to listOf("find & replace"),
        "find" to listOf("find & replace"),
        "length" to listOf("length & distance"),
        "distance" to listOf("length & distance"),
        "feet" to listOf("length & distance"),
        "meters" to listOf("length & distance"),
        "weight" to listOf("weight & mass"),
        "mass" to listOf("weight & mass"),
        "lbs" to listOf("weight & mass"),
        "kg" to listOf("weight & mass"),
        "temp" to listOf("temperature"),
        "celsius" to listOf("temperature"),
        "fahrenheit" to listOf("temperature"),
        "storage" to listOf("data storage"),
        "bytes" to listOf("data storage"),
        "mb" to listOf("data storage"),
        "gb" to listOf("data storage"),
        "speed" to listOf("speed & velocity"),
        "mph" to listOf("speed & velocity"),
        "kmh" to listOf("speed & velocity"),
        "roman" to listOf("roman numeral"),
        "tip" to listOf("tip & bill"),
        "bill" to listOf("tip & bill"),
        "bmi" to listOf("bmi & body health"),
        "health" to listOf("bmi & body health"),
        "average" to listOf("statistics & average"),
        "mean" to listOf("statistics & average"),
        "fraction" to listOf("fraction calculator"),
        "countdown" to listOf("countdown & milestone"),
        "uuid" to listOf("uuid / guid"),
        "guid" to listOf("uuid / guid"),
        "csv" to listOf("csv to json", "json to csv"),
        "tsv" to listOf("csv to json"),
        "dice" to listOf("random picker"),
        "coin" to listOf("random picker"),
        "roll" to listOf("random picker"),
        "slug" to listOf("url slug"),
        "unicode" to listOf("unicode & string inspector"),
        "inspector" to listOf("unicode & string inspector"),
        "rot13" to listOf("rot13 & caesar"),
        "caesar" to listOf("rot13 & caesar"),
        "cipher" to listOf("rot13 & caesar"),
        "sql" to listOf("sql query formatter"),
        "query" to listOf("sql query formatter"),
        "xml" to listOf("xml formatter"),
        "markdown" to listOf("markdown inspector"),
        "md" to listOf("markdown inspector"),
        "interest" to listOf("compound interest"),
        "compound" to listOf("compound interest"),
        "discount" to listOf("discount & sales tax"),
        "sale" to listOf("discount & sales tax"),
        "tax" to listOf("discount & sales tax"),
        "coupon" to listOf("discount & sales tax"),
        "area" to listOf("area & land"),
        "acre" to listOf("area & land"),
        "sqft" to listOf("area & land"),
        "hectare" to listOf("area & land"),
        "volume" to listOf("volume & liquid"),
        "gallon" to listOf("volume & liquid"),
        "liter" to listOf("volume & liquid"),
        "qr" to listOf("qr code payload", "qr code matrix"),
        "wifi" to listOf("qr code payload"),
        "vcard" to listOf("qr code payload"),
        "zip" to listOf("zip", "archive", "compress"),
        "unzip" to listOf("zip", "extract"),
        "archive" to listOf("zip"),
        "checksum" to listOf("checksum", "sha256", "md5"),
        "integrity" to listOf("checksum"),
        "exif" to listOf("exif", "metadata", "gps"),
        "gps" to listOf("exif", "privacy"),
        "camera" to listOf("exif"),
        "pdf" to listOf("pdf", "document", "print"),
        "document" to listOf("pdf"),
        "emi" to listOf("emi", "loan", "mortgage"),
        "loan" to listOf("emi", "amortization"),
        "mortgage" to listOf("emi"),
        "amortization" to listOf("emi"),
        "morse" to listOf("morse", "telegraph", "sos"),
        "sos" to listOf("morse"),
        "base32" to listOf("base32", "totp"),
        "totp" to listOf("base32"),
        "ascii" to listOf("ascii", "banner", "art"),
        "banner" to listOf("ascii"),
        "filter" to listOf("csv filter", "query"),
        "subnet" to listOf("subnet", "cidr", "ip"),
        "cidr" to listOf("subnet", "ip"),
        "ip" to listOf("subnet", "network"),
        "mac" to listOf("mac address", "hardware"),
        "ethernet" to listOf("mac address"),
        "cron" to listOf("cron expression", "schedule"),
        "crontab" to listOf("cron expression"),
        "schedule" to listOf("cron expression"),
        "hmac" to listOf("hmac", "hash"),
        "rsa" to listOf("rsa", "pem", "key"),
        "pem" to listOf("rsa", "key"),
        "keygen" to listOf("rsa", "key"),
        "yaml" to listOf("yaml", "json"),
        "yml" to listOf("yaml", "json"),
        "stats" to listOf("csv stats", "statistics"),
        "palette" to listOf("palette", "color", "harmony"),
        "harmony" to listOf("palette", "color"),
        "aspect" to listOf("aspect ratio", "resolution"),
        "resolution" to listOf("aspect ratio"),
        "fuel" to listOf("fuel cost", "mileage", "trip"),
        "gas" to listOf("fuel cost"),
        "mpg" to listOf("fuel cost"),
        "gpa" to listOf("gpa calculator", "grades"),
        "cgpa" to listOf("gpa calculator"),
        "grade" to listOf("gpa calculator"),
        "binary" to listOf("binary & hex", "bytes"),
        "leet" to listOf("leetspeak", "1337"),
        "1337" to listOf("leetspeak"),
        "pipeline" to listOf("pipeline chainer", "chain", "workflow"),
        "workflow" to listOf("pipeline chainer"),
        "ipv6" to listOf("ipv6 subnet", "network"),
        "port" to listOf("iana network port", "firewall"),
        "iana" to listOf("iana network port"),
        "aes" to listOf("aes-gcm authenticated encryption", "encrypt"),
        "encrypt" to listOf("aes-gcm authenticated encryption"),
        "decrypt" to listOf("aes-gcm authenticated encryption"),
        "bcrypt" to listOf("bcrypt work factor", "password"),
        "diff" to listOf("json structural tree diff", "text diff"),
        "patch" to listOf("json structural tree diff"),
        "ndjson" to listOf("ndjson / json lines"),
        "jsonl" to listOf("ndjson / json lines"),
        "scientific" to listOf("scientific & engineering notation", "magnitude"),
        "cash" to listOf("cash denomination", "money"),
        "denomination" to listOf("cash denomination"),
        "prime" to listOf("prime factorization"),
        "factor" to listOf("prime factorization"),
        "wrap" to listOf("word wrap & paragraph reflower"),
        "zalgo" to listOf("zalgo glitch text", "corrupt"),
        "glitch" to listOf("zalgo glitch text"),
        "anagram" to listOf("anagram & palindrome solver"),
        "palindrome" to listOf("anagram & palindrome solver"),
        "hertz" to listOf("audio frequency & musical note tuner", "pitch"),
        "tuner" to listOf("audio frequency & musical note tuner"),
        "pitch" to listOf("audio frequency & musical note tuner"),
        "dpi" to listOf("screen dpi & android density bucket"),
        "ppi" to listOf("screen dpi & android density bucket"),
        "color name" to listOf("css & html named color matcher"),
        "css color" to listOf("css & html named color matcher"),
        "useragent" to listOf("user-agent client & platform"),
        "ua" to listOf("user-agent client & platform"),
        "browser" to listOf("user-agent client & platform"),
        "semver" to listOf("semantic versioning (semver 2.0)"),
        "version" to listOf("semantic versioning (semver 2.0)"),
        "chmod" to listOf("chmod unix permissions"),
        "permission" to listOf("chmod unix permissions"),
        "rwx" to listOf("chmod unix permissions"),
        "http header" to listOf("http response header & security"),
        "security header" to listOf("http response header & security"),
        "csp" to listOf("http response header & security"),
        "hsts" to listOf("http response header & security"),
        "2fa" to listOf("offline totp / 2fa authenticator"),
        "otp" to listOf("offline totp / 2fa authenticator"),
        "authenticator" to listOf("offline totp / 2fa authenticator"),
        "diceware" to listOf("diceware cryptographic passphrase"),
        "passphrase" to listOf("diceware cryptographic passphrase"),
        "jsonpath" to listOf("jsonpath query & value extractor"),
        "tsv" to listOf("tsv <-> csv bi-directional"),
        "matrix" to listOf("matrix arithmetic & determinant"),
        "determinant" to listOf("matrix arithmetic & determinant"),
        "gaussian" to listOf("gaussian & normal distribution"),
        "zscore" to listOf("gaussian & normal distribution"),
        "cagr" to listOf("compound annual growth rate"),
        "growth" to listOf("compound annual growth rate"),
        "casing" to listOf("text case style detector"),
        "camelcase" to listOf("text case style detector"),
        "nato" to listOf("nato & icao radio phonetic"),
        "phonetic" to listOf("nato & icao radio phonetic"),
        "metronome" to listOf("metronome & bpm tempo tapper"),
        "bpm" to listOf("metronome & bpm tempo tapper"),
        "color blindness" to listOf("color vision deficiency & accessibility"),
        "protanopia" to listOf("color vision deficiency & accessibility"),
        "deuteranopia" to listOf("color vision deficiency & accessibility"),
        "supernet" to listOf("cidr route aggregation & supernet"),
        "aggregation" to listOf("cidr route aggregation & supernet"),
        "dns" to listOf("dns zone file & resource record"),
        "zone" to listOf("dns zone file & resource record"),
        "docker" to listOf("docker compose yaml & port conflict"),
        "compose" to listOf("docker compose yaml & port conflict"),
        "dialect" to listOf("sql dialect translator"),
        "shamir" to listOf("shamir's secret sharing threshold"),
        "steganography" to listOf("invisible unicode text steganography"),
        "stego" to listOf("invisible unicode text steganography"),
        "xml to json" to listOf("xml <-> json bi-directional"),
        "hexdump" to listOf("hex dump & binary memory inspector"),
        "polynomial" to listOf("quadratic & cubic polynomial root"),
        "cubic" to listOf("quadratic & cubic polynomial root"),
        "vector" to listOf("2d & 3d vector math engine"),
        "bmr" to listOf("bmr & daily caloric expenditure"),
        "tdee" to listOf("bmr & daily caloric expenditure"),
        "similarity" to listOf("string distance & similarity comparator"),
        "levenshtein" to listOf("string distance & similarity comparator"),
        "apca" to listOf("wcag 3 apca & perceptual contrast"),
        "interval" to listOf("musical interval & acoustic harmony"),
        "curl" to listOf("curl to code & http request parser"),
        "asn" to listOf("bgp autonomous system (asn) lookup & validator"),
        "bgp" to listOf("bgp autonomous system (asn) lookup & validator"),
        "gitignore" to listOf("multi-stack .gitignore generator"),
        "crontab" to listOf("crontab overlap & collision analyzer"),
        "hkdf" to listOf("hkdf key derivation (rfc 5869)"),
        "vigenere" to listOf("vigenère polyalphabetic cipher & cryptanalysis"),
        "json schema" to listOf("csv to json schema inferer"),
        "schema" to listOf("csv to json schema inferer"),
        "bst" to listOf("binary search tree (bst) visualizer & traversals"),
        "binary tree" to listOf("binary search tree (bst) visualizer & traversals"),
        "refinance" to listOf("loan refinance & breakeven comparator"),
        "trig" to listOf("trigonometric & hyperbolic calculator"),
        "trigonometry" to listOf("trigonometric & hyperbolic calculator"),
        "constant" to listOf("codata fundamental physical constants"),
        "physics" to listOf("codata fundamental physical constants"),
        "readability" to listOf("text readability & grade level analyzer"),
        "flesch" to listOf("text readability & grade level analyzer"),
        "justify" to listOf("typographic text justifier & word spacing"),
        "kelvin" to listOf("color temperature (kelvin) to rgb converter"),
        "temperature" to listOf("color temperature (kelvin) to rgb converter"),
        "decibel" to listOf("audio decibel & sound pressure (spl) calculator"),
        "spl" to listOf("audio decibel & sound pressure (spl) calculator"),
        // Phase 10 Synonyms
        "har" to listOf("http archive (.har) performance & waterfall analyzer"),
        "waterfall" to listOf("http archive (.har) performance & waterfall analyzer"),
        "prometheus" to listOf("prometheus & opentelemetry exposition metric parser"),
        "metrics" to listOf("prometheus & opentelemetry exposition metric parser"),
        "ssh key" to listOf("ssh public key inspector & fingerprint calculator"),
        "fingerprint" to listOf("ssh public key inspector & fingerprint calculator"),
        "jwk" to listOf("jwk inspector & rfc 7638 thumbprint calculator"),
        "thumbprint" to listOf("jwk inspector & rfc 7638 thumbprint calculator"),
        "argon2" to listOf("argon2 parameter cost & security calculator"),
        "kdf" to listOf("argon2 parameter cost & security calculator"),
        "rail fence" to listOf("rail fence (zig-zag) cipher & visualizer"),
        "zigzag" to listOf("rail fence (zig-zag) cipher & visualizer", "protobuf varint & zigzag decoder"),
        "geojson" to listOf("geojson validator & geometry inspector"),
        "gis" to listOf("geojson validator & geometry inspector"),
        "centroid" to listOf("geojson validator & geometry inspector"),
        "protobuf" to listOf("protobuf varint & zigzag decoder"),
        "varint" to listOf("protobuf varint & zigzag decoder"),
        "leb128" to listOf("protobuf varint & zigzag decoder"),
        "black scholes" to listOf("black-scholes option pricing & greeks calculator"),
        "option" to listOf("black-scholes option pricing & greeks calculator"),
        "greeks" to listOf("black-scholes option pricing & greeks calculator"),
        "doppler" to listOf("doppler effect (acoustic & relativistic) calculator"),
        "redshift" to listOf("doppler effect (acoustic & relativistic) calculator"),
        "vinculum" to listOf("extended roman numerals (vinculum notation) converter"),
        "advanced roman" to listOf("extended roman numerals (vinculum notation) converter"),
        "soundex" to listOf("soundex & metaphone phonetic name comparator"),
        "metaphone" to listOf("soundex & metaphone phonetic name comparator"),
        "ngram" to listOf("n-gram frequency & lexical diversity analyzer"),
        "bigram" to listOf("n-gram frequency & lexical diversity analyzer"),
        "trigram" to listOf("n-gram frequency & lexical diversity analyzer"),
        "binaural" to listOf("acoustic beat & binaural frequency analyzer"),
        "beat frequency" to listOf("acoustic beat & binaural frequency analyzer"),
        "brainwave" to listOf("acoustic beat & binaural frequency analyzer"),
        "color mix" to listOf("subtractive & additive color mixer"),
        "subtractive" to listOf("subtractive & additive color mixer")
    )

    fun search(query: String, tools: List<Tool<*, *>> = ToolRegistry.getAllTools()): List<SearchResultItem> {
        val trimmed = query.trim().lowercase(Locale.getDefault())
        if (trimmed.isEmpty()) {
            return tools.map { SearchResultItem(it, 0, "Default") }
        }

        val queryTokens = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val results = mutableListOf<SearchResultItem>()

        for (tool in tools) {
            val meta = tool.metadata
            var score = 0
            var matchedReason = ""

            val nameLower = meta.name.lowercase(Locale.getDefault())
            val descLower = meta.description.lowercase(Locale.getDefault())
            val tagsLower = meta.tags.map { it.lowercase(Locale.getDefault()) }
            val categoryLower = meta.category.title.lowercase(Locale.getDefault())

            // 1. Exact or prefix name match
            if (nameLower == trimmed) {
                score += 120
                matchedReason = "Exact name match"
            } else if (nameLower.startsWith(trimmed)) {
                score += 80
                matchedReason = "Name prefix match"
            } else if (nameLower.contains(trimmed)) {
                score += 60
                matchedReason = "Name contains '$trimmed'"
            }

            // 2. Token matches
            for (token in queryTokens) {
                // Check aliases
                val aliases = aliasDictionary[token] ?: emptyList()
                val expandedTokens = listOf(token) + aliases

                for (t in expandedTokens) {
                    if (tagsLower.any { it == t }) {
                        score += 50
                        if (matchedReason.isEmpty()) matchedReason = "Tag match (#$t)"
                    } else if (tagsLower.any { it.contains(t) }) {
                        score += 30
                        if (matchedReason.isEmpty()) matchedReason = "Tag contains '$t'"
                    }

                    if (categoryLower.contains(t)) {
                        score += 25
                        if (matchedReason.isEmpty()) matchedReason = "Category match"
                    }

                    if (descLower.contains(t)) {
                        score += 15
                        if (matchedReason.isEmpty()) matchedReason = "Description match"
                    }
                }

                // 3. Fuzzy match on tool name words
                if (token.length >= 3) {
                    for (word in nameLower.split(" ")) {
                        val distance = levenshteinDistance(token, word)
                        if (distance <= 1) {
                            score += 40
                            if (matchedReason.isEmpty()) matchedReason = "Fuzzy match (~$word)"
                            break
                        } else if (distance == 2 && token.length >= 5) {
                            score += 20
                            if (matchedReason.isEmpty()) matchedReason = "Fuzzy match (~$word)"
                            break
                        }
                    }
                }
            }

            if (score > 0) {
                results.add(SearchResultItem(tool, score, matchedReason))
            }
        }

        return results.sortedByDescending { it.relevanceScore }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
