@file:Suppress("PackageDirectoryMismatch")

package korlibs.io.serialization.xml

import korlibs.datastructure.flip
import korlibs.datastructure.iterators.fastForEach
import korlibs.datastructure.toCaseInsensitiveMap
import korlibs.io.file.VfsFile
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.io.util.*
import kotlin.collections.Iterable
import kotlin.collections.Iterator
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.arrayListOf
import kotlin.collections.asSequence
import kotlin.collections.contains
import kotlin.collections.filter
import kotlin.collections.first
import kotlin.collections.firstOrNull
import kotlin.collections.flatMap
import kotlin.collections.joinToString
import kotlin.collections.linkedMapOf
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.toMap
import kotlin.collections.toTypedArray

data class Xml(
    val type: Type,
    val name: String,
    val attributes: Map<String, String>,
    val allChildren: List<Xml>,
    val content: String
) {
    fun withExtraChild(node: Xml) = copy(allChildren = allChildren + node)

    val attributesLC = attributes.toCaseInsensitiveMap()
    val nameLC: String = name.toLowerCase().trim()
    val descendants: Sequence<Xml> get() = allChildren.asSequence().flatMap { it.descendants + it }
    val allChildrenNoComments get() = allChildren.filter { !it.isComment }
    val allNodeChildren get() = allChildren.filter { it.isNode }

    companion object {
        private const val NAME_RAW = "_raw_"
        private const val NAME_TEXT = "_text_"
        private const val NAME_CDATA = "_cdata_"
        private const val NAME_COMMENT = "_comment_"

        fun Tag(tagName: String, attributes: Map<String, Any?>, children: List<Xml>): Xml =
            Xml(Xml.Type.NODE, tagName, attributes.filter { it.value != null }.map { it.key to it.value.toString() }.toMap(), children, "")
        fun Raw(text: String): Xml = Xml(Xml.Type.TEXT, NAME_RAW, LinkedHashMap(), listOf(), text)
        fun Text(text: String): Xml = Xml(Xml.Type.TEXT, NAME_TEXT, LinkedHashMap(), listOf(), text)
        fun CData(text: String): Xml = Xml(Xml.Type.TEXT, NAME_CDATA, LinkedHashMap(), listOf(), text)
        fun Comment(text: String): Xml = Xml(Xml.Type.COMMENT, NAME_COMMENT, LinkedHashMap(), listOf(), text)

        //operator fun invoke(@Language("xml") str: String): Xml = parse(str)

        fun parse(str: String): Xml {
            try {
                val stream = Xml.Stream.parse(str).iterator()

                data class Level(val children: List<Xml>, val close: Xml.Stream.Element.CloseTag?)

                fun level(): Level {
                    val children = arrayListOf<Xml>()

                    while (stream.hasNext()) {
                        val tag = stream.next()
                        when (tag) {
                            is Xml.Stream.Element.ProcessingInstructionTag -> Unit
                            is Xml.Stream.Element.CommentTag -> children.add(Xml.Comment(tag.text))
                            is Xml.Stream.Element.Text -> children.add((if (tag.cdata) Xml.CData(tag.text) else Xml.Text(tag.text)))
                            is Xml.Stream.Element.OpenCloseTag -> children.add(Xml.Tag(tag.name, tag.attributes, listOf()))
                            is Xml.Stream.Element.OpenTag -> {
                                val out = level()
                                if (out.close?.name != tag.name) throw IllegalArgumentException("Expected ${tag.name} but was ${out.close?.name}")
                                children.add(Xml(Xml.Type.NODE, tag.name, tag.attributes, out.children, ""))
                            }
                            is Xml.Stream.Element.CloseTag -> return Level(children, tag)
                            else -> throw IllegalArgumentException("Unhandled $tag")
                        }
                    }

                    return Level(children, null)
                }

                val children = level().children
                return children.firstOrNull { it.type == Xml.Type.NODE }
                    ?: children.firstOrNull()
                    ?: Xml.Text("")
            } catch (t: NoSuchElementException) {
                println("ERROR: XML: $str thrown a NoSuchElementException")
                return Xml.Text("!!ERRORED!!")
            }
        }
    }

    val text: String
        get() = when (type) {
            Type.NODE -> allChildren.joinToString("") { it.text }
            Type.TEXT -> content
            Type.COMMENT -> ""
        }

    fun toOuterXmlIndentedString(indenter: Indenter = Indenter()): String = toOuterXmlIndented(indenter).toString()

    fun toOuterXmlIndented(indenter: Indenter = Indenter()): Indenter = indenter.apply {
        when (type) {
            Type.NODE -> {
                if (allChildren.isEmpty()) {
                    line("<$name$attributesStr/>")
                } else if (allChildren.size == 1 && allChildren[0].type == Type.TEXT) {
                    inline("<$name$attributesStr>")
                    inline(allChildren[0].content)
                    line("</$name>")
                } else {
                    line("<$name$attributesStr>")
                    indent {
                        allChildren.fastForEach { child ->
                            child.toOuterXmlIndented(indenter)
                        }
                    }
                    line("</$name>")
                }
            }
            else -> line(outerXml)
        }
    }

    val attributesStr: String get() = attributes.toList().map { " ${it.first}=\"${it.second}\"" }.joinToString("")

    val outerXml: String
        get() = when (type) {
            Type.NODE -> {
                if (allChildren.isEmpty()) {
                    "<$name$attributesStr/>"
                } else {
                    // @TODO: Kotlin 1.4-M3 regression: https://youtrack.jetbrains.com/issue/KT-40338
                    //val children = this.allChildren.map(Xml::outerXml).joinToString("")
                    val children = this.allChildren.map { it.outerXml }.joinToString("")
                    "<$name$attributesStr>$children</$name>"
                }
            }
            Type.TEXT -> when (name) {
                NAME_TEXT -> Entities.encode(content)
                NAME_CDATA -> "<![CDATA[$content]]>"
                NAME_RAW -> content
                else -> content
            }
            Type.COMMENT -> "<!--$content-->"
        }

    val innerXml: String
        get() = when (type) {
            // @TODO: Kotlin 1.4-M3 regression: https://youtrack.jetbrains.com/issue/KT-40338
            //Type.NODE -> this.allChildren.map(Xml::outerXml).joinToString("")
            Type.NODE -> this.allChildren.map { it.outerXml }.joinToString("")
            else -> outerXml
        }

    operator fun get(name: String): Iterable<Xml> = children(name)

    fun children(name: String): Iterable<Xml> = allChildren.filter { it.name.equals(name, ignoreCase = true) }
    fun child(name: String): Xml? = children(name).firstOrNull()
    fun childText(name: String): String? = child(name)?.text

    fun hasAttribute(key: String): Boolean = this.attributesLC.containsKey(key)
    fun attribute(name: String): String? = this.attributesLC[name]

    fun getString(name: String): String? = this.attributesLC[name]
    fun getInt(name: String): Int? = this.attributesLC[name]?.toInt()
    fun getLong(name: String): Long? = this.attributesLC[name]?.toLong()
    fun getDouble(name: String): Double? = this.attributesLC[name]?.toDouble()
    fun getFloat(name: String): Float? = this.attributesLC[name]?.toFloat()

    fun double(name: String, defaultValue: Double = 0.0): Double =
        this.attributesLC[name]?.toDoubleOrNull() ?: defaultValue

    fun boolean(name: String, defaultValue: Boolean = false): Boolean = booleanOrNull(name) ?: defaultValue

    fun booleanOrNull(name: String): Boolean? =
        when (str(name).toLowerCase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }

    fun float(name: String, defaultValue: Float = 0f): Float = this.attributesLC[name]?.toFloatOrNull() ?: defaultValue
    fun int(name: String, defaultValue: Int = 0): Int = this.attributesLC[name]?.toIntOrNull() ?: defaultValue
    fun long(name: String, defaultValue: Long = 0): Long = this.attributesLC[name]?.toLongOrNull() ?: defaultValue
    fun str(name: String, defaultValue: String = ""): String = this.attributesLC[name] ?: defaultValue
    fun uint(name: String, defaultValue: UInt = 0u): UInt = this.attributesLC[name]?.toUIntOrNull() ?: defaultValue

    fun doubleNull(name: String): Double? = this.attributesLC[name]?.toDoubleOrNull()
    fun floatNull(name: String): Float? = this.attributesLC[name]?.toFloatOrNull()
    fun intNull(name: String): Int? = this.attributesLC[name]?.toIntOrNull()
    fun longNull(name: String): Long? = this.attributesLC[name]?.toLongOrNull()
    fun strNull(name: String): String? = this.attributesLC[name]

    //override fun toString(): String = innerXml
    override fun toString(): String = outerXml

    enum class Type { NODE, TEXT, COMMENT }

    object Encode {
        fun encodeOpenTag(name: String, map: Map<String, Any?>, selfClose: Boolean = false): String = buildString {
            append("<")
            append(name)
            if (map.isNotEmpty()) {
                append(" ")
                append(quoteParams(map))
            }
            if (selfClose) {
                append("/")
            }
            append(">")
        }
        fun encodeCloseTag(name: String): String = "</$name>"
        fun quoteParams(map: Map<String, Any?>): String = map.entries.joinToString(" ") { it.key + "=" + quote(it.value) }

        fun quote(value: Any?): String = when (value) {
            is Number, is Boolean -> value.toString()
            else -> value?.toString()?.let { quote(it) } ?: "\"\""
        }
        fun quote(str: String): String = "\"${Entities.encode(str)}\""
    }

    object Entities {
        // Predefined entities in XML 1.0
        private val charToEntity = linkedMapOf('"' to "&quot;", '\'' to "&apos;", '<' to "&lt;", '>' to "&gt;", '&' to "&amp;")
        private val entities = StrReader.Literals.fromList(charToEntity.values.toTypedArray())
        private val entityToChar = charToEntity.flip()

        fun encode(str: String): String = str.eachBuilder {
            val entry = charToEntity[it]
            when {
                entry != null -> append(entry)
                else -> append(it)
            }
        }
        fun decode(str: String): String = decode(StrReader(str))
        fun decode(r: StrReader): String = buildString {
            while (!r.eof) {
                @Suppress("LiftReturnOrAssignment") // Performance?
                append(r.readUntil('&'))
                if (r.eof) break

                r.skipExpect('&')
                val value = r.readUntilIncluded(';') ?: ""
                val full = "&$value"
                when {
                    value.startsWith('#') -> {
                        var base = 10
                        var str = value.substring(1, value.length - 1)
                        if(str.startsWith("x")) {
                            base = 16
                            str = str.substring(1)
                        }
                        append(str.toIntOrNull(base)?.toChar())
                    }
                    entityToChar.contains(full) -> append(entityToChar[full])
                    else -> append(full)
                }
            }
        }
    }

    object Stream {
        fun parse(str: String): Iterable<Element> = parse(StrReader(str))
        fun parse(r: BaseStrReader): Iterable<Element> = Xml2Iterable(r)
        fun parse(r: CharReader): Iterable<Element> = Xml2Iterable(CharReaderStrReader(r))

        private fun BaseStrReader.matchStringOrId(): String? = matchSingleOrDoubleQuoteString() ?: matchIdentifier()

        private fun xmlSequence(r: BaseStrReader): Sequence<Element> = sequence<Element> {
            while (!r.eof) {
                val str = r.readUntil('<') ?: ""
                if (str.isNotEmpty()) {
                    yield(Element.Text(Xml.Entities.decode(str)))
                }

                if (r.eof) break

                r.skipExpect('<')
                val res: Element = when {
                    r.tryExpect("![CDATA[") -> {
                        val text = r.slice {
                            while (!r.eof) {
                                if (r.tryExpect("]]>", consume = false)) break
                                r.skip(1)
                            }
                        }
                        r.readExpect("]]>")
                        Element.Text(text).also { it.cdata = true }
                    }
                    r.tryExpect("!--") -> {
                        val text = r.slice {
                            while (!r.eof) {
                                if (r.tryExpect("-->", consume = false)) break
                                r.skip(1)
                            }
                        }
                        r.readExpect("-->")
                        Element.CommentTag(text)
                    }
                    else -> {
                        r.skipSpaces()
                        val processingInstruction = r.tryExpect('?')
                        val processingEntityOrDocType = r.tryExpect('!')
                        val close = r.tryExpect('/') || processingEntityOrDocType
                        r.skipSpaces()
                        val name = r.matchIdentifier()
                            ?: error("Couldn't match identifier after '<', offset=${r.pos}, around='${r.peek(10)}'")
                        r.skipSpaces()
                        val attributes = linkedMapOf<String, String>()
                        while (r.peekChar() != '?' && r.peekChar() != '/' && r.peekChar() != '>') {
                            val key = r.matchStringOrId() ?: throw IllegalArgumentException(
                                "Malformed document or unsupported xml construct around ~${r.peek(10)}~ for name '$name'"
                            )
                            r.skipSpaces()
                            if (r.tryExpect("=")) {
                                r.skipSpaces()
                                val argsQuote = r.matchStringOrId()
                                attributes[key] = when {
                                    argsQuote != null && !(argsQuote.startsWith("'") || argsQuote.startsWith("\"")) -> argsQuote
                                    argsQuote != null -> Xml.Entities.decode(argsQuote.substring(1, argsQuote.length - 1))
                                    else -> Xml.Entities.decode(r.matchIdentifier()!!)
                                }
                            } else {
                                attributes[key] = key
                            }
                            r.skipSpaces()
                        }
                        val openclose = r.tryExpect('/')
                        val processingInstructionEnd = r.tryExpect('?')
                        r.skipExpect('>')
                        when {
                            processingInstruction || processingEntityOrDocType -> Element.ProcessingInstructionTag(name, attributes)
                            openclose -> Element.OpenCloseTag(name, attributes)
                            close -> Element.CloseTag(name)
                            else -> Element.OpenTag(name, attributes)
                        }
                    }
                }

                yield(res)
            }
        }

        class Xml2Iterable(val reader2: BaseStrReader) : Iterable<Element> {
            val reader = reader2.clone()
            override fun iterator(): Iterator<Element> = xmlSequence(reader).iterator()
        }

        sealed class Element {
            data class ProcessingInstructionTag(val name: String, val attributes: Map<String, String>) : Element()
            data class OpenCloseTag(val name: String, val attributes: Map<String, String>) : Element()
            data class OpenTag(val name: String, val attributes: Map<String, String>) : Element()
            data class CommentTag(val text: String) : Element()
            data class CloseTag(val name: String) : Element()
            data class Text(val text: String, var cdata: Boolean = false) : Element()
        }
    }
}

