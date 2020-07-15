package com.soywiz.kmem

import kotlin.test.Test
import kotlin.test.assertEquals

class BufferTest {
    @Test
    fun test() {
        val ba = Int8BufferAlloc(3).apply {
            this[0] = -1
            this[1] = -2
            this[2] = -3
        }
        assertEquals(-1, ba[0])
        assertEquals(-2, ba[1])
        assertEquals(-3, ba[2])
    }
}