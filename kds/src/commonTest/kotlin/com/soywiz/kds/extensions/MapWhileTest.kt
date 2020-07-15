package com.soywiz.kds.extensions

import com.soywiz.kds.*
import kotlin.test.*

class MapWhileTest {
    @Test
    fun test() {
        assertEquals(listOf(0, 1, 2, 3), mapWhile({ it < 4 }) { it })
        assertEquals(listOf(0, 1, 2, 3), mapWhileArray({ it < 4 }) { it }.toList())
        assertEquals(intArrayOf(0, 1, 2, 3).toList(), mapWhileInt({ it < 4 }) { it }.toList())
        assertEquals(floatArrayOf(0f, 1f, 2f, 3f).toList(), mapWhileFloat({ it < 4 }) { it.toFloat() }.toList())
        assertEquals(doubleArrayOf(0.0, 1.0, 2.0, 3.0).toList(), mapWhileDouble({ it < 4 }) { it.toDouble() }.toList())
    }

    @Test
    fun test2() {
        val iterator = listOf(1, 2, 3).iterator()
        assertEquals(listOf(1, 2, 3), mapWhile({ iterator.hasNext() }) { iterator.next()})
    }
}