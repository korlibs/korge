package com.soywiz.korio.util.checksum

import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.test.*

class Adler32Test {
	@Test
	fun test() {
		assertEquals(1541148634, "The quick brown fox jumps over the lazy dog".toByteArray(UTF8).checksum(Adler32))
		assertEquals(1541148634, "The quick brown fox jumps over the lazy dog".toByteArray(UTF8).openSync().checksum(Adler32))
	}
}