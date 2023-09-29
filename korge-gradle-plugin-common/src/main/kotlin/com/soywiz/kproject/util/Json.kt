package com.soywiz.kproject.util

import com.soywiz.kproject.internal.*
import kotlin.math.*


object Json {
    fun parse(s: String): Any? = parse(StrReader(s))
    fun parseFast(s: String): Any? = parse(StrReader(s))

    fun parseDyn(s: String): Dyn = parse(s).dyn
    fun parseFastDyn(s: String): Dyn = parseFast(s).dyn

    fun stringify(obj: Dyn, pretty: Boolean = false): String = stringify(obj.value, pretty)
    fun stringify(obj: Any?, pretty: Boolean = false): String = when {
        //pretty -> Indenter().apply { stringifyPretty(obj, this) }.toString(doIndent = true, indentChunk = "\t")
        else -> StringBuilder().apply { stringify(obj, this) }.toString()
    }

    interface CustomSerializer {
        fun encodeToJson(b: StringBuilder)
    }

    private fun parse(s: StrReader): Any? = when (val ic = s.skipSpaces().peekChar()) {
        '{' -> parseObject(s)
        '[' -> parseArray(s)
        //'-', '+', in '0'..'9' -> { // @TODO: Kotlin native doesn't optimize char ranges
        '-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
            val dres = parseNumber(s)
            if (dres.toInt().toDouble() == dres) dres.toInt() else dres
        }
        't', 'f', 'n' -> parseBooleanOrNull(s)
        '"' -> s.readStringLit()
        else -> invalidJson("Not expected '$ic'")
    }

    private fun parseBooleanOrNull(s: StrReader): Boolean? {
        return when {
            s.tryRead("true") -> true
            s.tryRead("false") -> false
            s.tryRead("null") -> null
            else -> invalidJson()
        }
    }

    private fun parseObject(s: StrReader): Map<String, Any?> {
        s.skipExpect('{')
        return LinkedHashMap<String, Any?>().apply {
            obj@ while (true) {
                when (s.skipSpaces().read()) {
                    '}' -> break@obj; ',' -> continue@obj; else -> s.unread()
                }
                val key = parse(s) as String
                s.skipSpaces().skipExpect(':')
                val value = parse(s)
                this[key] = value
            }
        }
    }

    private fun parseArray(s: StrReader): Any {
        val out: MutableList<Any?> = arrayListOf()
        s.skipExpect('[')
        array@ while (true) {
            when (s.skipSpaces().read()) {
                ']' -> break@array; ',' -> continue@array; else -> s.unread()
            }
            out.add(parse(s))
        }
        return out
    }

    private fun parseNumber(s: StrReader): Double {
        val start = s.pos
        s.skipWhile { ((it >= '0') && (it <= '9')) || it == '.' || it == 'e' || it == 'E' || it == '-' || it == '+' }
        val end = s.pos
        //return s.str.substring(start, end).toDouble()
        return s.str.substring(start, end).toDouble()
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

    /*
    private fun stringifyPretty(obj: Any?, b: Indenter) {
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
    */

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

    private fun invalidJson(msg: String = "Invalid JSON"): Nothing = throw IllegalArgumentException(msg)
}

fun String.fromJson(): Any? = Json.parse(this)
fun Map<*, *>.toJson(pretty: Boolean = false): String = Json.stringify(this, pretty)

private class Indenter {
    fun toString(doIndent: Boolean = true, indentChunk: String = "\t"): String {
        TODO()
    }
}

private class StrReader(val str: String, var pos: Int = 0) {
    private val tempCharArray = CharArray(str.length)

    val length: Int = this.str.length
    val available: Int get() = length - this.pos
    val eof: Boolean get() = (this.pos >= this.str.length)
    val hasMore: Boolean get() = !eof

    fun tryRead(str: String): Boolean = tryLit(str, consume = true) != null

    fun tryLit(lit: String, consume: Boolean): String? {
        if (!String.substringEquals(this.str, this.pos, lit, 0, lit.length)) return null
        if (consume) this.pos += lit.length
        return lit
    }

    fun skipSpaces(): StrReader {
        this.skipWhile { it.isWhitespaceFast() }
        return this
    }

    fun peekChar(): Char = if (hasMore) this.str[this.pos] else '\u0000'
    fun read(): Char = if (hasMore) this.str[posSkip(1)] else '\u0000'
    fun readChar(): Char = read()

    fun unread(count: Int = 1): StrReader {
        this.pos -= count
        return this
    }

    private fun posSkip(count: Int): Int {
        val out = this.pos
        this.pos += count
        return out
    }

    fun skip(count: Int): StrReader {
        this.pos += count
        return this
    }

    inline fun skipWhile(filter: (Char) -> Boolean) {
        while (hasMore && filter(this.peekChar())) {
            this.readChar()
        }
    }

    fun skipExpect(expected: Char) {
        val readed = this.readChar()
        if (readed != expected) throw IllegalArgumentException("Expected '$expected' but found '$readed' at $pos")
    }

    fun readFixedSizeInt(count: Int, radix: Int = 10): Int {
        val readCount = min(available, count)
        skip(readCount)
        //return NumberParser.parseInt(str, pos - readCount, pos, radix)
        return str.substring(pos - readCount, pos).toInt(radix)
    }

    fun readStringLit(reportErrors: Boolean = true): String {
        val out = tempCharArray
        var outp = 0
        val quotec = read()
        when (quotec) {
            '"', '\'' -> Unit
            else -> invalidOp("Invalid string literal")
        }
        var closed = false
        loop@ while (hasMore) {
            when (val c = read()) {
                '\\' -> {
                    val cc = read()
                    out[outp++] = when (cc) {
                        '\\' -> '\\'; '/' -> '/'; '\'' -> '\''; '"' -> '"'
                        'b' -> '\b'; 'f' -> '\u000c'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'
                        'u' -> readFixedSizeInt(4, 16).toChar()
                        else -> throw IllegalArgumentException("Invalid char '$cc'")
                    }
                }
                quotec -> {
                    closed = true
                    break@loop
                }
                else -> {
                    out[outp++] = c
                }
            }
        }
        if (!closed && reportErrors) {
            throw RuntimeException("String literal not closed! '${this.str}'")
        }
        return out.concatToString(0, outp)
    }

}

private fun String.Companion.substringEquals(a: String, aIndex: Int, b: String, bIndex: Int, count: Int): Boolean {
    if (count == 0) return true
    if (aIndex < 0 || bIndex < 0) return false
    if (aIndex + count > a.length) return false
    if (bIndex + count > b.length) return false
    for (n in 0 until count) if (a[aIndex + n] != b[bIndex + n]) return false
    return true
}
