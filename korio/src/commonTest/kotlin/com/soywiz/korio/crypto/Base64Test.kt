package com.soywiz.korio.crypto

import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.krypto.encoding.*
import kotlin.test.*

class Base64Test {
	@Test
	fun name() {
		assertEquals("AQID", Base64.encode(byteArrayOf(1, 2, 3)))
		assertEquals("aGVsbG8=", Base64.encode("hello".toByteArray()))
		assertEquals("aGVsbG8=", Base64.encode("hello", UTF8))
		assertEquals(byteArrayOf(1, 2, 3).toList(), Base64.decode("AQID").toList())
		assertEquals("hello", Base64.decode("aGVsbG8=").toString(UTF8))
	}

    @Test
    fun testSeveral() {
        for (item in listOf("", "a", "aa", "aaa", "aaaa", "aaaaa", "Hello World!")) {
            assertEquals(item, item.toByteArray(UTF8).toBase64().fromBase64().toString(UTF8))
        }
    }

    @Test
    fun testGlobal() {
        assertEquals("hello", globalBase64)
        assertEquals("hello", ObjectBase64.globalBase64)
        assertTrue { DefaultTtfFont.isNotEmpty() }
    }
}

object ObjectBase64 {
    val globalBase64 = "aGVsbG8=".fromBase64().toString(UTF8)
}

val globalBase64 = "aGVsbG8=".fromBase64().toString(UTF8)
val DefaultTtfFont = DefaultTtfFontBase64.fromBase64().uncompress(ZLib)
