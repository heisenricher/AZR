package com.offline.toolbox.tools.data

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import kotlin.math.abs
import kotlin.math.max

data class BstVisualizerInput(
    val numbersList: String = "50, 30, 70, 20, 40, 60, 80, 10, 25"
)

data class BstVisualizerOutput(
    val totalNodes: Int,
    val treeHeight: Int,
    val isAvlBalanced: Boolean,
    val inOrderTraversal: List<Int>,
    val preOrderTraversal: List<Int>,
    val postOrderTraversal: List<Int>,
    val asciiTree: String,
    val formattedReport: String,
    val summary: String
)

class BinarySearchTreeVisualizerTool : Tool<BstVisualizerInput, BstVisualizerOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "bst_visualizer_tool",
        name = "Binary Search Tree (BST) Builder & Visualizer",
        description = "Build a binary search tree from numbers, analyze height, check AVL balance, and generate traversals and ASCII tree diagram.",
        category = ToolCategory.DATA,
        tags = listOf("bst", "tree", "binary search tree", "avl", "algorithm", "data structure", "traversal", "visualize"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "AccountTree"
    )

    private class Node(val value: Int) {
        var left: Node? = null
        var right: Node? = null
    }

    override suspend fun execute(input: BstVisualizerInput): ToolResult<BstVisualizerOutput> {
        val startTime = System.currentTimeMillis()
        val raw = input.numbersList.trim()

        if (raw.isEmpty()) {
            return ToolResult.Failure("Numbers list cannot be empty.")
        }

        val numbers = raw.split(",", " ", "\t", "\n")
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }

        if (numbers.isEmpty()) {
            return ToolResult.Failure("No valid integers found in input.")
        }

        var root: Node? = null
        for (num in numbers) {
            root = insert(root, num)
        }

        val height = getHeight(root)
        val isBalanced = checkBalance(root)

        val inOrder = mutableListOf<Int>()
        val preOrder = mutableListOf<Int>()
        val postOrder = mutableListOf<Int>()

        traverseInOrder(root, inOrder)
        traversePreOrder(root, preOrder)
        traversePostOrder(root, postOrder)

        val asciiBuilder = StringBuilder()
        renderAscii(root, "", true, asciiBuilder)
        val asciiStr = asciiBuilder.toString().trimEnd()

        val report = buildString {
            appendLine("BINARY SEARCH TREE (BST) AUDIT")
            appendLine("--------------------------------------------------")
            appendLine("Total Nodes:     ${numbers.size}")
            appendLine("Tree Height:     $height levels")
            appendLine("AVL Balanced:    ${if (isBalanced) "Yes (Balanced)" else "No (Skewed / Unbalanced)"}")
            appendLine()
            appendLine("TRAVERSALS:")
            appendLine("• In-order (Sorted): ${inOrder.joinToString(", ")}")
            appendLine("• Pre-order:         ${preOrder.joinToString(", ")}")
            appendLine("• Post-order:        ${postOrder.joinToString(", ")}")
            appendLine()
            appendLine("ASCII TREE DIAGRAM:")
            appendLine(asciiStr)
        }

        val summary = "BST: ${numbers.size} nodes, height $height (${if (isBalanced) "Balanced" else "Unbalanced"})"

        return ToolResult.Success(
            data = BstVisualizerOutput(
                totalNodes = numbers.size,
                treeHeight = height,
                isAvlBalanced = isBalanced,
                inOrderTraversal = inOrder,
                preOrderTraversal = preOrder,
                postOrderTraversal = postOrder,
                asciiTree = asciiStr,
                formattedReport = report,
                summary = summary
            ),
            executionTimeMs = System.currentTimeMillis() - startTime,
            summary = summary
        )
    }

    private fun insert(node: Node?, v: Int): Node {
        if (node == null) return Node(v)
        if (v < node.value) {
            node.left = insert(node.left, v)
        } else {
            node.right = insert(node.right, v)
        }
        return node
    }

    private fun getHeight(node: Node?): Int {
        if (node == null) return 0
        return 1 + max(getHeight(node.left), getHeight(node.right))
    }

    private fun checkBalance(node: Node?): Boolean {
        if (node == null) return true
        val lh = getHeight(node.left)
        val rh = getHeight(node.right)
        if (abs(lh - rh) > 1) return false
        return checkBalance(node.left) && checkBalance(node.right)
    }

    private fun traverseInOrder(node: Node?, out: MutableList<Int>) {
        if (node == null) return
        traverseInOrder(node.left, out)
        out.add(node.value)
        traverseInOrder(node.right, out)
    }

    private fun traversePreOrder(node: Node?, out: MutableList<Int>) {
        if (node == null) return
        out.add(node.value)
        traversePreOrder(node.left, out)
        traversePreOrder(node.right, out)
    }

    private fun traversePostOrder(node: Node?, out: MutableList<Int>) {
        if (node == null) return
        traversePostOrder(node.left, out)
        traversePostOrder(node.right, out)
        out.add(node.value)
    }

    private fun renderAscii(node: Node?, prefix: String, isTail: Boolean, sb: StringBuilder) {
        if (node == null) return
        sb.appendLine(prefix + (if (isTail) "└── " else "├── ") + node.value)
        val children = listOfNotNull(node.left, node.right)
        for (i in children.indices) {
            val isLast = (i == children.size - 1)
            renderAscii(children[i], prefix + (if (isTail) "    " else "│   "), isLast, sb)
        }
    }
}
