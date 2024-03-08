package korlibs.io.lang

import korlibs.memory.ByteArrayBuilder
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.*
import java.nio.charset.Charset as JCharset

actual val platformCharsetProvider: CharsetProvider = CharsetProvider { normalizedName, name ->
    for (n in listOf(name, normalizedName)) {
        if (JCharset.isSupported(n)) return@CharsetProvider JvmCharset(JCharset.forName(n))
    }
    return@CharsetProvider null
}

class JvmCharset(val charset: JCharset) : Charset(charset.name()) {
    val decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.IGNORE)
    val encoder = charset.newEncoder().onMalformedInput(CodingErrorAction.IGNORE)

    override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
        val bb = encoder.encode(CharBuffer.wrap(src, start, end))
        out.append(ByteArray(bb.remaining()).also { bb.get(it) })
    }

    override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int): Int {
        val bb = ByteBuffer.wrap(src, start, end - start)
        out.append(decoder.decode(bb))
        return bb.position() - start
    }

    override fun equals(other: Any?): Boolean = other is JvmCharset && this.charset == other.charset
    override fun hashCode(): Int = charset.hashCode()
    override fun toString(): String = "JvmCharset($name)"
}
