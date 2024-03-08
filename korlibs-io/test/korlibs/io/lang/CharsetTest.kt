package korlibs.io.lang

import korlibs.memory.ByteArrayBuilder
import korlibs.encoding.unhex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharsetTest {
    @Test
    fun testSurrogatePairs() {
        val text = "{Test\uD83D\uDE00}"
        assertEquals(
            listOf(123, 84, 101, 115, 116, -16, -97, -104, -128, 125),
            text.toByteArray(UTF8).map { it.toInt() }
        )
    }

    @Test
    fun testSurrogatePairsTwo() {
        val text = "{Test\uD83D\uDE00}"
        assertEquals(
            text,
            text.toByteArray(UTF8).toString(UTF8).toByteArray(UTF8).toString(UTF8)
        )
    }

    @Test
    fun testDecode() {
        val text = byteArrayOf(-87, 32, 50, 48, 48, 57, 32, 45, 32, 50, 48, 49)
        assertEquals(
            "\uFFFD 2009 - 201",
            text.toString(UTF8)
        )
    }

    @Test
    fun testSample() {
        val text = (0 until 255).map { it.toChar() }.joinToString("")
        assertEquals(
            text,
            text.toByteArray(UTF8).toString(UTF8)
        )
    }

    @Test
    fun testUTF16() {
        assertEquals("emoji", "0065006d006f006a0069".unhex.toString(UTF16_BE))
        assertEquals("emoji", "65006d006f006a006900".unhex.toString(UTF16_LE))
    }

    @Test
    fun testCharsetForName() {
        assertEquals(
            """
                UTF-8
                UTF-16-LE
                UTF-16-LE
                UTF-16-BE
                ISO-8859-1
                ISO-8859-1
            """.trimIndent(),
            listOf("UTF-8", "UTF-16", "UTF-16-LE", "UTF-16-BE", "LATIN-1", "ISO-8859-1")
                .joinToString("\n") { Charset.forName(it).name }
        )
        assertFailsWith<InvalidArgumentException> { Charset.forName("MY-UNKNOWN-CHARSET") }
    }

    @Test
    fun testCharsetForNameCustomProvider() {
        val MYDEMOCharsetName = "MYDEMO"

        val charset = object : Charset(MYDEMOCharsetName) {
            override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int): Unit = TODO()
            override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int): Int = TODO()
        }
        Charset.registerProvider({ normalizedName, _ -> if (normalizedName == "DEMO") charset else null }) {
            assertEquals(MYDEMOCharsetName, Charset.forName("DEMO").name)
            assertEquals(MYDEMOCharsetName, Charset.forName("DE-_mo").name)
            assertFailsWith<InvalidArgumentException> { Charset.forName("MY-UNKNOWN-CHARSET") }
        }
    }

    @Test
    fun testPartialDecode() {
        val charset = UTF8
        val text = "hello你好"
        val bytes = text.toByteArray(charset)
        val out = StringBuilder()
        val outIncomplete = StringBuilder()
        val read = charset.decode(out, bytes, 1, bytes.size)
        val readIncomplete = charset.decode(outIncomplete, bytes, 1, bytes.size - 1)
        assertEquals(
            """
                'ello你好' - 10
                'ello你' - 7
            """.trimIndent(),
            """
                '$out' - $read
                '$outIncomplete' - $readIncomplete
            """.trimIndent()
        )
    }
}
