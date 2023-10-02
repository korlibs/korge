@file:Suppress("PackageDirectoryMismatch")

package korlibs.io.serialization.json

import korlibs.datastructure.DoubleArrayList
import korlibs.datastructure.FastArrayList
import korlibs.datastructure.iterators.fastForEach
import korlibs.io.dynamic.*
import korlibs.io.lang.IOException
import korlibs.io.lang.invalidOp
import korlibs.io.util.Indenter
import korlibs.io.util.NumberParser
import korlibs.io.util.StrReader
import kotlin.collections.set

object Json {
    fun parse(s: String, context: Context = Context.DEFAULT): Any? = parse(StrReader(s), context)
    fun parseFast(s: String): Any? = parse(StrReader(s), Context.FAST)

    fun parseDyn(s: String, context: Context = Context.DEFAULT): Dyn = parse(s, context).dyn
    fun parseFastDyn(s: String): Dyn = parseFast(s).dyn

    fun stringify(obj: Dyn, pretty: Boolean = false): String = stringify(obj.value, pretty)
    fun stringify(obj: Any?, pretty: Boolean = false): String = when {
        pretty -> Indenter().apply { stringifyPretty(obj, this) }.toString(doIndent = true, indentChunk = "\t")
        else -> StringBuilder().apply { stringify(obj, this) }.toString()
    }

    interface CustomSerializer {
        fun encodeToJson(b: StringBuilder)
    }

    data class Context(val optimizedNumericLists: Boolean, val useFastArrayList: Boolean = false) {
        companion object {
            val DEFAULT = Context(optimizedNumericLists = false, useFastArrayList = false)
            val FAST = Context(optimizedNumericLists = true, useFastArrayList = true)
        }

        fun <T> createArrayList(): MutableList<T> {
            return if (useFastArrayList) FastArrayList() else ArrayList()
        }
    }

