package com.offline.toolbox.tools.media

import com.offline.toolbox.core.model.Tool
import com.offline.toolbox.core.model.ToolCapability
import com.offline.toolbox.core.model.ToolCategory
import com.offline.toolbox.core.model.ToolDataType
import com.offline.toolbox.core.model.ToolMetadata
import com.offline.toolbox.core.model.ToolResult
import java.nio.charset.StandardCharsets
import java.util.Locale

enum class QrErrorCorrection(val levelBits: Int, val formatBits: Int, val label: String) {
    L(0b01, 0b01, "Low (~7% recovery)"),
    M(0b00, 0b00, "Medium (~15% recovery)"),
    Q(0b11, 0b11, "Quartile (~25% recovery)"),
    H(0b10, 0b10, "High (~30% recovery)")
}

data class QrMatrixInput(
    val payload: String = "",
    val errorCorrection: QrErrorCorrection = QrErrorCorrection.M
)

data class QrMatrixOutput(
    val matrix: List<List<Boolean>>,
    val dimension: Int,
    val version: Int,
    val asciiArt: String,
    val svgMarkup: String,
    val summary: String
)

class QrMatrixGeneratorTool : Tool<QrMatrixInput, QrMatrixOutput> {
    override val metadata: ToolMetadata = ToolMetadata(
        id = "qr_matrix_generator_tool",
        name = "Offline QR Code Matrix Generator",
        description = "Generate 2D QR Code binary matrices, ASCII art representations, and SVG vector graphics.",
        category = ToolCategory.MEDIA,
        tags = listOf("qr", "qrcode", "matrix", "barcode", "svg", "generator", "offline", "scan"),
        inputType = ToolDataType.TEXT,
        outputType = ToolDataType.TEXT,
        capabilities = setOf(
            ToolCapability.SUPPORTS_CLIPBOARD_COPY,
            ToolCapability.SUPPORTS_SHARE,
            ToolCapability.PURE_CALCULATION
        ),
        iconName = "QrCode2"
    )

    override suspend fun execute(input: QrMatrixInput): ToolResult<QrMatrixOutput> {
        val startTime = System.currentTimeMillis()

        if (input.payload.isBlank()) {
            return ToolResult.Failure(
                message = "Payload is empty.",
                userGuidance = "Provide text, a URL, or a payload to generate a QR matrix."
            )
        }

        return try {
            val qr = QrEncoder.encodeText(input.payload, input.errorCorrection)
            val ascii = qr.toAscii()
            val svg = qr.toSvg()

            val summary = "Generated QR Code V${qr.version} (${qr.dimension}x${qr.dimension})"

            ToolResult.Success(
                data = QrMatrixOutput(
                    matrix = qr.matrix,
                    dimension = qr.dimension,
                    version = qr.version,
                    asciiArt = ascii,
                    svgMarkup = svg,
                    summary = summary
                ),
                executionTimeMs = System.currentTimeMillis() - startTime,
                summary = summary
            )
        } catch (e: Exception) {
            ToolResult.Failure("Failed to encode QR Code: ${e.message}", cause = e)
        }
    }
}

/**
 * Pure Kotlin QR Code Engine (ISO/IEC 18004 compliant).
 * Supports Versions 1 to 6 in 8-bit Byte mode.
 */
