package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayListSortTest {
    @Test
    fun test() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), intArrayListOf(5, 3, 6, 4, 1, 7, 2).apply { sort() }.toList())
        assertEquals(listOf(7, 6, 5, 4, 3, 2, 1), intArrayListOf(5, 3, 6, 4, 1, 7, 2).apply { sort(reversed = true) }.toList())
    }
}
