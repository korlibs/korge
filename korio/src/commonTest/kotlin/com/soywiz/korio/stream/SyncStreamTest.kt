package com.soywiz.korio.stream

import kotlin.test.*

class SyncStreamTest {
	@Test
	fun testRead() {
		assertEquals(0xFF, byteArrayOf(0xFF.toByte()).openSync().readU8())
		assertEquals(0x0201, byteArrayOf(0x01, 0x02).openSync().readU16LE())
		assertEquals(0x0102, byteArrayOf(0x01, 0x02).openSync().readU16BE())
		assertEquals(0x030201, byteArrayOf(0x01, 0x02, 0x03).openSync().readU24LE())
		assertEquals(0x010203, byteArrayOf(0x01, 0x02, 0x03).openSync().readU24BE())
		assertEquals(0x04030201L, byteArrayOf(0x01, 0x02, 0x03, 0x04).openSync().readU32LE())
		assertEquals(0x01020304L, byteArrayOf(0x01, 0x02, 0x03, 0x04).openSync().readU32BE())

		assertEquals(-1, byteArrayOf(0xFF.toByte()).openSync().readS8())
		assertEquals(-2, byteArrayOf(-2, -1).openSync().readS16LE())
		assertEquals(-2, byteArrayOf(-1, -2).openSync().readS16BE())
		assertEquals(-2, byteArrayOf(-2, -1, -1).openSync().readS24LE())
		assertEquals(-2, byteArrayOf(-1, -1, -2).openSync().readS24BE())
		assertEquals(0x04030201, byteArrayOf(0x01, 0x02, 0x03, 0x04).openSync().readS32LE())
		assertEquals(0x01020304, byteArrayOf(0x01, 0x02, 0x03, 0x04).openSync().readS32BE())

		assertEquals(0x0807060504030201L, byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x8).openSync().readS64LE())
		assertEquals(0x0102030405060708L, byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x8).openSync().readS64BE())

		assertEquals("hello", "hello\u0000world".openSync().readStringz())
	}

	@Test
	fun testWrite() {
		assertEquals(-3, MemorySyncStreamToByteArray { write8(-3) }.openSync().readS8())
		assertEquals(-3, MemorySyncStreamToByteArray { write16LE(-3) }.openSync().readS16LE())
		assertEquals(-3, MemorySyncStreamToByteArray { write16BE(-3) }.openSync().readS16BE())
		assertEquals(-3, MemorySyncStreamToByteArray { write24LE(-3) }.openSync().readS24LE())
		assertEquals(-3, MemorySyncStreamToByteArray { write24BE(-3) }.openSync().readS24BE())
		assertEquals(-3, MemorySyncStreamToByteArray { write32LE(-3) }.openSync().readS32LE())
		assertEquals(-3, MemorySyncStreamToByteArray { write32BE(-3) }.openSync().readS32BE())
		assertEquals(-3, MemorySyncStreamToByteArray { write64LE(-3) }.openSync().readS64LE())
		assertEquals(-3, MemorySyncStreamToByteArray { write64BE(-3) }.openSync().readS64BE())
	}
}