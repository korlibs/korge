package com.soywiz.korio.lang

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.internal.*
import kotlin.native.concurrent.SharedImmutable

abstract class Charset(val name: String) {
    // Just an estimation, might not be accurate, but hopefully will help setting StringBuilder and ByteArrayBuilder to a better initial capacity
    open fun estimateNumberOfCharactersForBytes(nbytes: Int): Int = nbytes * 2
    open fun estimateNumberOfBytesForCharacters(nchars: Int): Int = nchars * 2

	abstract fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int = 0, end: Int = src.length)
	abstract fun decode(out: StringBuilder, src: ByteArray, start: Int = 0, end: Int = src.size)

	companion object {
		fun forName(name: String): Charset {
			return UTF8
		}

        fun StringBuilder.appendCodePointV(codePoint: Int) {
            if (codePoint in 0xD800..0xDFFF || codePoint > 0xFFFF) {
                val U0 = codePoint - 0x10000
                val hs = U0.extract(10, 10)
                val ls = U0.extract(0, 10)
                append(((0b110110 shl 10) or (hs)).toChar())
                append(((0b110111 shl 10) or (ls)).toChar())
            } else {
                append(codePoint.toChar())
            }
        }

        inline fun decodeCodePoints(src: CharSequence, start: Int, end: Int, block: (codePoint: Int) -> Unit) {
            var highSurrogate = 0
            loop@for (n in start until end) {
                val char = src[n].toInt()
                val codePoint = if (char in 0xD800..0xDFFF) {
                    when (char.extract(10, 6)) {
                        0b110110 -> {
                            highSurrogate = char and 0x3FF
                            continue@loop
                        }
                        0b110111 -> {
                            0x10000 + ((highSurrogate shl 10) or (char and 0x3FF))
                        }
                        else -> error("Unknown $char")
                    }
                } else {
                    char
                }
                block(codePoint)
            }
        }
	}
}

open class UTC8CharsetBase(name: String) : Charset(name) {
    override fun estimateNumberOfCharactersForBytes(nbytes: Int): Int = nbytes * 2
    override fun estimateNumberOfBytesForCharacters(nchars: Int): Int = nchars * 2

    private fun createByte(codePoint: Int, shift: Int): Int = codePoint shr shift and 0x3F or 0x80

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
        decodeCodePoints(src, start, end) { codePoint ->
            if (codePoint and 0x7F.inv() == 0) { // 1-byte sequence
                out.append(codePoint.toByte())
            } else {
                when {
                    codePoint and 0x7FF.inv() == 0 -> // 2-byte sequence
                        out.append((codePoint shr 6 and 0x1F or 0xC0).toByte())
                    codePoint and 0xFFFF.inv() == 0 -> { // 3-byte sequence
                        out.append((codePoint shr 12 and 0x0F or 0xE0).toByte())
                        out.append((createByte(codePoint, 6)).toByte())
                    }
                    codePoint and -0x200000 == 0 -> { // 4-byte sequence
                        out.append((codePoint shr 18 and 0x07 or 0xF0).toByte())
                        out.append((createByte(codePoint, 12)).toByte())
                        out.append((createByte(codePoint, 6)).toByte())
                    }
                }
                out.append((codePoint and 0x3F or 0x80).toByte())
            }
        }
	}

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		if ((start < 0 || start > src.size) || (end < 0 || end > src.size)) error("Out of bounds")
		var i = start
		while (i < end) {
			val c = src[i].toInt() and 0xFF

            when (c shr 4) {
                in 0b0000..0b0111 -> {
                    // 0xxxxxxx
                    out.appendCodePointV(c)
                    i += 1
                }
                in 0b1100..0b1101 -> {
                    // 110x xxxx   10xx xxxx
                    out.appendCodePointV((c and 0x1F shl 6 or (src[i + 1].toInt() and 0x3F)))
                    i += 2
                }
                0b1110 -> {
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    out.appendCodePointV((c and 0x0F shl 12 or (src[i + 1].toInt() and 0x3F shl 6) or (src[i + 2].toInt() and 0x3F)))
                    i += 3
                }
                0b1111 -> {
                    // 1111 0xxx 10xx xxxx  10xx xxxx  10xx xxxx
                    out.appendCodePointV(0
                        .insert(src[i + 0].toInt().extract(0, 3), 18, 3)
                        .insert(src[i + 1].toInt().extract(0, 6), 12, 6)
                        .insert(src[i + 2].toInt().extract(0, 6), 6, 6)
                        .insert(src[i + 3].toInt().extract(0, 6), 0, 6)
                    )
                    i += 4

                }
                else -> {
                    out.append('\uFFFD')
                    i += 1
                    //TODO("${c shr 4}")
                }
            }
		}
	}
}

