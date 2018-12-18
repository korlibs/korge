package com.soywiz.korge.gradle

import groovy.util.*
import groovy.xml.*
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


class QXml private constructor(val obj: Any?, dummy: Boolean) {
	companion object {
		operator fun invoke(obj: Any?): QXml = if (obj is QXml) obj else QXml(obj, true)
	}
	val name: String get() = when (obj) {
		null -> "null"
		is Node -> obj.name().toString()
		else -> obj.toString()
	}

	val attributes: Map<String, String> get() = when (obj) {
		is Node -> obj.attributes() as Map<String, String>
		is NodeList -> obj.map { QXml(it).attributes }.reduce { acc, map -> acc + map }
		else -> mapOf()
	}

	val text: String? get() = when (obj) {
		null -> null
		is Node -> obj.text()
		is NodeList -> obj.text()
		else -> obj.toString()
	}

	val list: List<QXml> get() = when (obj) {
		is Node -> listOf(this)
		is NodeList -> obj.map { QXml(it) }
		else -> listOf()
	}

	val children: List<QXml> get() = when (obj) {
		is Node -> obj.children().map { QXml(it) }
		is NodeList -> obj.map { QXml(it) }
		else -> listOf()
	}

	val parent: QXml get() = when (obj) {
		is Node -> QXml(obj.parent())
		is NodeList -> QXml(obj.map { QXml(it).parent })
		else -> QXml(null)
	}

	fun remove() {
		when (obj) {
			is Node -> {
				(parent.obj as? Node?)?.remove(obj)
			}
		}
	}

	fun setValue(value: Any?) {
		when (obj) {
			is Node -> obj.setValue(value)
			is NodeList -> obj.map { QXml(it).setValue(value) }
		}
	}

	fun appendNode(name: String, attributes: Map<Any?, Any?>) {
		when (obj) {
			is Node -> obj.appendNode(name, attributes)
			is NodeList -> list.forEach { it.appendNode(name, attributes) }
		}
	}

	operator fun get(key: String): QXml {
		if (obj is Iterable<*>) {
			return QXml(obj.map { QXml(it)[key].obj })
		}
		if (obj is Node) {
			return QXml(obj.get(key))
		}
		return QXml(null)
	}
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
