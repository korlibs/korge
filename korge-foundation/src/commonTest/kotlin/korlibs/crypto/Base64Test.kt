package korlibs.crypto

import korlibs.encoding.Base64
import korlibs.encoding.fromBase64
import korlibs.encoding.toBase64
import kotlin.test.Test
import kotlin.test.assertEquals

class Base64Test {
    @Test
    fun name() {
        assertEquals("AQID", Base64.encode(byteArrayOf(1, 2, 3)))
        assertEquals("aGVsbG8=", Base64.encode("hello".encodeToByteArray()))
        assertEquals(byteArrayOf(1, 2, 3).toList(), Base64.decode("AQID").toList())
        assertEquals("hello", Base64.decode("aGVsbG8=").decodeToString())
    }

    @Test
    fun testSeveral() {
        for (item in listOf("", "a", "aa", "aaa", "aaaa", "aaaaa", "Hello World!")) {
            assertEquals(item, item.encodeToByteArray().toBase64().fromBase64().decodeToString())
        }
    }

    @Test
    fun testGlobal() {
        assertEquals("hello", globalBase64)
        assertEquals("hello", ObjectBase64.globalBase64)
    }

    @Test
    fun testIssue64DecodeWithMissingPadding() {
        assertEquals(
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ==",
            Base64.encode(Base64.decode("eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ"))
        )
    }
}

object ObjectBase64 {
    val globalBase64 = "aGVsbG8=".fromBase64().decodeToString()
}

val globalBase64 = "aGVsbG8=".fromBase64().decodeToString()