class QrCode internal constructor(
    val version: Int,
    val dimension: Int,
    val matrix: List<List<Boolean>>
) {
    fun toAscii(): String {
        val sb = StringBuilder()
        val quietZone = 2
        val fullSize = dimension + quietZone * 2

        // Top border
        for (y in 0 until quietZone) {
            sb.append("██".repeat(fullSize)).append("\n")
        }

        for (r in 0 until dimension) {
            sb.append("██".repeat(quietZone))
            for (c in 0 until dimension) {
                sb.append(if (matrix[r][c]) "  " else "██")
            }
            sb.append("██".repeat(quietZone)).append("\n")
        }

        // Bottom border
        for (y in 0 until quietZone) {
            sb.append("██".repeat(fullSize)).append("\n")
        }

        return sb.toString()
    }

    fun toSvg(moduleSizePx: Int = 10): String {
        val quietZone = 4
        val totalModules = dimension + quietZone * 2
        val totalPx = totalModules * moduleSizePx

        val sb = StringBuilder()
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" ")
        sb.append("width=\"$totalPx\" height=\"$totalPx\" viewBox=\"0 0 $totalPx $totalPx\">\n")
        sb.append("<rect width=\"100%\" height=\"100%\" fill=\"#FFFFFF\"/>\n")
        sb.append("<path fill=\"#000000\" d=\"")

        for (r in 0 until dimension) {
            for (c in 0 until dimension) {
                if (matrix[r][c]) {
                    val x = (c + quietZone) * moduleSizePx
                    val y = (r + quietZone) * moduleSizePx
                    sb.append("M$x,${y}h${moduleSizePx}v${moduleSizePx}h-${moduleSizePx}z ")
                }
            }
        }
        sb.append("\"/>\n</svg>")
        return sb.toString()
    }
}

internal object QrEncoder {
    // Version definitions: (totalCodewords, ecCodewordsPerBlock, numBlocks) for L, M, Q, H
    private data class VersionEcSpec(
        val totalCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocks: Int
    )

    private val VERSION_SPECS = mapOf(
        // Version 1 (21x21)
        (1 to QrErrorCorrection.L) to VersionEcSpec(26, 7, 1),
        (1 to QrErrorCorrection.M) to VersionEcSpec(26, 10, 1),
        (1 to QrErrorCorrection.Q) to VersionEcSpec(26, 13, 1),
        (1 to QrErrorCorrection.H) to VersionEcSpec(26, 17, 1),
        // Version 2 (25x25)
        (2 to QrErrorCorrection.L) to VersionEcSpec(44, 10, 1),
        (2 to QrErrorCorrection.M) to VersionEcSpec(44, 16, 1),
        (2 to QrErrorCorrection.Q) to VersionEcSpec(44, 22, 1),
        (2 to QrErrorCorrection.H) to VersionEcSpec(44, 28, 1),
        // Version 3 (29x29)
        (3 to QrErrorCorrection.L) to VersionEcSpec(70, 15, 1),
        (3 to QrErrorCorrection.M) to VersionEcSpec(70, 26, 1),
        (3 to QrErrorCorrection.Q) to VersionEcSpec(70, 18, 2),
        (3 to QrErrorCorrection.H) to VersionEcSpec(70, 22, 2),
        // Version 4 (33x33)
        (4 to QrErrorCorrection.L) to VersionEcSpec(100, 20, 1),
        (4 to QrErrorCorrection.M) to VersionEcSpec(100, 18, 2),
        (4 to QrErrorCorrection.Q) to VersionEcSpec(100, 26, 2),
        (4 to QrErrorCorrection.H) to VersionEcSpec(100, 16, 4),
        // Version 5 (37x37)
        (5 to QrErrorCorrection.L) to VersionEcSpec(134, 26, 1),
        (5 to QrErrorCorrection.M) to VersionEcSpec(134, 24, 2),
        (5 to QrErrorCorrection.Q) to VersionEcSpec(134, 18, 4),
        (5 to QrErrorCorrection.H) to VersionEcSpec(134, 22, 4),
        // Version 6 (41x41)
        (6 to QrErrorCorrection.L) to VersionEcSpec(172, 18, 2),
        (6 to QrErrorCorrection.M) to VersionEcSpec(172, 16, 4),
        (6 to QrErrorCorrection.Q) to VersionEcSpec(172, 24, 4),
        (6 to QrErrorCorrection.H) to VersionEcSpec(172, 28, 4)
    )

