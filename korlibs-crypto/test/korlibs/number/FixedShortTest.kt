package korlibs.number

import kotlin.test.Test
import kotlin.test.assertEquals

class FixedShortTest {
    @Test
    fun test() {
        // 1123456f + 1.123f -> 1123457.1f // Precision lost!
        assertEquals((-1).toFixedShort(), -(1.toFixedShort()))
        assertEquals((+1).toFixedShort(), +(1.toFixedShort()))
        assertEquals(1.toFixedShort(), 1.toFixedShort())
        assertEquals("1123.1".toFixedShort(), 1122f.toFixedShort() + 1.1.toFixedShort())
        assertEquals(0.5.toFixedShort(), 1.toFixedShort() / 2.toFixedShort())
        assertEquals(2.toFixedShort(), 1.toFixedShort() * 2.toFixedShort())
        assertEquals("2310.0".toFixedShort(), 1100.toFixedShort() * 2.1f.toFixedShort())
    }

    private fun FixedShort.intDec(): String = "${valueInt}:${valueDec}"

    @Test
    fun testIntRem() {
        assertEquals("3275:9", "3275.9".toFixedShort().intDec())
        assertEquals("-1:0", "-1.0".toFixedShort().intDec())
        assertEquals("-1:6", "-1.6".toFixedShort().intDec())
        assertEquals("-1:3", "-1.3".toFixedShort().intDec())
    }

    @Test
    fun testDenormals() {
        if (!FixedShort.HANDLE_DENORMALS) return

        assertEquals("NaN".toFixedShort(), Double.NaN.toFixedShort())
        assertEquals("-Infinity".toFixedShort(), Double.NEGATIVE_INFINITY.toFixedShort())
        assertEquals("Infinity".toFixedShort(), Double.POSITIVE_INFINITY.toFixedShort())

        assertEquals("NaN", Double.NaN.toFixedShort().toString())
        assertEquals("-Infinity", Double.NEGATIVE_INFINITY.toFixedShort().toString())
        assertEquals("Infinity", Double.POSITIVE_INFINITY.toFixedShort().toString())

        assertEquals("NaN", FixedShort.NaN.toString())
        assertEquals("-Infinity", FixedShort.NEGATIVE_INFINITY.toString())
        assertEquals("Infinity", FixedShort.POSITIVE_INFINITY.toString())
    }
}
