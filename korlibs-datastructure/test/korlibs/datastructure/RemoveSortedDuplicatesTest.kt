package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class RemoveSortedDuplicatesTest {
    @Test
    fun testRemoveSortedDuplicates() {
        assertEquals(arrayListOf<Int>(), arrayListOf<Int>().removeSortedDuplicates())
        assertEquals(arrayListOf(1), arrayListOf(1).removeSortedDuplicates())
        assertEquals(arrayListOf(1, 2), arrayListOf(1, 2).removeSortedDuplicates())
        assertEquals(arrayListOf(1, 2, 3), arrayListOf(1, 2, 3).removeSortedDuplicates())
        assertEquals(arrayListOf(1, 2, 3, 4), arrayListOf(1, 2, 3, 4).removeSortedDuplicates())
        assertEquals(arrayListOf(1, 2), arrayListOf(1, 1, 2, 2).removeSortedDuplicates())
        assertEquals(arrayListOf(1, 2), arrayListOf(1, 2, 2).removeSortedDuplicates())
        assertEquals(arrayListOf(1, 2), arrayListOf(1, 1, 2).removeSortedDuplicates())
        assertEquals(arrayListOf(1, 2, 3, 4, 5, 6, 7), arrayListOf(1, 1, 1, 1, 2, 3, 4, 5, 6, 7, 7, 7).removeSortedDuplicates())
    }

    @Test
    fun testWithoutSortedDuplicates() {
        assertEquals(listOf(1, 2), listOf(1, 1, 2, 2).withoutSortedDuplicates())
    }
}
