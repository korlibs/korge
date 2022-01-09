package com.soywiz.krypto.encoding

/**
 * Base64Url codec based on the Base64 codec and RFC 7515 Appendix C.
 */
object Base64Url {
    operator fun invoke(v: String) = decodeIgnoringSpaces(v)
    operator fun invoke(v: ByteArray) = encode(v)

    fun decode(str: String): ByteArray {
        var s = str.replace("-", "+").replace("_", "/")
        when (s.length % 4) {
            0 -> { }
            2 -> s += "=="
            3 -> s += "="
            else -> throw IllegalArgumentException("Illegal base64url string")
        }

        return Base64.decode(s)
    }

    fun decodeIgnoringSpaces(str: String): ByteArray {
        return decode(str.replace(" ", "").replace("\n", "").replace("\r", "").replace("\ts", ""))
    }

    fun encode(src: ByteArray, doPadding: Boolean = false): String {
        return Base64.encode(src).replace("+", "-")
            .replace("/", "_").let {
                if (!doPadding) {
                    it.split("=")[0]
                } else {
                    it
                }
            }
    }
}

fun String.fromBase64UrlIgnoreSpaces(): ByteArray = Base64Url.decode(this.replace(" ", "").replace("\n", "").replace("\r", ""))
fun String.fromBase64Url(ignoreSpaces: Boolean = false): ByteArray = if (ignoreSpaces) Base64Url.decodeIgnoringSpaces(this) else Base64Url.decode(this)
fun ByteArray.toBase64Url(): String = Base64Url.encode(this)
val ByteArray.base64Url: String get() = Base64Url.encode(this)