val Xml.isText get() = this.type == Xml.Type.TEXT
val Xml.isComment get() = this.type == Xml.Type.COMMENT
val Xml.isNode get() = this.type == Xml.Type.NODE

fun Iterable<Xml>.str(name: String, defaultValue: String = ""): String = this.first().attributes[name] ?: defaultValue
fun Iterable<Xml>.children(name: String): Iterable<Xml> = this.flatMap { it.children(name) }
val Iterable<Xml>.allChildren: Iterable<Xml> get() = this.flatMap(Xml::allChildren)
val Iterable<Xml>.allNodeChildren: Iterable<Xml> get() = this.flatMap(Xml::allNodeChildren)
val Iterable<Xml>.firstText: String? get() = this.firstOrNull()?.text
val Iterable<Xml>.text: String get() = this.joinToString("") { it.text }
operator fun Iterable<Xml>.get(name: String): Iterable<Xml> = this.children(name)

fun Sequence<Xml>.str(name: String, defaultValue: String = ""): String = this.first().attributes[name] ?: defaultValue
fun Sequence<Xml>.children(name: String): Sequence<Xml> = this.flatMap { it.children(name) }
val Sequence<Xml>.allChildren: Sequence<Xml> get() = this.flatMap(Xml::allChildren)
val Sequence<Xml>.allNodeChildren: Sequence<Xml> get() = this.flatMap(Xml::allNodeChildren)
val Sequence<Xml>.firstText: String? get() = this.firstOrNull()?.text
val Sequence<Xml>.text: String get() = this.joinToString("") { it.text }
operator fun Sequence<Xml>.get(name: String): Sequence<Xml> = this.children(name)

