package com.soywiz.kmem

import kotlin.test.Test
import kotlin.test.assertEquals

class Float16Test {
    @Test
    fun simple() {
        // Samples from: https://en.wikipedia.org/wiki/Half-precision_floating-point_format
        assertEquals(+1.0, Float16.fromBits(0x3c00).toDouble())
        assertEquals(-2.0, Float16.fromBits(0xc000).toDouble())
        assertEquals(-0.0, Float16.fromBits(0x8000).toDouble())
        assertEquals(+0.0, Float16.fromBits(0x0000).toDouble())
        assertEquals(Double.POSITIVE_INFINITY, Float16.fromBits(0x7c00).toDouble())
        assertEquals(Double.NEGATIVE_INFINITY, Float16.fromBits(0xfc00).toDouble())
    }
}