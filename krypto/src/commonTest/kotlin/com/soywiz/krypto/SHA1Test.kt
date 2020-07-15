package com.soywiz.krypto

import com.soywiz.krypto.encoding.ASCII
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
}
