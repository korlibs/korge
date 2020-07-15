package com.soywiz.korio.util.encoding

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import kotlin.native.concurrent.*

@SharedImmutable
private val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
@SharedImmutable
private val DECODE = IntArray(0x100).apply {
	for (n in 0..255) this[n] = -1
	for (n in TABLE.indices) this[TABLE[n].toInt()] = n
}

object Base64 {
	fun decode(str: String): ByteArray {
		val dst = ByteArray((str.length * 4) / 3 + 4)
		return dst.copyOf(decodeInline(dst, str.length) { str[it].toInt() and 0xFF })
	}

	fun decode(src: ByteArray, dst: ByteArray): Int
        = decodeInline(dst, src.size) { (src[it].toInt() and 0xFF) }

    private inline fun decodeInline(dst: ByteArray, size: Int, get: (n: Int) -> Int): Int {
        var m = 0
        var n = 0
        while (n < size) {
            val d = DECODE[get(n)]
            if (d < 0) {
                n++
                continue // skip character
            }
            val b0 = DECODE[get(n++)]
            val b1 = DECODE[get(n++)]
            val b2 = DECODE[get(n++)]
            val b3 = DECODE[get(n++)]
            dst[m++] = (b0 shl 2 or (b1 shr 4)).toByte()
            if (b2 < 64) {
                dst[m++] = (b1 shl 4 or (b2 shr 2)).toByte()
                if (b3 < 64) {
                    dst[m++] = (b2 shl 6 or b3).toByte()
                }
            }
        }
        return m
    }

	fun encode(src: String, charset: Charset): String = encode(src.toByteArray(charset))

    fun encode(src: ByteArray): String = encode(src, 0, src.size)

	@Suppress("UNUSED_CHANGED_VALUE")
	fun encode(src: ByteArray, start: Int, size: Int): String {
		val out = StringBuilder((size * 4) / 3 + 4)
		var ipos = start
        val iend = start + size
		val extraBytes = size % 3
		while (ipos < iend - 2) {
			val num = src.readU24BE(ipos)
			ipos += 3
			out.append(TABLE[(num ushr 18) and 0x3F])
			out.append(TABLE[(num ushr 12) and 0x3F])
			out.append(TABLE[(num ushr 6) and 0x3F])
			out.append(TABLE[(num ushr 0) and 0x3F])
		}
		if (extraBytes == 1) {
			val num = src.readU8(ipos++)
			out.append(TABLE[num ushr 2])
			out.append(TABLE[(num shl 4) and 0x3F])
			out.append('=')
			out.append('=')
		} else if (extraBytes == 2) {
			val tmp = (src.readU8(ipos++) shl 8) or src.readU8(ipos++)
			out.append(TABLE[tmp ushr 10])
			out.append(TABLE[(tmp ushr 4) and 0x3F])
			out.append(TABLE[(tmp shl 2) and 0x3F])
			out.append('=')
		}
		return out.toString()
	}
}

fun String.fromBase64IgnoreSpaces(): ByteArray = Base64.decode(this.replace(" ", "").replace("\n", "").replace("\r", ""))
fun String.fromBase64(): ByteArray = Base64.decode(this)
fun ByteArray.toBase64(): String = Base64.encode(this)
