package com.soywiz.krypto.encoding

object Hex {
    private const val DIGITS = "0123456789ABCDEF"
    val DIGITS_UPPER = DIGITS.toUpperCase()
    val DIGITS_LOWER = DIGITS.toLowerCase()

    fun isHexDigit(c: Char): Boolean = decodeChar(c) >= 0
    fun decodeHexDigit(c: Char): Int {
        val result = decodeChar(c)
        if (result < 0) error("Invalid hex digit '$c'")
        return result
    }
    fun decodeChar(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> -1
    }

    fun encodeCharLower(v: Int): Char = DIGITS_LOWER[v]
    fun encodeCharUpper(v: Int): Char = DIGITS_UPPER[v]

    operator fun invoke(v: String) = decode(v)
    operator fun invoke(v: ByteArray) = encode(v)

    fun appendHexByte(appender: Appendable, value: Int) {
        appender.append(encodeCharLower((value ushr 4) and 0xF))
        appender.append(encodeCharLower((value ushr 0) and 0xF))
    }

    fun decode(str: String, out: ByteArray = ByteArray(str.length / 2)): ByteArray =
        out.also { decode(str) { n, byte -> out[n] = byte } }

    inline fun decode(str: String, out: (Int, Byte) -> Unit) {
        for (n in 0 until str.length / 2) {
            val c0 = decodeHexDigit(str[n * 2 + 0])
            val c1 = decodeHexDigit(str[n * 2 + 1])
            out(n, ((c0 shl 4) or c1).toByte())
        }
    }

    fun encode(src: ByteArray): String = encodeBase(src, DIGITS_LOWER)
    fun encodeLower(src: ByteArray): String = encodeBase(src, DIGITS_LOWER)
    fun encodeUpper(src: ByteArray): String = encodeBase(src, DIGITS_UPPER)

    fun encode(src: ByteArray, dst: Appendable) = encode(src, dst, DIGITS_LOWER)
    fun encodeLower(src: ByteArray, dst: Appendable) = encode(src, dst, DIGITS_LOWER)
    fun encodeUpper(src: ByteArray, dst: Appendable) = encode(src, dst, DIGITS_UPPER)

    private fun encode(src: ByteArray, dst: Appendable, digits: String) {
        for (n in src.indices) {
            val v = src[n].toInt() and 0xFF
            dst.append(digits[(v ushr 4) and 0xF])
            dst.append(digits[(v ushr 0) and 0xF])
        }
    }

    private fun encodeBase(data: ByteArray, digits: String = DIGITS): String = buildString(data.size * 2) {
        encode(data, this, digits)
    }
}

fun Appendable.appendHexByte(value: Int) = Hex.appendHexByte(this, value)

fun String.fromHex(): ByteArray = Hex.decode(this)
val ByteArray.hexLower: String get() = Hex.encodeLower(this)
val ByteArray.hexUpper: String get() = Hex.encodeUpper(this)

fun Char.isHexDigit() = Hex.isHexDigit(this)

val List<String>.unhexIgnoreSpaces get() = joinToString("").unhexIgnoreSpaces
val String.unhexIgnoreSpaces: ByteArray get() = buildString {
    val str = this@unhexIgnoreSpaces
    for (n in 0 until str.length) {
        val c = str[n]
        if (c != ' ' && c != '\t' && c != '\n' && c != '\r') append(c)
    }
}.unhex
val String.unhex get() = Hex.decode(this)
val ByteArray.hex get() = Hex.encodeLower(this)

val Int.hex: String get() = "0x$shex"
val Int.shex: String
    get() {
        var out = ""
        for (n in 0 until 8) {
            val v = (this ushr ((7 - n) * 4)) and 0xF
            out += Hex.encodeCharUpper(v)
        }
        return out
    }

