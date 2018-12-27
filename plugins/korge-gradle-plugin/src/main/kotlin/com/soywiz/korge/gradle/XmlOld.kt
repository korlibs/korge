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


class QXml private constructor(val obj: Any?, dummy: Boolean) : Iterable<QXml> {
	override fun iterator(): Iterator<QXml> = list.iterator()

	companion object {
		operator fun invoke(obj: Any?): QXml = if (obj is QXml) obj else QXml(obj, true)
		operator fun invoke(xml: String): QXml = QXml(xmlParse(xml))
	}

	val isEmpty get() = (obj == null) || (obj is NodeList && obj.isEmpty())
	val isNotEmpty get() = !isEmpty

	val name: String get() = when (obj) {
		null -> "null"
		is Node -> obj.name().toString()
		else -> obj.toString()
	}

	val attributes: Map<String, String> get() = when (obj) {
		is Node -> obj.attributes() as MutableMap<String, String>
		is NodeList -> obj.map { QXml(it).attributes }.reduce { acc, map -> acc + map }
		else -> mapOf()
	}

	fun setAttribute(name: String, value: String) {
		when (obj) {
			is Node -> obj.attributes()[name] = value
			is NodeList -> for (o in obj) QXml(o).setAttribute(name, value)
			else -> Unit
		}
	}

	fun setAttributes(vararg pairs: Pair<String, String>) {
		for ((key, value) in pairs) setAttribute(key, value)
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
			is Iterable<*> -> {
				for (child in children) child.remove()
			}
			else -> {
				error("Can't remove $this")
			}
		}
	}

	fun setValue(value: Any?) {
		when (obj) {
			is Node -> obj.setValue(value)
			is NodeList -> obj.map { QXml(it).setValue(value) }
		}
	}

	fun appendNode(name: String, attributes: Map<String, Any?>): QXml {
		return when (obj) {
			is Node -> QXml(obj.appendNode(name, attributes.toMutableMap()))
			is NodeList -> QXml(list.map { it.appendNode(name, attributes.toMutableMap()) })
			else -> QXml(null)
		}
	}

    fun appendNode(name: String, vararg attributes: Pair<String, Any?>) = appendNode(name, attributes.toMap().toMutableMap())

	fun getOrAppendNode(name: String, vararg attributes: Pair<String, String>): QXml {
		return get(name).filter {
			attributes.all { attributes[it.first] == it.second }
		}.takeIf { it.isNotEmpty() }?.let { QXml(it) } ?: appendNode(name, *attributes)
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

	fun serialize(): String {
		return xmlSerialize(this.obj as Node)
	}

	override fun toString(): String = "QXml($obj)"
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

fun updateXml(xmlString: String, updater: QXml.() -> Unit): String = QXml(xmlString).apply(updater).serialize()