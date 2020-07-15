package com.soywiz.kds

import kotlin.test.Test
import kotlin.test.assertEquals

class CopyOnWriteFrozenSetTest {
    companion object {
        val list = CopyOnWriteFrozenSet<String>().also {
            it.add("hello")
        }
    }
    @Test
    fun test() {
        assertEquals(listOf("hello"), list.toList())
        list.add("world")
        assertEquals(listOf("hello", "world"), list.toList())
        list.add("world")
        assertEquals(listOf("hello", "world"), list.toList())
    }
}
