package korlibs.bignumber

import kotlin.test.Test
import kotlin.test.assertEquals

class IntExtTest {
    @Test
    fun testBitCount() {
        assertEquals(0, 0b0.countOneBits())
        assertEquals(1, 0b1.countOneBits())
        assertEquals(1, 0b10.countOneBits())
        assertEquals(2, 0b11.countOneBits())
        assertEquals(3, 0b111.countOneBits())
        assertEquals(3, 0b10101.countOneBits())
        assertEquals(3, 0b101010000.countOneBits())
    }

    @Test
    fun testTrailingZeros() {
        assertEquals(32, 0b0.countTrailingZeroBits())
        assertEquals(0, (-1).countTrailingZeroBits())
        assertEquals(0, 0b1.countTrailingZeroBits())
        assertEquals(1, 0b10.countTrailingZeroBits())
        assertEquals(0, 0b11.countTrailingZeroBits())
        assertEquals(0, 0b111.countTrailingZeroBits())
        assertEquals(0, 0b10101.countTrailingZeroBits())
        assertEquals(4, 0b101010000.countTrailingZeroBits())
    }

    @Test
    fun testLeadingZeros() {
        assertEquals(32, 0b0.countLeadingZeroBits())
        assertEquals(0, (-1).countLeadingZeroBits())
        assertEquals(31, 0b1.countLeadingZeroBits())
        assertEquals(30, 0b10.countLeadingZeroBits())
        assertEquals(30, 0b11.countLeadingZeroBits())
        assertEquals(29, 0b111.countLeadingZeroBits())
        assertEquals(27, 0b10101.countLeadingZeroBits())
        assertEquals(23, 0b0101010000.countLeadingZeroBits())
    }
}
