package com.soywiz.korau.sound.internal.jvm

import com.soywiz.kds.ByteArrayDeque
import java.io.InputStream

internal fun ByteArrayDeque.inputStream() = object : InputStream() {
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val out = this@inputStream.read(b, off, len)
        //if (out <= 0) return -1
        if (out <= 0) return 0
        return out
    }

    override fun read(): Int {
        return this@inputStream.readByte()
    }
}
