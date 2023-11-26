package korlibs.io.util.checksum

import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.stream.openSync
import kotlin.test.Test
import kotlin.test.assertEquals

class Adler32Test {
	@Test
	fun test() {
		assertEquals(1541148634, "The quick brown fox jumps over the lazy dog".toByteArray(UTF8).checksum(Adler32))
		assertEquals(1541148634, "The quick brown fox jumps over the lazy dog".toByteArray(UTF8).openSync().checksum(Adler32))
	}
}
