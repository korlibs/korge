package com.soywiz.korio.stream

import com.soywiz.korio.async.*
import kotlin.test.*

class BufferedStreamTest {
	@Test
	fun name() = suspendTest {
		val mem = MemorySyncStream().toAsync()
		val write = mem.duplicate()
		val read = mem.duplicate().buffered()
		//write.writeSync { for (n in 0 until 0x10000) write8(n) }
		for (n in 0 until 0x10000) write.write8(n)
		for (n in 0 until 0x10000) {
			if (read.readU8() != (n and 0xFF)) fail()
		}
		assertEquals(0, read.getAvailable())
		assertEquals(0, read.readBytesUpTo(10).size)
	}
}