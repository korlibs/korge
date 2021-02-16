package com.soywiz.korio.util

import com.soywiz.kds.iterators.*
import com.soywiz.korio.lang.*
import kotlin.collections.*
import kotlin.math.*
import com.soywiz.kds.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.internal.max2

class StrReader(val str: String, val file: String = "file", var pos: Int = 0) {
    private val tempCharArray = CharArray(str.length)

    companion object {
        fun literals(vararg lits: String): Literals = Literals.fromList(lits.toList().toTypedArray())
    }

    val length: Int = this.str.length
    val available: Int get() = length - this.pos
    val eof: Boolean get() = (this.pos >= this.str.length)
    val hasMore: Boolean get() = (this.pos < this.str.length)

    fun reset() = run { this.pos = 0 }
    fun createRange(range: IntRange): TRange = createRange(range.start, range.endInclusive + 1)
    fun createRange(start: Int = this.pos, end: Int = this.pos): TRange = TRange(start, end, this)

    fun readRange(length: Int): TRange {
        val range = TRange(this.pos, this.pos + length, this)
        this.pos += length
        return range
    }

    inline fun slice(action: () -> Unit): String? {
        val start = this.pos
        action()
        val end = this.pos
        return if (end > start) this.slice(start, end) else null
    }

    fun slice(start: Int, end: Int): String = this.str.substring(start, end)
    fun peek(count: Int): String = substr(this.pos, count)
    fun peek(): Char = if (hasMore) this.str[this.pos] else '\u0000'
    fun peekChar(): Char = if (hasMore) this.str[this.pos] else '\u0000'
    fun read(count: Int): String = this.peek(count).apply { skip(count) }
    fun skipUntil(char: Char) {
        val skipPos = this.str.indexOf(char, pos)
        pos = (if (skipPos >= 0) skipPos else length)
    }
    fun skipUntilIncluded(char: Char) {
        skipUntil(char)
        if (hasMore && peekChar() == char) skip(1)
    }
    //inline fun skipWhile(check: (Char) -> Boolean) = run { while (check(this.peekChar())) this.skip(1) }
    inline fun skipWhile(filter: (Char) -> Boolean) {
        while (hasMore && filter(this.peekChar())) {
            this.readChar()
        }
    }

    inline fun skipUntil(filter: (Char) -> Boolean) =
        run { while (hasMore && !filter(this.peekChar())) this.readChar() }

    inline fun matchWhile(check: (Char) -> Boolean): String? = slice { skipWhile(check) }

    fun readUntil(char: Char) = this.slice { skipUntil(char) }
    fun readUntilIncluded(char: Char) = this.slice { skipUntilIncluded(char) }
    inline fun readWhile(filter: (Char) -> Boolean) = this.slice { skipWhile(filter) } ?: ""
    inline fun readUntil(filter: (Char) -> Boolean) = this.slice { skipUntil(filter) } ?: ""
    fun unread(count: Int = 1): StrReader {
        this.pos -= count
        return this
    }
    fun readChar(): Char = if (hasMore) this.str[posSkip(1)] else '\u0000'
    fun read(): Char = if (hasMore) this.str[posSkip(1)] else '\u0000'
    // @TODO: https://youtrack.jetbrains.com/issue/KT-29577
    private fun posSkip(count: Int): Int {
        val out = this.pos
        this.pos += count
        return out
    }

    fun readRemaining(): String = read(available)

    fun readExpect(expected: String): String {
        val readed = this.read(expected.length)
        if (readed != expected) throw IllegalArgumentException("Expected '$expected' but found '$readed' at $pos")
        return readed
    }

    fun skipExpect(expected: Char) {
        val readed = this.readChar()
        if (readed != expected) throw IllegalArgumentException("Expected '$expected' but found '$readed' at $pos")
    }

    @Deprecated("This overload is slow")
    fun expect(expected: Char): String {
        skipExpect(expected)
        this.unread()
        return this.read(1)
    }

