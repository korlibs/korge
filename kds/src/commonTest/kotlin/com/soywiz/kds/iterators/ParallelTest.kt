package com.soywiz.kds.iterators

import kotlin.test.*

class ParallelTest {
    @Test
    fun test() {
        for (n in 0 until 256) {
            val list = (0 until n).map { it }
            assertEquals(list.map { it * 2 }, list.parallelMap { it * 2 })
        }
    }
}
