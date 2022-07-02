package com.soywiz.kmem

import kotlin.test.Test
import kotlin.test.assertEquals

class FixedTest {
    @Test
    fun test() {
        // 1123456f + 1.123f -> 1123457.1f // Precision lost!
        assertEquals(1.fixed, 1.fixed)
        assertEquals("1123457.123".fixed, 1123456f.fixed + 1.123f.fixed)
        assertEquals(0.5.fixed, 1.fixed / 2.fixed)
        assertEquals(2.fixed, 1.fixed * 2.fixed)
    }
}
