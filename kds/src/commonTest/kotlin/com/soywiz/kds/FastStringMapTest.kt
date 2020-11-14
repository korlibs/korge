package com.soywiz.kds

import kotlin.test.*

class FastStringMapTest {
    @Test
    fun testFastKeyForEach() {
        val map = FastStringMap<Int>()
        map["hello"] = 10
        map["world"] = 20
        val out = arrayListOf<String>()
        map.fastKeyForEach { out.add(it) }
        assertEquals(listOf("hello", "world"), out.sorted())
    }
}
