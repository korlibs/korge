package com.soywiz.korio.stream

import kotlin.test.*

class MemorySyncStreamTest {
	@Test
	fun name() {
		val v = MemorySyncStream(byteArrayOf(0, 0, 1, 0))
		assertEquals(0x100, v.readS32BE())
	}
}