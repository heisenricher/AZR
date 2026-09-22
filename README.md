# AZR — Offline Android Utility Toolbox

[![100% Offline](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg)](#privacy--zero-network-guarantee)
[![Tests Passing](https://img.shields.io/badge/Unit%20Tests-423%20Passed-brightgreen.svg)](#automated-testing--quality)
[![Tools Count](https://img.shields.io/badge/Utilities-180%20Tools-blue.svg)](#utility-catalog-180-tools)
[![Android](https://img.shields.io/badge/Android-SDK%2026--35-green.svg)](#technical-stack)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-purple.svg)](#user-interface--design-system)

**AZR (Offline Android Utility Toolbox)** is a premium, privacy-first, 100% offline Swiss Army knife of utilities engineered natively for Android. Designed for everyday users, power users, and developers alike, AZR operates with an uncompromising zero-network guarantee.

---

## Key Highlights

- **180 Production-Grade Utilities**: The 180 Tools Milestone spanning Linux systemd unit validation, Dockerfile security linting, Apache .htaccess rewrite rules, Kubernetes YAML resource and security inspection, classical Polybius Square coordinate matrix ciphers, Affine modular arithmetic ciphers with coprime checks, GeoHash spatial index codecs with 8-neighbor bounding boxes, CBOR (RFC 8949) binary hex inspection and JSON conversion, Ordinary and Annuity Due financial valuation schedules, NOAA Heat Index and NWS Wind Chill apparent temperature physics, Tsiolkovsky rocket propulsion equation solvers with mission delta-v budgets, English Porter Stemmer morphological reduction, Caesar cipher automated frequency cryptanalysis, Audio SNR/THD/ENOB acoustic metrics, and HEX Porter-Duff alpha color compositing.
- **Multi-Tool Pipeline Chaining Engine (`PipelineEngine`)**: Execute multi-step sequential transformations (e.g. `Clean -> Case Convert -> Base64 -> Hash`) in a single atomic pass with intermediate audit trails.
- **Zero Network Guarantee**: Absolutely **NO** `android.permission.INTERNET` in `AndroidManifest.xml`. No telemetry, no cloud analytics, no remote SDKs.
- **On-Device Cryptography**: Classical coordinate ciphers (Polybius Square, Affine modular arithmetic), automated Caesar frequency cryptanalysis, BIP-39 mnemonic seed entropy verification, Vernam One-Time Pad cipher with CSPRNG key generation, RFC 9106 Argon2 memory/time cost sizing, RFC 7638 JWK SHA-256 thumbprints, OpenSSH public key SHA256/MD5 fingerprints, RFC 5869 HKDF Key Derivation, Polyalphabetic Vigenère with Index of Coincidence, Shamir's $(k, n)$ threshold secret sharing over $GF(256)$, RFC 6238 TOTP 2FA tokens, Diceware entropy, AES-256-GCM / CBC encryption, RSA Key Pair generation (PKCS#8 & X.509 PEM), Keyed HMAC, and multi-algorithm hashing.
- **Advanced Perceptual Contrast & Colorimetry**: Porter-Duff "Source Over" alpha compositing, CIEDE2000 ($\Delta E_{00}$) perceptual difference standard, physical pigment subtractive color mixing (CMY/RYB absorption), screen additive RGB blending, modern light-adapted APCA (WCAG 3.0), and Planckian locus blackbody Kelvin-to-sRGB chromaticity approximations.
- **OWASP Path Traversal Defenses**: Built-in protection against Zip Slip and directory traversal attacks during local archive inspection and extraction.
- **High-Performance In-Memory Search Engine**: Sub-millisecond fuzzy search with synonym dictionaries and alias expansion across all 180 utilities.
- **423 Automated Unit Tests**: 100% test pass rate covering algorithms, edge cases, cryptographic vectors, and exploit defenses.

---

## Utility Catalog (180 Tools)

### 1. Developer & Network Tools (43 Tools)
- **Systemd Service Unit Validator & Security Auditor**: Validate Linux systemd `.service` unit syntax (`[Unit]`, `[Service]`, `[Install]`), ExecStart command presence, restart policies, and audit process sandboxing directives (`NoNewPrivileges`, `ProtectSystem`, `ProtectHome`, `PrivateTmp`).
- **Dockerfile Linter & Container Security Auditor**: Analyze Dockerfiles offline for multi-stage builds, root user exposure warnings, unpinned `:latest` tags, package manager cache cleanup (`--no-cache`), and layer caching order.
- **Apache .htaccess Linter & Rewrite Validator**: Validate Apache `.htaccess` directives offline, check `RewriteEngine On`, parse `RewriteCond` expressions, validate `RewriteRule` flags (`[L]`, `[R=301]`, `[QSA]`, `[NC]`), directory indexing (`Options -Indexes`), and security headers.
- **Kubernetes Resource & Security Manifest Inspector**: Inspect Kubernetes YAML manifests (Deployments, Pods, Services), audit CPU and memory resource requests/limits, verify liveness/readiness probes, and validate pod security contexts (`runAsNonRoot`, `readOnlyRootFilesystem`, `allowPrivilegeEscalation`).
- **Nginx Configuration & Reverse Proxy Validator**: Validate Nginx server blocks, proxy_pass reverse proxy routes, detect unbalanced braces, missing semicolons, and audit security headers offline.
- **SQLite PRAGMA Optimizer & Tuning Inspector**: Generate and evaluate performance-tuned SQLite PRAGMA scripts (WAL journal mode, synchronous durability, memory-mapped I/O, cache size) for Android Room and high-concurrency systems.
- **HMAC TOTP/HOTP Key URI Builder (RFC 6238)**: Construct and parse standard `otpauth://totp/` key URIs for Google Authenticator and 2FA password managers with Base32 secret key validation.
- **POSIX Umask & File Permissions Calculator**: Calculate effective file and directory permissions from POSIX octal umasks (`0666 & ~umask` and `0777 & ~umask`) with security exposure audits.
- **HTTP Archive (.har) Offline Log Analyzer**: Parse HTTP Archive (.har) logs offline, inspect payload sizes, analyze waterfall timings (DNS, connect, TTFB, receive), and rank slowest API requests.
- **Prometheus & OpenTelemetry Metric Exposition Parser**: Parse standard Prometheus exposition text format, validate counter/gauge/histogram/summary syntax, and inspect metric label series.
- **SSH Public Key Inspector & Fingerprint Calculator**: Inspect OpenSSH public keys (ed25519, rsa, ecdsa), extract bit lengths and comments, and compute SHA-256 and MD5 fingerprints.
- **JWK Inspector & RFC 7638 Thumbprint Calculator**: Inspect RSA, EC, OKP, and oct JSON Web Keys (JWK) and calculate deterministic RFC 7638 canonical SHA-256 thumbprints.
- **cURL Command Parser & Code Generator**: Parse offline cURL shell commands into HTTP components and generate equivalent Kotlin, Python, and JavaScript snippets.
- **BGP Autonomous System Number (ASN) Directory**: Lookup offline BGP Autonomous System Numbers, convert between ASPLAIN and ASDOT notations, and classify public/private ranges.
- **GitIgnore Template & Multi-Stack Generator**: Generate standardized `.gitignore` files combining curated rules for Android, Kotlin, Gradle, Node.js, Python, IDEs, and OS environments.
- **Cron Schedule Overlap & Collision Comparator**: Compare two cron schedules offline to identify overlapping runs, concurrency conflicts, and calculate next shared executions.
- **CIDR Route Aggregation & Supernet Calculator**: Summarize multiple IPv4 subnets into the most compact aggregated CIDR blocks and analyze route table economy.
- **DNS Zone File & Resource Record Inspector**: Parse offline DNS zone files and BIND resource records (A, AAAA, CNAME, MX, TXT, NS, SOA) and audit TTLs.
- **Docker Compose YAML & Port Conflict Validator**: Validate docker-compose specifications offline, detect overlapping host port bindings, and check service dependencies.
- **SQL Dialect Translator (Postgres, MySQL, SQLite)**: Translate DDL schemas and queries between PostgreSQL, MySQL, and SQLite dialects with automatic type adaptation.
- **User-Agent Inspector & Device Classifier**: Offline User-Agent parser extracting browser family, rendering engine, OS, CPU architecture, and device class.
- **Semantic Versioning Comparator & Range Evaluator**: SemVer 2.0.0 parser, prerelease/build comparator, npm range constraint checker (`^`, `~`), and next bump calculator.
- **Unix Chmod Permissions & Octal Calculator**: Bidirectional converter between octal notation (`0755`) and symbolic flags (`rwxr-xr-x`) with SUID, SGID, and Sticky bit support.
- **HTTP Response Header Parser & Security Auditor**: HTTP response header parser and security hygiene analyzer (HSTS, CSP, X-Frame-Options, MIME sniffing).
- **Multi-Tool Pipeline Chainer**: Chain multiple offline text, crypto, and developer transformations into an atomic sequential pipeline with an audit trail.
- **IPv6 Subnet & Address Analyzer**: RFC 5952 canonical compression, full expansion, CIDR prefix subnetting, and address scope classification.
- **IANA Network Port & Protocol Directory**: Offline directory of standard IANA network ports, transport protocols (TCP/UDP), and security risk audit notes.
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

### 2. Security & Privacy (21 Tools)
- **Polybius Square 5x5 Matrix Cipher**: Encode and decode text into 2D grid coordinates (1-5, 1-5) using the classical Polybius Square cipher with optional keyword alphabet permutation.
- **Affine Modular Arithmetic Cipher**: Encrypt, decrypt, and brute-force solve the classical Affine substitution cipher using modular arithmetic $E(x) = (ax + b) \pmod{26}$, $\gcd(a, 26) = 1$ validation, and modular inverse derivation.
- **Vernam One-Time Pad (OTP) Cipher & Key Generator**: Information-theoretically unbreakable Vernam One-Time Pad cipher with CSPRNG key generator and Shannon perfect secrecy validation.
- **BIP-39 Mnemonic Seed & Entropy Validator**: Validate BIP-0039 cryptocurrency seed phrases, extract 11-bit word indexes, and verify SHA-256 entropy checksums offline against the official 2,048-word dictionary.
- **Argon2 Parameter Cost & Security Calculator**: Calculate recommended memory size ($m$), iterations ($t$), and parallelism ($p$) for Argon2id/i/d per RFC 9106.
- **Rail Fence (Zig-Zag) Cipher & Visualizer**: Encode and decode classical Rail Fence transposition ciphers with arbitrary rail depths and ASCII matrix path visualization.
- **HKDF Key Derivation Function (RFC 5869)**: Derive cryptographically strong subkeys from master keying material using RFC 5869 HMAC-Extract and HMAC-Expand (SHA-256 and SHA-512).
- **Vigenère Polyalphabetic Cipher & Cryptanalysis**: Encrypt or decrypt text using the classical Vigenère cipher and compute Index of Coincidence (IoC) for key length estimation.
- **Shamir's Secret Sharing Threshold Cryptography**: Information-theoretically secure $(k, n)$ threshold secret splitting and reconstruction over Galois Field $GF(256)$.
- **Invisible Unicode Text Steganography**: Hide confidential messages invisibly inside plain text using zero-width Unicode characters, and extract concealed payloads.
- **Time-Based One-Time Password Generator (RFC 6238 TOTP)**: Compute RFC 6238 TOTP and RFC 4226 HOTP verification codes with Base32 secret keys, custom time steps, and epoch offsets.
- **Diceware Cryptographic Passphrase Generator**: Generate ultra-secure, human-memorable multi-word passphrases with simulated 5-dice rolls and entropy calculations.
- **AES-GCM Authenticated Encryption**: On-device authenticated symmetric encryption (AES-256-GCM / CBC) with PBKDF2 key derivation and random salt/IV envelope.
- **Bcrypt Work Factor & Hash Analyzer**: Decompose bcrypt hash structures, inspect iteration work factors, and evaluate offline cracking resistance.
- **HMAC Message Authentication Generator**: Calculate cryptographic HMAC-SHA256, HMAC-SHA512, HMAC-SHA1, and HMAC-MD5 signatures with text or hex secret keys.
- **RSA Key Pair & PEM Generator**: Generate on-device RSA 1024, 2048, and 4096-bit public/private key pairs with standard PKCS#8 / X.509 PEM export.
- **Cryptographic Hash Generator**: MD5, SHA-1, SHA-256, SHA-384, and SHA-512 with uppercase/lowercase hex toggles.
- **Hash & Checksum Verifier**: Compare input text hashes against expected checksums with verification status indicators.
- **Secure Password & Passphrase Generator**: Cryptographically secure RNG (`SecureRandom`) supporting uppercase, lowercase, digits, symbols, and multi-word passphrases.
- **Password Strength Evaluator**: Offline entropy calculation, zxcvbn-inspired pattern scoring, crack time estimates, and actionable improvement tips.
- **UUID / GUID v4 Generator**: Generate single or batch cryptographically random UUID v4 identifiers with uppercase and hyphen toggles.

### 3. Data & Formats (19 Tools)
- **GeoHash Spatial Index Codec**: Encode latitude/longitude coordinates to Geohash spatial index strings, decode to coordinate bounding boxes with resolution errors, and compute 8-neighbor grids.
- **CBOR Binary Hex Inspector & JSON Converter**: Parse and inspect Concise Binary Object Representation (CBOR / RFC 8949) hex strings into structured JSON, annotated hex breakdowns, and RFC diagnostic notation.
- **OGC Well-Known Text (WKT) Geometry Parser & GeoJSON Converter**: Parse OGC WKT 2D geometries (POINT, LINESTRING, POLYGON, MULTIPOINT), calculate perimeter/area/bounding boxes, and convert to GeoJSON.
- **BitTorrent Bencode Parser & Serializer**: Parse, inspect, and serialize BitTorrent Bencode data structures (integers, byte strings, lists, dictionaries) to and from formatted JSON.
- **GeoJSON Validator & Geometry Inspector**: Validate RFC 7946 GeoJSON geometries, calculate bounding boxes [minX, minY, maxX, maxY], and compute geometric centroids.
- **Protobuf Varint & ZigZag Decoder**: Decode Protocol Buffers variable-length integers (LEB128 Varint) and signed ZigZag integers (sint32, sint64) with bitwise explanations.
- **CSV to JSON Schema Inferer**: Infer standard Draft-07 JSON Schema specifications from tabular CSV data with automatic type detection (integer, number, boolean, date-time, string).
- **Binary Search Tree (BST) Builder & Visualizer**: Build a binary search tree from numbers, analyze height, check AVL balance, and generate in/pre/post-order traversals and ASCII tree diagrams.
- **XML <-> JSON Bi-Directional Converter**: Convert XML documents to structured JSON and JSON objects to valid XML with attribute and hierarchy preservation.
- **Hex Dump & Binary Memory Inspector (xxd)**: Format binary or text data into standard 16-byte hex dump layout with offsets, hex pairs, and printable ASCII representation.
- **JSONPath Query & Value Extractor**: Lightweight offline JSONPath query engine supporting dot/bracket notation, array wildcards (`[*]`), and index slicing (`[0]`).
- **TSV <-> CSV Bi-Directional Converter**: Convert Tab-Separated Values into Comma-Separated Values and vice-versa with RFC 4180 quote escaping.
- **JSON Structural Tree Diff & Patch**: Compare two JSON payloads structurally to detect additions, deletions, replacements, and generate RFC 6902 JSON Patch.
- **NDJSON / JSON Lines Converter**: Bi-directional conversion between newline-delimited JSON (NDJSON/JSONL) streams and standard JSON arrays.
- **YAML <-> JSON Converter**: Bi-directional conversion between YAML documents and structured JSON payloads offline.
- **CSV Data Profiler & Column Statistics**: Analyze CSV datasets with column data type inference, null counts, unique cardinality, and numeric averages.
- **CSV to JSON Converter**: Parse delimiter-separated values into formatted JSON arrays.
- **JSON to CSV Converter**: Flatten JSON object arrays into standard comma-separated tabular data.
- **CSV Filter, Query & Sort Engine**: Query and filter CSV rows by numeric and text conditions, sort columns, deduplicate, and reshape datasets.

### 4. Math, Science, Finance & Health (31 Tools)
- **Ordinary & Annuity Due Calculator**: Calculate Future Value (FV), Present Value (PV), and Periodic Payment (PMT) for ordinary annuities and annuities due with compounding amortization schedules.
- **Heat Index & Wind Chill Apparent Temp Calculator**: Calculate apparent "feels-like" temperature using the NOAA Heat Index (Rothfusz equation) and NWS Wind Chill Index with clinical risk categories.
- **Tsiolkovsky Rocket Propulsion Equation Solver**: Solve the ideal rocket equation $\Delta v = I_{sp} \cdot g_0 \cdot \ln(m_0 / m_f)$ for delta-v, propellant mass, dry mass, or required specific impulse with orbital mission feasibility budgets.
- **Monte Carlo Pi Numerical Simulator & Error Estimator**: Simulate numerical integration of Pi using Monte Carlo quadrant sampling, tracking standard error and convergence trajectory.
- **Bond Yield to Maturity (YTM) & Duration Solver**: Compute exact bond Yield to Maturity using Newton-Raphson iteration, Macaulay/Modified duration, and convexity.
- **2D Kinematics & Projectile Trajectory Calculator**: Calculate ballistic trajectory, apogee, flight time, range, impact velocity, and time-stepped waypoints for 2D projectile motion.
- **Black-Scholes Option Pricing & Greeks Calculator**: Compute theoretical European Call and Put option prices and primary risk Greeks (Delta, Gamma, Theta, Vega, Rho).
- **Doppler Effect (Acoustic & Relativistic) Calculator**: Compute acoustic Doppler frequency shifts (temperature-adjusted sound speed) and relativistic optical Doppler shifts ($\beta = v/c$).
- **Extended Roman Numerals (Vinculum Notation) Converter**: Convert integers up to 3,999,999 to and from Roman numerals using the Vinculum overline notation ($\overline{\text{V}}, \overline{\text{X}}, \overline{\text{M}}$).
- **Loan Refinance & Breakeven Comparator**: Evaluate mortgage/loan refinancing offers, monthly payment reduction, closing cost breakeven horizons, and lifetime interest savings.
- **Trigonometric & Hyperbolic Function Engine**: Precise circular (sin, cos, tan, sec, csc, cot) and hyperbolic (sinh, cosh, tanh) trigonometry across Degrees, Radians, and Gradians.
- **CODATA Fundamental Scientific Constants**: Offline reference directory of 23+ fundamental physical, chemical, electromagnetic, and astronomical constants with SI units.
- **Quadratic & Cubic Polynomial Root Solver**: Calculate exact real and complex roots for quadratic and cubic polynomials with discriminant analysis and factoring.
- **2D & 3D Vector Math Engine**: Compute dot product, cross product, vector magnitude, unit vectors, angle between vectors, and projections.
- **BMR & Daily Caloric Expenditure (TDEE)**: Calculate Basal Metabolic Rate and Total Daily Energy Expenditure using Mifflin-St Jeor formula with macro targets.
- **Matrix Arithmetic & Determinant Calculator**: Compute determinant, inverse matrix, transpose, trace, and scalar multiplication for 2x2 and 3x3 matrices.
- **Gaussian & Normal Distribution Calculator**: Calculate Probability Density Function (PDF), Cumulative Distribution Function (CDF), Z-score, and 95%/99% confidence intervals.
- **Compound Annual Growth Rate (CAGR)**: Calculate annualized growth rate, absolute return percentage, net capital gains, and 10/20-year future compounding projections.
- **Scientific & Engineering Notation**: Convert numbers across Standard Decimal, Scientific, Engineering, and SI Metric prefixes (Giga, Mega, Micro, Nano).
- **Cash Denomination & Register Splitter**: Breakdown total cash amounts into optimal note and coin denominations across USD, EUR, GBP, and INR.
- **Prime Factorization & Divisors**: Decompose numbers into prime factor powers, test primality, list all divisors, and compute Euler's totient.
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

### 5. Text, Writing & Typography (28 Tools)
- **Porter Stemmer Morphological Analyzer**: Algorithmic English Porter Stemmer (Steps 1a through 5b) reducing inflected words to morphological base stems for NLP and search indexing.
- **Caesar Cipher Automated Frequency Breaker**: Automated cryptanalysis of Caesar cipher rotations (1-25) using Chi-squared ($\chi^2$) English letter frequency matching to instantly recover plaintext without a key.
- **Run-Length Encoding (RLE) Text Compression Engine**: Losslessly compress and decompress repetitive text sequences using Run-Length Encoding (RLE) with compression ratio and run statistics.
- **Levenshtein 2D Dynamic Programming Cost Matrix & Alignment Visualizer**: Compute Wagner-Fischer 2D dynamic programming matrix, Levenshtein distance, similarity score, and optimal edit alignment.
- **Soundex & Metaphone Phonetic Name Comparator**: Generate American Soundex (e.g. S530) and Philips Metaphone phonetic keys for names and evaluate phonetic similarity.
- **N-Gram Frequency & Lexical Diversity Analyzer**: Tokenize prose into unigrams, bigrams, and trigrams, compute frequency rankings, and calculate Type-Token Ratio (TTR).
- **Readability & Grade Level Analyzer**: Analyze text comprehension and grade level using Flesch Reading Ease, Flesch-Kincaid, Gunning Fog, and Coleman-Liau indexes.
- **Typographic Full Text Justifier**: Format and align paragraphs to exact column widths with typographic space distribution (book/typeset justification).
- **Markdown Table Formatter & Padded Aligner**: Format raw tabular data, CSV, TSV, or misaligned pipes into perfectly padded, aligned GitHub-Flavored Markdown tables.
- **String Distance & Similarity Comparator**: Calculate Levenshtein distance, Jaro-Winkler similarity, Hamming distance, and Sørensen-Dice bigram coefficient between text strings.
- **Text Case Style Detector & Tokenizer**: Identify identifier conventions (camelCase, PascalCase, snake_case, kebab-case, CONSTANT_CASE) and extract word tokens.
- **NATO & ICAO Radio Phonetic Alphabet**: Translate text into standard aviation/radio phonetic words (Alfa, Bravo, Charlie, Delta...).
- **Word Wrap & Paragraph Reflower**: Wrap text to specific column widths (72, 80, 100) with custom line prefixes, margins, and hanging indents.
- **Zalgo Glitch Text & Sanitizer**: Generate chaotic cursed glitch text using Unicode combining diacritics, or sanitize and strip existing zalgo artifacts.
- **Anagram & Palindrome Solver**: Verify anagrams, detect palindromes, inspect letter frequency signatures, and generate permutations.
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

### 6. Media, Audio & Acoustics (11 Tools)
- **Audio SNR, THD & ENOB Quality Calculator**: Calculate audio Signal-to-Noise Ratio (SNR), Total Harmonic Distortion (THD), Effective Number of Bits (ENOB), and theoretical quantization limits.
- **Architectural Acoustics Reverberation Time (RT60) Calculator**: Calculate architectural Sabine and Norris-Eyring reverberation times (RT60), room mode absorption, and critical listening distance.
- **Acoustic Beat & Binaural Frequency Analyzer**: Calculate interference beat frequencies ($|f_1 - f_2|$), carrier frequency, and classify brainwave states (Delta, Theta, Alpha, Beta, Gamma).
- **Acoustic Decibel & Signal Gain Engine**: Calculate Sound Pressure Level (dB SPL), audio signal voltage gain (dBu, dBV, dBm), and inverse-square distance loss.
- **Musical Interval & Acoustic Harmony Calculator**: Compute 12-TET vs Just Intonation acoustic frequencies, harmonic ratios, and cents discrepancy.
- **Metronome & BPM Tempo Tapper**: Tap tempo calculator with Italian musical markings (Allegro, Andante, Presto) and note division intervals in ms.
- **Audio Frequency & Musical Note Tuner**: Translate acoustic frequency in Hertz (Hz) to musical note names, octaves, MIDI numbers, and pitch cents tuning offsets.
- **Screen DPI & Android Density Bucket Scaler**: Calculate PPI/DPI from screen resolution and diagonal, classify Android density buckets, and convert dp <-> px.
- **Aspect Ratio & Resolution Scaler**: Calculate aspect ratios, detect display standards (Full HD, 4K UHD), and scale dimensions proportionally.
- **EXIF Metadata & Privacy Inspector**: Decode hidden camera EXIF tags, capture timestamps, device models, and detect GPS privacy leaks in photos.
- **Offline QR Code Matrix Generator**: Generate 2D QR code binary matrices, ASCII art representations, and SVG vector graphics offline (ISO/IEC 18004).

### 7. Color & Design (9 Tools)
- **HEX Alpha Color Porter-Duff Blender**: Blend translucent foreground colors over background layers using Porter-Duff "Source Over" compositing in sRGB or linear color spaces with WCAG contrast checks.
- **CIEDE2000 Color Difference (ΔE00) Calculator**: Calculate perceptual color difference using the official CIEDE2000 (ΔE00) standard and CIE76 with Just Noticeable Difference (JND) rating.
- **Subtractive & Additive Color Mixer**: Simulate physical pigment subtractive color mixing (CMY/RYB absorption) and digital screen additive RGB light mixing.
- **Color Temperature (Kelvin) to RGB Converter**: Convert Correlated Color Temperature (1,000K to 40,000K) to sRGB chromaticity using Planckian blackbody locus approximations.
- **WCAG 3 APCA & Perceptual Contrast Engine**: Evaluate color contrast using the Advanced Perceptual Contrast Algorithm (APCA / WCAG 3) alongside WCAG 2.1 AA/AAA metrics.
- **Color Vision Deficiency & Accessibility Simulator**: Simulate Protanopia, Deuteranopia, Tritanopia, and Achromatopsia via Vienot & Brettel matrices.
- **CSS & HTML Named Color Matcher**: Lookup 140+ official W3C/CSS named colors and 3D Euclidean color distance matching for nearest named color discovery.
- **Harmonious Color Palette Generator**: Generate complementary, triadic, analogous, tetradic, split-complementary, and monochromatic color palettes in HSL space.
- **Color Converter & WCAG Contrast Checker**: Convert across HEX, RGB, and HSL formats with WCAG 2.1 AA/AAA contrast ratio compliance evaluation.

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

All 180 utilities and engine subsystems are verified with automated unit tests covering edge cases, boundary values, security sanitization, and cryptographic vectors:

```bash
# Run all unit tests
./gradlew testDebugUnitTest
```

**Results:**
- **423 tests completed**
- **423 tests passed**
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
