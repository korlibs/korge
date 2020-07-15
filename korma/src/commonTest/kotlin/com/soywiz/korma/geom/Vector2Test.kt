package com.soywiz.korma.geom

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class Vector2Test {
    @Test
    fun name() {
        val v = IPoint(1, 1.0)
        //assertEquals(sqrt(2.0), v.length, 0.001)
        assertEquals(sqrt(2.0), v.length)
    }

    @Test
    fun testString() {
        assertEquals("(1, 2)", IPoint(1, 2).toString())

    }
}
