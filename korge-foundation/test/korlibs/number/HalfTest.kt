package korlibs.number

import kotlin.test.Test
import kotlin.test.assertEquals

class HalfTest {
    @Test
    fun simple() {
        // Samples from: https://en.wikipedia.org/wiki/Half-precision_floating-point_format
        assertEquals(+1.0, Half.fromBits(0x3c00).toDouble())
        assertEquals(-2.0, Half.fromBits(0xc000).toDouble())
        assertEquals(-0.0, Half.fromBits(0x8000).toDouble())
        assertEquals(+0.0, Half.fromBits(0x0000).toDouble())
        assertEquals(Double.POSITIVE_INFINITY, Half.fromBits(0x7c00).toDouble())
        assertEquals(Double.NEGATIVE_INFINITY, Half.fromBits(0xfc00).toDouble())
    }

    @Test
    fun simpleFloat() {
        // Samples from: https://en.wikipedia.org/wiki/Half-precision_floating-point_format
        assertEquals(+1.0f, Half.fromBits(0x3c00).toFloat())
        assertEquals(-2.0f, Half.fromBits(0xc000).toFloat())
        assertEquals(-0.0f, Half.fromBits(0x8000).toFloat())
        assertEquals(+0.0f, Half.fromBits(0x0000).toFloat())
        assertEquals(Float.POSITIVE_INFINITY, Half.fromBits(0x7c00).toFloat())
        assertEquals(Float.NEGATIVE_INFINITY, Half.fromBits(0xfc00).toFloat())
    }

    @Test
    fun testToHalf() {
        assertEquals(0x3c00, 1f.toHalf().rawBits.toInt())
        assertEquals(0xc000, (-2f).toHalf().rawBits.toInt())
    }

    @Test
    fun testConversions() {
        (Float.NaN).also { assertEquals(it, it.toHalf().toFloat()) }
        (Float.NEGATIVE_INFINITY).also { assertEquals(it, it.toHalf().toFloat()) }
        (Float.POSITIVE_INFINITY).also { assertEquals(it, it.toHalf().toFloat()) }
        1f.also { assertEquals(it, it.toHalf().toFloat()) }
        0f.also { assertEquals(it, it.toHalf().toFloat()) }
        2f.also { assertEquals(it, it.toHalf().toFloat()) }
        (-2f).also { assertEquals(it, it.toHalf().toFloat()) }
        (.125f).also { assertEquals(it, it.toHalf().toFloat()) }
        1024f.also { assertEquals(it, it.toHalf().toFloat()) }
    }

    @Test
    fun testConversionsEx() {
        for (n in 0 until 1024) {
            n.toFloat().also { assertEquals(it, it.toHalf().toFloat()) }
            (-(n.toFloat())).also { assertEquals(it, it.toHalf().toFloat()) }
        }
    }

    @Test
    fun testArithmetic() {
        assertEquals((-1f).toHalf(), -(1f.toHalf()))
        assertEquals((+1f).toHalf(), +(1f.toHalf()))
        assertEquals(3f.toHalf(), 1f.toHalf() + 2f.toHalf())
        assertEquals((-1f).toHalf(), 1f.toHalf() - 2f.toHalf())
        assertEquals((-2f).toHalf(), 2f.toHalf() * (-1f).toHalf())
        assertEquals((-.5f).toHalf(), 2f.toHalf() / (-4f).toHalf())
    }
}
