package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class SqliteTuningProfile(val displayName: String, val description: String) {
    ANDROID_ROOM_RECOMMENDED(
        "Android Room / Mobile App",
        "WAL mode with synchronous=NORMAL. Concurrent reads during write transactions with minimal battery drain."
    ),
    HIGH_CONCURRENCY_READ_HEAVY(
        "High-Concurrency Read-Heavy",
        "WAL mode, large 256MB mmap_size, 64MB cache, and 10s busy timeout for multi-threaded access."
    ),
    MAX_DURABILITY_FINANCIAL(
        "Maximum Durability / ACID Strict",
        "synchronous=EXTRA/FULL with WAL or ROLLBACK journal. Guarantees power-loss durability at write speed cost."
    ),
    IN_MEMORY_FAST_INGESTION(
        "Ultra-Fast Batch Ingest / Ephemeral",
        "synchronous=OFF with memory temp storage. Max insert speed for temporary caches, bulk loads, or ETL."
    ),
    CUSTOM("Custom Settings", "Fine-tune individual PRAGMA options manually.")
}

data class SqlitePragmaInput(
    val profile: SqliteTuningProfile = SqliteTuningProfile.ANDROID_ROOM_RECOMMENDED,
    val customJournalMode: String = "WAL",
    val customSynchronous: String = "NORMAL",
    val customCacheSizeKiB: Int = 64000,
    val customMmapSizeMB: Int = 128,
    val foreignKeys: Boolean = true,
    val busyTimeoutMs: Int = 5000
)

data class PragmaDirectiveDetail(
    val pragmaCommand: String,
    val category: String,
    val explanation: String
)

data class SqlitePragmaOutput(
    val profileName: String,
    val generatedSqlScript: String,
    val directives: List<PragmaDirectiveDetail>,
    val readSpeedRating: String,
    val writeSpeedRating: String,
    val crashSafetyRating: String,
    val formattedReport: String,
    val summary: String
)

