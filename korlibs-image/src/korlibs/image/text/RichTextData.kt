package korlibs.image.text

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.io.lang.*

data class RichTextData(
    val lines: List<Line>,
    val defaultStyle: Style = lines.firstOrNull()?.defaultStyle ?: Style.DEFAULT
) : List<RichTextData.Line> by lines, Extra by Extra.Mixin() {
    constructor(vararg lines: Line) : this(lines.toList())

    constructor(text: String, style: Style) : this(text.split("\n").map { Line(TextNode(it, style = style)) })

    constructor(
        text: String,
        font: Font = Style.DEFAULT.font,
        textSize: Double = Style.DEFAULT.textSize,
        italic: Boolean = Style.DEFAULT.italic,
        bold: Boolean = Style.DEFAULT.bold,
        underline: Boolean = Style.DEFAULT.underline,
        color: RGBA? = Style.DEFAULT.color,
        canBreak: Boolean = Style.DEFAULT.canBreak,
    ) : this(text, style = Style(font, textSize, italic, bold, underline, color, canBreak))

    val text: String by lazy { lines.joinToString("\n") { it.text } }
    val width: Float by lazy { lines.maxOf { it.width.toDouble() }.toFloat() }
    val height: Float by lazy { lines.sumOf { it.maxLineHeight.toDouble() }.toFloat() }

    val allFonts: Set<Font> by lazy {
        mutableSetOf<Font>().also {
            for (line in lines) it.addAll(line.allFonts)
        }
    }

    // @TODO: For now, only plain text is supported
    //constructor(vararg node: Node) : this(node.toList())

    operator fun plus(other: RichTextData): RichTextData {
        if (this.lines.isEmpty()) return other
        if (other.lines.isEmpty()) return this
        return RichTextData(
            this.lines.dropLast(1) + Line(this.lines.last().nodes + other.lines.first().nodes) + other.lines.drop(1),
        )
    }

    //data class Line(val nodes: List<Node>) : List<RichTextData.Node> by nodes, Extra by Extra.Mixin() {
    data class Line(val nodes: List<Node>, val defaultLineStyle: Style? = null) : Extra by Extra.Mixin() {
        constructor(vararg nodes: Node) : this(nodes.toList())
        val defaultStyle: Style by lazy { nodes.filterIsInstance<TextNode>().firstOrNull()?.style ?: defaultLineStyle ?: Style.DEFAULT }
        val defaultLastStyle: Style by lazy { nodes.filterIsInstance<TextNode>().lastOrNull()?.style ?: defaultLineStyle ?: Style.DEFAULT }
        val text: String by lazy { nodes.joinToString("") { it.text ?: "" } }
        val width: Double by lazy { if (nodes.isNotEmpty()) nodes.sumOf { it.width } else 0.0 }
        val maxLineHeight: Double by lazy { if (nodes.isNotEmpty()) nodes.maxOf { it.lineHeight } else TextNode("", defaultStyle).lineHeight }
        val maxHeight: Double by lazy { if (nodes.isNotEmpty()) nodes.maxOf { it.height } else TextNode("", defaultStyle).height }
        val allFonts: Set<Font> by lazy {
            mutableSetOf<Font>().also {
                for (node in nodes) {
                    if (node is TextNode) it.add(node.style.font)
                }
            }
        }
        fun trimSpaces(): Line {
            val out = nodes.toDeque()
            while (true) {
                val first = out.firstOrNull()
                val last = out.lastOrNull()
                if (first is TextNode) {
                    if (first.text.isBlank()) {
                        out.removeFirst()
                        continue
                    }
                    val trimmed = first.text.trimStart()
                    if (trimmed != first.text) {
                        out.removeFirst()
                        out.addFirst(first.copy(text = trimmed))
                        continue
                    }
                }
                if (last is TextNode) {
                    if (last.text.isBlank()) {
                        out.removeLast()
                        continue
                    }
                    val trimmed = last.text.trimEnd()
                    if (trimmed != last.text) {
                        out.removeLast()
                        out.addLast(last.copy(text = trimmed))
                        continue
                    }
                }
                break
            }
            return Line(out.toList())
        }

        fun withStyle(style: Style): Line {
            return Line(nodes.map { it.withStyle(style) }, style)
        }
    }

    interface Node {
        fun withStyle(style: Style): Node = this
        val text: String?
        val width: Double
        val height: Double
        val lineHeight: Double get() = height
    }

    data class Style(
        val font: Font,
        val textSize: Double = 16.0,
        val italic: Boolean = false,
        val bold: Boolean = false,
        val underline: Boolean = false,
        val color: RGBA? = null,
        val canBreak: Boolean = true,
    ) {
        fun withText(text: String): RichTextData = RichTextData(text, this)

        companion object {
            val DEFAULT = Style(textSize = 16.0, font = DefaultTtfFont)
        }
    }

    data class TextNode(
        override val text: String,
        val style: Style = Style.DEFAULT,
    ) : Node, Extra by Extra.Mixin() {
        init {
            require(!text.contains('\n')) { "Single RichTextData nodes cannot have line breaks" }
        }

        override fun withStyle(style: Style): TextNode = TextNode(text, style)

        val bounds: TextMetrics by lazy { style.font.getTextBounds(style.textSize, text) }
        override val width: Double get() = bounds.width
        override val lineHeight: Double get() = bounds.lineHeight
        override val height: Double get() = bounds.ascent //- bounds.descent
    }

    fun trimSpaces(): RichTextData = RichTextData(lines.map { it.trimSpaces() })

    fun limit(
        maxLineWidth: Double = Double.POSITIVE_INFINITY,
        maxHeight: Double = Double.POSITIVE_INFINITY,
        includePartialLines: Boolean = true,
        ellipsis: String? = null,
        trimSpaces: Boolean = false,
        includeFirstLineAlways: Boolean = true,
    ): RichTextData {
        var out = this
        var removedWords = false
        if (maxLineWidth != Double.POSITIVE_INFINITY) {
            out = out.wordWrap(maxLineWidth)
        }
        if (maxHeight != Double.POSITIVE_INFINITY) {
            out = out.limitHeight(maxHeight, includePartialLines = includePartialLines, includeFirstLineAlways = includeFirstLineAlways).also {
                if (it != out) removedWords = true
            }
        }
        if (maxLineWidth != Double.POSITIVE_INFINITY && ellipsis != null && removedWords && out.lines.isNotEmpty()) {
            val line = out.lines.last()
            val lastLine = fitEllipsis(maxLineWidth, out.lines.last(), TextNode(ellipsis, line.defaultLastStyle))
            out = RichTextData(out.dropLast(1) + lastLine)
        }
        if (trimSpaces) {
            out = out.trimSpaces()
        }
        return out
    }

    fun limitHeight(maxHeight: Double, includePartialLines: Boolean = true, includeFirstLineAlways: Boolean = true): RichTextData {
        var currentHeight = 0.0
        val outLines = arrayListOf<Line>()
        for (line in lines) {
            currentHeight += line.maxLineHeight
            if (currentHeight >= maxHeight) {
                if (includePartialLines || (includeFirstLineAlways && outLines.isEmpty())) {
                    outLines += line
                }
                break
            }
            outLines += line
        }
        return RichTextData(outLines)
    }

    fun wordWrap(maxLineWidth: Double, splitLetters: Boolean = false): RichTextData {
        val maxLineWidth = maxLineWidth.coerceAtLeast(0.1)
        val outLines = arrayListOf<Line>()
        var currentLineWidth: Double = 0.0
        val currentLine = arrayListOf<Node>()

        fun addNode(node: Node) {
            val lastNode = currentLine.lastOrNull()
            // Merge
            if (currentLine.isNotEmpty() && node is TextNode && lastNode is TextNode && lastNode.copy(text = node.text) == node && node.style.canBreak && node.text.isNotBlank() && lastNode.text.isNotBlank()) {
                currentLine.removeLast()
                currentLineWidth -= lastNode.bounds.width
                return addNode(node.copy(text = lastNode.text + node.text))
            } else {
                currentLine.add(node)
                currentLineWidth += node.width
            }
        }

        fun finishLine() {
            if (currentLine.isEmpty()) return
            outLines += Line(currentLine.toList())
            currentLine.clear()
            currentLineWidth = 0.0
        }

        done@for (line in lines) {
            val deque = Deque<Node>().also { it.addAll(line.nodes) }
            while (deque.isNotEmpty()) {
                val node = deque.removeFirst()
                val width = node.width

                if (currentLineWidth >= maxLineWidth) {
                    finishLine()
                }

                val fullyFitsInLine = width <= maxLineWidth
                val fitsInRemainingLine = currentLineWidth + width <= maxLineWidth

                when {
                    // Node doesn't fit the area, so we will have to split into smaller chunks
                    node is TextNode && (node.style.canBreak && (!fullyFitsInLine || splitLetters)) -> {
                    //node.canBreak && !fullyFitsInLine -> {
                        val division = divide(node.text)
                        // No more divisions possible, let's add it even if overflows (possibly only a single letter)
                        if (division.size == 1) {
                            if (!fitsInRemainingLine) {
                                finishLine()
                            }
                            addNode(node)
                        } else {
                            deque.addAllFirst(division.map { node.copy(text = it) })
                        }
                    }
                    // Node doesn't fit the available space in line, but will fit in the next one
                    !fitsInRemainingLine -> {
                        finishLine()
                        addNode(node)
                    }
                    // Node fits the line
                    else -> {
                        addNode(node)
                    }
                }
            }

            finishLine()
        }
        return RichTextData(outLines)
    }

    fun withStyle(style: Style): RichTextData = RichTextData(lines.map { it.withStyle(style) })
    fun withText(text: String): RichTextData = RichTextData(text, defaultStyle)

    companion object {
        internal fun Node.nonBreakable(): Node = if (this is TextNode) this.copy(style = style.copy(canBreak = false)) else this

        internal fun fitEllipsis(maxLineWidth: Double, line: Line, addNode: Node = TextNode("...", line.defaultLastStyle)): Line {
            val chunk = RichTextData(Line(listOf(addNode.nonBreakable()) + line.nodes)).wordWrap(maxLineWidth, splitLetters = true)
            val nodes = chunk.lines.first().nodes
            return Line(nodes.drop(1) + nodes.first())
        }

        //fun Char.isSymbol(): Boolean {
        //    return when (this) {
        //        ' ', ',', '.', ';', ':', '\n', '\r', '\t' -> true
        //        else -> false
        //    }
        //}

        private val NON_WORDS = Regex("\\W+")

        fun divide(text: String): List<String> {
            if (text.isEmpty()) return emptyList()
            val parts = tokenize(text)
            if (parts.size == 1) {
                return text.splitInChunks(1)
            } else {
                return parts
            }
        }

        fun tokenize(text: String): List<String> {
            return text.splitKeep(NON_WORDS).flatMap {
                if (it.matches(NON_WORDS)) {
                    it.map { "$it" }
                } else {
                    listOf(it)
                }
            }
            /*
            val reader = StrReader(text)
            val words = arrayListOf<String>()
            while (true) {
                val char = reader.peekChar()
                if (char.isSymbol()) {
                    words += "$char"
                    continue
                }
                words += reader.readWhile { !it.isSymbol() }
            }
            return words

             */
        }
    }
}
