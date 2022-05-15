package com.soywiz.korio.crypto

import com.soywiz.korio.compression.deflate.ZLib
import com.soywiz.korio.compression.uncompress
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.lang.toString
import com.soywiz.korio.util.encoding.encode
import com.soywiz.krypto.encoding.Base64
import com.soywiz.krypto.encoding.fromBase64
import com.soywiz.krypto.encoding.toBase64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
