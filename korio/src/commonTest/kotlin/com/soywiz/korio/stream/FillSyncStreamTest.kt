package com.soywiz.korio.stream

import kotlin.test.*

class FillSyncStreamTest {
	@Test
	fun name() {
		assertEquals(0, FillSyncStream(0).readS8())
		assertEquals(-1, FillSyncStream(0xFF).readS8())
		assertEquals(-1, FillSyncStream(0xFF).readS16LE())
		assertEquals(-1, FillSyncStream(0xFF).readS16BE())
	}
}