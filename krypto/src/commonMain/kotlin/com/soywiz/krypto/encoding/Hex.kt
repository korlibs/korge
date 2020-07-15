package com.soywiz.krypto.encoding

object Hex {
    val DIGITS = "0123456789ABCDEF"
    val DIGITS_UPPER = DIGITS.toUpperCase()
    val DIGITS_LOWER = DIGITS.toLowerCase()

    fun isHexDigit(c: Char) = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
    fun decodeHexDigit(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> (c - 'a') + 10
        in 'A'..'F' -> (c - 'A') + 10
        else -> error("Invalid hex digit '$c'")
    }

    operator fun invoke(v: String) = decode(v)
    operator fun invoke(v: ByteArray) = encode(v)

    fun decode(str: String): ByteArray {
        val out = ByteArray(str.length / 2)
        var m = 0
        for (n in out.indices) {
            val c0 = decodeHexDigit(str[m++])
            val c1 = decodeHexDigit(str[m++])
            out[n] = ((c0 shl 4) or c1).toByte()
        }
        return out
    }

    fun encode(src: ByteArray): String = encodeBase(src,DIGITS_LOWER)
    fun encodeLower(src: ByteArray): String = encodeBase(src, DIGITS_LOWER)
    fun encodeUpper(src: ByteArray): String = encodeBase(src, DIGITS_UPPER)

    private fun encodeBase(data: ByteArray, digits: String = DIGITS): String = buildString(data.size * 2) {
        for (n in data.indices) {
            val v = data[n].toInt() and 0xFF
            append(digits[(v ushr 4) and 0xF])
            append(digits[(v ushr 0) and 0xF])
        }
    }
}

fun String.fromHex(): ByteArray = Hex.decode(this)
val ByteArray.hexLower: String get() = Hex.encodeLower(this)
val ByteArray.hexUpper: String get() = Hex.encodeUpper(this)
val ByteArray.hex: String get() = Hex.encodeLower(this)
