package korlibs.memory

import kotlin.test.Test
import kotlin.test.assertEquals

class FloatZeroNanInfinite {
    @Test
    fun isAlmostZero() {
        // Float
        assertEquals(true, 0f.isAlmostZero())
        assertEquals(true, 1e-19f.isAlmostZero())
        assertEquals(true, 1e-20f.isAlmostZero())
        assertEquals(false, 1e-18f.isAlmostZero())

        // Double
        assertEquals(true, 0.0.isAlmostZero())
        assertEquals(true, 1e-19.isAlmostZero())
        assertEquals(true, 1e-20.isAlmostZero())
        assertEquals(false, 1e-18.isAlmostZero())
    }

    @Test
    fun isNanOrInfinite() {
        // Float
        assertEquals(false, (-1f).isNanOrInfinite())
        assertEquals(false, 0f.isNanOrInfinite())
        assertEquals(false, 1e-19f.isNanOrInfinite())
        assertEquals(false, 1e25f.isNanOrInfinite())
        assertEquals(true, Float.NaN.isNanOrInfinite())
        assertEquals(true, Float.NEGATIVE_INFINITY.isNanOrInfinite())
        assertEquals(true, Float.POSITIVE_INFINITY.isNanOrInfinite())

        // Double
        assertEquals(false, (-1.0).isNanOrInfinite())
        assertEquals(false, 0.0.isNanOrInfinite())
        assertEquals(false, 1e-19.isNanOrInfinite())
        assertEquals(false, 1e25.isNanOrInfinite())
        assertEquals(true, Double.NaN.isNanOrInfinite())
        assertEquals(true, Double.NEGATIVE_INFINITY.isNanOrInfinite())
        assertEquals(true, Double.POSITIVE_INFINITY.isNanOrInfinite())
    }
}