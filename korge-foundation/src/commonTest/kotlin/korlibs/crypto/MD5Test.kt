package korlibs.crypto

import korlibs.encoding.ASCII
import kotlin.test.Test
import kotlin.test.assertEquals

class MD5Test {
    @Test
    fun testEmpty() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", MD5.digest(ASCII("")).hex)
    }

    @Test
    fun test1() {
        assertEquals("0cc175b9c0f1b6a831c399e269772661", MD5.digest(ASCII("a")).hex)

        assertEquals("0cc175b9c0f1b6a831c399e269772661", ASCII("a").hash(MD5).hex)
        assertEquals("0cc175b9c0f1b6a831c399e269772661", ASCII("a").md5().hex)
    }

    @Test
    fun test2() {
        assertEquals("014842d480b571495a4a0363793f7367", MD5.digest(ByteArray(64) { 'a'.toByte() }).hex)
    }

    @Test
    fun test3() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", MD5.digest(ASCII("abc")).hex)
        assertEquals("900150983cd24fb0d6963f7d28e17f72", MD5().update(ASCII("a")).update(ASCII("bc")).digest().hex)
        assertEquals("f96b697d7cb7938d525a2f31aaf161d0", MD5.digest(ASCII("message digest")).hex)
        assertEquals("c3fcd3d76192e4007dfb496cca67e13b", MD5.digest(ASCII("abcdefghijklmnopqrstuvwxyz")).hex)
        assertEquals("d174ab98d277d9f5a5611c2c9f419d9f", MD5.digest(ASCII("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")).hex)
        assertEquals("57edf4a22be3c955ac49da2e2107b67a", MD5.digest(ASCII("12345678901234567890123456789012345678901234567890123456789012345678901234567890")).hex)
    }
}
