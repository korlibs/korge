package com.soywiz.korma.interpolation

import kotlin.math.abs
import kotlin.test.*

class EasingTest {
    @Test
    fun testLinear() {
        assertEquals(0.0, Easing.LINEAR(0.0))
        assertEquals(0.1, Easing.LINEAR(0.1))
        assertEquals(0.5, Easing.LINEAR(0.5))
        assertEquals(0.9, Easing.LINEAR(0.9))
        assertEquals(1.0, Easing.LINEAR(1.0))
    }

    @Test
    fun testSmooth() {
        assertEquals(0.0, Easing.SMOOTH(0.0))
        assertEquals(0.028000000000000004, Easing.SMOOTH(0.1))
        assertEquals(0.5, Easing.SMOOTH(0.5))
        assertEquals(0.972, Easing.SMOOTH(0.9))
        assertEquals(1.0, Easing.SMOOTH(1.0))
    }

    @Test
    fun testSine() {
        assertEquals(0.0, Easing.EASE_SINE(0.0))
        assertEquals(1.0, Easing.EASE_SINE(1.0))
    }

    @Test
    fun testStartsAtZero() {
        Easing.ALL.values.forEach { easing ->
            val v = easing(0.0)
            assertTrue(abs(v) < 0.0001, "Easing $easing did not start at 0, was $v")
        }
    }

    @Test
    fun testEndsAtOne() {
        Easing.ALL.values.forEach { easing ->
            val v = easing(1.0)
            assertTrue(abs(v - 1.0) < 0.0001, "Easing $easing did not end at 1, was $v")
        }
    }
}
