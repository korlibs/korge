package com.soywiz.korim.text

import com.soywiz.kds.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korio.lang.*

data class RichTextData(
    val lines: List<Line>
) : List<RichTextData.Line> by lines, Extra by Extra.Mixin() {
    constructor(vararg lines: Line) : this(lines.toList())

    val text: String by lazy { lines.joinToString("\n") { it.text } }

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
    data class Line(val nodes: List<Node>) : Extra by Extra.Mixin() {
        constructor(vararg nodes: Node) : this(nodes.toList())
        val defaultNodeStyle: Node by lazy {
            nodes.firstOrNull() ?: Node("", textSize = 16.0, font = DefaultTtfFont)
        }
        val text: String by lazy { nodes.joinToString("") { it.text } }
        val width: Double by lazy { nodes.sumOf { it.bounds.width } }
        val maxLineHeight: Double by lazy { nodes.maxOf { it.bounds.lineHeight } }
        val maxHeight: Double by lazy { nodes.maxOf { it.bounds.height } }
    }

    data class Node(
        val text: String,
        val textSize: Double,
        val font: Font,
        val italic: Boolean = false,
        val bold: Boolean = false,
        val underline: Boolean = false,
        val color: RGBA = Colors.BLACK,
        val canBreak: Boolean = true,
    ) : Extra by Extra.Mixin() {
        init {
            require(!text.contains('\n')) { "Single RichTextData nodes cannot have line breaks" }
        }

        val bounds by lazy { font.getTextBounds(textSize, text) }
    }

    fun limit(maxLineWidth: Double = Double.POSITIVE_INFINITY, maxHeight: Double = Double.POSITIVE_INFINITY, includePartialLines: Boolean = true, ellipsis: String? = null): RichTextData {
        var out = this
        var removedWords = false
        if (maxLineWidth != Double.POSITIVE_INFINITY) {
            out = out.wordWrap(maxLineWidth)
        }
        if (maxHeight != Double.POSITIVE_INFINITY) {
            out = out.limitHeight(maxHeight, includePartialLines = includePartialLines).also {
                if (it != out) removedWords = true
            }
        }
        if (maxLineWidth != Double.POSITIVE_INFINITY && ellipsis != null && removedWords && out.lines.isNotEmpty()) {
            val line = out.lines.last()
            val lastLine = fitEllipsis(maxLineWidth, out.lines.last(), line.defaultNodeStyle.copy(text = ellipsis))
            out = RichTextData(out.dropLast(1) + lastLine)
        }
        return out
    }

    fun limitHeight(maxHeight: Double, includePartialLines: Boolean = true): RichTextData {
        var currentHeight: Double = 0.0
        val outLines = arrayListOf<Line>()
        for (line in lines) {
            currentHeight += line.maxLineHeight
            if (currentHeight >= maxHeight) {
                if (includePartialLines) {
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
            // Merge
            if (currentLine.isNotEmpty() && currentLine.last().copy(text = node.text) == node && node.canBreak) {
                val lastNode = currentLine.removeLast()
                currentLineWidth -= lastNode.bounds.width
                return addNode(lastNode.copy(text = lastNode.text + node.text))
            } else {
                currentLine.add(node)
                currentLineWidth += node.bounds.width
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
                val width = node.bounds.width

                if (currentLineWidth >= maxLineWidth) {
                    finishLine()
                }

                val fullyFitsInLine = width <= maxLineWidth
                val fitsInRemainingLine = currentLineWidth + width <= maxLineWidth

                when {
                    // Node doesn't fit the area, so we will have to split into smaller chunks
                    node.canBreak && (!fullyFitsInLine || splitLetters) -> {
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

    companion object {
        internal fun fitEllipsis(maxLineWidth: Double, line: Line, addNode: Node = line.defaultNodeStyle.copy("...")): Line {
            val chunk = RichTextData(Line(listOf(addNode.copy(canBreak = false)) + line.nodes)).wordWrap(maxLineWidth, splitLetters = true)
            val nodes = chunk.lines.first().nodes
            return Line(nodes.drop(1) + nodes.first())
        }

        operator fun invoke(
            text: String,
            textSize: Double,
            font: Font,
            italic: Boolean = false,
            bold: Boolean = false,
            underline: Boolean = false,
            color: RGBA = Colors.BLACK,
        ): RichTextData {
            return RichTextData(text.split("\n").map { Line(Node(it, textSize, font, italic, bold, underline, color)) })
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
