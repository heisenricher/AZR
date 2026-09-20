package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

enum class JsonDiffOp {
    ADD,
    REMOVE,
    REPLACE
}

data class JsonDiffEntry(
    val op: JsonDiffOp,
    val path: String,
    val oldValue: Any? = null,
    val newValue: Any? = null
)

data class JsonDiffInput(
    val originalJson: String = "{\n  \"name\": \"Alice\",\n  \"age\": 30,\n  \"skills\": [\"Kotlin\", \"Android\"]\n}",
    val modifiedJson: String = "{\n  \"name\": \"Alice\",\n  \"age\": 31,\n  \"city\": \"Tokyo\",\n  \"skills\": [\"Kotlin\", \"Jetpack Compose\"]\n}"
)

data class JsonDiffOutput(
    val totalDifferences: Int,
    val additionsCount: Int,
    val deletionsCount: Int,
    val modificationsCount: Int,
    val differences: List<JsonDiffEntry>,
    val rfc6902PatchJson: String,
    val formattedReport: String,
    val summary: String
)

class JsonDiffTool : Tool<JsonDiffInput, JsonDiffOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "json_diff_tool",
        name = "JSON Structural Tree Diff & Patch",
        description = "Compare two JSON payloads structurally to detect additions, deletions, replacements, and generate RFC 6902 JSON Patch.",
        category = ToolCategory.DATA,
        tags = listOf("json", "diff", "compare", "patch", "rfc6902", "tree", "changes", "structural"),
        inputType = ToolDataType.JSON,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Difference"
    )

    override suspend fun execute(input: JsonDiffInput): ToolResult<JsonDiffOutput> {
        val startTime = System.currentTimeMillis()

        val origObj = parseJson(input.originalJson)
            ?: return ToolResult.Failure("Original JSON is invalid or malformed.")
        val modObj = parseJson(input.modifiedJson)
            ?: return ToolResult.Failure("Modified JSON is invalid or malformed.")

        val diffs = mutableListOf<JsonDiffEntry>()
        compareNodes("", origObj, modObj, diffs)

        val adds = diffs.count { it.op == JsonDiffOp.ADD }
        val dels = diffs.count { it.op == JsonDiffOp.REMOVE }
        val mods = diffs.count { it.op == JsonDiffOp.REPLACE }

        val patchArray = JSONArray()
        for (d in diffs) {
            val patchOp = JSONObject()
            when (d.op) {
                JsonDiffOp.ADD -> {
                    patchOp.put("op", "add")
                    patchOp.put("path", d.path)
                    patchOp.put("value", d.newValue)
                }
                JsonDiffOp.REMOVE -> {
                    patchOp.put("op", "remove")
                    patchOp.put("path", d.path)
                }
                JsonDiffOp.REPLACE -> {
                    patchOp.put("op", "replace")
                    patchOp.put("path", d.path)
                    patchOp.put("value", d.newValue)
                }
            }
            patchArray.put(patchOp)
        }

        val patchJson = patchArray.toString(2)

        val report = buildString {
            appendLine("JSON STRUCTURAL TREE DIFF REPORT")
            appendLine("--------------------------------------------------")
            appendLine("Total Changes:    ${diffs.size} (+$adds, -$dels, ~$mods)")
            appendLine()
            if (diffs.isEmpty()) {
                appendLine("✓ Both JSON documents are structurally identical.")
            } else {
                appendLine("Differences:")
                diffs.forEach { d ->
                    when (d.op) {
                        JsonDiffOp.ADD -> appendLine("  + [ADD]     ${d.path} → ${d.newValue}")
                        JsonDiffOp.REMOVE -> appendLine("  - [REMOVE]  ${d.path} (was: ${d.oldValue})")
                        JsonDiffOp.REPLACE -> appendLine("  ~ [REPLACE] ${d.path} : ${d.oldValue} → ${d.newValue}")
                    }
                }
                appendLine()
                appendLine("RFC 6902 JSON Patch Specification:")
                appendLine(patchJson)
            }
        }

        val summary = if (diffs.isEmpty()) "Identical JSON documents" else "Found ${diffs.size} difference(s) (+$adds, -$dels, ~$mods)"

        return ToolResult.Success(
            data = JsonDiffOutput(
                totalDifferences = diffs.size,
                additionsCount = adds,
                deletionsCount = dels,
                modificationsCount = mods,
                differences = diffs,
                rfc6902PatchJson = patchJson,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun parseJson(s: String): Any? {
        val trimmed = s.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val tokener = JSONTokener(trimmed)
            tokener.nextValue()
        } catch (_: Exception) {
            null
        }
    }

    private fun compareNodes(path: String, orig: Any?, mod: Any?, diffs: MutableList<JsonDiffEntry>) {
        if (orig == null && mod == null) return

        if (orig == null && mod != null) {
            diffs.add(JsonDiffEntry(JsonDiffOp.ADD, path.ifEmpty { "/" }, null, mod))
            return
        }
        if (orig != null && mod == null) {
            diffs.add(JsonDiffEntry(JsonDiffOp.REMOVE, path.ifEmpty { "/" }, orig, null))
            return
        }

        if (orig is JSONObject && mod is JSONObject) {
            val origKeys = orig.keys().asSequence().toSet()
            val modKeys = mod.keys().asSequence().toSet()

            for (k in origKeys - modKeys) {
                diffs.add(JsonDiffEntry(JsonDiffOp.REMOVE, "$path/$k", orig.opt(k), null))
            }
            for (k in modKeys - origKeys) {
                diffs.add(JsonDiffEntry(JsonDiffOp.ADD, "$path/$k", null, mod.opt(k)))
            }
            for (k in origKeys.intersect(modKeys)) {
                compareNodes("$path/$k", orig.opt(k), mod.opt(k), diffs)
            }
        } else if (orig is JSONArray && mod is JSONArray) {
            val maxLen = maxOf(orig.length(), mod.length())
            for (i in 0 until maxLen) {
                if (i >= orig.length()) {
                    diffs.add(JsonDiffEntry(JsonDiffOp.ADD, "$path/$i", null, mod.opt(i)))
                } else if (i >= mod.length()) {
                    diffs.add(JsonDiffEntry(JsonDiffOp.REMOVE, "$path/$i", orig.opt(i), null))
                } else {
                    compareNodes("$path/$i", orig.opt(i), mod.opt(i), diffs)
                }
            }
        } else {
            if (orig.toString() != mod.toString()) {
                diffs.add(JsonDiffEntry(JsonDiffOp.REPLACE, path.ifEmpty { "/" }, orig, mod))
            }
        }
    }
}
