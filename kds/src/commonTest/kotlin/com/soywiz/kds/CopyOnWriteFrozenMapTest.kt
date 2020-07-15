package com.soywiz.kds

import kotlin.test.Test
import kotlin.test.assertEquals

class CopyOnWriteFrozenMapTest {
    companion object {
        val map = CopyOnWriteFrozenMap<String, String>().also {
            it["hello"] = "world"
        }
    }
    @Test
    fun test() {
        assertEquals("world", map["hello"])
        map["hello"] = "demo"
        assertEquals("demo", map["hello"])
        //assertEquals(true, false)
    }
}