    private val ALIGNMENT_COORDS = mapOf(
        1 to intArrayOf(),
        2 to intArrayOf(6, 18),
        3 to intArrayOf(6, 22),
        4 to intArrayOf(6, 26),
        5 to intArrayOf(6, 30),
        6 to intArrayOf(6, 34)
    )

    fun encodeText(text: String, ec: QrErrorCorrection): QrCode {
        val rawBytes = text.toByteArray(StandardCharsets.UTF_8)

        // Find smallest version that fits payload in 8-bit byte mode
        var chosenVersion = -1
        var chosenSpec: VersionEcSpec? = null

        for (v in 1..6) {
            val spec = VERSION_SPECS[v to ec] ?: continue
            val dataCapacity = spec.totalCodewords - (spec.ecCodewordsPerBlock * spec.numBlocks)
            // Header overhead: 4 bits mode + 8 bits length (for V1..9) = 12 bits -> 2 bytes overhead
            if (rawBytes.size <= dataCapacity - 2) {
                chosenVersion = v
                chosenSpec = spec
                break
            }
        }

        if (chosenVersion == -1 || chosenSpec == null) {
            throw IllegalArgumentException("Text payload too large for compact QR engine (max ~130 bytes).")
        }

        val totalDataCodewords = chosenSpec.totalCodewords - (chosenSpec.ecCodewordsPerBlock * chosenSpec.numBlocks)

        // 1. Bit stream generation
        val bitBuffer = BitBuffer()
        // Mode indicator for 8-bit byte mode: 0100
        bitBuffer.appendBits(0b0100, 4)
        // Character count indicator (8 bits for versions 1..9)
        bitBuffer.appendBits(rawBytes.size, 8)
        // Data bytes
        for (b in rawBytes) {
            bitBuffer.appendBits(b.toInt() and 0xFF, 8)
        }

        // Terminator: up to 4 zero bits
        val remainingBits = (totalDataCodewords * 8) - bitBuffer.bitLength
        val termBits = remainingBits.coerceAtMost(4).coerceAtLeast(0)
        bitBuffer.appendBits(0, termBits)

        // Byte align with zeros
        val padZeros = (8 - (bitBuffer.bitLength % 8)) % 8
        bitBuffer.appendBits(0, padZeros)

        // Pad bytes 0xEC and 0x11
        var padByte = 0xEC
        while (bitBuffer.byteLength < totalDataCodewords) {
            bitBuffer.appendBits(padByte, 8)
            padByte = if (padByte == 0xEC) 0x11 else 0xEC
        }

        val dataCodewords = bitBuffer.toByteArray()

        // 2. Error Correction Codewords per block
        val blocks = splitIntoBlocks(dataCodewords, chosenSpec.numBlocks)
        val ecBlocks = mutableListOf<ByteArray>()
        for (block in blocks) {
            ecBlocks.add(calculateReedSolomonEc(block, chosenSpec.ecCodewordsPerBlock))
        }

        // 3. Interleave data and EC codewords
        val finalCodewords = ByteArrayOutputStream()
        val maxBlockLen = blocks.maxOf { it.size }
        for (i in 0 until maxBlockLen) {
            for (b in blocks) {
                if (i < b.size) finalCodewords.write(b[i].toInt() and 0xFF)
            }
        }
        for (i in 0 until chosenSpec.ecCodewordsPerBlock) {
            for (ecB in ecBlocks) {
                if (i < ecB.size) finalCodewords.write(ecB[i].toInt() and 0xFF)
            }
        }

        val allBytes = finalCodewords.toByteArray()

        // 4. Matrix construction
        val dimension = 17 + 4 * chosenVersion
        val grid = Array(dimension) { BooleanArray(dimension) }
        val isFunction = Array(dimension) { BooleanArray(dimension) }

        placeFunctionPatterns(grid, isFunction, chosenVersion, dimension)

        // 5. Place data bits using zigzag traversal
        placeDataBits(grid, isFunction, allBytes, dimension)

        // 6. Apply standard mask (Mask 0: (row + col) % 2 == 0) and draw format bits
        applyMask(grid, isFunction, dimension)
        drawFormatInfo(grid, ec, 0, dimension)

        val resultList = grid.map { row -> row.toList() }
        return QrCode(chosenVersion, dimension, resultList)
    }

