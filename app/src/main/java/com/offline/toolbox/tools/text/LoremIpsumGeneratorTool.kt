package com.offline.toolbox.tools.text

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.security.SecureRandom

enum class LoremUnit(val label: String) {
    PARAGRAPHS("Paragraphs"),
    SENTENCES("Sentences"),
    WORDS("Words")
}

data class LoremIpsumInput(
    val unit: LoremUnit = LoremUnit.PARAGRAPHS,
    val count: Int = 3,
    val startWithStandardLead: Boolean = true
)

class LoremIpsumGeneratorTool : Tool<LoremIpsumInput, String> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "lorem_ipsum_generator",
        name = "Lorem Ipsum Generator",
        description = "Generate placeholder Latin dummy text by paragraphs, sentences, or words.",
        category = ToolCategory.TEXT,
        tags = listOf("lorem", "ipsum", "placeholder", "dummy text", "latin", "generator", "mock"),
        inputType = ToolDataType.NONE,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.SUPPORTS_CHAINING,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "FormatQuote"
    )

    private val random = SecureRandom()

    private val words = listOf(
        "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit",
        "sed", "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore",
        "magna", "aliqua", "enim", "ad", "minim", "veniam", "quis", "nostrud",
        "exercitation", "ullamco", "laboris", "nisi", "aliquip", "ex", "ea", "commodo",
        "consequat", "duis", "aute", "irure", "in", "reprehenderit", "voluptate",
        "velit", "esse", "cillum", "fugiat", "nulla", "pariatur", "excepteur", "sint",
        "occaecat", "cupidatat", "non", "proident", "sunt", "culpa", "qui", "officia",
        "deserunt", "mollit", "anim", "id", "est", "laborum", "viverra", "maecenas",
        "accumsan", "lacus", "vel", "facilisis", "volutpat", "est", "velit", "egestas",
        "dui", "id", "ornare", "arcu", "odio", "ut", "sem", "nulla", "pharetra",
        "diam", "sit", "amet", "nisl", "suscipit", "adipiscing", "bibendum", "est"
    )

    override suspend fun execute(input: LoremIpsumInput): ToolResult<String> {
        val startTime = System.currentTimeMillis()
        val count = input.count.coerceIn(1, 100)

        val output = when (input.unit) {
            LoremUnit.WORDS -> generateWords(count, input.startWithStandardLead)
            LoremUnit.SENTENCES -> generateSentences(count, input.startWithStandardLead)
            LoremUnit.PARAGRAPHS -> generateParagraphs(count, input.startWithStandardLead)
        }

        return ToolResult.Success(
            data = output,
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = "Generated $count ${input.unit.label.lowercase()}"
        )
    }

    private fun generateWords(count: Int, startWithLead: Boolean): String {
        val list = mutableListOf<String>()
        if (startWithLead) {
            val lead = listOf("Lorem", "ipsum", "dolor", "sit", "amet,", "consectetur", "adipiscing", "elit")
            list.addAll(lead.take(count))
        }
        while (list.size < count) {
            list.add(words[random.nextInt(words.size)])
        }
        return list.joinToString(" ").replaceFirstChar { it.uppercaseChar() }
    }

    private fun generateSentences(count: Int, startWithLead: Boolean): String {
        val sentences = mutableListOf<String>()
        for (i in 0 until count) {
            if (i == 0 && startWithLead) {
                sentences.add("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.")
            } else {
                val wordCount = 8 + random.nextInt(12)
                val sentenceWords = (0 until wordCount).map { words[random.nextInt(words.size)] }
                val sentenceText = sentenceWords.joinToString(" ").replaceFirstChar { it.uppercaseChar() } + "."
                sentences.add(sentenceText)
            }
        }
        return sentences.joinToString(" ")
    }

    private fun generateParagraphs(count: Int, startWithLead: Boolean): String {
        val paragraphs = mutableListOf<String>()
        for (i in 0 until count) {
            val sentenceCount = 4 + random.nextInt(4)
            paragraphs.add(generateSentences(sentenceCount, i == 0 && startWithLead))
        }
        return paragraphs.joinToString("\n\n")
    }
}
