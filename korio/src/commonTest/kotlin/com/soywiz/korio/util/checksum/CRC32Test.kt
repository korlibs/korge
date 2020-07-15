package com.soywiz.korio.util.checksum

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.test.*

class CRC32Test {
	@Test
	fun test() {
		assertEquals(0x414fa339, "The quick brown fox jumps over the lazy dog".toByteArray(UTF8).checksum(CRC32))
		assertEquals(0x414fa339, "The quick brown fox jumps over the lazy dog".toByteArray(UTF8).openSync().checksum(CRC32))
	}

	@Test
	fun test2() = suspendTest {
		assertEquals(0x414fa339, "The quick brown fox jumps over the lazy dog".toByteArray(UTF8).openAsync().checksum(CRC32))
	}
}