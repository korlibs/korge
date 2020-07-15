package com.soywiz.kds

import kotlin.test.Test
import kotlin.test.assertEquals

class CopyOnWriteFrozenListTest {
    companion object {
        val list = CopyOnWriteFrozenList<String>().also {
            it.add("hello")
        }
    }
    @Test
    fun test() {
        assertEquals(listOf("hello"), list.toList())
        list.add("world")
        assertEquals(listOf("hello", "world"), list.toList())
        list.add("world")
        assertEquals(listOf("hello", "world", "world"), list.toList())
    }
}