fun String.toXml(): Xml = Xml.parse(this)

// language=html
fun Xml(
    // language=html
    str: String
): Xml = Xml.parse(str)

fun Xml.descendants(name: String) = descendants.filter { it.name.equals(name, ignoreCase = true) }
fun Xml.firstDescendant(name: String) = descendants(name).firstOrNull()

suspend fun VfsFile.readXml(): Xml = Xml(this.readString())

class XmlBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal val nodes = arrayListOf<Xml>()
    fun node(node: Xml) = node.also { nodes += node }
    inline fun node(tag: String, vararg props: Pair<String, Any?>, block: XmlBuilder.() -> Unit = {}): Xml =
        Xml.Tag(tag, props.filter { it.second != null }.toMap(), XmlBuilder().apply(block).nodes).also { nodes += it }
    fun comment(comment: String): Xml = Xml.Comment(comment).also { nodes += it }
    fun text(text: String): Xml = Xml.Text(text).also { nodes += it }
    fun cdata(text: String): Xml = Xml.CData(text).also { nodes += it }.also { if (text.contains("]]>")) error("A cdata node cannot contain the ]]> literal") }
    fun raw(text: String): Xml = Xml.Raw(text).also { nodes += it }
}

inline fun buildXml(rootTag: String, vararg props: Pair<String, Any?>, crossinline block: XmlBuilder.() -> Unit = {}): Xml =
    XmlBuilder().node(rootTag, *props, block = block)

inline fun Xml(rootTag: String, vararg props: Pair<String, Any?>, block: XmlBuilder.() -> Unit = {}): Xml =
    XmlBuilder().node(rootTag, *props, block = block)
inline fun Xml(rootTag: String, props: Map<String, Any?>?, block: XmlBuilder.() -> Unit = {}): Xml =
    XmlBuilder().node(rootTag, *(props ?: emptyMap()).map { it.key to it.value }.toTypedArray(), block = block)
