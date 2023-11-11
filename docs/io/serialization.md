---
permalink: /io/serialization/
group: io
layout: default
title: Serialization
title_prefix: KorIO
description: "JSON, TOML, YAML, XML and Properties serialization"
fa-icon: fa-code
priority: 9
artifact: 'com.soywiz.korge:korge-core'
package: korlibs.serialization
---

KorIO has utilities for serializing and deserializing typical formats.



## JSON

```kotlin
object Json {
    fun parse(s: String): Any?
    fun parse(s: StrReader): Any?

    fun stringify(obj: Any?, pretty: Boolean = false): String
    fun stringify(obj: Any?, b: StringBuilder)
    fun stringifyPretty(obj: Any?, b: Indenter)

    interface CustomSerializer {
        fun encodeToJson(b: StringBuilder)
    }
}

fun String.fromJson(): Any? = Json.parse(this)
fun Map<*, *>.toJson(pretty: Boolean = false): String = Json.stringify(this, pretty)
```

## TOML

```kotlin
object TOML {
    fun parseToml(str: String, out: MutableMap<String, Any?> = LinkedHashMap()): Map<String, Any?>
}
```

## YAML

```kotlin
object Yaml {
    fun decode(str: String): Any?
    fun read(str: String): Any?

    // Pseudo-internal API
    fun StrReader.tokenize(): List<Token>
    interface Token {
        data class LINE(val str: String, val level: Int) : Token
        data class ID(val str: String) : Token
        data class STR(val str: String, val ustr: String) : Token
        data class SYMBOL(val str: String) : Token
    }
}
```

## XML

XML parser comes in two flavors.

### The streaming API:

```kotlin
object Xml.Stream {
    fun parse(str: String): Iterable<Xml.Element>
    fun parse(r: StrReader): Iterable<Xml.Element>
}
sealed class Xml.Element {
    class ProcessingInstructionTag(val name: String, val attributes: Map<String, String>) : Xml.Element()
    class OpenCloseTag(val name: String, val attributes: Map<String, String>) : Xml.Element()
    class OpenTag(val name: String, val attributes: Map<String, String>) : Xml.Element()
    class CommentTag(val text: String) : Xml.Element()
    class CloseTag(val name: String) : Xml.Element()
    class Text(val text: String) : Xml.Element()
}
```

### The SimpleXML-like API:

```kotlin
// Creating a new Xml from Data
fun Xml(str: String): Xml
fun String.toXml(): Xml
suspend fun VfsFile.readXml(): Xml

// Encoding and decoding Xml entities
object Xml.Entities {
    fun encode(str: String): String
    fun decode(str: String): String
    fun decode(r: StrReader): String
}

class Xml {
    companion object {
        // To create nodes
        fun Tag(tagName: String, attributes: Map<String, Any?>, children: List<Xml>): Xml
        fun Text(text: String): Xml
        fun Comment(text: String): Xml
        fun parse(str: String): Xml
    }

    enum class Type { NODE, TEXT, COMMENT }

    // Node Type Information
    val type: Type
    val isText: Boolean
    val isComment: Boolean
    val isNode: Boolean

    val name: String
    val nameLC: String

    val attributes: Map<String, String>
    val attributesLC: Map<String, T>

    val descendants: Iterable<Xml>
    val allChildren: List<Xml>
    val allChildrenNoComments: List<Xml>
    val allNodeChildren: List<Xml>

    val content: String
    val text: String

    val attributesStr: String
    val outerXml: String
    val innerXml: String

    fun toOuterXmlIndented(indenter: Indenter = Indenter()): Indenter
    fun childText(name: String): String?

    // Children access
    operator fun get(name: String): Iterable<Xml>
    fun children(name: String): Iterable<Xml>
    fun child(name: String): Xml?

    // Attribute reading
    fun hasAttribute(key: String): Boolean
    fun attribute(name: String): String?

    fun getString(name: String): String?
    fun getInt(name: String): Int?
    fun getLong(name: String): Long?
    fun getDouble(name: String): Double?
    fun getFloat(name: String): Float?

    fun double(name: String, defaultValue: Double = 0.0): Double
    fun float(name: String, defaultValue: Float = 0f): Float
    fun int(name: String, defaultValue: Int = 0): Int
    fun long(name: String, defaultValue: Long = 0): Long
    fun str(name: String, defaultValue: String = ""): String

    fun doubleNull(name: String): Double?
    fun floatNull(name: String): Float?
    fun intNull(name: String): Int?
    fun longNull(name: String): Long?
    fun strNull(name: String): String?

    override fun toString(): String = outerXml
}

fun Iterable<Xml>.str(name: String, defaultValue: String = ""): String
fun Iterable<Xml>.children(name: String): Iterable<Xml>
val Iterable<Xml>.allChildren: Iterable<Xml>
val Iterable<Xml>.allNodeChildren: Iterable<Xml>

val Iterable<Xml>.firstText: String?
val Iterable<Xml>.text: String
operator fun Iterable<Xml>.get(name: String): Iterable<Xml>
```

## Properties

```kotlin
class Props(private val props: LinkedHashMap<String, String> = LinkedHashMap<String, String>()) : MutableMap<String, String> by props {
	companion object {
		fun load(str: String): Props
	}

	fun deserializeAdd(str: String)
	fun deserializeNew(str: String)

	fun serialize(): String
}

suspend fun VfsFile.loadProperties(charset: Charset = UTF8): Props
suspend fun VfsFile.saveProperties(props: Props, charset: Charset = UTF8)
```
