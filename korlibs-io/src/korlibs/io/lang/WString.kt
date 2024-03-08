package korlibs.io.lang

import korlibs.math.clamp

// @TODO: UTf-8 variant with seeking points?
// @TODO: trying to be more space efficient for long strings?
// @TODO: while having a decent performance

/**
 * UTF-32 String. Each element in the [codePoints] array represents a character.
 *
 * While on plain [String] it requires surrogate pairs to represent some characters.
 */
//inline
class WString private constructor(private val codePoints: IntArray, private val string: String) {
    val length get() = codePoints.size

    operator fun get(index: Int): WChar = WChar(codePoints[index])
    inline fun getOrElse(index: Int, defaultValue: (Int) -> WChar): WChar {
        if (index < 0 || index >= length) return defaultValue(index)
        return this[index]
    }
    fun codePointAt(index: Int) = this[index].codePoint

    fun substring(startIndex: Int): WString = WString(codePoints.copyOfRange(startIndex, codePoints.size), string.substring(startIndex))
    fun substring(startIndex: Int, endIndex: Int): WString = WString(codePoints.copyOfRange(startIndex, endIndex), string.substring(startIndex, endIndex))

    fun toCodePointIntArray() = codePoints.copyOf()

    companion object {
        private val EMPTY = WString(intArrayOf(), "")

        operator fun invoke(codePoints: IntArray) = fromCodePoints(codePoints)
        operator fun invoke(string: String) = fromString(string)

        // Decode surrogate pairs
        fun fromString(string: String): WString {
            if (string == "") return EMPTY
            val codePoints = IntArray(string.length)
            val length = string.forEachCodePoint { index, codePoint, error -> codePoints[index] = codePoint }
            return WString(codePoints.copyOf(length), string)
        }

        fun fromCodePoints(codePoints: IntArray): WString {
            val surrogateCount = codePoints.count { it >= 0x10000 }
            val out = StringBuilder(codePoints.size + surrogateCount)
            for (codePoint in codePoints) {
                if (codePoint > 0xFFFF) {
                    val U1 = codePoint - 0x10000
                    val W1 = 0xD800 or ((U1 ushr 10) and 0x3FF)
                    val W2 = 0xDC00 or ((U1 ushr 0) and 0x3FF)
                    out.append(W1.toChar())
                    out.append(W2.toChar())
                } else {
                    out.append(codePoint.toChar())
                }
            }
            return WString(codePoints, out.toString())
        }
    }

    private var cachedHashCodeValue = 0
    private var cachedHashCode = false

    override fun hashCode(): Int {
        if (!cachedHashCode) {
            cachedHashCode = true
            cachedHashCodeValue = this.codePoints.contentHashCode()
        }
        return cachedHashCodeValue
    }
    override fun equals(other: Any?): Boolean = (other is WString) && this.codePoints.contentEquals(other.codePoints) //this.string == other.string

    // Encode surrogate pairs
    override fun toString(): String = string
}

inline fun WString.forEachCodePoint(block: (index: Int, codePoint: Int, error: Boolean) -> Unit): Int {
    for (n in 0 until this.length) {
        block(n, this[n].code, false)
    }
    return this.length
}

inline fun String.forEachCodePoint(block: (index: Int, codePoint: Int, error: Boolean) -> Unit): Int {
    val string = this
    var m = 0
    var n = 0
    while (n < string.length) {
        var value = string[n++].code
        var error = false
        // High surrogate
        if ((value and 0xF800) == 0xD800 && n < string.length) {
            val extra = string[n++].code
            if ((extra and 0xFC00) != 0xDC00) {
                n--
                error = true
            } else {
                val dataHigh = (value and 0x3FF)
                val dataLow = (extra and 0x3FF)

                value = (dataLow or (dataHigh shl 10)) + 0x10000
            }
        }
        block(m++, value, error)
    }
    return m
}

fun String.toWString() = WString(this)

fun WString.substr(start: Int, length: Int = this.length): WString {
    val low = (if (start >= 0) start else this.length + start).clamp(0, this.length)
    val high = (if (length >= 0) low + length else this.length + length).clamp(0, this.length)
    return if (high < low) WString("") else this.substring(low, high)
}


inline class WChar(val codePoint: Int) {
    val code: Int get() = codePoint
    fun toChar(): Char = codePoint.toChar()
    fun toInt(): Int = codePoint
}

class WStringReader(val str: WString, var position: Int = 0) {
    constructor(str: String, position: Int = 0) : this(str.toWString(), position)
    val length: Int get() = str.length
    val available: Int get() = str.length - position
    val eof: Boolean get() = position >= str.length
    val hasMore: Boolean get() = !eof
    fun read(): WChar = str[position++]
    fun peek(offset: Int = 0): WChar = str.getOrElse(this.position + offset) { WChar(0) }
    fun skip(count: Int) { position += count }
    fun substr(offset: Int, len: Int = str.length): WString = str.substr(this.position + offset, len)
}

inline fun <T> WStringReader?.keep(block: () -> T): T {
    //return ::position.keep { block() } // @TODO: Is this optimized in Kotlin?
    val old = this?.position ?: 0
    try {
        return block()
    } finally {
        this?.position = old
    }
}
