@file:Suppress("PackageDirectoryMismatch")

package korlibs.template.internal

import korlibs.template.util.KorteDeferred
import kotlinx.atomicfu.locks.*
import kotlin.coroutines.coroutineContext
import kotlin.reflect.*

internal typealias Lock = SynchronizedObject
internal inline operator fun <T> Lock.invoke(block: () -> T): T = synchronized(this@invoke) { block() }

internal class KorteAsyncCache {
    private val lock = Lock()
    @PublishedApi
    internal val deferreds = LinkedHashMap<String, KorteDeferred<*>>()

    fun invalidateAll() {
        lock { deferreds.clear() }
    }

    @Suppress("UNCHECKED_CAST")
    suspend operator fun <T> invoke(key: String, gen: suspend () -> T): T {
        val ctx = coroutineContext
        val deferred =
            lock { (deferreds.getOrPut(key) { KorteDeferred.asyncImmediately(ctx) { gen() } } as KorteDeferred<T>) }
        return deferred.await()
    }

    suspend fun <T> call(key: String, gen: suspend () -> T): T {
        return invoke(key, gen)
    }
}

internal val korteInvalidOp: Nothing get() = throw RuntimeException()
internal fun korteInvalidOp(msg: String): Nothing = throw RuntimeException(msg)

internal class korteExtraProperty<R, T : Any>(val getExtraMap: R.() -> MutableMap<String, Any>, val name: String? = null, val default: () -> T) {
    inline operator fun getValue(thisRef: R, property: KProperty<*>): T =
        getExtraMap(thisRef)[name ?: property.name] as T? ?: default()

    inline operator fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        getExtraMap(thisRef)[name ?: property.name] = value
    }
}

internal class KorteStrReader(val str: String, var pos: Int = 0) {
    val length get() = str.length
    val hasMore get() = pos < length

    inline fun skipWhile(f: (Char) -> Boolean) { while (hasMore && f(peek())) skip() }
    fun skipUntil(f: (Char) -> Boolean): Unit = skipWhile { !f(it) }

    // @TODO: https://youtrack.jetbrains.com/issue/KT-29577
    private fun posSkip(count: Int): Int {
        val out = this.pos
        this.pos += count
        return out
    }

    fun skip() = skip(1)
    fun peekChar(): Char = if (hasMore) this.str[this.pos] else '\u0000'
    fun peek(): Char = if (hasMore) this.str[this.pos] else '\u0000'
    fun read(): Char = if (hasMore) this.str[posSkip(1)] else '\u0000'
    fun unread() = skip(-1)

    fun substr(start: Int, len: Int = length - pos): String {
        val start = (start).coerceIn(0, length)
        val end = (start + len).coerceIn(0, length)
        return this.str.substring(start, end)
    }

    fun skip(count: Int) = this.apply { this.pos += count }
    fun peek(count: Int): String = this.substr(this.pos, count)
    fun read(count: Int): String = this.peek(count).also { skip(count) }

    fun readUntil(v: Char): String? {
        val start = pos
        skipUntil { it == v }
        val end = pos
        return if (hasMore) this.str.substring(start, end) else null
    }

    private inline fun readBlock(callback: () -> Unit): String {
        val start = pos
        callback()
        val end = pos
        return substr(start, end - start)
    }

    fun skipSpaces() = skipWhile { it.isWhitespaceFast() }
    fun readWhile(f: (Char) -> Boolean): String = readBlock { skipWhile(f) }
    fun readUntil(f: (Char) -> Boolean): String = readBlock { skipUntil(f) }
}

internal fun KorteStrReader.readStringLit(reportErrors: Boolean = true): String {
    val out = StringBuilder()
    val quotec = read()
    when (quotec) {
        '"', '\'' -> Unit
        else -> throw RuntimeException("Invalid string literal")
    }
    var closed = false
    while (hasMore) {
        val c = read()
        if (c == '\\') {
            val cc = read()
            out.append(
                when (cc) {
                    '\\' -> '\\'; '/' -> '/'; '\'' -> '\''; '"' -> '"'
                    'b' -> '\b'; 'f' -> '\u000c'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'
                    'u' -> read(4).toInt(0x10).toChar()
                    else -> throw RuntimeException("Invalid char '$cc'")
                }
            )
        } else if (c == quotec) {
            closed = true
            break
        } else {
            out.append(c)
        }
    }
    if (!closed && reportErrors) {
        throw RuntimeException("String literal not closed! '${this.str}'")
    }
    return out.toString()
}

