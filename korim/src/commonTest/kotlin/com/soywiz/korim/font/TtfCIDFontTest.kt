package com.soywiz.korim.font

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.stream.openFastStream
import com.soywiz.krypto.encoding.unhexIgnoreSpaces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TtfCIDFontTest {
    @Test
    fun testRealEncoding() {
        // -2.25 :: 1e e2 a2 5f
        // 0.140541E–3 :: 1e 0a 14 05 41 c3 ff

        TtfCIDFont.CFF.apply {
            assertEquals("-2.25", "1e e2 a2 5f".unhexIgnoreSpaces.openFastStream().readEncodedRealString())
            assertEquals("0.140541E-3", "1e 0a 14 05 41 c3 ff".unhexIgnoreSpaces.openFastStream().readEncodedRealString())
        }
    }

    @Test
    fun testReadDict() {
        // Table 4 Integer Format Examples
        TtfCIDFont.CFF.apply {
            assertEquals(0, "8b".unhexIgnoreSpaces.openFastStream().readDICTElement())
            assertEquals(100, "ef".unhexIgnoreSpaces.openFastStream().readDICTElement())
            assertEquals(-100, "27".unhexIgnoreSpaces.openFastStream().readDICTElement())
            assertEquals(1000, "fa7c".unhexIgnoreSpaces.openFastStream().readDICTElement())
            assertEquals(-1000, "fe7c".unhexIgnoreSpaces.openFastStream().readDICTElement())
            assertEquals(10000, "1c 27 10".unhexIgnoreSpaces.openFastStream().readDICTElement())
            assertEquals(-10000, "1c d8 f0".unhexIgnoreSpaces.openFastStream().readDICTElement())
            assertEquals(100000, "1d 00 01 86 a0".unhexIgnoreSpaces.openFastStream().readDICTElement())
            assertEquals(-100000, "1d ff fe 79 60".unhexIgnoreSpaces.openFastStream().readDICTElement())
        }
    }

    @Test
    fun testReadHeader() = suspendTest {
        assertFailsWith<UnsupportedOperationException> {
            val font1 = resourcesVfs["helvetica.otf"].readTtfFont(preload = true)
            println("font1=$font1")
        }
    }

    @Test
    fun testSpecRef() {
        /*
        0000000 0100 0401 0001 0101 1341 4243 4445 462b |?.??.????ABCDEF+|
        0000010 5469 6d65 732d 526f 6d61 6e00 0101 011f |Times-Roman.????|
        0000020 f81b 00f8 1c02 f81d 03f8 1904 1c6f 000d |??.??????????o.?|
        0000030 fb3c fb6e fa7c fa16 05e9 11b8 f112 0003 |?<?n?|????????.?|
        0000040 0101 0813 1830 3031 2e30 3037 5469 6d65 |?????001.007Time|
        0000050 7320 526f 6d61 6e54 696d 6573 0000 0002 |s RomanTimes...?|
        0000060 0101 0203 0e0e 7d99 f92a 99fb 7695 f773 |??????}??*??v??s|
        0000070 8b06 f79a 93fc 7c8c 077d 99f8 5695 f75e |??????|??}??V??^|
        0000080 9908 fb6e 8cf8 7393 f710 8b09 a70a df0b |???n??s?????????|
        0000090 f78e 14 |??? |
         */

        val cidBytes = "0100 0401 0001 0101 1341 4243 4445 462b 5469 6d65 732d 526f 6d61 6e00 0101 011f f81b 00f8 1c02 f81d 03f8 1904 1c6f 000d fb3c fb6e fa7c fa16 05e9 11b8 f112 0003 0101 0813 1830 3031 2e30 3037 5469 6d65 7320 526f 6d61 6e54 696d 6573 0000 0002 0101 0203 0e0e 7d99 f92a 99fb 7695 f773 8b06 f79a 93fc 7c8c 077d 99f8 5695 f75e 9908 fb6e 8cf8 7393 f710 8b09 a70a df0b f78e 14".unhexIgnoreSpaces
        TtfCIDFont.CFF.apply {
            cidBytes.openFastStream().readCFF()
        }

    }
}