    fun skip(count: Int = 1): StrReader {
        this.pos += count
        return this
    }

    private fun substr(pos: Int, length: Int): String {
        return this.str.substring(min2(pos, this.length), min2(pos + length, this.length))
    }

    fun tryLit(lit: String): String? {
        if (substr(this.pos, lit.length) != lit) return null
        this.pos += lit.length
        return lit
    }

    fun tryLitRange(lit: String): TRange? =
        if (substr(this.pos, lit.length) == lit) this.readRange(lit.length) else null

    fun matchLit(lit: String): String? = tryLit(lit)
    fun matchLitRange(lit: String): TRange? = tryLitRange(lit)

    fun matchLitListRange(lits: Literals): TRange? {
        lits.lengths.fastForEach { len ->
            if (lits.contains(substr(this.pos, len))) return this.readRange(len)
        }
        return null
    }

    fun skipSpaces() = this.apply { this.skipWhile { it.isWhitespaceFast() } }

    fun matchIdentifier() = matchWhile { it.isLetterDigitOrUnderscore() || it == '-' || it == '~' || it == ':' }
    fun matchSingleOrDoubleQuoteString(): String? {
        return when (this.peekChar()) {
            '\'', '"' -> {
                this.slice {
                    val quoteType = this.readChar()
                    this.readUntil(quoteType)
                    this.readChar()
                }
            }
            else -> null
        }
    }

    fun tryRegex(v: Regex): String? {
        val result = v.find(this.str.substring(this.pos)) ?: return null
        val m = result.groups[0]!!.value
        this.pos += m.length
        return m
    }

    fun tryRegexRange(v: Regex): TRange? {
        val result = v.find(this.str.substring(this.pos)) ?: return null
        return this.readRange(result.groups[0]!!.value.length)
    }

    fun matchStartEnd(start: String, end: String): String? {
        if (substr(this.pos, start.length) != start) return null
        val startIndex = this.pos
        val index = this.str.indexOf(end, this.pos)
        if (index < 0) return null
        //trace(index);
        this.pos = index + end.length
        return this.slice(startIndex, this.pos)
    }

    fun clone(): StrReader = StrReader(str, file, pos)

    fun tryRead(str: String): Boolean {
        if (peek(str.length) == str) {
            skip(str.length)
            return true
        }
        return false
    }

    class Literals(
        private val lits: Array<String>,
        private val map: MutableMap<String, Boolean>,
        val lengths: Array<Int>
    ) {
        companion object {
            fun invoke(vararg lits: String): Literals =
                fromList(lits.toCollection(arrayListOf<String>()).toTypedArray())

            //fun invoke(lits:Array<String>): Literals = fromList(lits)
            fun fromList(lits: Array<String>): Literals {
                val lengths = lits.map { it.length }.sorted().reversed().distinct().toTypedArray()
                val map = linkedMapOf<String, Boolean>()
                lits.fastForEach { lit ->
                    map[lit] = true
                }
                return Literals(lits, map, lengths)
            }
        }

        fun contains(lit: String) = map.containsKey(lit)

        fun matchAt(str: String, offset: Int): String? {
            lengths.fastForEach { len ->
                val id = str.substr(offset, len)
                if (contains(id)) return id
            }
            return null
        }

        override fun toString() = "Literals(${lits.joinToString(" ")})"
    }

    class TRange(val min: Int, val max: Int, val reader: StrReader) {
        companion object {
            fun combine(a: TRange, b: TRange): TRange {
                return TRange(min2(a.min, b.min), max2(a.max, b.max), a.reader)
            }

            fun combineList(list: List<TRange>): TRange? {
                if (list.isEmpty()) return null
                val first = list[0]
                var min = first.min
                var max = first.max
                list.fastForEach { i ->
                    min = min2(min, i.min)
                    max = max2(max, i.max)
                }
                return TRange(min, max, first.reader)
            }

            fun createDummy() = TRange(0, 0, StrReader(""))
        }