    private fun splitIntoBlocks(data: ByteArray, numBlocks: Int): List<ByteArray> {
        val baseSize = data.size / numBlocks
        val extra = data.size % numBlocks
        val blocks = mutableListOf<ByteArray>()
        var offset = 0
        for (i in 0 until numBlocks) {
            val size = baseSize + (if (i >= numBlocks - extra) 1 else 0)
            blocks.add(data.copyOfRange(offset, offset + size))
            offset += size
        }
        return blocks
    }

    private fun placeFunctionPatterns(
        grid: Array<BooleanArray>,
        func: Array<BooleanArray>,
        version: Int,
        dim: Int
    ) {
        // Finder patterns at (0,0), (dim-7, 0), (0, dim-7)
        drawFinderPattern(grid, func, 0, 0)
        drawFinderPattern(grid, func, dim - 7, 0)
        drawFinderPattern(grid, func, 0, dim - 7)

        // Separators and format info reserve
        for (r in 0..8) {
            for (c in 0..8) {
                func[r][c] = true
                func[dim - 1 - r][c] = true
                func[r][dim - 1 - c] = true
            }
        }

        // Timing patterns
        for (i in 8 until dim - 8) {
            val bit = (i % 2 == 0)
            grid[6][i] = bit
            func[6][i] = true
            grid[i][6] = bit
            func[i][6] = true
        }

        // Alignment patterns
        val coords = ALIGNMENT_COORDS[version] ?: intArrayOf()
        for (r in coords) {
            for (c in coords) {
                if (!func[r][c]) {
                    drawAlignmentPattern(grid, func, r, c)
                }
            }
        }

        // Dark module at (dim-8, 8)
        grid[dim - 8][8] = true
        func[dim - 8][8] = true
    }

    private fun drawFinderPattern(grid: Array<BooleanArray>, func: Array<BooleanArray>, r: Int, c: Int) {
        for (dr in -1..7) {
            for (dc in -1..7) {
                val row = r + dr
                val col = c + dc
                if (row in grid.indices && col in grid.indices) {
                    func[row][col] = true
                    grid[row][col] = (dr in 0..6 && dc in 0..6) && (dr == 0 || dr == 6 || dc == 0 || dc == 6 || (dr in 2..4 && dc in 2..4))
                }
            }
        }
    }

