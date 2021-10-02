package com.soywiz.krypto.encoding

object Hex {
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

    fun encodeCharLower(v: Int): Char = if (v >= 10) ('a' + (v - 10)) else ('0' + v)
    fun encodeCharUpper(v: Int): Char = if (v >= 10) ('A' + (v - 10)) else ('0' + v)

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

    fun encode(src: ByteArray): String = encodeLower(src)
    fun encodeLower(src: ByteArray): String = encodeBase(src) { encodeCharLower(it) }
    fun encodeUpper(src: ByteArray): String = encodeBase(src) { encodeCharUpper(it) }

    fun encode(src: ByteArray, dst: Appendable) = encodeLower(src, dst)
    fun encodeLower(src: ByteArray, dst: Appendable) = encode(src, dst) { encodeCharLower(it) }
    fun encodeUpper(src: ByteArray, dst: Appendable) = encode(src, dst) { encodeCharUpper(it) }

    inline private fun encode(src: ByteArray, dst: Appendable, digits: (Int) -> Char) {
        for (n in src.indices) {
            val v = src[n].toInt() and 0xFF
            dst.append(digits((v ushr 4) and 0xF))
            dst.append(digits((v ushr 0) and 0xF))
        }
    }

    inline private fun encodeBase(data: ByteArray, digits: (Int) -> Char): String = buildString(data.size * 2) {
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
    get() = buildString(8) {
        for (n in 0 until 8) {
            val v = (this@shex ushr ((7 - n) * 4)) and 0xF
            append(Hex.encodeCharUpper(v))
        }
    }


val Byte.hex: String get() = "0x$shex"
val Byte.shex: String
    get() = buildString(2) {
        append(Hex.encodeCharUpper((this@shex.toInt() ushr 4) and 0xF))
        append(Hex.encodeCharUpper((this@shex.toInt() ushr 0) and 0xF))
    }

