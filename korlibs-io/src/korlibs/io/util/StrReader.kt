package korlibs.io.util

import korlibs.datastructure.*
import korlibs.datastructure.iterators.fastForEach
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.util.*
import kotlin.collections.*
import kotlin.math.max
import kotlin.math.min

/**
 * @TODO: Make this an interface, but inline functions would need to be extension methods breaking source-compatibility
 **/
abstract class BaseStrReader : SimpleStrReader {
    abstract val eof: Boolean
    abstract val pos: Int
    override val hasMore: Boolean get() = !eof

    abstract fun peekOffset(offset: Int = 0): Char
    abstract fun peek(count: Int): String

    override fun peekChar(): Char = peekOffset(0)
    override fun readChar(): Char {
        val out = peekChar()
        skip(1)
        return out
    }
    open fun readUntil(char: Char): String? {
        val out = StringBuilder()
        while (hasMore) {
            val result = peekChar()
            if (result == char) break
            out.append(result)
            skip(1)
        }
        return out.toString()
    }

    abstract fun skip(count: Int = 1): BaseStrReader
    fun skipExpect(expected: Char) {
        val readed = this.readChar()
        if (readed != expected) throw IllegalArgumentException("Expected '$expected' but found '$readed' at $pos")
    }
    abstract fun tryLit(lit: String, consume: Boolean = true): String?

    abstract fun clone(): BaseStrReader

    open fun startBuffering(): Int = pos
    abstract fun endBuffering(start: Int): String

    inline fun slice(action: () -> Unit): String {
        val start = startBuffering()
        try {
            action()
        } finally {
            return endBuffering(start)
        }
    }

    open fun skipSpaces(): BaseStrReader {
        this.skipWhile { it.isWhitespaceFast() }
        return this
    }

    fun tryRead(str: String): Boolean = tryLit(str, consume = true) != null

    fun matchIdentifier(): String? = matchWhile { it.isLetterDigitOrUnderscore() || it == '-' || it == '~' || it == ':' }.takeIf { it.isNotEmpty() }

    fun matchSingleOrDoubleQuoteString(): String? = when (this.peekChar()) {
        '\'', '"' -> {
            this.slice {
                val quoteType = this.readChar()
                this.readUntil(quoteType)
                this.readChar()
            }
        }
        else -> null
    }

    fun tryExpect(str: String, consume: Boolean = true): Boolean {
        for (n in 0 until str.length) {
            if (this.peekOffset(n) != str[n]) return false
        }
        if (consume) skip(str.length)
        return true
    }

    fun tryExpect(str: Char): Boolean {
        if (peekChar() != str) return false
        skip(1)
        return true
    }

    fun read(count: Int): String = this.peek(count).apply { skip(count) }

    fun readExpect(expected: String): String {
        val readed = this.read(expected.length)
        if (readed != expected) throw IllegalArgumentException("Expected '$expected' but found '$readed' at $pos")
        return readed
    }

    inline fun skipWhile(filter: (Char) -> Boolean) {
        while (hasMore && filter(this.peekChar())) {
            this.readChar()
        }
    }

    inline fun skipUntil(filter: (Char) -> Boolean): Unit {
        while (hasMore && !filter(this.peekChar())) this.readChar()
    }

    inline fun matchWhile(check: (Char) -> Boolean): String = slice { skipWhile(check) }
}

class CharReaderStrReader(val reader: CharReader) : BaseStrReader() {
    val deque = CharDeque()

    private var buffer = StringBuilder()
    private var bufferingPos = -1
    private var _eof: Boolean = false
    override val eof: Boolean get() {
        ensure(1)
        return _eof && deque.isEmpty()
    }
    override var pos: Int = 0
        private set

    fun ensure(count: Int) {
        if (count > 0 && deque.size < count) {
            val read = reader.read(count)
            if (read.isEmpty()) _eof = true
            deque.addAll(read.toCharArray())
        }
    }

    override fun peekOffset(offset: Int): Char {
        check(offset >= 0)
        ensure(offset + 1)
        if (deque.size <= offset) return '\u0000'
        return deque[offset]
    }

    override fun peek(count: Int): String {
        ensure(count)
        return buildString(count) {
            for (n in 0 until min(deque.size, count)) append(deque[n])
        }
    }

    override fun skip(count: Int): BaseStrReader {
        ensure(count)
        for (n in 0 until count) {
            val res = deque.removeFirst()
            if (bufferingPos >= 0) {
                buffer.append(res)
            }
            pos++
        }
        return this
    }