internal fun Char.isWhitespaceFast(): Boolean = this == ' ' || this == '\t' || this == '\r' || this == '\n'

internal infix fun Int.umod(other: Int): Int {
    val rm = this % other
    val remainder = if (rm == -0) 0 else rm
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

internal fun Char.isLetterDigitOrUnderscore(): Boolean = this.isLetterOrDigit() || this == '_' || this == '$'
internal fun Char.isPrintable(): Boolean = this in '\u0020'..'\u007e' || this in '\u00a1'..'\u00ff'

internal const val HEX_DIGITS_LOWER = "0123456789abcdef"
internal fun String.isQuoted(): Boolean = this.startsWith('"') && this.endsWith('"')
internal fun String?.quote(): String = if (this != null) "\"${this.escape()}\"" else "null"
internal fun String.unquote(): String = if (isQuoted()) this.substring(1, this.length - 1).unescape() else this
internal fun String._escape(unicode: Boolean): String {
    val out = StringBuilder(this.length + 16)
    for (c in this) {
        when (c) {
            '\\' -> out.append("\\\\")
            '"' -> out.append("\\\"")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            else -> when {
                !unicode && c in '\u0000'..'\u001f' -> {
                    out.append("\\x")
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 4) and 0xF])
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 0) and 0xF])
                }
                unicode && !c.isPrintable() -> {
                    out.append("\\u")
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 12) and 0xF])
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 8) and 0xF])
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 4) and 0xF])
                    out.append(HEX_DIGITS_LOWER[(c.code ushr 0) and 0xF])
                }
                else -> out.append(c)
            }
        }
    }
    return out.toString()
}
internal fun String.escape(): String = _escape(unicode = false)
internal fun String.escapeUnicode(): String = _escape(unicode = true)
internal fun String.unescape(): String {
    val out = StringBuilder()
    var n = 0
    while (n < this.length) {
        val c = this[n++]
        when (c) {
            '\\' -> {
                val c2 = this[n++]
                when (c2) {
                    '\\' -> out.append('\\')
                    '"' -> out.append('\"')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'x', 'u' -> {
                        val N = if (c2 == 'u') 4 else 2
                        val chars = this.substring(n, n + N)
                        n += N
                        out.append(chars.toInt(16).toChar())
                    }
                    else -> {
                        out.append("\\$c2")
                    }
                }
            }
            else -> out.append(c)
        }
    }
    return out.toString()
}

internal fun String.htmlspecialchars(): String = buildString(this@htmlspecialchars.length + 16) {
    for (it in this@htmlspecialchars) {
        when (it) {
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            else -> append(it)
        }
    }
}
internal fun Json_stringify(value: Any?): String = buildString(128) { this.jsonStringify(value) }
internal fun StringBuilder.jsonStringify(value: Any?) {
    when (value) {
        null -> append("null")
        is Boolean -> append(value == true)
        is Number -> append(value)
        is String -> append('"').append(value.escapeUnicode()).append('"')
        is Iterable<*> -> {
            append('[')
            var first = true
            for (v in value) {
                if (!first) append(',')
                jsonStringify(v)
                first = false
            }
            append(']')
        }
        is Map<*, *> -> {
            append('{')
            var first = true
            for ((k, v) in value) {
                if (!first) append(',')
                jsonStringify(k.toString())
                append(':')
                jsonStringify(v)
                first = false
            }
            append('}')
        }
        else -> TODO()
    }
}

