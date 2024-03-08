package korlibs.io.stream

import kotlin.test.Test
import kotlin.test.assertEquals

class MemorySyncStreamTest {
	@Test
	fun name() {
		val v = MemorySyncStream(byteArrayOf(0, 0, 1, 0))
		assertEquals(0x100, v.readS32BE())
	}
}
