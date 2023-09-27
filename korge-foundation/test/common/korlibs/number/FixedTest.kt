package korlibs.number

import kotlin.test.Test
import kotlin.test.assertEquals

class FixedTest {
    @Test
    fun test() {
        // 1123456f + 1.123f -> 1123457.1f // Precision lost!
        assertEquals((-1).fixed, -(1.fixed))
        assertEquals((+1).fixed, +(1.fixed))
        assertEquals(1.fixed, 1.fixed)
        assertEquals("1123457.12".fixed, 1123456f.fixed + 1.12f.fixed)
        assertEquals(0.5.fixed, 1.fixed / 2.fixed)
        assertEquals(2.fixed, 1.fixed * 2.fixed)
        assertEquals("21474836.47".fixed, 1123456f.fixed * 77.33f.fixed)
    }

    private fun Fixed.intDec(): String = "${valueInt}:${valueDec}"

    @Test
    fun testIntRem() {
        assertEquals("21474836:47", "21474836.47".fixed.intDec())
        assertEquals("-1:0", "-1.00".fixed.intDec())
        assertEquals("-1:6", "-1.06".fixed.intDec())
        assertEquals("-1:37", "-1.37".fixed.intDec())
    }

    @Test
    fun testDenormals() {
        if (!Fixed.HANDLE_DENORMALS) return

        assertEquals("NaN".fixed, Double.NaN.fixed)
        assertEquals("-Infinity".fixed, Double.NEGATIVE_INFINITY.fixed)
        assertEquals("Infinity".fixed, Double.POSITIVE_INFINITY.fixed)

        assertEquals("NaN", Double.NaN.fixed.toString())
        assertEquals("-Infinity", Double.NEGATIVE_INFINITY.fixed.toString())
        assertEquals("Infinity", Double.POSITIVE_INFINITY.fixed.toString())

        assertEquals("NaN", Fixed.NaN.toString())
        assertEquals("-Infinity", Fixed.NEGATIVE_INFINITY.toString())
        assertEquals("Infinity", Fixed.POSITIVE_INFINITY.toString())
    }
}
