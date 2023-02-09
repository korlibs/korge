package com.soywiz.ktruth

import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectionSubject<T : Any>(val actual: Collection<T>) {
    fun isEmpty() {
        assertTrue(actual.isEmpty())
    }
    fun isNotEmpty() {
        assertTrue(actual.isNotEmpty())
    }
    fun hasSize(expectedSize: Int) {
        assertEquals(expectedSize, actual.size)
    }
    fun containsExactlyUnordered(vararg elements: T) {
        val actualSet = actual.toSet()
        val expectedSet = elements.toSet()
        assertEquals(expectedSet.size, actualSet.size)
        for (expected in expectedSet) {
            assertTrue(actualSet.contains(expected))
        }
    }
}