private val formatRegex = Regex("%([-]?\\d+)?(\\w)")
internal fun String.format(vararg params: Any): String {
    var paramIndex = 0
    return formatRegex.replace(this) { mr ->
        val param = params[paramIndex++]
        //println("param: $param")
        val size = mr.groupValues[1]
        val type = mr.groupValues[2]
        val str = when (type) {
            "d" -> (param as Number).toLong().toString()
            "X", "x" -> {
                val res = when (param) {
                    is Int -> param.toUInt().toString(16)
                    else -> (param as Number).toLong().toULong().toString(16)
                }
                if (type == "X") res.uppercase() else res.lowercase()
            }
            else -> "$param"
        }
        val prefix = if (size.startsWith('0')) '0' else ' '
        val asize = size.toIntOrNull()
        var str2 = str
        if (asize != null) {
            while (str2.length < asize) {
                str2 = prefix + str2
            }
        }
        str2
    }
}

internal class Pool<T>(val gen: () -> T) {
    private val allocated = arrayListOf<T>()
    fun alloc(): T = if (allocated.isNotEmpty()) allocated.removeLast() else gen()
    fun free(value: T): Unit = run { allocated.add(value) }
    inline fun <R> alloc(block: (T) -> R): R {
        val v = alloc()
        try {
            return block(v)
        } finally {
            free(v)
        }
    }
}

internal val invalidOp: Nothing get() = throw RuntimeException()
internal fun invalidOp(msg: String): Nothing = throw RuntimeException(msg)

internal class ListReader<T> constructor(val list: List<T>, val ctx: T? = null) {
    class OutOfBoundsException(val list: ListReader<*>, val pos: Int) : RuntimeException()

    var position = 0
    val size: Int get() = list.size
    val eof: Boolean get() = position >= list.size
    val hasMore: Boolean get() = position < list.size
    fun peekOrNull(): T? = list.getOrNull(position)
    fun peek(): T = list.getOrNull(position) ?: throw OutOfBoundsException(this, position)
    fun tryPeek(ahead: Int): T? = list.getOrNull(position + ahead)
    fun skip(count: Int = 1) = this.apply { this.position += count }
    fun read(): T = peek().apply { skip(1) }
    fun tryPrev(): T? = list.getOrNull(position - 1)
    fun prev(): T = tryPrev() ?: throw OutOfBoundsException(this, position - 1)
    fun tryRead(): T? = if (hasMore) read() else null
    fun prevOrContext(): T = tryPrev() ?: ctx ?: throw TODO("Context not defined")
    override fun toString(): String = "ListReader($list)"
}

internal class StrReader(val str: String, var pos: Int = 0) {
    val length get() = str.length
    val hasMore get() = pos < length

    inline fun skipWhile(f: (Char) -> Boolean) { while (hasMore && f(peek())) skip() }
    fun skipUntil(f: (Char) -> Boolean): Unit = skipWhile { !f(it) }

    // @TODO: https://youtrack.jetbrains.com/issue/KT-29577
    private fun posSkip(count: Int): Int {
        val out = this.pos
        this.pos += count
        return out
    }

    fun skip() = skip(1)
    fun peek(): Char = if (hasMore) this.str[this.pos] else '\u0000'
    fun read(): Char = if (hasMore) this.str[posSkip(1)] else '\u0000'
    fun unread() = skip(-1)

    fun substr(start: Int, len: Int = length - pos): String {
        val start = (start).coerceIn(0, length)
        val end = (start + len).coerceIn(0, length)
        return this.str.substring(start, end)
    }

    fun skip(count: Int) = this.apply { this.pos += count }
    fun peek(count: Int): String = this.substr(this.pos, count)
    fun read(count: Int): String = this.peek(count).also { skip(count) }

    fun readUntil(v: Char): String? {
        val start = pos
        skipUntil { it == v }
        val end = pos
        return if (hasMore) this.str.substring(start, end) else null
    }

    private inline fun readBlock(callback: () -> Unit): String {
        val start = pos
        callback()
        val end = pos
        return substr(start, end - start)
    }

    fun skipSpaces() = skipWhile { it.isWhitespaceFast() }
    fun readWhile(f: (Char) -> Boolean): String = readBlock { skipWhile(f) }
    fun readUntil(f: (Char) -> Boolean): String = readBlock { skipUntil(f) }
}

