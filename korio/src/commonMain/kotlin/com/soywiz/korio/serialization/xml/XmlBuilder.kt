package com.soywiz.korio.serialization.xml

class XmlBuilder internal constructor() {
    private val nodes = arrayListOf<Xml>()
    fun node(node: Xml) = node.also { nodes += node }
    fun node(tag: String, vararg props: Pair<String, Any?>, block: XmlBuilder.() -> Unit = {}): Xml =
        Xml.Tag(tag, props.filter { it.second != null }.toMap(), XmlBuilder().apply(block).nodes).also { nodes += it }
    fun comment(comment: String): Xml = Xml.Comment(comment).also { nodes += it }
    fun text(text: String): Xml = Xml.Text(text).also { nodes += it }
    fun cdata(text: String): Xml = Xml.CData(text).also { nodes += it }.also { if (text.contains("]]>")) error("A cdata node cannot contain the ]]> literal") }
    fun raw(text: String): Xml = Xml.Raw(text).also { nodes += it }
}

fun buildXml(rootTag: String, vararg props: Pair<String, Any?>, block: XmlBuilder.() -> Unit): Xml =
    XmlBuilder().node(rootTag, *props, block = block)

fun Xml(rootTag: String, vararg props: Pair<String, Any?>, block: XmlBuilder.() -> Unit): Xml =
    XmlBuilder().node(rootTag, *props, block = block)
fun Xml(rootTag: String, vararg props: Pair<String, Any?>): Xml =
    XmlBuilder().node(rootTag, *props, block = {})
fun Xml(rootTag: String, props: Map<String, Any?>): Xml =
    XmlBuilder().node(rootTag, *props.map { it.key to it.value }.toTypedArray(), block = {})
fun Xml(rootTag: String, props: Map<String, Any?>, block: XmlBuilder.() -> Unit): Xml =
    XmlBuilder().node(rootTag, *props.map { it.key to it.value }.toTypedArray(), block = block)
