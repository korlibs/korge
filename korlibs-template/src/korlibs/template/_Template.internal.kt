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
    val out = StringBuilder(this.length)
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
