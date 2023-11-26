package korlibs.time.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class PaddedTest {
    @Test
    fun testPadded() {
        assertEquals("15.013", 15.013.padded(2, 3))
        assertEquals("01.013", 1.013.padded(2, 3))
        assertEquals("1.013", 1.013333.padded(1, 3))
        assertEquals("1.100", 1.1.padded(1, 3))
        assertEquals("1.010", 1.010.padded(1, 3))
    }
}
