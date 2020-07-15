package com.soywiz.korim.color

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorSwizzleTest {
    @Test
    fun testSwizzle() {
        assertEquals(RGBA.float(0, 0, 1, 1), Colors.RED.swizzle("bgr"))
        assertEquals(RGBA.float(1, 1, 0, 1), Colors.RED.swizzle("rrb"))
        assertEquals(RGBA.float(1, 1, 1, 1), Colors.RED.swizzle("rrr"))
    }
}
