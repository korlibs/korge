package korlibs.crypto

import korlibs.encoding.ASCII
import kotlin.test.Test
import kotlin.test.assertEquals

class MD4Test {
    @Test
    fun testEmpty() {
        assertEquals("31d6cfe0d16ae931b73c59d7e0c089c0", MD4.digest(ASCII("")).hex)
    }

    @Test
    fun test1() {
        assertEquals("bde52cb31de33e46245e05fbdbd6fb24", MD4.digest(ASCII("a")).hex)

        assertEquals("bde52cb31de33e46245e05fbdbd6fb24", ASCII("a").hash(MD4).hex)
        assertEquals("bde52cb31de33e46245e05fbdbd6fb24", ASCII("a").md4().hex)
    }

    @Test
    fun test2() {
        assertEquals("52f5076fabd22680234a3fa9f9dc5732", MD4.digest(ByteArray(64) { 'a'.toByte() }).hex)
    }

    @Test
    fun test3() {
        assertEquals("a448017aaf21d8525fc10ae87aa6729d", MD4.digest(ASCII("abc")).hex)
        assertEquals("a448017aaf21d8525fc10ae87aa6729d", MD4().update(ASCII("a")).update(ASCII("bc")).digest().hex)
        assertEquals("d9130a8164549fe818874806e1c7014b", MD4.digest(ASCII("message digest")).hex)
        assertEquals("d79e1c308aa5bbcdeea8ed63df412da9", MD4.digest(ASCII("abcdefghijklmnopqrstuvwxyz")).hex)
        assertEquals("043f8582f241db351ce627e153e7f0e4", MD4.digest(ASCII("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")).hex)
        assertEquals("e33b4ddc9c38f2199c3e7b164fcc0536", MD4.digest(ASCII("12345678901234567890123456789012345678901234567890123456789012345678901234567890")).hex)
    }
}
