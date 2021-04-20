package com.soywiz.korge.gradle.util

import groovy.util.*
import groovy.xml.*
// Why is this required?
import groovy.xml.XmlParser
import groovy.xml.XmlNodePrinter
import java.io.*

/*
class KorgeXml(val file: File) {
	val xmlText = file.readText()
	val korge = QXml(xmlParse(xmlText))

	val name get() = korge["name"].text ?: "untitled"
	val description get() = korge["description"].text ?: ""
	val orientation get() = korge["orientation"].text ?: "default"
	val plugins
		get() = korge["plugins"].list.first().children.associate {
			it.name to it.attributes
		}
}
*/

fun NodeList.toFlatNodeList(): List<Node> = this.flatMap {
    when (it) {
        is Node -> listOf(it)
        is NodeList -> it.toFlatNodeList()
        else -> error("Unsupported it")
    }
}

class QXml private constructor(val nodes: List<Node>, dummy: Boolean) : Iterable<QXml> {
	override fun iterator(): Iterator<QXml> = list.iterator()

	companion object {
        operator fun invoke(xml: String): QXml =
			QXml(xmlParse(xml))
        operator fun invoke(obj: Node): QXml =
			QXml(listOf(obj), true)
        operator fun invoke(obj: List<Node>): QXml =
			QXml(obj, true)
        operator fun invoke(obj: List<QXml>, dummy: Boolean = false): QXml =
			QXml(obj.flatMap { it.nodes })
        operator fun invoke(obj: NodeList): QXml =
			QXml(obj.toFlatNodeList(), true)
	}

	val isEmpty get() = nodes.isEmpty()
	val isNotEmpty get() = !isEmpty

	val name: String get() = when (nodes.size) {
		0 -> "null"
		1 -> nodes.first().name().toString()
		else -> nodes.toString()
	}

	val attributes: Map<String, String> get() = when {
        nodes.isEmpty() -> mapOf()
        else -> nodes.map { it.attributes() as Map<String, String> }.reduce { acc, map -> acc + map }
    }

	fun setAttribute(name: String, value: String) {
        for (node in nodes) node.attributes()[name] = value
	}

	fun setAttributes(vararg pairs: Pair<String, String>) {
		for ((key, value) in pairs) setAttribute(key, value)
	}

	val text: String? get() = when (nodes.size) {
		0 -> null
		1 -> (nodes.first() as Node).text()
		else -> nodes.map { it.text() }.joinToString("")
	}

	val list: List<QXml> get() = nodes.map { QXml(it) }

	val children: List<QXml> get() = nodes.flatMap { it.children() as List<Node> }.map {
		QXml(
			it
		)
	}

	val parent: QXml get() = QXml(nodes.map { it.parent() })

	fun remove() {
        for (node in nodes) node.parent().remove(node)
	}

	fun setValue(value: Any?) {
        for (node in nodes) node.setValue(value)
	}

	fun appendNode(name: String, attributes: Map<String, Any?>): QXml =
		QXml(nodes.map { it.appendNode(name, attributes.toMutableMap()) })

    fun appendNode(name: String, vararg attributes: Pair<String, Any?>) = appendNode(name, attributes.toMap().toMutableMap())

	fun getOrAppendNode(name: String, vararg attributes: Pair<String, String>): QXml {
		return get(name).filter { node ->
			attributes.all { node.attributes[it.first] == it.second }
		}.takeIf { it.isNotEmpty() }?.let {
			QXml(it)
		} ?: appendNode(name, *attributes)
	}

	operator fun get(key: String): QXml =
		QXml(NodeList(nodes.map { it.get(key) }))

	fun serialize(): String = xmlSerialize(nodes.first())

	override fun toString(): String = "QXml($nodes)"
}

fun Node?.rchildren(): List<Node> {
	if (this == null) return listOf()
	return this.children() as List<Node>
}

fun Node.getFirstAt(key: String) = getAt(key).firstOrNull()
fun Node.getAt(key: String) = this.getAt(QName(key)) as List<Node>
operator fun Node.get(key: String) = this.getAt(QName(key))

fun xmlParse(xml: String): Node {
    return XmlParser().parseText(xml)
}

fun xmlSerialize(xml: Node): String {
    val sw = StringWriter()
    sw.write("<?xml version='1.0' encoding='utf-8'?>\n")
    val xnp = XmlNodePrinter(PrintWriter(sw))
    xnp.isNamespaceAware = true
    xnp.isPreserveWhitespace = true
    xnp.print(xml)
    return sw.toString()
}

fun updateXml(xmlString: String, updater: QXml.() -> Unit): String = QXml(
	xmlString
).apply(updater).serialize()
