package korlibs.crypto

import korlibs.encoding.ASCII
import kotlin.test.Test
import kotlin.test.assertEquals

class SHA256Test {
    @Test
    fun test() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", SHA256.digest(byteArrayOf()).hex)
    }

    @Test
    fun test2() {
        assertEquals("ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb", SHA256.digest(ByteArray(64) { 'a'.toByte() }).hex)

        assertEquals("ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb", ByteArray(64) { 'a'.toByte() }.hash(SHA256).hex)
        assertEquals("ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb", ByteArray(64) { 'a'.toByte() }.sha256().hex)
    }

    @Test
    fun test3() {
        assertEquals("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592", SHA256.digest(ASCII("The quick brown fox jumps over the lazy dog")).hex)
        assertEquals("539deb4a951195ca3377514b8a44b95061b4fcd5ae21b29be3748cc835992b52", SHA256.digest(ASCII("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab")).hex)
    }
}
