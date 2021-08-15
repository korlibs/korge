package com.soywiz.korio.lang

// @TODO: UTf-8 variant with seeking points?
// @TODO: trying to be more space efficient for long strings?
// @TODO: while having a decent performance

/**
 * UTF-32 String. Each element in the [codePoints] array represents a character.
 *
 * While on plain [String] it requires surrogate pairs to represent some characters.
 */
//inline
class WString(private val codePoints: IntArray) {
    private val hashCode = codePoints.contentHashCode()
    val length get() = codePoints.size
    operator fun get(index: Int): WChar = WChar(codePoints[index])
    fun codePointAt(index: Int) = this[index].codePoint

    fun substring(startIndex: Int): WString = WString(codePoints.copyOfRange(startIndex, codePoints.size))
    fun substring(startIndex: Int, endIndex: Int): WString = WString(codePoints.copyOfRange(startIndex, endIndex))

    fun toIntArray() = codePoints.copyOf()

    companion object {
        operator fun invoke(string: String) = fromString(string)

        // Decode surrogate pairs
        fun fromString(string: String): WString {
            val codePoints = IntArray(string.length)
            var n = 0
            string.forEachCodePoint { codePoint, error ->
                codePoints[n++] = codePoint
            }
            return WString(codePoints.copyOf(n))
        }
    }

    override fun hashCode(): Int = hashCode
    override fun equals(other: Any?): Boolean = (other is WString) && this.codePoints.contentEquals(other.codePoints)

    // Encode surrogate pairs
    override fun toString(): String {
        val surrogateCount = codePoints.count { it >= 0x10000 }
        val out = CharArray(length + surrogateCount)
        var n = 0
        for (codePoint in codePoints) {
            if (codePoint > 0xFFFF) {
                val U1 = codePoint - 0x10000
                val W1 = 0xD800 or ((U1 ushr 10) and 0x3FF)
                val W2 = 0xDC00 or ((U1 ushr 0) and 0x3FF)
                out[n++] = W1.toChar()
                out[n++] = W2.toChar()
            } else {
                out[n++] = codePoint.toChar()
            }
        }
        return String_fromCharArray(out)
    }
}

inline fun String.forEachCodePoint(block: (codePoint: Int, error: Boolean) -> Unit) {
    val string = this
    var n = 0
    while (n < string.length) {
        var value = string[n++].code
        var error = false
        // High surrogate
        if ((value and 0xD800) == 0xD800) {
            val extra = string[n++].code
            if ((extra and 0xDC00) != 0xDC00) {
                n--
                error = true
            } else {
                val dataHigh = (value and 0x3FF)
                val dataLow = (extra and 0x3FF)

                value = (dataLow or (dataHigh shl 10)) + 0x10000
            }
        }
        block(value, error)
    }
}

fun String.toWString() = WString(this)

inline class WChar(val codePoint: Int) {
    val code: Int get() = codePoint
    fun toChar(): Char = codePoint.toChar()
}
