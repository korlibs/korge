package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayListReverseTest {
    @Test
    fun test() {
        assertEquals(listOf(), intArrayListOf().apply { reverse() }.toList())
        assertEquals(listOf(1), intArrayListOf(1).apply { reverse() }.toList())
        assertEquals(listOf(2, 1), intArrayListOf(1, 2).apply { reverse() }.toList())
        assertEquals(listOf(3, 2, 1), intArrayListOf(1, 2, 3).apply { reverse() }.toList())
        assertEquals(listOf(4, 3, 2, 1), intArrayListOf(1, 2, 3, 4).apply { reverse() }.toList())
    }

    @Test
    fun test2() {
        assertEquals(listOf(1, 3, 2, 4), intArrayListOf(1, 2, 3, 4).apply { reverse(1, 3) }.toList())
    }
}
