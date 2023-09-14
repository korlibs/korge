package korlibs.io.stream

import kotlin.test.Test
import kotlin.test.assertEquals

class FillSyncStreamTest {
	@Test
	fun name() {
		assertEquals(0, FillSyncStream(0).readS8())
		assertEquals(-1, FillSyncStream(0xFF).readS8())
		assertEquals(-1, FillSyncStream(0xFF).readS16LE())
		assertEquals(-1, FillSyncStream(0xFF).readS16BE())
	}
}
