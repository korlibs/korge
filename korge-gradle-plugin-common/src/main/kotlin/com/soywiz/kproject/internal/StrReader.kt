package com.soywiz.kproject.internal

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

internal fun Char.isWhitespaceFast(): Boolean = this == ' ' || this == '\t' || this == '\r' || this == '\n'
