package korlibs.io.stream

import korlibs.util.*
import kotlin.test.*

class FastByteArrayInputStreamTest {
	@kotlin.test.Test
	fun name() {
		val v = FastByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
		assertEquals(4, v.available)
		assertEquals("01020304", "%08X".format(v.readS32BE()))
		assertEquals(0, v.available)
		assertEquals(4, v.position)
		assertEquals(4, v.length)
	}

    @kotlin.test.Test
    fun testSkipToAlign() {
        val v = FastByteArrayInputStream(ByteArray(32))
        v.skip(3)
        assertEquals(3, v.position)
        v.skipToAlign(4)
        assertEquals(4, v.position)
        v.skipToAlign(4)
        assertEquals(4, v.position)
        v.skip(1)
        assertEquals(5, v.position)
        v.skipToAlign(4)
        assertEquals(8, v.position)
    }
}