internal fun StrReader.readStringLit(reportErrors: Boolean = true): String {
    val out = StringBuilder()
    val quotec = read()
    when (quotec) {
        '"', '\'' -> Unit
        else -> throw RuntimeException("Invalid string literal")
    }
    var closed = false
    while (hasMore) {
        val c = read()
        if (c == '\\') {
            val cc = read()
            out.append(
                when (cc) {
                    '\\' -> '\\'; '/' -> '/'; '\'' -> '\''; '"' -> '"'
                    'b' -> '\b'; 'f' -> '\u000c'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'
                    'u' -> read(4).toInt(0x10).toChar()
                    else -> throw RuntimeException("Invalid char '$cc'")
                }
            )
        } else if (c == quotec) {
            closed = true
            break
        } else {
            out.append(c)
        }
    }
    if (!closed && reportErrors) {
        throw RuntimeException("String literal not closed! '${this.str}'")
    }
    return out.toString()
}

object Yaml {
    fun decode(str: String) = read(ListReader(tokenize(str)), level = 0)
    fun read(str: String) = read(ListReader(tokenize(str)), level = 0)

    private fun parseStr(toks: List<Token>): Any? {
        if (toks.size == 1 && toks[0] is Token.STR) return toks[0].ustr
        return parseStr(toks.joinToString("") { it.ustr })
    }

    private fun parseStr(str: String) = when (str) {
        "null" -> null
        "true" -> true
        "false" -> false
        else -> str.toIntOrNull() ?: str.toDoubleOrNull() ?: str
    }

    //const val TRACE = true
    const val TRACE = false
    private val EMPTY_SET = setOf<String>()
    private val SET_COMMA_END_ARRAY = setOf(",", "]")

    private fun read(s: ListReader<Token>, level: Int): Any? = s.run {
        var list: ArrayList<Any?>? = null
        var map: MutableMap<String, Any?>? = null
        var lastMapKey: String? = null
        var lastMapValue: Any? = null

        val levelStr = if (TRACE) "  ".repeat(level) else ""

        linehandle@ while (s.hasMore) {
            val token = s.peek()
            val line = token as? Token.LINE
            val lineLevel = line?.level
            if (TRACE && line != null) println("${levelStr}LINE($lineLevel)")
            if (lineLevel != null && lineLevel > level) {
                // child level
                val res = read(s, lineLevel)
                if (list != null) {
                    if (TRACE) println("${levelStr}CHILD.list.add: $res")
                    list.add(res)
                } else {
                    if (TRACE) println("${levelStr}CHILD.return: $res")
                    return res
                }
            } else if (lineLevel != null && lineLevel < level) {
                // parent level
                if (TRACE) println("${levelStr}PARENT: level < line.level")
                break
            } else {
                // current level
                if (line != null) s.read()
                if (s.eof) break
                val item = s.peek()
                when (item.str) {
                    "-" -> {
                        if (s.read().str != "-") invalidOp
                        if (list == null) {
                            list = arrayListOf()
                            if (map != null && lastMapKey != null && lastMapValue == null) {
                                map[lastMapKey] = list
                            }
                        }
                        if (TRACE) println("${levelStr}LIST_ITEM...")
                        val res = read(s, level + 1)
                        if (TRACE) println("${levelStr}LIST_ITEM: $res")
                        list.add(res)
                    }
                    "[" -> {
                        if (s.read().str != "[") invalidOp
                        val olist = arrayListOf<Any?>()
                        array@ while (s.peek().str != "]") {
                            olist += readOrString(s, level, SET_COMMA_END_ARRAY, supportNonSpaceSymbols = false)
                            val p = s.peek().str
                            when (p) {
                                "," -> { s.read(); continue@array }
                                "]" -> break@array
                                else -> invalidOp("Unexpected '$p'")
                            }
                        }
                        if (s.read().str != "]") invalidOp
                        return olist
                    }
                    else -> {
                        val keyIds = s.readId()
                        val sp = s.peekOrNull() ?: Token.EOF
                        if (s.eof || (sp.str != ":" || (sp is Token.SYMBOL && !sp.isNextWhite))) {
                            val key = parseStr(keyIds)
                            if (TRACE) println("${levelStr}LIT: $key")
                            return key
                        } else {
                            val key = parseStr(keyIds).toString()
                            if (map == null) map = LinkedHashMap()
                            if (s.read().str != ":") invalidOp
                            if (TRACE) println("${levelStr}MAP[$key]...")
                            val next = s.peekOrNull()
                            val nextStr = next?.str
                            val hasSpaces = next is Token.SYMBOL && next.isNextWhite
                            val nextIsSpecialSymbol = nextStr == "[" || nextStr == "{" || (nextStr == "-" && hasSpaces)
                            val value = readOrString(s, level, EMPTY_SET, supportNonSpaceSymbols = !nextIsSpecialSymbol)
                            lastMapKey = key
                            lastMapValue = value
                            map[key] = value
                            list = null
                            if (TRACE) println("${levelStr}MAP[$key]: $value")
                        }
                    }
                }
            }
        }

        if (TRACE) println("${levelStr}RETURN: list=$list, map=$map")

        return map ?: list
    }

