package korlibs.number

import kotlin.test.Test
import kotlin.test.assertEquals

class FixedLongTest {
    @Test
    fun test() {
        // 1123456f + 1.123f -> 1123457.1f // Precision lost!
        assertEquals((-1).fixedLong, -(1.fixedLong))
        assertEquals((+1).fixedLong, +(1.fixedLong))
        assertEquals(1.fixedLong, 1.fixedLong)
        assertEquals("99991234".fixedLong, 99991234L.fixedLong)
        assertEquals("99999991234".fixedLong, 99999991234L.fixedLong)
        assertEquals("9999999991235.1272".fixedLong, 9999999991234L.fixedLong + 1.1272f.fixedLong)
        assertEquals(0.5.fixedLong, 1.fixedLong / 2.fixedLong)
        assertEquals(2.fixedLong, 1.fixedLong * 2.fixedLong)
        //assertEquals("21474836.47".FixedLong, 1123456f.fixedLong * 77.33f.fixedLong)
    }

    private fun FixedLong.intDec(): String = "${valueInt}:${valueDec}"

    @Test
    fun testIntRem() {
        assertEquals("21474836:4700", "21474836.47".fixedLong.intDec())
        assertEquals("-1:0", "-1.00".fixedLong.intDec())
        assertEquals("-1:600", "-1.06".fixedLong.intDec())
        assertEquals("-1:3700", "-1.37".fixedLong.intDec())
    }

    @Test
    fun testDenormals() {
        if (!FixedLong.HANDLE_DENORMALS) return

        assertEquals("NaN".fixedLong, Double.NaN.fixedLong)
        assertEquals("-Infinity".fixedLong, Double.NEGATIVE_INFINITY.fixedLong)
        assertEquals("Infinity".fixedLong, Double.POSITIVE_INFINITY.fixedLong)

        assertEquals("NaN", Double.NaN.fixedLong.toString())
        assertEquals("-Infinity", Double.NEGATIVE_INFINITY.fixedLong.toString())
        assertEquals("Infinity", Double.POSITIVE_INFINITY.fixedLong.toString())

        assertEquals("NaN", FixedLong.NaN.toString())
        assertEquals("-Infinity", FixedLong.NEGATIVE_INFINITY.toString())
        assertEquals("Infinity", FixedLong.POSITIVE_INFINITY.toString())
    }
}
