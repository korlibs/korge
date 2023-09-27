package korlibs.io.lang

import korlibs.io.util.*
import korlibs.memory.ByteArrayBuilder
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Uint8Array

external class TextDecoder(charset: String) : JsAny {
    val encoding: String
    fun decode(data: ArrayBufferView): String
}

external class TextEncoder(charset: String) : JsAny {
    val encoding: String
    fun encode(data: String): Uint8Array
}


actual val platformCharsetProvider: CharsetProvider = CharsetProvider { normalizedName, name ->
    for (n in listOf(name, normalizedName)) {
        try {
            val te = wrapWasmJsExceptions { TextEncoder(n) }
            val td = wrapWasmJsExceptions { TextDecoder(n) }
            return@CharsetProvider JsCharset(te, td)
            //} catch (e: dynamic) { // @TODO: Not working on WASM. Do we really have a Throwable from JS?
        } catch (e: Throwable) {
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

    override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int): Int {
        out.append(textDecoder.decode(src.toInt8Array().subarray(start, end)))
        // @TODO: This charset won't support partial characters.
        return end - start
    }

    override fun equals(other: Any?): Boolean = other is JsCharset && this.name == other.name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = "JsCharset($name)"
}
