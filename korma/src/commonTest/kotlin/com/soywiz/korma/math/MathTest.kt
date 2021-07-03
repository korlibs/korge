package com.soywiz.korma.math

import com.soywiz.kds.rotated
import kotlin.math.E
import kotlin.test.*

class MathTest {
    @Test
    fun testMinMax34() {
        for (n in 0 until 4) {
            val list = listOf(1, 2, 3, 4).rotated(n)
            assertEquals(1, min(list[0], list[1], list[2], list[3]))
            assertEquals(4, max(list[0], list[1], list[2], list[3]))
        }
        for (n in 0 until 3) {
            val list = listOf(1, 2, 3).rotated(n)
            assertEquals(1, min(list[0], list[1], list[2]))
            assertEquals(3, max(list[0], list[1], list[2]))
        }
    }

    @Test
    fun testConvertRange() {
        assertEquals(300.0, 100.0.convertRange(50.0, 150.0, 200.0, 400.0))
    }

    @Test
    fun testLog() {
        assertEquals(10, log(1024, 2))
        assertEquals(10, log2(1024))
        assertEquals(4, log(10000, 10))
        assertEquals(4, log10(10000))
    }


    @Test
    fun testLn() {
        assertEquals(0, ln(1))
        assertEquals(2, ln(20))
    }

    @Test
    fun testSmoothStep() {
        assertEquals(0.0, 100.0.smoothstep(100.0, 200.0))
        assertEquals(0.028000000000000004, 110.0.smoothstep(100.0, 200.0))
        assertEquals(0.5, 150.0.smoothstep(100.0, 200.0))
        assertEquals(0.972, 190.0.smoothstep(100.0, 200.0))
        assertEquals(1.0, 200.0.smoothstep(100.0, 200.0))
    }

    @Test
    fun testClamp() {
        assertEquals(1, (-1).clamp(1, 10))
        assertEquals(1, 0.clamp(1, 10))
        assertEquals(1, 1.clamp(1, 10))
        assertEquals(5, 5.clamp(1, 10))
        assertEquals(10, 10.clamp(1, 10))
        assertEquals(10, 10.clamp(1, 10))

        assertEquals(1f, (-1f).clamp(1f, 10f))
        assertEquals(1f, 0f.clamp(1f, 10f))
        assertEquals(1f, 1f.clamp(1f, 10f))
        assertEquals(5f, 5f.clamp(1f, 10f))
        assertEquals(10f, 10f.clamp(1f, 10f))
        assertEquals(10f, 10f.clamp(1f, 10f))

        assertEquals(1L, (-1L).clamp(1L, 10L))
        assertEquals(1L, 0L.clamp(1L, 10L))
        assertEquals(1L, 1L.clamp(1L, 10L))
        assertEquals(5L, 5L.clamp(1L, 10L))
        assertEquals(10L, 10L.clamp(1L, 10L))
        assertEquals(10L, 10L.clamp(1L, 10L))

        assertEquals(1.0, (-1.0).clamp(1.0, 10.0))
        assertEquals(1.0, 0.0.clamp(1.0, 10.0))
        assertEquals(1.0, 1.0.clamp(1.0, 10.0))
        assertEquals(5.0, 5.0.clamp(1.0, 10.0))
        assertEquals(10.0, 10.0.clamp(1.0, 10.0))
        assertEquals(10.0, 10.0.clamp(1.0, 10.0))
    }

    @Test
    fun testBetweenInclusive() {
        assertEquals(false, 0.0.betweenInclusive(1.0, 10.0))
        assertEquals(false, 0.9.betweenInclusive(1.0, 10.0))
        assertEquals(true, 1.0.betweenInclusive(1.0, 10.0))
        assertEquals(true, 1.1.betweenInclusive(1.0, 10.0))
        assertEquals(true, 9.9.betweenInclusive(1.0, 10.0))
        assertEquals(true, 10.0.betweenInclusive(1.0, 10.0))
        assertEquals(false, 10.1.betweenInclusive(1.0, 10.0))
    }

    @Test
    fun testRoundDecimalPlaces() {
        assertEquals(11.23, 11.232425.roundDecimalPlaces(2))
    }

    @Test
    fun testIsEquivalent() {
        assertEquals(true, isEquivalent(0.99999999, 1.0))
        assertEquals(false, isEquivalent(0.9, 1.0))
    }

    @Test
    fun testIsAlmostZero() {
        assertEquals(true, 0.0.isAlmostZero())
        assertEquals(true, 1e-20.isAlmostZero())
        assertEquals(false, 0.1.isAlmostZero())

        assertEquals(true, 0.0f.isAlmostZero())
        assertEquals(true, 1e-20f.isAlmostZero())
        assertEquals(false, 0.1f.isAlmostZero())
    }

    @Test
    fun testIsNanOrInfinite() {
        assertEquals(false, 0.0.isNanOrInfinite())
        assertEquals(false, (-0.1).isNanOrInfinite())
        assertEquals(false, 1.0.isNanOrInfinite())
        assertEquals(false, Double.MAX_VALUE.isNanOrInfinite())
        assertEquals(false, Double.MIN_VALUE.isNanOrInfinite())
        assertEquals(true, Double.POSITIVE_INFINITY.isNanOrInfinite())
        assertEquals(true, Double.NEGATIVE_INFINITY.isNanOrInfinite())
        assertEquals(true, Double.NaN.isNanOrInfinite())

        assertEquals(false, 0f.isNanOrInfinite())
        assertEquals(false, (-.1f).isNanOrInfinite())
        assertEquals(false, 1f.isNanOrInfinite())
        assertEquals(false, Float.MAX_VALUE.isNanOrInfinite())
        assertEquals(false, Float.MIN_VALUE.isNanOrInfinite())
        assertEquals(true, Float.POSITIVE_INFINITY.isNanOrInfinite())
        assertEquals(true, Float.NEGATIVE_INFINITY.isNanOrInfinite())
        assertEquals(true, Float.NaN.isNanOrInfinite())
    }

    @Test
    fun testIsMultipleOf() {
        assertTrue(4.isMultipleOf(2))
        assertTrue(25L.isMultipleOf(5L))

        assertEquals(12, 10.nextMultipleOf(3))
        assertEquals(15L, 11L.nextMultipleOf(5L))

        assertEquals(9, 10.prevMultipleOf(3))
        assertEquals(10L, 11L.prevMultipleOf(5L))
    }
}
