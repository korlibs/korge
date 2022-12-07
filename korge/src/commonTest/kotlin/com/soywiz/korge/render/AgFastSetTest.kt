package com.soywiz.korge.render

import kotlin.test.*

class AgFastSetTest {
    @Test
    fun test() {
        val set = AgFastSet<String>()
        set.add("a")
        assertEquals(true, "a" in set)
        set.add("b")
        assertEquals(true, "b" in set)
        set.add("c")

        assertEquals(false, "d" in set)
        assertEquals(true, "c" in set)
        assertEquals(true, "b" in set)
        assertEquals(true, "a" in set)

        set.add("d")
        assertEquals(true, "d" in set)
        assertEquals(true, "c" in set)
        assertEquals(true, "b" in set)
        assertEquals(true, "a" in set)

        set.remove("c")
        assertEquals(true, "d" in set)
        assertEquals(false, "c" in set)
        assertEquals(true, "b" in set)
        assertEquals(true, "a" in set)
    }
}
