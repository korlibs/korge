package korlibs.image.font

import korlibs.datastructure.doubleArrayListOf
import korlibs.logger.*
import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import korlibs.io.stream.openFastStream
import korlibs.math.geom.vector.VectorPath
import korlibs.encoding.unhex
import korlibs.encoding.unhexIgnoreSpaces
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
    val logger = Logger("TtfCIDFontTest")

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
        val font1 = resourcesVfs["helvetica.otf"].readTtfFont()
        val cff = font1._cff
        assertNotNull(cff)

        //println(cff.getGlyphVector(2).advanceWidth)
        //return@suspendTest

        fun getPath(index: Int, flipY: Boolean = false): String {
            val path = cff.getGlyphVector(index, flipY = flipY)
            //return "width=${path.advanceWidth.toInt()}, path=${path.path.toSvgString()}"
            return path.path.toSvgString()
        }

        fun rangeWidths(range: IntRange): String = range.joinToString(",") { cff.getGlyphVector(it).advanceWidth.toInt().toString() }

        // Read CFF glyphs directly from the parsed CFF
        assertEquals(
            """
                no glyph M0,0 L500,0 L500,700 L0,700 Z M0,700 M250,395 L80,650 L420,650 Z M420,650 M280,350 L450,605 L450,95 Z M450,95 M80,50 L250,305 L420,50 Z M420,50 M50,605 L220,350 L50,95 Z M50,95
                space 
                ! M198,199 L217,482 L217,714 L79,714 L79,482 L98,199 Z M98,199 M79,132 L79,0 L217,0 L217,132 Z M217,132
                ( M190,714 C92,558,50,405,50,265 C50,125,92,-28,190,-184 L296,-184 C213,-33,182,96,182,265 C182,434,213,563,296,714 Z M296,714
                * M154,714 L154,586 L32,626 L7,548 L129,508 L53,404 L120,356 L195,460 L270,356 L337,404 L261,508 L383,548 L358,626 L236,586 L236,714 Z M236,714
                # M0,174 L74,174 L52,0 L149,0 L170,174 L268,174 L246,0 L343,0 L365,174 L451,174 L451,286 L378,286 L394,408 L480,408 L480,520 L407,520 L429,694 L332,694 L310,520 L213,520 L234,694 L138,694 L116,520 L30,520 L30,408 L103,408 L87,286 L0,286 Z M0,286 M199,408 L297,408 L281,286 L184,286 Z M184,286
                ${'$'} M269,269 C307,249,327,222,327,172 C327,135,310,101,269,92 Z M269,92 M205,442 C171,462,156,490,156,526 C156,568,175,593,205,602 M205,-101 L269,-101 L269,-14 C401,-5,471,56,471,200 C471,311,422,350,348,381 L269,413 L269,602 C303,591,315,556,316,505 L454,505 L454,519 C454,608,416,696,269,708 L269,777 L205,777 L205,708 C87,695,18,626,18,504 C18,414,58,347,155,312 L205,294 L205,92 C152,105,147,157,147,212 L9,212 L9,191 C9,88,39,-1,205,-14 Z M205,-14
                © M495,298 C489,256,455,228,406,228 C342,228,302,287,302,360 C302,436,338,492,407,492 C457,492,485,464,495,425 L591,425 C578,525,501,578,412,578 C284,578,208,483,208,360 C208,239,288,142,415,142 C500,142,580,198,595,298 Z M595,298 M141,357 C141,502,251,622,400,622 C549,622,659,502,659,357 C659,212,549,92,400,92 C251,92,141,212,141,357 M29,357 C29,152,195,-14,400,-14 C605,-14,771,152,771,357 C771,562,605,728,400,728 C195,728,29,562,29,357 Z M29,357
                Ç M214,-92 L236,-112 C247,-108,256,-106,270,-106 C297,-106,314,-120,314,-140 C314,-159,292,-174,264,-174 C239,-174,209,-167,195,-159 L176,-205 C192,-214,232,-224,276,-224 C357,-224,402,-188,402,-130 C402,-83,357,-56,318,-56 C306,-56,297,-58,285,-61 L315,-13 C472,4,505,129,505,273 L361,273 C361,119,323,92,271,92 C209,92,179,131,179,347 C179,549,196,622,269,622 C334,622,355,577,355,473 L499,473 C499,577,484,728,278,728 C62,728,35,574,35,357 C35,146,60,-5,261,-14 Z M261,-14
                æ M296,163 C296,104,248,80,211,80 C174,80,159,114,159,155 C159,203,181,230,237,242 C254,246,277,255,296,269 Z M296,269 M424,330 L424,351 C424,401,440,458,494,458 C550,458,560,412,563,330 Z M563,330 M168,373 L168,388 C168,430,190,458,232,458 C278,458,296,433,296,392 C296,356,282,340,241,329 L163,308 C68,282,27,241,27,140 C27,68,61,-14,157,-14 C242,-14,288,-1,331,79 C350,32,394,-14,493,-14 C622,-14,682,56,687,181 L563,181 C555,115,536,80,491,80 C441,80,424,142,424,205 L424,248 L695,248 L695,279 C695,444,659,552,499,552 C441,552,392,529,372,488 C343,537,304,552,233,552 C129,552,44,510,44,386 L44,373 Z M44,373
                § M38,37 L38,22 C38,-84,103,-152,240,-152 C368,-152,444,-96,444,23 C444,81,423,121,390,147 C437,175,468,238,468,289 C468,376,419,423,358,452 L239,508 C192,530,175,554,175,577 C175,609,199,634,237,634 C264,634,282,628,292,615 C302,602,306,585,306,566 L438,566 C438,660,384,728,248,728 C134,728,43,682,43,563 C43,516,64,478,103,449 C48,421,13,368,13,306 C13,242,45,188,119,152 L242,93 C289,70,306,46,306,9 C306,-24,287,-58,240,-58 C200,-58,170,-28,170,22 L170,37 Z M170,37 M165,272 C143,284,131,307,131,326 C131,351,148,378,186,401 L282,350 C333,323,350,302,350,268 C350,247,338,222,303,193 C291,202,278,210,264,218 Z M264,218
                ‰ M872,181 C872,252,876,292,916,292 C956,292,960,252,960,181 C960,102,956,62,916,62 C876,62,872,102,872,181 Z M872,181 M766,177 C766,48,792,-14,916,-14 C1040,-14,1066,48,1066,177 C1066,306,1040,368,916,368 C792,368,766,306,766,177 Z M766,177 M27,517 C27,388,53,326,177,326 C301,326,327,388,327,517 C327,646,301,708,177,708 C53,708,27,646,27,517 Z M27,517 M133,521 C133,592,137,632,177,632 C217,632,221,592,221,521 C221,442,217,402,177,402 C137,402,133,442,133,521 Z M133,521 M428,177 C428,48,454,-14,578,-14 C702,-14,728,48,728,177 C728,306,702,368,578,368 C454,368,428,306,428,177 Z M428,177 M534,181 C534,252,538,292,578,292 C618,292,622,252,622,181 C622,102,618,62,578,62 C538,62,534,102,534,181 Z M534,181 M128,-31 L217,-31 L623,725 L534,725 Z M534,725
                ! M198,-199 L217,-482 L217,-714 L79,-714 L79,-482 L98,-199 Z M98,-199 M79,-132 L79,0 L217,0 L217,-132 Z M217,-132
                500,240,296,463,480,480,778,593
                260,296,296,390,600,240,370,240
                332,480,480,480,480,480,480,480
                480,480,480,240,240,600,600,600
                481,800,556,556,537,574,481,463
            """.trimIndent(),
            """
                no glyph ${getPath(0)}
                space ${getPath(1)}
                ! ${getPath(2)}
                ( ${getPath(9)}
                * ${getPath(11)}
                # ${getPath(4)}
                ${'$'} ${getPath(5)}
                © ${getPath(170)}
                Ç ${getPath(177)}
                æ ${getPath(144)}
                § ${getPath(102)}
                ‰ ${getPath(122)}
                ! ${font1.getGlyphByChar('!')?.path?.path?.toSvgString()}
                ${rangeWidths(0..7)}
                ${rangeWidths(8..15)}
                ${rangeWidths(16..23)}
                ${rangeWidths(24..31)}
                ${rangeWidths(32..39)}
            """.trimIndent()
        )
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

    @Test
    fun testDecodePush() {
        val stack = doubleArrayListOf()
        TtfCIDFont.CharStringType2.eval(VectorPath(), "1cfb6e".unhex.openFastStream(), TtfCIDFont.CharStringType2.EvalContext(), stack = stack)
        assertEquals(listOf(-1170.0), stack.toList())
    }
}
