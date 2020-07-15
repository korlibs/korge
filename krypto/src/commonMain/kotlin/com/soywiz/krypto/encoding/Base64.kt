package com.soywiz.krypto.encoding

object Base64 {
    private val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    private val DECODE = IntArray(0x100).apply {
        for (n in 0..255) this[n] = -1
        for (n in 0 until TABLE.length) {
            this[TABLE[n].toInt()] = n
        }
    }

    operator fun invoke(v: String) = decodeIgnoringSpaces(v)
    operator fun invoke(v: ByteArray) = encode(v)

    fun decode(str: String): ByteArray {
        val src = ByteArray(str.length) { str[it].toByte() }
        val dst = ByteArray(src.size)
        return dst.copyOf(decode(src, dst))
    }

    fun decodeIgnoringSpaces(str: String): ByteArray {
        return decode(str.replace(" ", "").replace("\n", "").replace("\r", ""))
    }

    fun decode(src: ByteArray, dst: ByteArray): Int {
        var m = 0

        var n = 0
        while (n < src.size) {
            val d = DECODE[src.readU8(n)]
            if (d < 0) {
                n++
                continue // skip character
            }

            val b0 = DECODE[src.readU8(n++)]
            val b1 = DECODE[src.readU8(n++)]
            val b2 = DECODE[src.readU8(n++)]
            val b3 = DECODE[src.readU8(n++)]
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

    @Suppress("UNUSED_CHANGED_VALUE")
    fun encode(src: ByteArray): String {
        val out = StringBuilder((src.size * 4) / 3 + 4)
        var ipos = 0
        val extraBytes = src.size % 3
        while (ipos < src.size - 2) {
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

    private fun ByteArray.readU8(index: Int): Int = this[index].toInt() and 0xFF
    private fun ByteArray.readU24BE(index: Int): Int =
        (readU8(index + 0) shl 16) or (readU8(index + 1) shl 8) or (readU8(index + 2) shl 0)
}

fun String.fromBase64(ignoreSpaces: Boolean = false): ByteArray = if (ignoreSpaces) Base64.decodeIgnoringSpaces(this) else Base64.decode(this)
val ByteArray.base64: String get() = Base64.encode(this)
