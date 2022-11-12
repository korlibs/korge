package com.soywiz.korim.text

import com.soywiz.kds.*
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
    fun openTagsForStyle(style: RichTextData.Style) {
        if (style.bold && !currentStyle.bold) out += "<b>"
        if (style.italic && !currentStyle.italic) out += "<i>"
        if (style.textSize != currentStyle.textSize) out += "<font size=${currentStyle.textSize.niceStr}>"
        currentStyle = style
    }
    fun closeTagsForStyle(style: RichTextData.Style) {
        if (!style.bold && currentStyle.bold) out += "</b>"
        if (!style.italic && currentStyle.italic) out += "</i>"
        if (style.textSize != currentStyle.textSize) out += "</font>"
    }
    for ((index, line) in lines.withIndex()) {
        for (node in line.nodes) {
            when (node) {
                is RichTextData.TextNode -> {
                    val style = node.style
                    closeTagsForStyle(style)
                    openTagsForStyle(style)
                    out += node.text.htmlspecialchars()
                }
            }
        }
        if (index != lines.size - 1) {
            out += "<br/>\n"
        }
    }
    closeTagsForStyle(defaultStyle)
    return out
}
