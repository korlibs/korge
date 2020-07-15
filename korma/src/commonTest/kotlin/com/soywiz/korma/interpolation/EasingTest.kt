package com.soywiz.korma.interpolation

import kotlin.test.*

class EasingTest {
    @Test
    fun test() {
        assertEquals(0.0, Easing.LINEAR(0.0))
        assertEquals(0.1, Easing.LINEAR(0.1))
        assertEquals(0.5, Easing.LINEAR(0.5))
        assertEquals(0.9, Easing.LINEAR(0.9))
        assertEquals(1.0, Easing.LINEAR(1.0))

        assertEquals(0.0, Easing.SMOOTH(0.0))
        assertEquals(0.028000000000000004, Easing.SMOOTH(0.1))
        assertEquals(0.5, Easing.SMOOTH(0.5))
        assertEquals(0.972, Easing.SMOOTH(0.9))
        assertEquals(1.0, Easing.SMOOTH(1.0))
    }
}
