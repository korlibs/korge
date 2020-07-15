package com.soywiz.kmem

import kotlin.test.Test
import kotlin.test.assertEquals

class ArraysTest {
    @Test
    fun floatArrayFromIntArray() {
        val fa = IntArray(16).asFloatArray()
        val ia = fa.asIntArray()
        fa[0] = 1f
        assertEquals(0x3f800000, ia[0])
    }
}