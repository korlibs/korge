package com.soywiz.korim.text

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.*

fun RichTextData.Companion.fromHTML(
    // language=HTML prefix=<body> suffix=</body>
    html: String,
    style: RichTextData.Style = RichTextData.Style.DEFAULT
): RichTextData {
    val lines = fastArrayListOf<RichTextData.Line>()
    val nodes = fastArrayListOf<RichTextData.Node>()
    fun flushLine() {
        if (nodes.isEmpty()) return
        lines.add(RichTextData.Line(nodes.toList()))
    }
    fun processNode(node: Xml, style: RichTextData.Style) {
        if (node.nameLC == "br") {
            flushLine()
            return
        }
        if (node.isText) {
            nodes += RichTextData.TextNode(node.text, style)
        }
        var rstyle = style
        when (node.nameLC) {
            "b", "strong" -> rstyle = rstyle.copy(bold = true)
            "i", "em" -> rstyle = rstyle.copy(italic = true)
            "font", "span" -> {
                val textSize = node.doubleNull("size")
                rstyle = rstyle.copy(textSize = textSize ?: rstyle.textSize)
            }
        }
        for (child in node.allChildrenNoComments) {
            processNode(child, rstyle)
        }
    }
    processNode(Xml("<div>$html</div>"), style)
    flushLine()
    return RichTextData(lines)
}

fun RichTextData.toHTML(): String {
    var out = ""
    val defaultStyle = RichTextData.Style.DEFAULT
    var currentStyle = defaultStyle

    val tagStack = Stack<String>()

    fun openTagsForStyle(style: RichTextData.Style) {
        val removeTags = arrayListOf<String>()
        val tags = LinkedHashMap<String, Map<String, Any?>>()
        if (style.textSize != currentStyle.textSize) {
            removeTags += "font"
            tags += "font" to mapOf("size" to currentStyle.textSize.niceStr)
        }
        if (style.bold != currentStyle.bold) {
            removeTags += "b"
            if (style.bold) tags += "b" to emptyMap()
        }
        if (style.italic != currentStyle.italic) {
            removeTags += "i"
            if (style.italic) tags += "i" to emptyMap()
        }
        // To open a new tag of the same type, we have to close other tags first
        for (key in removeTags.reversed()) {
            if (tagStack.contains(key)) {
                while (tagStack.isNotEmpty()) {
                    val element = tagStack.pop()
                    out += Xml.Encode.encodeCloseTag(element)
                    if (element == key) {
                        break
                    }
                }
            }
        }
        for ((key, value) in tags) {
            out += Xml.Encode.encodeOpenTag(key, value)
            tagStack.push(key)
        }
        currentStyle = style
    }

    for ((index, line) in lines.withIndex()) {
        for (node in line.nodes) {
            when (node) {
                is RichTextData.TextNode -> {
                    val style = node.style
                    tagStack.peek()
                    openTagsForStyle(style)
                    out += node.text.htmlspecialchars()
                }
            }
        }
        if (index != lines.size - 1) {
            out += "<br/>\n"
        }
    }
    while (tagStack.isNotEmpty()) {
        val tag = tagStack.pop()
        out += Xml.Encode.encodeCloseTag(tag)
    }
    return out
}
