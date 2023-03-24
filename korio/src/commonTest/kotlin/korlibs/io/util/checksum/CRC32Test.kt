package korlibs.io.util.checksum

import korlibs.io.async.suspendTest
import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.stream.openAsync
import korlibs.io.stream.openSync
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