    fun parse(s: StrReader, context: Context = Context.DEFAULT): Any? = when (val ic = s.skipSpaces().peekChar()) {
        '{' -> parseObject(s, context)
        '[' -> parseArray(s, context)
        //'-', '+', in '0'..'9' -> { // @TODO: Kotlin native doesn't optimize char ranges
        '-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
            val dres = parseNumber(s)
            if (dres.toInt().toDouble() == dres) dres.toInt() else dres
        }
        't', 'f', 'n', 'u' -> parseBooleanOrNull(s, context)
        '"' -> s.readStringLit()
        else -> invalidJson("Not expected '$ic'")
    }

    private fun parseBooleanOrNull(s: StrReader, context: Context = Context.DEFAULT): Boolean? {
        return when {
            s.tryRead("true") -> true
            s.tryRead("false") -> false
            s.tryRead("null") -> null
            s.tryRead("undefined") -> null
            else -> invalidJson()
        }
    }

    private fun parseObject(s: StrReader, context: Context = Context.DEFAULT): Map<String, Any?> {
        s.skipExpect('{')
        return LinkedHashMap<String, Any?>().apply {
            obj@ while (true) {
                when (s.skipSpaces().read()) {
                    '}' -> break@obj; ',' -> continue@obj; else -> s.unread()
                }
                val key = parse(s, context) as String
                s.skipSpaces().skipExpect(':')
                val value = parse(s, context)
                this[key] = value
            }
        }
    }

    private fun parseArray(s: StrReader, context: Context = Context.DEFAULT): Any {
        var out: MutableList<Any?>? = null
        var outNumber: DoubleArrayList? = null
        s.skipExpect('[')
        array@ while (true) {
            when (s.skipSpaces().read()) {
                ']' -> break@array; ',' -> continue@array; else -> s.unread()
            }
            val v = s.peekChar()
            if (out == null && context.optimizedNumericLists && (v in '0'..'9' || v == '-')) {
                if (outNumber == null) {
                    outNumber = DoubleArrayList()
                }
                outNumber.add(parseNumber(s))
            } else {
                if (out == null) out = context.createArrayList()
                if (outNumber != null) {
                    outNumber.fastForEach { out.add(it) }
                    outNumber = null
                }
                out.add(parse(s, context))
            }
        }
        return outNumber ?: out ?: context.createArrayList<Any?>()
    }

    private fun Char.isNumberStart() = when (this) {
        '-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true
        else -> false
    }

    private fun parseNumber(s: StrReader): Double {
        val start = s.pos
        s.skipWhile { ((it >= '0') && (it <= '9')) || it == '.' || it == 'e' || it == 'E' || it == '-' || it == '+' }
        val end = s.pos
        //return s.str.substring(start, end).toDouble()
        return NumberParser.parseDouble(s.str, start, end)
    }

    fun stringify(obj: Any?, b: StringBuilder) {
        when (obj) {
            null -> b.append("null")
            is Boolean -> b.append(if (obj) "true" else "false")
            is Map<*, *> -> {
                b.append('{')
                for ((i, v) in obj.entries.withIndex()) {
                    if (i != 0) b.append(',')
                    stringify(v.key, b)
                    b.append(':')
                    stringify(v.value, b)
                }
                b.append('}')
            }
            is Iterable<*> -> {
                b.append('[')
                for ((i, v) in obj.withIndex()) {
                    if (i != 0) b.append(',')
                    stringify(v, b)
                }
                b.append(']')
            }
            is Enum<*> -> encodeString(obj.name, b)
            is String -> encodeString(obj, b)
            is Number -> b.append("$obj")
            is CustomSerializer -> obj.encodeToJson(b)
            else -> invalidOp("Don't know how to serialize $obj") //encode(ClassFactory(obj::class).toMap(obj), b)
        }
    }

    fun stringifyPretty(obj: Any?, b: Indenter) {
        when (obj) {
            null -> b.inline("null")
            is Boolean -> b.inline(if (obj) "true" else "false")
            is Map<*, *> -> {
                b.line("{")
                b.indent {
                    val entries = obj.entries
                    for ((i, v) in entries.withIndex()) {
                        if (i != 0) b.line(",")
                        b.inline(encodeString("" + v.key))
                        b.inline(": ")
                        stringifyPretty(v.value, b)
                        if (i == entries.size - 1) b.line("")
                    }
                }
                b.inline("}")
            }
            is Iterable<*> -> {
                b.line("[")
                b.indent {
                    val entries = obj.toList()
                    for ((i, v) in entries.withIndex()) {
                        if (i != 0) b.line(",")
                        stringifyPretty(v, b)
                        if (i == entries.size - 1) b.line("")
                    }
                }
                b.inline("]")
            }
            is String -> b.inline(encodeString(obj))
            is Number -> b.inline("$obj")
            is CustomSerializer -> b.inline(StringBuilder().apply { obj.encodeToJson(this) }.toString())
            else -> {
                invalidOp("Don't know how to serialize $obj")
                //encode(ClassFactory(obj::class).toMap(obj), b)
            }
        }
    }

    private fun encodeString(str: String) = StringBuilder().apply { encodeString(str, this) }.toString()

    private fun encodeString(str: String, b: StringBuilder) {
        b.append('"')
        for (c in str) {
            when (c) {
                '\\' -> b.append("\\\\"); '/' -> b.append("\\/"); '\'' -> b.append("\\'")
                '"' -> b.append("\\\""); '\b' -> b.append("\\b"); '\u000c' -> b.append("\\f")
                '\n' -> b.append("\\n"); '\r' -> b.append("\\r"); '\t' -> b.append("\\t")
                else -> b.append(c)
            }
        }
        b.append('"')
    }

    private fun invalidJson(msg: String = "Invalid JSON"): Nothing = throw IOException(msg)
}

fun String.fromJson(): Any? = Json.parse(this)
fun Map<*, *>.toJson(pretty: Boolean = false): String = Json.stringify(this, pretty)
