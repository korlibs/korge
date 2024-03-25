package korlibs.io.util.checksum

import korlibs.io.async.suspendTest
import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.stream.*
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

    @Test
    fun testAsyncOutputWithChecksumUpdater() = suspendTest {
        val updater = CRC32.updater()
        val out = MemoryAsyncStream()
        val out2 = out.withChecksumUpdater(updater)
        assertEquals(0, updater.current)
        out2.writeString("The quick brown fox jumps over the lazy dog")
        assertEquals(0x414fa339, updater.current)
    }
}
