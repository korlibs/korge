package com.soywiz.korio.lang

import com.soywiz.krypto.encoding.*
import kotlin.test.*

class UTF8Test {
	@Test
	fun test() {
		assertEquals(byteArrayOf('h'.toByte(), 'e'.toByte(), 'l'.toByte(), 'l'.toByte(), 'o'.toByte()).hex, "hello".toByteArray(UTF8).hex)
		assertEquals("hello", byteArrayOf('h'.toByte(), 'e'.toByte(), 'l'.toByte(), 'l'.toByte(), 'o'.toByte()).toString(UTF8))
	}
}
