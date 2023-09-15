package korlibs.crypto

import korlibs.encoding.Base64
import korlibs.encoding.fromBase64
import korlibs.encoding.toBase64
import kotlin.test.Test
import kotlin.test.assertEquals

class Base64UrlTest {
    @Test
    fun name() {
        assertEquals("AQID", Base64.encode(byteArrayOf(1, 2, 3), true))
        assertEquals("aGVsbG8", Base64.encode("hello".encodeToByteArray(), true))
        assertEquals(byteArrayOf(1, 2, 3).toList(), Base64.decode("AQID", true).toList())
        assertEquals("hello", Base64.decode("aGVsbG8", true).decodeToString())
    }

    @Test
    fun testSeveral() {
        for (item in listOf("", "a", "aa", "aaa", "aaaa", "aaaaa", "Hello World!")) {
            assertEquals(
                item,
                item.encodeToByteArray().toBase64(true).fromBase64(ignoreSpaces = false, url = true).decodeToString()
            )
        }
    }

    @Test
    fun testGlobal() {
        assertEquals("hello", globalBase64Url)
        assertEquals("hello", ObjectBase64Url.globalBase64)
    }

    @Test
    fun testIssue64DecodeWithMissingPadding() {
        assertEquals(
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ",
            Base64.encode(
                Base64.decode(
                    "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ",
                    url = true
                ),
                url = true,
                doPadding = false
            )
        )
    }

    @Test
    fun testIssue64DecodeWithPadding() {
        assertEquals(
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ==",
            Base64.encode(
                Base64.decode(
                    "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ==",
                    url = true
                ),
                url = true,
                doPadding = true
            )
        )
    }
}

object ObjectBase64Url {
    val globalBase64 = "aGVsbG8".fromBase64(ignoreSpaces = true, url = true).decodeToString()
}

val globalBase64Url = "aGVsbG8".fromBase64(ignoreSpaces = true, url = true).decodeToString()