        fun contains(index: Int): Boolean = index >= this.min && index <= this.max
        override fun toString() = "$min:$max"

        val file: String get() = this.reader.file
        val text: String get () = this.reader.slice(this.min, this.max)

        fun startEmptyRange(): TRange = TRange(this.min, this.min, this.reader)
        fun endEmptyRange(): TRange = TRange(this.max, this.max, this.reader)
        fun displace(offset: Int): TRange = TRange(this.min + offset, this.max + offset, this.reader)
    }

    fun readFixedSizeInt(count: Int, radix: Int = 10): Int {
        val readCount = min2(available, count)
        skip(readCount)
        return NumberParser.parseInt(str, pos - readCount, pos, radix)
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
                        else -> throw IOException("Invalid char '$cc'")
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
        return String_fromCharArray(out, 0, outp)
    }


    fun tryReadInt(default: Int): Int {
        var digitCount = 0
        var integral = 0
        var mult = 1
        loop@ while (!eof) {
            when (val c = peek()) {
                '-' -> {
                    skip(1)
                    mult *= -1
                }
                in '0'..'9' -> {
                    val digit = c - '0'
                    skip(1)
                    digitCount++
                    integral *= 10
                    integral += digit
                }
                else -> {
                    break@loop
                }
            }
        }
        return if (digitCount == 0) default else integral
    }

    fun tryReadNumber(default: Double = Double.NaN): Double {
        val start = pos
        skipWhile {
            @Suppress("ConvertTwoComparisonsToRangeCheck")
            (it >= '0' && it <= '9') || (it == '+') || (it == '-') || (it == 'e') || (it == 'E') || (it == '.')
        }
        val end = pos
        if (end == start) return default
        return NumberParser.parseDouble(this.str, start, end)
    }

    fun tryExpect(str: String): Boolean {
        for (n in 0 until str.length) {
            if (this.peekOffset(n) != str[n]) return false
        }
        skip(str.length)
        return true
    }

    fun tryExpect(str: Char): Boolean {
        if (peekChar() != str) return false
        skip(1)
        return true
    }

    fun peekOffset(offset: Int = 0): Char = this.str.getOrElse(pos + offset) { '\u0000' }

    fun readFloats(list: FloatArrayList = FloatArrayList(7)): FloatArrayList {
        while (!eof) {
            val pos0 = pos
            val float = skipSpaces().tryReadNumber().toFloat()
            skipSpaces()
            val pos1 = pos
            if (pos1 == pos0) error("Invalid number at $pos0 in '$str'")
            list.add(float)
            //println("float: $float, ${reader.pos}/${reader.length}")
        }
        return list
    }

    fun readIds(list: ArrayList<String> = ArrayList(7)): ArrayList<String> {
        while (!eof) {
            val pos0 = pos
            val id = skipSpaces().tryReadId() ?: ""
            skipSpaces()
            val pos1 = pos
            if (pos1 == pos0) error("Invalid identifier at $pos0 in '$str'")
            list.add(id)
            //println("float: $float, ${reader.pos}/${reader.length}")
        }
        return list
    }

    fun readInts(list: IntArrayList = IntArrayList(7)): IntArrayList {
        while (!eof) {
            val pos0 = pos
            val v = skipSpaces().tryReadInt(0)
            skipSpaces()
            val pos1 = pos
            if (pos1 == pos0) error("Invalid int at $pos0 in '$str'")
            list.add(v)
            //println("float: $float, ${reader.pos}/${reader.length}")
        }
        return list
    }

    fun tryReadId(): String? {
        val start = pos
        skipWhile {
            @Suppress("ConvertTwoComparisonsToRangeCheck")
            (it >= '0' && it <= '9') || (it >= 'a' && it <= 'z') || (it >= 'A' && it <= 'Z') || (it == '_') || (it == '.')
        }
        val end = pos
        if (end == start) return null
        return this.str.substring(start, end)
    }
}

fun String.reader(file: String = "file", pos: Int = 0): StrReader = StrReader(this, file, pos)
