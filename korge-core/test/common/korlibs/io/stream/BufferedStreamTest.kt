package korlibs.io.stream

import korlibs.io.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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
