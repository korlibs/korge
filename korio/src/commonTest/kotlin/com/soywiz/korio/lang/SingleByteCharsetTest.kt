package com.soywiz.korio.lang

import kotlin.test.*

class SingleByteCharsetTest {
	@Test
	fun test() {
		val charset = SingleByteCharset("demo", String(CharArray(256) { (it + 0x1000).toChar() }))
		assertEquals("\u1000\u1001\u1002\u1003", byteArrayOf(0, 1, 2, 3).toString(charset))
		assertEquals(byteArrayOf(0, 1, 2, 3).toList(), "\u1000\u1001\u1002\u1003".toByteArray(charset).toList())
		assertEquals(byteArrayOf('?'.toByte(), '?'.toByte()).toList(), "\u4000\u4001".toByteArray(charset).toList())
	}
}