    private fun ListReader<Token>.readId(): List<Token> {
        val tokens = arrayListOf<Token>()
        while (hasMore) {
            val token = peek()
            if (token is Token.ID || token is Token.STR || ((token is Token.SYMBOL) && token.str == "-") || ((token is Token.SYMBOL) && token.str == ":" && !token.isNextWhite)) {
                tokens.add(token)
                read()
            } else {
                break
            }
        }
        return tokens
    }

    private fun readOrString(s: ListReader<Token>, level: Int, delimiters: Set<String>, supportNonSpaceSymbols: Boolean): Any? {
        val sp = s.peek()
        return if (sp is Token.ID || (supportNonSpaceSymbols && sp is Token.SYMBOL && !sp.isNextWhite)) {
            var str = ""
            str@while (s.hasMore) {
                val p = s.peek()
                if (p is Token.LINE) break@str
                if (p.str in delimiters) break@str
                str += s.read().str
            }
            parseStr(str)
        } else {
            read(s, level + 1)
        }
    }

    fun tokenize(str: String): List<Token> = StrReader(str.replace("\r\n", "\n")).tokenize()

    private fun StrReader.tokenize(): List<Token> {
        val out = arrayListOf<Token>()

        val s = this
        var str = ""
        fun flush() {
            if (str.isNotBlank() && str.isNotEmpty()) {
                out += Token.ID(str.trim()); str = ""
            }
        }

        val indents = ArrayList<Int>()
        linestart@ while (hasMore) {
            // Line start
            flush()
            val indentStr = readWhile(kotlin.Char::isWhitespace).replace("\t", "     ")
            if (indentStr.contains('\n')) continue@linestart  // ignore empty lines with possible additional indent
            val indent = indentStr.length
            if (indents.isEmpty() || indent > indents.last()) {
                indents += indent
            } else {
                while (indents.isNotEmpty() && indent < indents.last()) indents.removeAt(indents.size - 1)
                if (indents.isEmpty()) invalidOp
            }
            val indentLevel = indents.size - 1
            while (out.isNotEmpty() && out.last() is Token.LINE) out.removeAt(out.size - 1)
            out += Token.LINE(indentStr, indentLevel)
            while (hasMore) {
                val c = read()
                when (c) {
                    ':', '-', '[', ']', ',' -> {
                        flush(); out += Token.SYMBOL("$c", peek())
                    }
                    '#' -> {
                        flush(); readUntilLineEnd(); skip(); continue@linestart
                    }
                    '\n' -> {
                        flush(); continue@linestart
                    }
                    '"', '\'' -> {
                        flush()
                        s.unread()
                        out += Token.STR(s.readStringLit())
                    }
                    else -> str += c
                }
            }
        }
        flush()
        return out
    }

    interface Token {
        val str: String
        val ustr get() = str

        object EOF : Token {
            override val str: String = ""
        }

        data class LINE(override val str: String, val level: Int) : Token {
            override fun toString(): String = "LINE($level)"
        }

        data class ID(override val str: String) : Token
        data class STR(override val str: String) : Token {
            override val ustr = str.unquote()
        }

        data class SYMBOL(override val str: String, val next: Char) : Token {
            val isNextWhite: Boolean get() = next == ' ' || next == '\t' || next == '\n' || next == '\r'
        }
    }

    private fun StrReader.readUntilLineEnd() = this.readUntil { it == '\n' }
}
