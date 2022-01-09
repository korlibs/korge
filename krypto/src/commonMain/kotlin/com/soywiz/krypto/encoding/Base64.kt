package com.soywiz.krypto.encoding

object Base64 {
    private val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    private val DECODE = IntArray(0x100).apply {
        for (n in 0..255) this[n] = -1
        for (n in 0 until TABLE.length) {
            this[TABLE[n].code] = n
        }
    }

    /**
     * Base64 decodes [v] to a ByteArray. Set [url] to true if [v] is Base64Url encoded.
     */
    operator fun invoke(v: String, url: Boolean = false) = decodeIgnoringSpaces(v, url)

    /**
     * Base64 encodes [v] to a String. Set [url] to true to use Base64Url encoding.
     */
    operator fun invoke(v: ByteArray, url: Boolean = false) = encode(v, url)

    /**
     * Base64 decodes [str] to a ByteArray. Set [url] to true if [str] is Base64Url encoded.
     */
    fun decode(str: String, url: Boolean = false): ByteArray {
        val src = ByteArray(str.length) { str[it].code.toByte() }
        val dst = ByteArray(src.size)
        return dst.copyOf(decode(src, dst, url))
    }

    /**
     * Base64 decodes [str] to a ByteArray after removing spaces, newlines, carriage returns, and tabs.
     * Set [url] to true if [str] is Base64Url encoded.
     */
    fun decodeIgnoringSpaces(str: String, url: Boolean = false): ByteArray {
        return decode(str.replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", ""), url)
    }

    /**
     * Base64 decodes [src] to [dst]. Set [url] to true if [src] is Base64Url encoded.
     */
    fun decode(src: ByteArray, dst: ByteArray, url: Boolean = false): Int {
        if (url) {
            base64UrlToBase64(src)
        }

        var m = 0
        var n = 0
        while (n < src.size) {
            val d = DECODE[src.readU8(n)]
            if (d < 0) {
                n++
                continue // skip character
            }

            val b0 = if (n < src.size) DECODE[src.readU8(n++)] else 64
            val b1 = if (n < src.size) DECODE[src.readU8(n++)] else 64
            val b2 = if (n < src.size) DECODE[src.readU8(n++)] else 64
            val b3 = if (n < src.size) DECODE[src.readU8(n++)] else 64
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

    /**
     * Base64 encodes [src] to a String. Set [url] to true if the Base64Url encoding character set should be used.
     * If [url] is true, [doPadding] can optionally be set to true to include padding characters in the output.
     * [doPadding] is ignored if [url] is false.
     */
    @Suppress("UNUSED_CHANGED_VALUE")
    fun encode(src: ByteArray, url: Boolean = false, doPadding: Boolean = false): String {
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

        return if (url) {
            base64ToBase64Url(out.toString(), doPadding)
        } else {
            out.toString()
        }
    }

    /**
     * Converts a Base64Url encoded ByteArray to a Base64 encoded ByteArray without adding padding.
     */
    private fun base64UrlToBase64(src: ByteArray) {
        src.forEachIndexed { index, byte ->
            if (byte == '-'.code.toByte()) {
                src[index] = '+'.code.toByte()
            } else if (byte == '_'.code.toByte()) {
                src[index] = '/'.code.toByte()
            }
        }
    }

    /**
     * Converts a Base64 encoded String to a Base64Url encoded String.
     */
    private fun base64ToBase64Url(src: String, doPadding: Boolean = false): String {
        return src.replace("+", "-")
            .replace("/", "_").let {
                if (!doPadding) {
                    it.substringBefore("=")
                } else {
                    it
                }
            }
    }

    private fun ByteArray.readU8(index: Int): Int = this[index].toInt() and 0xFF
    private fun ByteArray.readU24BE(index: Int): Int =
        (readU8(index + 0) shl 16) or (readU8(index + 1) shl 8) or (readU8(index + 2) shl 0)
}

fun String.fromBase64IgnoreSpaces(url: Boolean = false): ByteArray =
    Base64.decode(this.replace(" ", "").replace("\n", "").replace("\r", ""), url)

fun String.fromBase64(ignoreSpaces: Boolean = false, url: Boolean = false): ByteArray =
    if (ignoreSpaces) Base64.decodeIgnoringSpaces(this, url) else Base64.decode(this, url)

fun ByteArray.toBase64(url: Boolean = false, doPadding: Boolean = false): String = Base64.encode(this, url, doPadding)
val ByteArray.base64: String get() = Base64.encode(this)
val ByteArray.base64Url: String get() = Base64.encode(this, true)
