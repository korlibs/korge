package korlibs.memory

import korlibs.math.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumbersTest {
    @Test
    fun testIlog2() {
        for (n in 0 .. 30) {
            assertEquals(n, ilog2(1 shl n))
            if (n > 2) {
                assertEquals(n, ilog2((1 shl n) + 1))
            }
        }
    }

    @Test
    fun oddEven() {
        assertTrue { 0.isEven }
        assertTrue { !1.isEven }
        assertTrue { 2.isEven }
        assertTrue { !3.isEven }

        assertTrue { !0.isOdd }
        assertTrue { 1.isOdd }
        assertTrue { !2.isOdd }
        assertTrue { 3.isOdd }
    }

    @Test
    fun isPowerOfTwo() {
        assertEquals(true, 0.isPowerOfTwo)
        assertEquals(true, 1.isPowerOfTwo)
        assertEquals(true, 2.isPowerOfTwo)
        assertEquals(false, 3.isPowerOfTwo)
        assertEquals(true, 4.isPowerOfTwo)
        assertEquals(false, 5.isPowerOfTwo)

        assertEquals(false, 1023.isPowerOfTwo)
        assertEquals(true, 1024.isPowerOfTwo)
        assertEquals(false, 1025.isPowerOfTwo)
        for (n in 0..31) {
            if (n >= 2) assertEquals(false, ((1 shl n) - 1).isPowerOfTwo)
            assertEquals(true, (1 shl n).isPowerOfTwo)
            if (n >= 2) assertEquals(false, ((1 shl n) + 1).isPowerOfTwo)
        }

        assertEquals(0, 0.nextPowerOfTwo)
        assertEquals(1, 1.nextPowerOfTwo)
        assertEquals(2, 2.nextPowerOfTwo)
        assertEquals(4, 3.nextPowerOfTwo)
        assertEquals(4, 4.nextPowerOfTwo)
        assertEquals(8, 5.nextPowerOfTwo)
        assertEquals(16, 10.nextPowerOfTwo)
        assertEquals(16, 16.nextPowerOfTwo)
        assertEquals(32, 17.nextPowerOfTwo)
        assertEquals(64, 33.nextPowerOfTwo)
        assertEquals(64, 64.nextPowerOfTwo)
        assertEquals(128, 65.nextPowerOfTwo)
        assertEquals(128, 127.nextPowerOfTwo)
        assertEquals(128, 128.nextPowerOfTwo)

        assertEquals(0, 0.prevPowerOfTwo)
        assertEquals(1, 1.prevPowerOfTwo)
        assertEquals(2, 2.prevPowerOfTwo)
        assertEquals(2, 3.prevPowerOfTwo)
        assertEquals(4, 4.prevPowerOfTwo)
        assertEquals(4, 5.prevPowerOfTwo)
        assertEquals(8, 10.prevPowerOfTwo)
        assertEquals(16, 16.prevPowerOfTwo)
        assertEquals(16, 17.prevPowerOfTwo)
        assertEquals(32, 33.prevPowerOfTwo)
        assertEquals(64, 64.prevPowerOfTwo)
        assertEquals(64, 65.prevPowerOfTwo)
        assertEquals(64, 127.prevPowerOfTwo)
        assertEquals(128, 128.prevPowerOfTwo)
    }
}
