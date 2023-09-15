package korlibs.crypto

import korlibs.encoding.ASCII
import kotlin.test.Test
import kotlin.test.assertEquals

class SHA1Test {
    @Test
    fun name() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", SHA1.digest(ASCII("")).hex)
        assertEquals("86f7e437faa5a7fce15d1ddcb9eaeaea377667b8", SHA1.digest(ASCII("a")).hex)
        assertEquals(
            "32d10c7b8cf96570ca04ce37f2a19d84240d3a89",
            SHA1.digest(ASCII("abcdefghijklmnopqrstuvwxyz")).hex
        )

        assertEquals("86f7e437faa5a7fce15d1ddcb9eaeaea377667b8", ASCII("a").hash(SHA1).hex)
        assertEquals("86f7e437faa5a7fce15d1ddcb9eaeaea377667b8", ASCII("a").sha1().hex)
    }

    @Test
    fun testReset() {
        val sha1 = SHA1()
        sha1.update(ByteArray(16))
        assertEquals(ByteArray(16).sha1().hex, sha1.digest().hex)

        sha1.reset()
        sha1.update(ByteArray(20))
        assertEquals(ByteArray(20).sha1().hex, sha1.digest().hex)
    }
}
