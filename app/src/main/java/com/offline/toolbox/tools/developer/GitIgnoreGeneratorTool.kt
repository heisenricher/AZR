package com.offline.toolbox.tools.developer

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult

enum class GitIgnorePreset(val displayName: String) {
    ANDROID("Android (APK, build/, local.properties)"),
    KOTLIN("Kotlin (kotlin-js-store, .kotlin)"),
    GRADLE("Gradle (.gradle/, build/, wrapper jars)"),
    NODE("Node.js (node_modules, .npm, npm-debug.log)"),
    PYTHON("Python (__pycache__, *.pyc, .venv)"),
    JAVA("Java (*.class, *.jar, *.war)"),
    MACOS("macOS (.DS_Store, .AppleDouble)"),
    WINDOWS("Windows (Thumbs.db, desktop.ini)"),
    INTELLIJ("IntelliJ IDEA (.idea/, *.iml)"),
    VSCODE("VS Code (.vscode/*, !.vscode/settings.json)")
}

data class GitIgnoreInput(
    val selectedPresets: List<GitIgnorePreset> = listOf(
        GitIgnorePreset.ANDROID,
        GitIgnorePreset.KOTLIN,
        GitIgnorePreset.GRADLE,
        GitIgnorePreset.INTELLIJ,
        GitIgnorePreset.MACOS
    )
)

data class GitIgnoreOutput(
    val generatedGitIgnore: String,
    val totalRules: Int,
    val presetCount: Int,
    val formattedReport: String,
    val summary: String
)

class GitIgnoreGeneratorTool : Tool<GitIgnoreInput, GitIgnoreOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "gitignore_generator_tool",
        name = "GitIgnore Template & Multi-Stack Generator",
        description = "Generate standardized .gitignore files combining curated rules for Android, Kotlin, Gradle, Node.js, Python, IDEs, and OS environments.",
        category = ToolCategory.DEVELOPER,
        tags = listOf("git", "gitignore", "android", "kotlin", "gradle", "node", "python", "ignore", "version control"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "Block"
    )

    private val presetTemplates = mapOf(
        GitIgnorePreset.ANDROID to """
            # Android
            *.apk
            *.aab
            *.ap_
            *.dex
            build/
            captures/
            local.properties
            .cxx/
            *.keystore
            !debug.keystore
        """.trimIndent(),

        GitIgnorePreset.KOTLIN to """
            # Kotlin
            .kotlin/
            kotlin-js-store/
            *.kotlin_module
        """.trimIndent(),

        GitIgnorePreset.GRADLE to """
            # Gradle
            .gradle/
            build/
            !gradle/wrapper/gradle-wrapper.jar
            !**/src/main/**/build/
            !**/src/test/**/build/
        """.trimIndent(),

        GitIgnorePreset.NODE to """
            # Node.js
            node_modules/
            npm-debug.log*
            yarn-debug.log*
            yarn-error.log*
            .pnpm-debug.log*
            .env.local
            .env.development.local
            .env.test.local
            .env.production.local
        """.trimIndent(),

        GitIgnorePreset.PYTHON to """
            # Python
            __pycache__/
            *.py[cod]
            *${'$'}py.class
            *.so
            .Python
            env/
            venv/
            .venv/
            *.egg-info/
            dist/
        """.trimIndent(),

        GitIgnorePreset.JAVA to """
            # Java
            *.class
            *.log
            *.ctxt
            .mtj.tmp/
            *.jar
            *.war
            *.nar
            *.ear
            *.zip
            *.tar.gz
            *.rar
        """.trimIndent(),

        GitIgnorePreset.MACOS to """
            # macOS
            .DS_Store
            .AppleDouble
            .LSOverride
            Icon
            ._*
            .Spotlight-V100
            .Trashes
        """.trimIndent(),

        GitIgnorePreset.WINDOWS to """
            # Windows
            Thumbs.db
            Thumbs.db:encryptable
            ehthumbs.db
            ehthumbs_vista.db
            *.stackdump
            [Dd]esktop.ini
            ${'$'}RECYCLE.BIN/
        """.trimIndent(),

        GitIgnorePreset.INTELLIJ to """
            # IntelliJ IDEA
            .idea/
            *.iml
            *.ipr
            *.iws
            out/
            !options/
            !modules.xml
        """.trimIndent(),

        GitIgnorePreset.VSCODE to """
            # Visual Studio Code
            .vscode/*
            !.vscode/settings.json
            !.vscode/tasks.json
            !.vscode/launch.json
            !.vscode/extensions.json
            *.code-workspace
        """.trimIndent()
    )

    override suspend fun execute(input: GitIgnoreInput): ToolResult<GitIgnoreOutput> {
        val startTime = System.currentTimeMillis()
        val presets = if (input.selectedPresets.isNotEmpty()) input.selectedPresets else listOf(GitIgnorePreset.ANDROID)

        val sb = StringBuilder()
        sb.appendLine("# ==================================================")
        sb.appendLine("# .gitignore generated by AZR Offline Toolbox")
        sb.appendLine("# Stacks: ${presets.joinToString(", ") { it.name }}")
        sb.appendLine("# ==================================================")
        sb.appendLine()

        val rulesSet = mutableSetOf<String>()
        var totalRules = 0

        for (p in presets) {
            val template = presetTemplates[p] ?: continue
            sb.appendLine(template)
            sb.appendLine()

            template.lines().forEach { line ->
                val tr = line.trim()
                if (tr.isNotEmpty() && !tr.startsWith("#")) {
                    rulesSet.add(tr)
                    totalRules++
                }
            }
        }

        val outputText = sb.toString().trim()

        val report = buildString {
            appendLine("GITIGNORE RECIPE SUMMARY")
            appendLine("--------------------------------------------------")
            appendLine("Selected Ecosystems: ${presets.size}")
            appendLine("Presets:             ${presets.joinToString(", ") { it.name }}")
            appendLine("Total Active Rules:  ${rulesSet.size} unique patterns")
            appendLine()
            appendLine("OUTPUT .GITIGNORE:")
            appendLine(outputText)
        }

        val summary = ".gitignore with ${presets.size} preset(s) and ${rulesSet.size} rules"

        return ToolResult.Success(
            data = GitIgnoreOutput(
                generatedGitIgnore = outputText,
                totalRules = rulesSet.size,
                presetCount = presets.size,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }
}
