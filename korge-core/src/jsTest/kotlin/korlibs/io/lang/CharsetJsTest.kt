package korlibs.io.lang

import korlibs.encoding.hexLower
import korlibs.encoding.unhex
import kotlin.test.Test
import kotlin.test.assertEquals

class CharsetJsTest {
    // JS TextEncoder only supports UTF-8
    @Test
    fun testPlatformCharset() {
        val charset = platformCharsetProvider.invoke("GB2312", "GB-2312")!!
        val text = "hello你好"
        assertEquals(charset, Charset.forName("GB-2312"))
        assertEquals("68656c6c6fe4bda0e5a5bd", text.toByteArray(UTF8).hexLower)
        //assertEquals("68656c6c6fc4e3bac3", text.toByteArray(charset).hexLower)
        assertEquals(text, "68656c6c6fc4e3bac3".unhex.toString(charset))
    }
}
