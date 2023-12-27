package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

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

        assertEquals(null, map.nearestLowExcludingExact(4))
        assertEquals(null, map.nearestLowExcludingExact(5))
        assertEquals(5, map.nearestLowExcludingExact(6))
        assertEquals(5, map.nearestLowExcludingExact(9))
        assertEquals(10, map.nearestLowExcludingExact(11))
        assertEquals(10, map.nearestLowExcludingExact(14))
        assertEquals(10, map.nearestLowExcludingExact(15))
        assertEquals(15, map.nearestLowExcludingExact(16))
        assertEquals(15, map.nearestLowExcludingExact(19))
        assertEquals(15, map.nearestLowExcludingExact(20))
        assertEquals(20, map.nearestLowExcludingExact(21))
        assertEquals(20, map.nearestLowExcludingExact(30))

        assertEquals(5, map.nearestHighExcludingExact(4))
        assertEquals(10, map.nearestHighExcludingExact(5))
        assertEquals(10, map.nearestHighExcludingExact(6))
        assertEquals(10, map.nearestHighExcludingExact(9))
        assertEquals(15, map.nearestHighExcludingExact(10))
        assertEquals(15, map.nearestHighExcludingExact(11))
        assertEquals(15, map.nearestHighExcludingExact(14))
        assertEquals(20, map.nearestHighExcludingExact(15))
        assertEquals(20, map.nearestHighExcludingExact(16))
        assertEquals(20, map.nearestHighExcludingExact(19))
        assertEquals(null, map.nearestHighExcludingExact(20))
        assertEquals(null, map.nearestHighExcludingExact(21))
        assertEquals(null, map.nearestHighExcludingExact(30))

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

    @Test
    fun testSortedMapOf() {
        val sortedValues = sortedMapOf(2 to "two", 3 to "three", 1 to "one").values.toList()
        assertEquals(listOf("one", "two", "three"), sortedValues)
    }
}
