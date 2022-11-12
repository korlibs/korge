package com.soywiz.korim.text

import com.soywiz.kds.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*

data class RichTextData(
    val lines: List<Line>
) {
    constructor(vararg lines: Line) : this(lines.toList())

    val text: String by lazy { lines.joinToString("\n") { it.text } }

    // @TODO: For now, only plain text is supported
    //constructor(vararg node: Node) : this(node.toList())

    operator fun plus(other: RichTextData): RichTextData {
        if (lines.isEmpty()) return other
        if (other.lines.isEmpty()) return this
        return RichTextData(
            lines.dropLast(1) + Line(lines.last().nodes + other.lines.first().nodes) + other.lines.drop(1),
        )
    }

    data class Line(val nodes: List<Node>) {
        constructor(vararg nodes: Node) : this(nodes.toList())

        val text: String by lazy { nodes.joinToString("") { it.text } }
        val maxHeight: Double by lazy {
            nodes.maxOf { it.bounds.height }
        }
    }

    data class Node(
        val text: String,
        val textSize: Double,
        val font: Font,
        val italic: Boolean = false,
        val bold: Boolean = false,
        val underline: Boolean = false,
        val color: RGBA = Colors.BLACK,
    ) {
        init {
            require(!text.contains('\n')) { "Single RichTextData nodes cannot have line breaks" }
        }

        val bounds by lazy { font.getTextBounds(textSize, text) }
    }

    fun wordWrap(size: ISize, overflowEllipsis: Boolean = false): RichTextData {
        val outLines = arrayListOf<Line>()
        var currentLineWidth: Double = 0.0
        val currentLine = arrayListOf<Node>()

        fun addNode(node: Node) {
            // Merge
            if (currentLine.isNotEmpty() && currentLine.last().copy(text = node.text) == node) {
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

        for (line in lines) {
            val deque = Deque<Node>().also { it.addAll(line.nodes) }
            while (deque.isNotEmpty()) {
                val node = deque.removeFirst()
                val width = node.bounds.width

                when {
                    // Node doesn't fit the area, so we will have to split into smaller chunks
                    width > size.width -> {
                        val division = divide(node.text)
                        // No more divisions possible, let's add it even if overflows (possibly only a single letter)
                        if (division.size == 1) {
                            addNode(node)
                            finishLine()
                        } else {
                            deque.addAllFirst(division.map { node.copy(text = it) })
                        }
                    }
                    // Node doesn't fit the available space in line, but will fit in the next one
                    currentLineWidth + width > size.width -> {
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
