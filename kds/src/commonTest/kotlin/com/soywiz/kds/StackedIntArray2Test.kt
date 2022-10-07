package com.soywiz.kds

import kotlin.test.Test
import kotlin.test.assertEquals

class StackedIntArray2Test {
    @Test
    fun test() {
        val value = StackedIntArray2(2, 2)
        assertEquals(-1, value.getFirst(0, 0))
        value.push(0, 0, 1)
        value.push(0, 0, 2)
        value.push(0, 0, 3)

        assertEquals(3, value.getStackLevel(0, 0))
        assertEquals(1, value.getFirst(0, 0))
        assertEquals(2, value.get(0, 0, 1))
        assertEquals(3, value.getLast(0, 0))

        value.removeLast(0, 0)
        assertEquals(1, value.getFirst(0, 0))
        assertEquals(2, value.getLast(0, 0))
        assertEquals(2, value.getStackLevel(0, 0))

        value.removeLast(0, 0)
        assertEquals(1, value.getFirst(0, 0))
        assertEquals(1, value.getLast(0, 0))
        assertEquals(1, value.getStackLevel(0, 0))

        value.removeLast(0, 0)
        assertEquals(-1, value.getFirst(0, 0))
        assertEquals(-1, value.getLast(0, 0))
        assertEquals(0, value.getStackLevel(0, 0))

        assertEquals(-1, value.getFirst(1, 0))
        assertEquals(0, value.getStackLevel(1, 0))
    }
}
