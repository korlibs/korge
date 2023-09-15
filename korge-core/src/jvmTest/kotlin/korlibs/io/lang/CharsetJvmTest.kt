package korlibs.io.lang

import korlibs.encoding.hex
import korlibs.encoding.hexLower
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals

class CharsetJvmTest {
    @Test
    fun testSurrogatePairs() {
        val text = "{Test\uD83D\uDE00}"
        assertEquals(
            text.toByteArray(UTF8).toList(),
            text.toByteArray(kotlin.text.Charsets.UTF_8).toList()
        )
    }

    @Test
    fun testSurrogatePairsTwo() {
        val text = "{Test\uD83D\uDE00}"
        //val text = "\uD83D\uDE00"
        assertEquals(
            text,
            text.toByteArray(UTF8).toString(UTF8).toByteArray(UTF8).toString(UTF8)
        )
    }

    @Test
    fun testPlatformCharset() {
        val charset = platformCharsetProvider.invoke("GB2312", "GB-2312")!!
        val text = "hello你好"
        assertEquals(charset, korlibs.io.lang.Charset.forName("GB-2312"))
        assertEquals("68656c6c6fe4bda0e5a5bd", text.toByteArray(UTF8).hexLower)
        assertEquals("68656c6c6fc4e3bac3", text.toByteArray(charset).hexLower)
        assertEquals(text, text.toByteArray(charset).toString(charset))
    }

    @Test
    fun testPlatformCharsetPartialDecode() {
        val charset = platformCharsetProvider.invoke("GB2312", "GB-2312")!!
        val text = "hello你好"
        val bytes = text.toByteArray(charset)
        val out = StringBuilder()
        val outIncomplete = StringBuilder()
        val read = charset.decode(out, bytes, 1, bytes.size)
        val readIncomplete = charset.decode(outIncomplete, bytes, 1, bytes.size - 1)
        assertEquals(
            """
                'ello你好' - 8
                'ello你' - 7
            """.trimIndent(),
            """
                '$out' - $read
                '$outIncomplete' - $readIncomplete
            """.trimIndent()
        )
    }
}
