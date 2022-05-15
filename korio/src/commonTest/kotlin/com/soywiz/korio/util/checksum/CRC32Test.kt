package com.soywiz.korio.util.checksum

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.openSync
import kotlin.test.Test
import kotlin.test.assertEquals

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