abstract class BaseSingleByteCharset(name: String) : Charset(name) {
    override fun estimateNumberOfCharactersForBytes(nbytes: Int): Int = nbytes
    override fun estimateNumberOfBytesForCharacters(nchars: Int): Int = nchars
}

open class SingleByteCharset(name: String, val conv: String) : BaseSingleByteCharset(name) {
	val v: IntIntMap = IntIntMap().apply {
		for (n in 0 until conv.length) this[conv[n].toInt()] = n
	}

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		for (n in start until end) {
			val c = src[n].toInt()
			out.append(if (v.contains(c)) v[c].toByte() else '?'.toByte())
		}
	}

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		for (n in start until end) {
			out.append(conv[src[n].toInt() and 0xFF])
		}
	}
}

object ISO_8859_1 : SingleByteCharset("ISO-8859-1", buildString { for (n in 0 until 256) append(n.toChar()) })

@SharedImmutable
expect val UTF8: Charset

class UTF16Charset(val le: Boolean) : Charset("UTF-16-" + (if (le) "LE" else "BE")) {
    override fun estimateNumberOfCharactersForBytes(nbytes: Int): Int = nbytes * 2
    override fun estimateNumberOfBytesForCharacters(nchars: Int): Int = nchars * 2

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		for (n in start until end step 2) out.append(src.readS16(n, le).toChar())
	}

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		val temp = ByteArray(2)
		for (n in start until end) {
			temp.write16(0, src[n].toInt(), le)
			out.append(temp)
		}
	}
}

object ASCII : BaseSingleByteCharset("ASCII") {
	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		for (n in start until end) out.append(src[n].toByte())
	}

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		for (n in start until end) out.append(src[n].toChar())
	}
}

@SharedImmutable
val LATIN1 = ISO_8859_1
@SharedImmutable
val UTF16_LE = UTF16Charset(le = true)
@SharedImmutable
val UTF16_BE = UTF16Charset(le = false)

object Charsets {
	val UTF8 get() = com.soywiz.korio.lang.UTF8
	val LATIN1 get() = com.soywiz.korio.lang.LATIN1
	val UTF16_LE get() = com.soywiz.korio.lang.UTF16_LE
	val UTF16_BE get() = com.soywiz.korio.lang.UTF16_BE
}

fun String.toByteArray(charset: Charset = UTF8): ByteArray {
	val out = ByteArrayBuilder(charset.estimateNumberOfBytesForCharacters(this.length))
	charset.encode(out, this)
	return out.toByteArray()
}

fun ByteArray.toString(charset: Charset): String {
	val out = StringBuilder(charset.estimateNumberOfCharactersForBytes(this.size))
	charset.decode(out, this)
	return out.toString()
}

fun ByteArray.readStringz(o: Int, size: Int, charset: Charset = UTF8): String {
	var idx = o
	val stop = min2(this.size, o + size)
	while (idx < stop) {
		if (this[idx] == 0.toByte()) break
		idx++
	}
	return this.copyOfRange(o, idx).toString(charset)
}

fun ByteArray.readStringz(o: Int, charset: Charset = UTF8): String {
	return readStringz(o, size - o, charset)
}

fun ByteArray.readString(o: Int, size: Int, charset: Charset = UTF8): String {
	return this.copyOfRange(o, o + size).toString(charset)
}
