package korlibs.bignumber.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class IntExtTest {
    @Test
    fun testBitCount() {
        assertEquals(0, 0b0.bitCount())
        assertEquals(1, 0b1.bitCount())
        assertEquals(1, 0b10.bitCount())
        assertEquals(2, 0b11.bitCount())
        assertEquals(3, 0b111.bitCount())
        assertEquals(3, 0b10101.bitCount())
        assertEquals(3, 0b101010000.bitCount())
    }

    @Test
    fun testTrailingZeros() {
        assertEquals(32, 0b0.trailingZeros())
        assertEquals(0, (-1).trailingZeros())
        assertEquals(0, 0b1.trailingZeros())
        assertEquals(1, 0b10.trailingZeros())
        assertEquals(0, 0b11.trailingZeros())
        assertEquals(0, 0b111.trailingZeros())
        assertEquals(0, 0b10101.trailingZeros())
        assertEquals(4, 0b101010000.trailingZeros())
    }

    @Test
    fun testLeadingZeros() {
        assertEquals(32, 0b0.leadingZeros())
        assertEquals(0, (-1).leadingZeros())
        assertEquals(31, 0b1.leadingZeros())
        assertEquals(30, 0b10.leadingZeros())
        assertEquals(30, 0b11.leadingZeros())
        assertEquals(29, 0b111.leadingZeros())
        assertEquals(27, 0b10101.leadingZeros())
        assertEquals(23, 0b0101010000.leadingZeros())
    }
}
