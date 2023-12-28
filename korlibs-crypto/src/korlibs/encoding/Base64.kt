package korlibs.encoding

object Base64 {
    private val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    private val TABLE_URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_="
    private val DECODE = TABLE.toDecodeArray()
    private val DECODE_URL = TABLE_URL.toDecodeArray()

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

    fun decode(ignoreSpaces: Boolean, str: String, url: Boolean = false): ByteArray {
        return if (ignoreSpaces) Base64.decodeIgnoringSpaces(str, url) else Base64.decode(str, url)
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
        val decodeArray = if (url) {
            DECODE_URL
        } else {
            DECODE
        }

        var m = 0
        var n = 0
        while (n < src.size) {
            val d = decodeArray[src.readU8(n)]
            if (d < 0) {
                n++
                continue // skip character
            }

            val b0 = if (n < src.size) decodeArray[src.readU8(n++)] else 64
            val b1 = if (n < src.size) decodeArray[src.readU8(n++)] else 64
            val b2 = if (n < src.size) decodeArray[src.readU8(n++)] else 64
            val b3 = if (n < src.size) decodeArray[src.readU8(n++)] else 64
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
        val encodeTable = if (url) {
            TABLE_URL
        } else {
            TABLE
        }

        val out = StringBuilder((src.size * 4) / 3 + 4)
        var ipos = 0
        val extraBytes = src.size % 3
        while (ipos < src.size - 2) {
            val num = src.readU24BE(ipos)
            ipos += 3

            out.append(encodeTable[(num ushr 18) and 0x3F])
            out.append(encodeTable[(num ushr 12) and 0x3F])
            out.append(encodeTable[(num ushr 6) and 0x3F])
            out.append(encodeTable[(num ushr 0) and 0x3F])
        }

        if (extraBytes == 1) {
            val num = src.readU8(ipos++)
            out.append(encodeTable[num ushr 2])
            out.append(encodeTable[(num shl 4) and 0x3F])
            if (!url || (url && doPadding)) {
                out.append('=')
                out.append('=')
            }
        } else if (extraBytes == 2) {
            val tmp = (src.readU8(ipos++) shl 8) or src.readU8(ipos++)
            out.append(encodeTable[tmp ushr 10])
            out.append(encodeTable[(tmp ushr 4) and 0x3F])
            out.append(encodeTable[(tmp shl 2) and 0x3F])
            if (!url || (url && doPadding)) {
                out.append('=')
            }
        }

        return out.toString()
    }

    private fun ByteArray.readU8(index: Int): Int = this[index].toInt() and 0xFF
    private fun ByteArray.readU24BE(index: Int): Int =
        (readU8(index + 0) shl 16) or (readU8(index + 1) shl 8) or (readU8(index + 2) shl 0)
}
/** Converts a Base64 encoded string to a Byte Array with options to ignore spaces before decoding and use URL-safe encoding*/
fun String.fromBase64IgnoreSpaces(url: Boolean = false): ByteArray = Base64.decodeIgnoringSpaces(this, url)

/**Decodes the current string from Base64 encoding and takes two optional parameters: ignore Spaces, a Boolean to ignore spaces during decoding (default is false), and url, a Boolean for URL-safe encoding (default is false.*/
fun String.fromBase64(ignoreSpaces: Boolean = false, url: Boolean = false): ByteArray = Base64.decode(ignoreSpaces, this, url)

/**Encodes a byte array to a Base64 string with options for URL-safe encoding and padding. */
fun ByteArray.toBase64(url: Boolean = false, doPadding: Boolean = false): String = Base64.encode(this, url, doPadding)

/**Defines a property extension to easily obtain the Base64 encoded string representation of a byte array. It uses the Base64.encode(this) function to perform the encoding. */
val ByteArray.base64: String get() = Base64.encode(this)

/**Defines a property extension to easily obtain the URL-safe Base64 encoded string representation of a byte array. It uses the Base64.encode(this, true) function, where the second parameter specifies that URL-safe encoding should be used.*/
val ByteArray.base64Url: String get() = Base64.encode(this, true)

private fun String.toDecodeArray(): IntArray = IntArray(0x100).also {
    for (n in 0..255) it[n] = -1
    for (n in indices) {
        it[this[n].code] = n
    }
}
