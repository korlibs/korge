package com.soywiz.korio.lang

import com.soywiz.kmem.ByteArrayBuilder
import com.soywiz.korio.runtime.node.toByteArray
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Uint8Array

external class TextDecoder(charset: String) {
    val encoding: String
    fun decode(data: ArrayBufferView): String
}

external class TextEncoder(charset: String) {
    val encoding: String
    fun encode(data: String): Uint8Array
}

actual val platformCharsetProvider: CharsetProvider = CharsetProvider { normalizedName, name ->
    for (n in listOf(name, normalizedName)) {
        try {
            val te = TextEncoder(n)
            val td = TextDecoder(n)
            return@CharsetProvider JsCharset(te, td)
        } catch (e: dynamic) {
            continue
        }
    }
    return@CharsetProvider null
}

class JsCharset(val textEncoder: TextEncoder, val textDecoder: TextDecoder) : Charset(textDecoder.encoding) {
    override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
        if (textEncoder.encoding != textDecoder.encoding) unsupported("Unsupported encoding '${textDecoder.encoding}'")
        out.append(textEncoder.encode(src.substring(start, end)).toByteArray())
    }

    override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
        out.append(textDecoder.decode(src.unsafeCast<Uint8Array>().subarray(start, end)))
    }

    override fun equals(other: Any?): Boolean = other is JsCharset && this.name == other.name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = "JsCharset($name)"
}