class SqlitePragmaInspectorTool : Tool<SqlitePragmaInput, SqlitePragmaOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "sqlite_pragma_inspector_tool",
        name = "SQLite PRAGMA Optimizer & Tuning Inspector",
        description = "Generate, analyze, and optimize SQLite PRAGMA commands for WAL mode, concurrency, memory cache, and durability profiles.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("sqlite", "pragma", "database", "wal", "performance", "tuning", "cache", "android", "room", "sql"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Database"
    )

    override suspend fun execute(input: SqlitePragmaInput): ToolResult<SqlitePragmaOutput> {
        val startTime = System.currentTimeMillis()

        val directives = mutableListOf<PragmaDirectiveDetail>()
        val sqlBuilder = StringBuilder()

        val journalMode: String
        val synchronous: String
        val cacheSize: Int
        val mmapSize: Long
        val foreignKeys: Boolean
        val busyTimeout: Int

        when (input.profile) {
            SqliteTuningProfile.ANDROID_ROOM_RECOMMENDED -> {
                journalMode = "WAL"
                synchronous = "NORMAL"
                cacheSize = -64000 // Negative means KiB in SQLite (-64000 = ~64MB)
                mmapSize = 67108864L // 64 MB
                foreignKeys = true
                busyTimeout = 5000
            }
            SqliteTuningProfile.HIGH_CONCURRENCY_READ_HEAVY -> {
                journalMode = "WAL"
                synchronous = "NORMAL"
                cacheSize = -128000 // 128 MB
                mmapSize = 268435456L // 256 MB
                foreignKeys = true
                busyTimeout = 10000
            }
            SqliteTuningProfile.MAX_DURABILITY_FINANCIAL -> {
                journalMode = "WAL"
                synchronous = "FULL"
                cacheSize = -32000
                mmapSize = 0L // Disable mmap for strict sync
                foreignKeys = true
                busyTimeout = 15000
            }
            SqliteTuningProfile.IN_MEMORY_FAST_INGESTION -> {
                journalMode = "MEMORY"
                synchronous = "OFF"
                cacheSize = -256000 // 256 MB
                mmapSize = 536870912L // 512 MB
                foreignKeys = false
                busyTimeout = 30000
            }
            SqliteTuningProfile.CUSTOM -> {
                journalMode = input.customJournalMode.uppercase()
                synchronous = input.customSynchronous.uppercase()
                cacheSize = -input.customCacheSizeKiB.coerceAtLeast(1000)
                mmapSize = input.customMmapSizeMB.toLong() * 1024L * 1024L
                foreignKeys = input.foreignKeys
                busyTimeout = input.busyTimeoutMs.coerceIn(500, 60000)
            }
        }

        fun addPragma(cmd: String, cat: String, expl: String) {
            directives.add(PragmaDirectiveDetail(cmd, cat, expl))
            sqlBuilder.appendLine("$cmd;")
        }

        addPragma(
            "PRAGMA journal_mode = $journalMode",
            "Concurrency",
            if (journalMode == "WAL") "Write-Ahead Logging permits readers to proceed concurrently without blocking writers." else "Classic rollback journal."
        )
        addPragma(
            "PRAGMA synchronous = $synchronous",
            "Durability",
            when (synchronous) {
                "NORMAL" -> "Safe for application and OS crashes with WAL mode; only power outage before checkpoint can roll back recent commits."
                "FULL" -> "Strict fsync() on each transaction. 100% power-loss safe."
                "OFF" -> "Operating system handles syncing asynchronously. Max throughput but database corruptible if OS crashes."
                else -> "Custom sync level."
            }
        )
        addPragma(
            "PRAGMA cache_size = $cacheSize",
            "Memory",
            "Allocates ${kotlin.math.abs(cacheSize)} KiB of in-memory page cache to minimize disk I/O."
        )
        if (mmapSize > 0) {
            addPragma(
                "PRAGMA mmap_size = $mmapSize",
                "I/O Optimization",
                "Memory-maps up to ${mmapSize / (1024 * 1024)} MB of database directly into process virtual memory."
            )
        }
        addPragma(
            "PRAGMA foreign_keys = ${if (foreignKeys) "ON" else "OFF"}",
            "Integrity",
            if (foreignKeys) "Enforces relational foreign key constraint validation." else "Disables foreign key checks for faster ingestion."
        )
        addPragma(
            "PRAGMA busy_timeout = $busyTimeout",
            "Locking",
            "Sleeps and retries up to $busyTimeout ms when database is locked by concurrent transaction before throwing SQLITE_BUSY."
        )
        addPragma(
            "PRAGMA temp_store = MEMORY",
            "Temp Storage",
            "Stores temporary tables, indices, and view materializations in RAM instead of disk."
        )

        val (readScore, writeScore, crashScore) = when (journalMode) {
            "WAL" -> when (synchronous) {
                "NORMAL" -> Triple("Very Fast (Concurrent mmap)", "High (WAL appends)", "High (OS Crash Proof)")
                "FULL" -> Triple("Fast", "Moderate (fsync per commit)", "Maximum (Power-Loss Proof)")
                else -> Triple("Maximum", "Maximum", "Moderate")
            }
            "MEMORY" -> Triple("Maximum (In-RAM)", "Maximum", "Low (Process Crash Loses Data)")
            else -> Triple("Moderate", "Low (Rollback Locking)", "High")
        }

        val sqlScript = sqlBuilder.toString().trimEnd()

        val report = buildString {
            appendLine("SQLITE PRAGMA TUNING & PERFORMANCE AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Target Profile:       ${input.profile.displayName}")
            appendLine("Description:          ${input.profile.description}")
            appendLine("Read Throughput:      $readScore")
            appendLine("Write Throughput:     $writeScore")
            appendLine("Crash Durability:     $crashScore")
            appendLine("--------------------------------------------------")
            appendLine("GENERATED SQL PRAGMA SCRIPT:")
            appendLine(sqlScript)
            appendLine("--------------------------------------------------")
            appendLine("DIRECTIVE IMPACT BREAKDOWN:")
            directives.forEach { d ->
                appendLine(" • ${d.pragmaCommand} [${d.category}]")
                appendLine("   ↳ ${d.explanation}")
            }
        }

        val output = SqlitePragmaOutput(
            profileName = input.profile.name,
            generatedSqlScript = sqlScript,
            directives = directives,
            readSpeedRating = readScore,
            writeSpeedRating = writeScore,
            crashSafetyRating = crashScore,
            formattedReport = report,
            summary = "SQLite PRAGMAs: $journalMode, sync=$synchronous, cache=${kotlin.math.abs(cacheSize)} KiB"
        )

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Generated SQLite PRAGMA script for ${input.profile.displayName}"
        )
    }
}
