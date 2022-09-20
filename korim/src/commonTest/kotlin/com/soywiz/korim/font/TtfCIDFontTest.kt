package com.soywiz.korim.font

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.stream.openFastStream
import com.soywiz.krypto.encoding.unhexIgnoreSpaces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * * TOOLS for debugging and reference:
 * - https://fontdrop.info/#/?darkmode=true
 * - https://yqnn.github.io/svg-path-editor/
 * - https://github.com/caryll/otfcc/blob/617837bc6a74b0cf6be9e77a8ed1d2ffb39ea425/lib/libcff/cff-parser.c#L342
 * - https://github.com/RazrFalcon/ttf-parser/blob/master/src/tables/cff/cff2.rs
 * - https://github.com/nothings/stb/blob/master/stb_truetype.h
 * - https://github.com/opentypejs/opentype.js
 */
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
        val font1 = resourcesVfs["helvetica.otf"].readTtfFont(preload = false)
        val cff = font1._cff
        assertNotNull(cff)

        //println(cff.getGlyphVector(2).advanceWidth)
        //return@suspendTest

        fun getPath(index: Int, flipY: Boolean = false): String {
            val path = cff.getGlyphVector(index, flipY = flipY)
            //return "width=${path.advanceWidth.toInt()}, path=${path.path.toSvgString()}"
            return path.path.toSvgString()
        }

        // Read CFF glyphs directly from the parsed CFF
        assertEquals(message = "no glyph", actual = getPath(0), expected = "M0,0 L500,0 L500,700 L0,700 Z M250,395 L80,650 L420,650 Z M280,350 L450,605 L450,95 Z M80,50 L250,305 L420,50 Z M50,605 L220,350 L50,95 Z")
        assertEquals(message = "space", actual = getPath(1), expected = "")
        assertEquals(message = "!", actual = getPath(2), expected = "M198,199 L217,482 L217,714 L79,714 L79,482 L98,199 Z M79,132 L79,0 L217,0 L217,132 Z")
        assertEquals(message = "(", actual = getPath(9), expected = "M190,714 C92,558,50,405,50,265 C50,125,92,-28,190,-184 L296,-184 C213,-33,182,96,182,265 C182,434,213,563,296,714 Z")
        assertEquals(message = "*", actual = getPath(11), expected = "M154,714 L154,586 L32,626 L7,548 L129,508 L53,404 L120,356 L195,460 L270,356 L337,404 L261,508 L383,548 L358,626 L236,586 L236,714 Z")
        assertEquals(message = "#", actual = getPath(4), expected = "M0,174 L74,174 L52,0 L149,0 L170,174 L268,174 L246,0 L343,0 L365,174 L451,174 L451,286 L378,286 L394,408 L480,408 L480,520 L407,520 L429,694 L332,694 L310,520 L213,520 L234,694 L138,694 L116,520 L30,520 L30,408 L103,408 L87,286 L0,286 Z M199,408 L297,408 L281,286 L184,286 Z")
        assertEquals(message = "\$", actual = getPath(5), expected = "M269,269 C307,249,327,222,327,172 C327,135,310,101,269,92 Z M205,442 C171,462,156,490,156,526 C156,568,175,593,205,602 M205,-101 L269,-101 L269,-14 C401,-5,471,56,471,200 C471,311,422,350,348,381 L269,413 L269,602 C303,591,315,556,316,505 L454,505 L454,519 C454,608,416,696,269,708 L269,777 L205,777 L205,708 C87,695,18,626,18,504 C18,414,58,347,155,312 L205,294 L205,92 C152,105,147,157,147,212 L9,212 L9,191 C9,88,39,-1,205,-14 Z")
        assertEquals(message = "©", actual = getPath(170), expected = "M495,298 C489,256,455,228,406,228 C342,228,302,287,302,360 C302,436,338,492,407,492 C457,492,485,464,495,425 L591,425 C578,525,501,578,412,578 C284,578,208,483,208,360 C208,239,288,142,415,142 C500,142,580,198,595,298 Z M141,357 C141,502,251,622,400,622 C549,622,659,502,659,357 C659,212,549,92,400,92 C251,92,141,212,141,357 M29,357 C29,152,195,-14,400,-14 C605,-14,771,152,771,357 C771,562,605,728,400,728 C195,728,29,562,29,357 Z")
        assertEquals(message = "Ç", actual = getPath(177), expected = "M214,-92 L236,-112 C247,-108,256,-106,270,-106 C297,-106,314,-120,314,-140 C314,-159,292,-174,264,-174 C239,-174,209,-167,195,-159 L176,-205 C192,-214,232,-224,276,-224 C357,-224,402,-188,402,-130 C402,-83,357,-56,318,-56 C306,-56,297,-58,285,-61 L315,-13 C472,4,505,129,505,273 L361,273 C361,119,323,92,271,92 C209,92,179,131,179,347 C179,549,196,622,269,622 C334,622,355,577,355,473 L499,473 C499,577,484,728,278,728 C62,728,35,574,35,357 C35,146,60,-5,261,-14 Z")
        assertEquals(message = "æ", actual = getPath(144), expected = "M296,163 C296,104,248,80,211,80 C174,80,159,114,159,155 C159,203,181,230,237,242 C254,246,277,255,296,269 Z M424,330 L424,351 C424,401,440,458,494,458 C550,458,560,412,563,330 Z M168,373 L168,388 C168,430,190,458,232,458 C278,458,296,433,296,392 C296,356,282,340,241,329 L163,308 C68,282,27,241,27,140 C27,68,61,-14,157,-14 C242,-14,288,-1,331,79 C350,32,394,-14,493,-14 C622,-14,682,56,687,181 L563,181 C555,115,536,80,491,80 C441,80,424,142,424,205 L424,248 L695,248 L695,279 C695,444,659,552,499,552 C441,552,392,529,372,488 C343,537,304,552,233,552 C129,552,44,510,44,386 L44,373 Z")
        assertEquals(message = "§", actual = getPath(102), expected = "M38,37 L38,22 C38,-84,103,-152,240,-152 C368,-152,444,-96,444,23 C444,81,423,121,390,147 C437,175,468,238,468,289 C468,376,419,423,358,452 L239,508 C192,530,175,554,175,577 C175,609,199,634,237,634 C264,634,282,628,292,615 C302,602,306,585,306,566 L438,566 C438,660,384,728,248,728 C134,728,43,682,43,563 C43,516,64,478,103,449 C48,421,13,368,13,306 C13,242,45,188,119,152 L242,93 C289,70,306,46,306,9 C306,-24,287,-58,240,-58 C200,-58,170,-28,170,22 L170,37 Z M165,272 C143,284,131,307,131,326 C131,351,148,378,186,401 L282,350 C333,323,350,302,350,268 C350,247,338,222,303,193 C291,202,278,210,264,218 Z")
        assertEquals(message = "‰", actual = getPath(122), expected = "M872,181 C872,252,876,292,916,292 C956,292,960,252,960,181 C960,102,956,62,916,62 C876,62,872,102,872,181 Z M766,177 C766,48,792,-14,916,-14 C1040,-14,1066,48,1066,177 C1066,306,1040,368,916,368 C792,368,766,306,766,177 Z M27,517 C27,388,53,326,177,326 C301,326,327,388,327,517 C327,646,301,708,177,708 C53,708,27,646,27,517 Z M133,521 C133,592,137,632,177,632 C217,632,221,592,221,521 C221,442,217,402,177,402 C137,402,133,442,133,521 Z M428,177 C428,48,454,-14,578,-14 C702,-14,728,48,728,177 C728,306,702,368,578,368 C454,368,428,306,428,177 Z M534,181 C534,252,538,292,578,292 C618,292,622,252,622,181 C622,102,618,62,578,62 C538,62,534,102,534,181 Z M128,-31 L217,-31 L623,725 L534,725 Z")

        fun rangeWidths(range: IntRange): String = range.joinToString(",") { cff.getGlyphVector(it).advanceWidth.toInt().toString() }

        assertEquals("500,240,296,463,480,480,778,593", rangeWidths(0..7))
        assertEquals("260,296,296,390,600,240,370,240", rangeWidths(8..15))
        assertEquals("332,480,480,480,480,480,480,480", rangeWidths(16..23))
        assertEquals("480,480,480,240,240,600,600,600", rangeWidths(24..31))
        assertEquals("481,800,556,556,537,574,481,463", rangeWidths(32..39))

        // Read glyphs from the font normally
        assertEquals("M198,-199 L217,-482 L217,-714 L79,-714 L79,-482 L98,-199 Z M79,-132 L79,0 L217,0 L217,-132 Z", font1.getGlyphByChar('!')?.path?.path?.toSvgString())
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
