package com.soywiz.krypto

import com.soywiz.krypto.encoding.Base64Url
import com.soywiz.krypto.encoding.fromBase64Url
import com.soywiz.krypto.encoding.toBase64Url
import kotlin.test.Test
import kotlin.test.assertEquals

class Base64UrlTest {
    @Test
    fun name() {
        assertEquals("AQID", Base64Url.encode(byteArrayOf(1, 2, 3)))
        assertEquals("aGVsbG8", Base64Url.encode("hello".encodeToByteArray()))
        assertEquals(byteArrayOf(1, 2, 3).toList(), Base64Url.decode("AQID").toList())
        assertEquals("hello", Base64Url.decode("aGVsbG8").decodeToString())
    }

    @Test
    fun testSeveral() {
        for (item in listOf("", "a", "aa", "aaa", "aaaa", "aaaaa", "Hello World!")) {
            assertEquals(item, item.encodeToByteArray().toBase64Url().fromBase64Url().decodeToString())
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
            Base64Url.encode(Base64Url.decode("eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ"))
        )
    }

    @Test
    fun testIssue64DecodeWithPadding() {
        assertEquals(
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ==",
            Base64Url.encode(
                Base64Url.decode("eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ=="),
                true
            )
        )
    }
}

object ObjectBase64Url {
    val globalBase64 = "aGVsbG8".fromBase64Url().decodeToString()
}

val globalBase64Url = "aGVsbG8".fromBase64Url().decodeToString()
