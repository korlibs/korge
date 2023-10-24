package korlibs.io.lang

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.datastructure.lock.*
import korlibs.memory.*
import kotlin.math.*
import kotlin.native.concurrent.*

fun interface CharsetProvider {
    operator fun invoke(normalizedName: String, name: String): Charset?
}

expect val platformCharsetProvider: CharsetProvider

private val CHARSET_PROVIDERS = arrayListOf<CharsetProvider>()
private val CHARSET_PROVIDERS_LOCK = NonRecursiveLock()


abstract class Charset(val name: String) {
    // Just an estimation, might not be accurate, but hopefully will help setting StringBuilder and ByteArrayBuilder to a better initial capacity
    open fun estimateNumberOfCharactersForBytes(nbytes: Int): Int = nbytes * 2
    open fun estimateNumberOfBytesForCharacters(nchars: Int): Int = nchars * 2

	abstract fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int = 0, end: Int = src.length)

    /**
     * Decodes the [src] [ByteArray] [start]-[end] range using this [Charset]
     * and writes the result into the [out] [StringBuilder].
     *
     * Returns the number of consumed bytes ([end]-[start] if not under-flowing) and less if a character is not complete.
     **/
    abstract fun decode(out: StringBuilder, src: ByteArray, start: Int = 0, end: Int = src.size): Int

	companion object {
        inline fun <T> registerProvider(provider: CharsetProvider, block: () -> T): T {
            registerProvider(provider)
            return try {
                block()
            } finally {
                unregisterProvider(provider)
            }
        }

        fun registerProvider(provider: CharsetProvider) {
            CHARSET_PROVIDERS_LOCK {
                CHARSET_PROVIDERS.add(provider)
            }
        }

        fun unregisterProvider(provider: CharsetProvider) {
            CHARSET_PROVIDERS_LOCK {
                CHARSET_PROVIDERS.remove(provider)
            }
        }

        fun forName(name: String): Charset {
            val normalizedName = name.uppercase().replace("_", "").replace("-", "")
            when (normalizedName) {
                "UTF8" -> return UTF8
                "UTF16", "UTF16LE" -> return UTF16_LE
                "UTF16BE" -> return UTF16_BE
                "ISO88591", "LATIN1" -> return ISO_8859_1
            }
            CHARSET_PROVIDERS_LOCK {
                CHARSET_PROVIDERS.fastForEach { provider ->
                    provider(normalizedName, name)?.let { return it }
                }
            }
            platformCharsetProvider(normalizedName, name)?.let { return it }
            invalidArg("Unknown charset '$name'")
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

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int): Int {
		if ((start < 0 || start > src.size) || (end < 0 || end > src.size)) error("Out of bounds")
		var i = start
		loop@while (i < end) {
			val c = src[i].toInt() and 0xFF

            when (c shr 4) {
                in 0b0000..0b0111 -> {
                    // 0xxxxxxx
                    out.appendCodePointV(c)
                    i += 1
                }
                in 0b1100..0b1101 -> {
                    // 110x xxxx   10xx xxxx
                    if (i + 1 >= end) break@loop
                    out.appendCodePointV((c and 0x1F shl 6 or (src[i + 1].toInt() and 0x3F)))
                    i += 2
                }
                0b1110 -> {
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    if (i + 2 >= end) break@loop
                    out.appendCodePointV((c and 0x0F shl 12 or (src[i + 1].toInt() and 0x3F shl 6) or (src[i + 2].toInt() and 0x3F)))
                    i += 3
                }
                0b1111 -> {
                    // 1111 0xxx 10xx xxxx  10xx xxxx  10xx xxxx
                    if (i + 3 >= end) break@loop
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
        return i - start
	}
}

abstract class BaseSingleByteCharset(name: String) : Charset(name) {
    override fun estimateNumberOfCharactersForBytes(nbytes: Int): Int = nbytes
    override fun estimateNumberOfBytesForCharacters(nchars: Int): Int = nchars
}

open class SingleByteCharset(name: String, val conv: String) : BaseSingleByteCharset(name) {
	val v: IntIntMap = IntIntMap().apply {
		for (n in 0 until conv.length) this[conv[n].code] = n
	}

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		for (n in start until end) {
			val c = src[n].code
			out.append(if (v.contains(c)) v[c].toByte() else '?'.toByte())
		}
	}

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int): Int {
		for (n in start until end) {
			out.append(conv[src[n].toInt() and 0xFF])
		}
        return end - start
	}
}

object ISO_8859_1 : SingleByteCharset("ISO-8859-1", buildString { for (n in 0 until 256) append(n.toChar()) })

@SharedImmutable
expect val UTF8: Charset

class UTF16Charset(val le: Boolean) : Charset("UTF-16-" + (if (le) "LE" else "BE")) {
    override fun estimateNumberOfCharactersForBytes(nbytes: Int): Int = nbytes * 2
    override fun estimateNumberOfBytesForCharacters(nchars: Int): Int = nchars * 2

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int): Int {
        var consumed = 0
		for (n in start until end step 2) {
            val char = src.getS16(n, le).toChar()
            out.append(char)
            consumed += 2
        }
        return consumed
	}

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		val temp = ByteArray(2)
        for (n in start until end) {
            temp.set16(0, src[n].code, le)
            out.append(temp)
        }
	}
}

object ASCII : SingleByteCharset("ASCII", CharArray(128) { it.toChar() }.concatToString() + "\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u00c1\u00c2\u00c0\u00a9\u2563\u2551\u2557\u255d\u00a2\u00a5\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u00e3\u00c3\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u00a4\u00f0\u00d0\u00ca\u00cb\u00c8\u0131\u00cd\u00ce\u00cf\u2518\u250c\u2588\u2584\u00a6\u00cc\u2580\u00d3\u00df\u00d4\u00d2\u00f5\u00d5\u00b5\u00fe\u00de\u00da\u00db\u00d9\u00fd\u00dd\u00af\u00b4\u00ad\u00b1\u2017\u00be\u00b6\u00a7\u00f7\u00b8\u00b0\u00a8\u00b7\u00b9\u00b3\u00b2\u25a0\u00a0")

@SharedImmutable
val LATIN1 = ISO_8859_1
@SharedImmutable
val UTF16_LE = UTF16Charset(le = true)
@SharedImmutable
val UTF16_BE = UTF16Charset(le = false)

object Charsets {
	val UTF8 get() = korlibs.io.lang.UTF8
	val LATIN1 get() = korlibs.io.lang.LATIN1
	val UTF16_LE get() = korlibs.io.lang.UTF16_LE
	val UTF16_BE get() = korlibs.io.lang.UTF16_BE
}

fun String.toByteArray(charset: Charset = UTF8, start: Int = 0, end: Int = this.length): ByteArray {
	val out = ByteArrayBuilder(charset.estimateNumberOfBytesForCharacters(end - start))
	charset.encode(out, this, start, end)
	return out.toByteArray()
}

fun ByteArray.toString(charset: Charset, start: Int = 0, end: Int = this.size): String {
	val out = StringBuilder(charset.estimateNumberOfCharactersForBytes(end - start))
	charset.decode(out, this, start, end)
	return out.toString()
}

fun ByteArray.readStringz(o: Int, size: Int, charset: Charset = UTF8): String {
	var idx = o
	val stop = min(this.size, o + size)
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