    private fun drawAlignmentPattern(grid: Array<BooleanArray>, func: Array<BooleanArray>, centerR: Int, centerC: Int) {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val r = centerR + dr
                val c = centerC + dc
                func[r][c] = true
                grid[r][c] = (dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0))
            }
        }
    }

    private fun placeDataBits(
        grid: Array<BooleanArray>,
        func: Array<BooleanArray>,
        data: ByteArray,
        dim: Int
    ) {
        var byteIndex = 0
        var bitIndex = 7
        var upwards = true
        var c = dim - 1

        while (c > 0) {
            if (c == 6) c-- // Skip vertical timing column

            val rows = if (upwards) (dim - 1 downTo 0) else (0 until dim)
            for (r in rows) {
                for (colOffset in 0..1) {
                    val col = c - colOffset
                    if (!func[r][col]) {
                        var bit = false
                        if (byteIndex < data.size) {
                            bit = ((data[byteIndex].toInt() ushr bitIndex) and 1) == 1
                            bitIndex--
                            if (bitIndex < 0) {
                                bitIndex = 7
                                byteIndex++
                            }
                        }
                        grid[r][col] = bit
                    }
                }
            }
            upwards = !upwards
            c -= 2
        }
    }

    private fun applyMask(grid: Array<BooleanArray>, func: Array<BooleanArray>, dim: Int) {
        // Mask 0: (row + col) % 2 == 0
        for (r in 0 until dim) {
            for (c in 0 until dim) {
                if (!func[r][c] && (r + c) % 2 == 0) {
                    grid[r][c] = !grid[r][c]
                }
            }
        }
    }

    private fun drawFormatInfo(grid: Array<BooleanArray>, ec: QrErrorCorrection, mask: Int, dim: Int) {
        val data = (ec.formatBits shl 3) or mask
        // 15 bits BCH format computation
        var rem = data shl 10
        val generator = 0x537
        for (i in 14 downTo 10) {
            if ((rem and (1 shl i)) != 0) {
                rem = rem xor (generator shl (i - 10))
            }
        }
        val formatBits = ((data shl 10) or rem) xor 0x5412

        // Write format bits around top-left finder, and split between top-right and bottom-left
        for (i in 0..5) grid[8][i] = ((formatBits ushr (14 - i)) and 1) == 1
        grid[8][7] = ((formatBits ushr 8) and 1) == 1
        grid[8][8] = ((formatBits ushr 7) and 1) == 1
        grid[7][8] = ((formatBits ushr 6) and 1) == 1
        for (i in 0..5) grid[5 - i][8] = ((formatBits ushr (5 - i)) and 1) == 1

        for (i in 0..7) grid[dim - 1 - i][8] = ((formatBits ushr i) and 1) == 1
        for (i in 0..7) grid[8][dim - 8 + i] = ((formatBits ushr (7 - i)) and 1) == 1
    }

    // Reed-Solomon Code Generator over GF(256)
    private val EXP = IntArray(512)
    private val LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            EXP[i] = x
            EXP[i + 255] = x
            LOG[x] = i
            x = (x shl 1)
            if (x >= 256) x = x xor 0x11D
        }
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return EXP[LOG[x] + LOG[y]]
    }

    private fun calculateReedSolomonEc(data: ByteArray, ecCount: Int): ByteArray {
        // Build generator polynomial
        var gen = intArrayOf(1)
        for (i in 0 until ecCount) {
            val root = EXP[i]
            val nextGen = IntArray(gen.size + 1)
            for (j in gen.indices) {
                nextGen[j] = nextGen[j] xor gfMul(gen[j], root)
                nextGen[j + 1] = nextGen[j + 1] xor gen[j]
            }
            gen = nextGen
        }

        // Remainder division
        val remainder = IntArray(ecCount)
        for (b in data) {
            val factor = (b.toInt() and 0xFF) xor remainder[0]
            for (i in 0 until ecCount - 1) {
                remainder[i] = remainder[i + 1] xor gfMul(gen[gen.size - 2 - i], factor)
            }
            remainder[ecCount - 1] = gfMul(gen[0], factor)
        }

        val res = ByteArray(ecCount)
        for (i in 0 until ecCount) {
            res[i] = remainder[i].toByte()
        }
        return res
    }
}

internal class BitBuffer {
    private val bytes = mutableListOf<Byte>()
    var bitLength: Int = 0
        private set

    val byteLength: Int get() = (bitLength + 7) / 8

    fun appendBits(value: Int, numBits: Int) {
        for (i in numBits - 1 downTo 0) {
            val bit = (value ushr i) and 1
            if (bitLength % 8 == 0) {
                bytes.add(0)
            }
            if (bit == 1) {
                val lastIdx = bytes.size - 1
                bytes[lastIdx] = (bytes[lastIdx].toInt() or (1 shl (7 - (bitLength % 8)))).toByte()
            }
            bitLength++
        }
    }

    fun toByteArray(): ByteArray = bytes.toByteArray()
}

internal class ByteArrayOutputStream {
    private var buf = ByteArray(32)
    private var count = 0

    fun write(b: Int) {
        if (count >= buf.size) {
            buf = buf.copyOf(buf.size * 2)
        }
        buf[count++] = b.toByte()
    }

    fun toByteArray(): ByteArray = buf.copyOf(count)
}
