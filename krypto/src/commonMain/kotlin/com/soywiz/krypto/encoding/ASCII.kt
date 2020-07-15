package com.soywiz.krypto.encoding

object ASCII {
    operator fun invoke(v: String): ByteArray = encode(v)
    operator fun invoke(v: ByteArray): String = decode(v)

    fun encode(str: String): ByteArray =
        ByteArray(str.length).also { out -> for (n in out.indices) out[n] = str[n].toByte() }
    fun decode(data: ByteArray): String =
        String(CharArray(data.size).also { out -> for (n in out.indices) out[n] = data[n].toChar() })
}

fun String.fromAscii(): ByteArray = ASCII(this)
val ByteArray.ascii: String get() = ASCII(this)
