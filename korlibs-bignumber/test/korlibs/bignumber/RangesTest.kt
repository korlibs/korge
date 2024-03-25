package korlibs.bignumber

import kotlin.test.*

class RangesTest {
    @Test
    fun progressionTest() {
        assertEquals((1.bi..4.bi).toList().size, 4)
        assertEquals((1.bi..4.bi step 2.bi).toList().size, 2)
        assertEquals((1.bi..4.bi step 5.bi).toList().size, 1)
    }

    @Test
    fun containsTest() {
        assertTrue(BigInt.ZERO in BigInt("-99999999999999999999999999999999999999999999999")..BigInt("99999999999999999999999999999999999999999999999"))
        assertTrue(BigNum.ZERO in BigNum("-99999999999999999999999999999999999999999999999")..BigNum("99999999999999999999999999999999999999999999999"))
        assertTrue(BigNum.ZERO in BigNum("-0.000000000000000000000000000000000000000000001")..BigNum("0.000000000000000000000000000000000000000000001"))
    }
}
