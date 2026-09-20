# AZR — Offline Android Utility Toolbox

[![100% Offline](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg)](#privacy--zero-network-guarantee)
[![Tests Passing](https://img.shields.io/badge/Unit%20Tests-191%20Passed-brightgreen.svg)](#automated-testing--quality)
[![Tools Count](https://img.shields.io/badge/Utilities-75%20Tools-blue.svg)](#utility-catalog-75-tools)
[![Android](https://img.shields.io/badge/Android-SDK%2026--35-green.svg)](#technical-stack)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-purple.svg)](#user-interface--design-system)

**AZR (Offline Android Utility Toolbox)** is a premium, privacy-first, 100% offline Swiss Army knife of utilities engineered natively for Android. Designed for everyday users, power users, and developers alike, AZR operates with an uncompromising zero-network guarantee.

---

## Key Highlights

- **75 Production-Grade Utilities**: Expanded coverage across Networking, Cryptography, Media, Finance, Data Formats, Text Operations, and Unit Converters.
- **Multi-Tool Pipeline Chaining Engine (`PipelineEngine`)**: Execute multi-step sequential transformations (e.g. `Clean -> Case Convert -> Base64 -> Hash`) in a single atomic pass with intermediate audit trails.
- **Zero Network Guarantee**: Absolutely **NO** `android.permission.INTERNET` in `AndroidManifest.xml`. No telemetry, no cloud analytics, no remote SDKs.
- **On-Device Cryptography**: Full support for RSA Key Pair generation (PKCS#8 & X.509 PEM), Keyed HMAC signatures, and multi-algorithm hashing entirely on-device.
- **OWASP Path Traversal Defenses**: Built-in protection against Zip Slip and directory traversal attacks during local archive inspection and extraction.
- **High-Performance In-Memory Search Engine**: Sub-millisecond fuzzy search with synonym dictionaries and alias expansion.
- **191 Automated Unit Tests**: 100% test pass rate covering algorithms, edge cases, cryptographic vectors, and exploit defenses.

---

## Utility Catalog (75 Tools)

### 1. Developer & Network Tools (17 Tools)
- **Multi-Tool Pipeline Chainer**: Chain multiple offline text, crypto, and developer transformations into an atomic sequential pipeline with an audit trail.
- **IPv4 Subnet & CIDR Calculator**: Calculate network addresses, broadcast IPs, usable host ranges, wildcard masks, and CIDR subnet details.
- **MAC Address Formatter & Inspector**: Normalize MAC addresses across 4 notations (colon, hyphen, Cisco dot, raw), check multicast/local bits, and identify OUI vendors.
- **Cron Expression Explainer & Schedule Simulator**: Translate 5-field cron schedules into plain English and project upcoming run times.
- **JSON Formatter & Validator**: Format, prettify, compact, and validate JSON syntax with error pointers.
- **Base64 Converter**: Standard and URL-safe Base64 encoding/decoding with padding controls.
- **Base32 Encoder & Decoder (RFC 4648)**: Encode and decode RFC 4648 Base32 and Base32Hex strings used in TOTP 2FA keys.
- **JWT (JSON Web Token) Decoder**: Decode header, payload, and inspect expiration timestamps locally without network calls.
- **URL Encoder & Decoder**: RFC 3986 percent-encoding for URLs and query parameters.
- **HTML Entity Encoder & Decoder**: Encode and decode standard HTML named and numeric character references.
- **Unix Epoch Timestamp Converter**: Convert between Unix epoch seconds/milliseconds and ISO 8601 human-readable dates.
- **Number Base Converter**: Convert across Binary (Base 2), Octal (Base 8), Decimal (Base 10), and Hexadecimal (Base 16).
- **Regex Tester & Evaluator**: Real-time regular expression testing, group extraction, and match counting.
- **SQL Query Formatter**: Format and beautify complex SQL statements with keyword uppercase normalization.
- **XML Formatter & Prettifier**: Indent and structure XML documents with syntax validation.
- **Markdown Text Inspector**: Word, character, and line counts with markdown structural summaries.
- **HTML to Markdown Converter**: Convert HTML markup into clean, readable GitHub-Flavored Markdown offline.

### 2. Security & Privacy (7 Tools)
- **HMAC Message Authentication Generator**: Calculate cryptographic HMAC-SHA256, HMAC-SHA512, HMAC-SHA1, and HMAC-MD5 signatures with text or hex secret keys.
- **RSA Key Pair & PEM Generator**: Generate on-device RSA 1024, 2048, and 4096-bit public/private key pairs with standard PKCS#8 / X.509 PEM export.
- **Cryptographic Hash Generator**: MD5, SHA-1, SHA-256, SHA-384, and SHA-512 with uppercase/lowercase hex toggles.
- **Hash & Checksum Verifier**: Compare input text hashes against expected checksums with verification status indicators.
- **Secure Password & Passphrase Generator**: Cryptographically secure RNG (`SecureRandom`) supporting uppercase, lowercase, digits, symbols, and multi-word passphrases.
- **Password Strength Evaluator**: Offline entropy calculation, zxcvbn-inspired pattern scoring, crack time estimates, and actionable improvement tips.
- **UUID / GUID v4 Generator**: Generate single or batch cryptographically random UUID v4 identifiers with uppercase and hyphen toggles.

### 3. Data & Formats (5 Tools)
- **YAML <-> JSON Converter**: Bi-directional conversion between YAML documents and structured JSON payloads offline.
- **CSV Data Profiler & Column Statistics**: Analyze CSV datasets with column data type inference, null counts, unique cardinality, and numeric averages.
- **CSV to JSON Converter**: Parse delimiter-separated values into formatted JSON arrays.
- **JSON to CSV Converter**: Flatten JSON object arrays into standard comma-separated tabular data.
- **CSV Filter, Query & Sort Engine**: Query and filter CSV rows by numeric and text conditions, sort columns, deduplicate, and reshape datasets.

### 4. Color & Design (2 Tools)
- **Harmonious Color Palette Generator**: Generate complementary, triadic, analogous, tetradic, split-complementary, and monochromatic color palettes in HSL space.
- **Color Converter & WCAG Contrast Checker**: Convert across HEX, RGB, and HSL formats with WCAG 2.1 AA/AAA contrast ratio compliance evaluation.

### 5. Image, Media & Forensics (3 Tools)
- **Aspect Ratio & Resolution Scaler**: Calculate aspect ratios (16:9, 4:3, 21:9, etc.), detect display standards (Full HD, 4K UHD), and scale dimensions proportionally.
- **EXIF Metadata & Privacy Inspector**: Decode hidden camera EXIF tags, capture timestamps, device models, and detect GPS privacy leaks in photos.
- **Offline QR Code Matrix Generator**: Generate 2D QR code binary matrices, ASCII art representations, and SVG vector graphics offline (ISO/IEC 18004).

### 6. Math, Finance & Everyday Calculations (10 Tools)
- **Fuel Cost & Road Trip Splitter**: Calculate vehicle fuel consumption, road trip expenses, cost per mile/km, and split among passengers.
- **GPA & Academic Grade Calculator**: Calculate semester GPA, cumulative CGPA, credit-weighted averages, and honors status across 4.0, 5.0, and 10.0 scales.
- **Loan EMI & Amortization Calculator**: Calculate monthly loan EMIs, interest breakdowns, prepayment savings, and month-by-month amortization schedules.
- **Percentage Calculator**: 4 calculation modes including percentage of value, percentage increase/decrease, reverse percentages, and ratios.
- **Tip & Bill Splitter**: Calculate tips, split bills evenly across party members, and round totals.
- **BMI & Health Metric Calculator**: Metric and Imperial Body Mass Index with WHO classification and healthy weight ranges.
- **Average & Statistical Summary**: Mean, median, mode, sample variance, standard deviation, min, max, and range.
- **Fraction Calculator**: Add, subtract, multiply, and divide fractions with mixed number and decimal conversions.
- **Compound Interest Calculator**: Future value projections, compound frequencies (daily, monthly, quarterly, annually), and interest earned.
- **Discount & Sales Tax Calculator**: Calculate final discounted prices with sales tax additions and savings breakdowns.

### 7. Text & Writing (13 Tools)
- **Binary & Hex Multi-Stream Converter**: Simultaneous multi-representation stream converter between Text, 8-bit Binary, Hex, and Decimal bytes.
- **Leetspeak Generator & Decoder**: Transforms text into hacker leetspeak (1337) across Basic, Intermediate, and Advanced symbol levels, with decoding support.
- **Text Case Converter**: Sentence case, lowercase, UPPERCASE, Title Case, camelCase, snake_case, kebab-case, and CONSTANT_CASE.
- **Word & Character Counter**: Comprehensive metrics including word count, character count, syllable estimates, reading time, and speaking time.
- **Text Cleaner & Normalizer**: Strip extra spaces, collapse empty lines, remove non-ASCII characters, and normalize whitespace.
- **Sort & Line Operations**: Sort lines alphabetically or numerically, reverse order, deduplicate, and trim lines.
- **Find & Replace**: Case-sensitive and case-insensitive substring search and replace.
- **Lorem Ipsum Dummy Text Generator**: Generate customizable dummy paragraphs, sentences, or words for layout prototyping.
- **Text Diff Comparator**: Side-by-side line-by-line comparison highlighting additions, deletions, and modifications.
- **URL Slug Generator**: Create clean, SEO-friendly kebab-case URL slugs with punctuation stripping.
- **Unicode & String Inspector**: Inspect individual code points, UTF-8/UTF-16 byte sizes, and character categories.
- **ROT13 & Caesar Cipher**: Classic rotational substitution ciphers with customizable rotation offsets (1-25).
- **Morse Code Translator & Timing Engine**: Bi-directional translation between text and International Morse Code with ITU-R timing unit calculations.
- **ASCII Art Banner & Box Generator**: Generate large 5-row block font banners and decorative Unicode box borders (Single, Double, Rounded, ASCII).

### 8. File Utilities & Forensics (3 Tools)
- **ZIP Archiver & Security Inspector**: Create ZIP archives, inspect compression manifests, and validate against NIST/OWASP Zip Slip path traversal vulnerabilities.
- **File & Data Checksum Calculator**: Compute and verify streaming chunked MD5, SHA-1, SHA-256, SHA-512, and CRC32 hashes on text or Base64 payloads.
- **Offline PDF Document Builder**: Generate standard multi-page PDF documents from text, notes, or code with customizable margins, fonts, and pagination.

### 9. Unit Converters (8 Tools)
- **Length & Distance**: Millimeters, Centimeters, Meters, Kilometers, Inches, Feet, Yards, and Miles.
- **Weight & Mass**: Milligrams, Grams, Kilograms, Metric Tons, Ounces, and Pounds.
- **Temperature**: Celsius, Fahrenheit, and Kelvin conversions.
- **Data Storage**: Decimal (KB, MB, GB, TB) and Binary (KiB, MiB, GiB, TiB) conversions with IEEE compliance.
- **Speed & Velocity**: m/s, km/h, mph, knots, and ft/s.
- **Roman Numerals**: Convert between integers (1 to 3999) and standard Roman numerals.
- **Area & Land**: Square Meters, Square Kilometers, Square Feet, Acres, and Hectares.
- **Volume & Liquid**: Milliliters, Liters, Cubic Meters, US Fluid Ounces, US Gallons, and Imperial Gallons.

### 10. Date & Time (3 Tools)
- **Date Difference Calculator**: Exact durations in years, months, weeks, days, and total hours between two calendar dates.
- **Date Add & Subtract**: Add or subtract days, weeks, months, or years, with an optional business days only mode (skipping weekends).
- **Countdown & Milestone Tracker**: Live countdowns to future milestones and target dates with percentage progress bars.

### 11. Random Generators (3 Tools)
- **Random Number Generator**: Cryptographically secure integer and floating-point random number generator with custom ranges and unique sets.
- **Random Choice Picker & Dice Roller**: Pick random items from a custom list or simulate multi-sided dice rolls (D4, D6, D8, D10, D12, D20, D100).
- **QR Code Payload Builder**: Construct standardized, offline QR code payload strings for WiFi network join, vCard contacts, SMS, Email, and Geo-coordinates.

---

## Multi-Tool Pipeline Engine

The `PipelineEngine` enables users to configure multi-step sequential execution pipelines where output streams from one tool are validated and piped as inputs into subsequent utilities:

```
[Raw Text] → [Text Cleaner] → [Case Converter] → [Base64 Encoder] → [SHA-256 Hash]
```

### Built-in Recipes:
1. **Clean & Sluggify URL**: Trim & sanitize text -> lower case -> clean kebab-case URL slug.
2. **Hacker Obfuscator**: Convert to Leetspeak (1337) -> apply ROT13 cipher -> encode into Base64.
3. **Dev Payload Checksum**: Normalize whitespace -> Base64 encode -> compute SHA-256 fingerprint.
4. **Morse Code & Hex Stream**: International Morse translation -> 8-bit binary & hex byte stream.

Each execution pass captures an audit log of intermediate execution outputs, step durations, and validation health.

---

## Technical Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.1.0 |
| **Android SDK** | Min SDK 26 (Android 8.0) / Target SDK 35 (Android 15) |
| **UI Framework** | Jetpack Compose (BOM 2024.12.01) with Material 3 |
| **Local Database** | Room 2.6.1 with KSP code generator |
| **Preferences** | Jetpack DataStore Preferences 1.1.1 |
| **Concurrency** | Kotlin Coroutines 1.9.0 |
| **Testing** | JUnit 4, Turbine, Coroutines Test |

---

## Automated Testing & Quality

All 75 utilities and engine subsystems are verified with automated unit tests covering edge cases, boundary values, security sanitization, and cryptographic vectors:

```bash
# Run all unit tests
./gradlew testDebugUnitTest
```

**Results:**
- **191 tests completed**
- **191 tests passed**
- **0 failures, 0 skipped (100% pass rate)**

---

## Building from Source

### Prerequisites
- JDK 21 (e.g. JetBrains Runtime / OpenJDK 21)
- Android SDK Platform 35
- Gradle 8.7+

### Build Commands
```bash
# Compile and package debug APK
./gradlew assembleDebug

# Output APK location
# app/build/outputs/apk/debug/app-debug.apk
```

---

## License

This project is licensed under the Apache License 2.0.
