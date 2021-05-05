package com.soywiz.kds

import kotlin.test.*

class FastArrayListTest {
    @Test
    fun test() {
        val fal = FastArrayList<Int>()
        fal.add(1)
        fal.add(2)
        fal.add(3)
        fal.add(4)
        assertEquals(0, fal.indexOf(1))
        assertEquals(1, fal.indexOf(2))
        assertEquals(2, fal.indexOf(3))
        assertEquals(3, fal.indexOf(4))

        assertEquals("[1, 2, 3, 4]", fal.toString())
        fal.removeAt(1)
        assertEquals("[1, 3, 4]", fal.toString())
        fal.removeAt(1)
        assertEquals("[1, 4]", fal.toString())
        fal.removeAt(0)
        assertEquals("[4]", fal.toString())
        fal.removeAt(0)
        assertEquals("[]", fal.toString())

        assertEquals(-1, fal.indexOf(1))
        assertEquals(-1, fal.indexOf(2))
        assertEquals(-1, fal.indexOf(3))
        assertEquals(-1, fal.indexOf(4))

        fal.add(0, 4)
        fal.add(0, 3)
        fal.add(0, 1)
        fal.add(1, 2)
        assertEquals("[1, 2, 3, 4]", fal.toString())

    }
}
