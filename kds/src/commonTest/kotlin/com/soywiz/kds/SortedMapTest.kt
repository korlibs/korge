package com.soywiz.kds

import kotlin.test.*

class SortedMapTest {
    @Test
    fun test() {
        val map = SortedMap<Int, Int>()
        map[10] = 10
        map[5] = 5
        map[20] = 20
        map[15] = 15

        assertEquals(5, map[5])
        assertEquals(10, map[10])
        assertEquals(15, map[15])
        assertEquals(20, map[20])
        assertEquals(null, map[1])

        assertEquals(fastArrayListOf(5, 10, 15, 20), map.keysToList())

        assertEquals(null, map.nearestLow(4))
        assertEquals(5, map.nearestLow(5))
        assertEquals(5, map.nearestLow(6))
        assertEquals(5, map.nearestLow(9))
        assertEquals(10, map.nearestLow(11))
        assertEquals(10, map.nearestLow(14))
        assertEquals(15, map.nearestLow(15))
        assertEquals(15, map.nearestLow(16))
        assertEquals(15, map.nearestLow(19))
        assertEquals(20, map.nearestLow(20))
        assertEquals(20, map.nearestLow(21))
        assertEquals(20, map.nearestLow(30))

        assertEquals(5, map.nearestHigh(4))
        assertEquals(5, map.nearestHigh(5))
        assertEquals(10, map.nearestHigh(6))
        assertEquals(10, map.nearestHigh(9))
        assertEquals(10, map.nearestHigh(10))
        assertEquals(15, map.nearestHigh(11))
        assertEquals(15, map.nearestHigh(14))
        assertEquals(15, map.nearestHigh(15))
        assertEquals(20, map.nearestHigh(16))
        assertEquals(20, map.nearestHigh(19))
        assertEquals(20, map.nearestHigh(20))
        assertEquals(null, map.nearestHigh(21))
        assertEquals(null, map.nearestHigh(30))

        map.remove(15)

        run {
            assertEquals(null, map.nearestLow(4))
            assertEquals(5, map.nearestLow(5))
            assertEquals(5, map.nearestLow(6))
            assertEquals(5, map.nearestLow(9))
            assertEquals(10, map.nearestLow(11))
            assertEquals(10, map.nearestLow(14))
            assertEquals(20, map.nearestLow(20))
            assertEquals(20, map.nearestLow(21))
            assertEquals(20, map.nearestLow(30))

            assertEquals(5, map.nearestHigh(4))
            assertEquals(5, map.nearestHigh(5))
            assertEquals(10, map.nearestHigh(6))
            assertEquals(10, map.nearestHigh(9))
            assertEquals(10, map.nearestHigh(10))

            assertEquals(20, map.nearestHigh(16))
            assertEquals(20, map.nearestHigh(19))
            assertEquals(20, map.nearestHigh(20))
            assertEquals(null, map.nearestHigh(21))
            assertEquals(null, map.nearestHigh(30))
        }
    }
}