    override fun tryLit(lit: String, consume: Boolean): String? {
        ensure(lit.length)
        for (n in 0 until lit.length) {
            if (peekOffset(n) != lit[n]) return null
        }
        skip(lit.length)
        return lit
    }

    override fun clone(): BaseStrReader {
        //TODO("Not yet implemented")

        return CharReaderStrReader(reader.clone()).also {
            for (n in 0 until this.deque.size) it.deque.add(this.deque[n])
            it.buffer.append(this.buffer)
            it._eof = this._eof
            it.bufferingPos = this.bufferingPos
            it.pos = this.pos
        }
    }

    override fun startBuffering(): Int {
        bufferingPos = pos
        return pos
    }

    override fun endBuffering(start: Int): String {
        val out = buffer.substring(start - bufferingPos, pos - bufferingPos)
        if (bufferingPos == start) {
            bufferingPos = -1
            buffer.clear()
        }
        return out
    }
}

class StrReader(val str: String, val file: String = "file", override var pos: Int = 0) : BaseStrReader() {
    private val tempCharArray = CharArray(str.length)

    companion object {
        fun literals(vararg lits: String): Literals = Literals.fromList(lits.toList().toTypedArray())
    }

    val length: Int = this.str.length
    val available: Int get() = length - this.pos
    override val eof: Boolean get() = (this.pos >= this.str.length)

    fun reset() { this.pos = 0 }
    fun createRange(range: IntRange): TRange = createRange(range.start, range.endInclusive + 1)
    fun createRange(start: Int = this.pos, end: Int = this.pos): TRange = TRange(start, end, this)

    fun readRange(length: Int): TRange {
        val range = TRange(this.pos, this.pos + length, this)
        this.pos += length
        return range
    }

    override fun endBuffering(start: Int): String = slice(start, pos)

    override fun skipSpaces(): StrReader {
        super.skipSpaces()
        return this
    }
    fun slice(start: Int, end: Int): String {
        if (start == end) return ""
        return this.str.substring(start, end)
    }
    override fun peek(count: Int): String = substr(this.pos, count)
    override fun peekChar(): Char = if (hasMore) this.str[this.pos] else '\u0000'

    fun skipUntil(char: Char) {
        val skipPos = this.str.indexOf(char, pos)
        pos = (if (skipPos >= 0) skipPos else length)
    }
    fun skipUntilIncluded(char: Char) {
        skipUntil(char)
        if (hasMore && peekChar() == char) skip(1)
    }
    //inline fun skipWhile(check: (Char) -> Boolean) = run { while (check(this.peekChar())) this.skip(1) }
    override fun readUntil(char: Char) = this.slice { skipUntil(char) }
    fun readUntilIncluded(char: Char) = this.slice { skipUntilIncluded(char) }
    inline fun readWhile(filter: (Char) -> Boolean) = this.slice { skipWhile(filter) } ?: ""
    inline fun readUntil(filter: (Char) -> Boolean) = this.slice { skipUntil(filter) } ?: ""
    fun unread(count: Int = 1): StrReader {
        this.pos -= count
        return this
    }
    override fun readChar(): Char = if (hasMore) this.str[posSkip(1)] else '\u0000'
    fun read(): Char = if (hasMore) this.str[posSkip(1)] else '\u0000'
    // @TODO: https://youtrack.jetbrains.com/issue/KT-29577
    private fun posSkip(count: Int): Int {
        val out = this.pos
        this.pos += count
        return out
    }

    fun readRemaining(): String = read(available)

    override fun skip(count: Int): StrReader {
        this.pos += count
        return this
    }

    private fun substr(pos: Int, length: Int): String {
        return this.str.substring(min(pos, this.length), min(pos + length, this.length))
    }

    override fun tryLit(lit: String, consume: Boolean): String? {
        if (!String.substringEquals(this.str, this.pos, lit, 0, lit.length)) return null
        if (consume) this.pos += lit.length
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

    override fun clone(): StrReader = StrReader(str, file, pos)

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
                return TRange(min(a.min, b.min), max(a.max, b.max), a.reader)
            }

            fun combineList(list: List<TRange>): TRange? {
                if (list.isEmpty()) return null
                val first = list[0]
                var min = first.min
                var max = first.max
                list.fastForEach { i ->
                    min = min(min, i.min)
                    max = max(max, i.max)
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
        val readCount = min(available, count)
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
            when (val c = peekChar()) {
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

    override fun peekOffset(offset: Int): Char = this.str.getOrElse(pos + offset) { '\u0000' }

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
