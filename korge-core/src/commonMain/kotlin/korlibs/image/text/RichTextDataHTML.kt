package korlibs.image.text

import korlibs.datastructure.*
import korlibs.image.color.*
import korlibs.io.serialization.xml.*
import korlibs.io.util.*

fun RichTextData.Companion.fromHTML(
    // language=HTML prefix=<body> suffix=</body>
    html: String,
    style: RichTextData.Style = RichTextData.Style.DEFAULT
): RichTextData {
    val lines = fastArrayListOf<RichTextData.Line>()
    val nodes = fastArrayListOf<RichTextData.Node>()
    fun flushLine(force: Boolean = false) {
        if (nodes.isEmpty() && !force) return
        lines.add(RichTextData.Line(nodes.toList()))
        nodes.clear()
    }
    fun processNode(node: Xml, style: RichTextData.Style) {
        if (node.nameLC == "br") {
            nodes.add(RichTextData.TextNode("", style))
            flushLine(force = true)
            return
        }
        if (node.isText) {
            nodes += RichTextData.TextNode(node.text, style)
        }
        val rstyle = style.copy(
            bold = if (node.nameLC == "b" || node.nameLC == "strong") true else style.bold,
            italic = if (node.nameLC == "i" || node.nameLC == "em") true else style.italic,
            underline = if (node.nameLC == "u") true else style.underline,
            textSize = node.doubleNull("size") ?: style.textSize,
            color = node.strNull("color")?.let { Colors[it] } ?: style.color,
        )

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
        if (style.textSize != currentStyle.textSize || style.color != currentStyle.color) {
            removeTags += "font"
            tags += "font" to buildMap {
                if (style.textSize != currentStyle.textSize) put("size", style.textSize.toInt())
                style.color?.let { put("color", it.toHtmlNamedString()) }
            }
        }
        if (style.bold != currentStyle.bold) {
            removeTags += "b"
            if (style.bold) tags += "b" to emptyMap()
        }
        if (style.italic != currentStyle.italic) {
            removeTags += "i"
            if (style.italic) tags += "i" to emptyMap()
        }
        if (style.underline != currentStyle.underline) {
            removeTags += "u"
            if (style.underline) tags += "u" to emptyMap()
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
