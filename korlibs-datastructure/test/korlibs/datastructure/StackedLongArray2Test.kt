package korlibs.datastructure

import kotlin.test.*

class StackedLongArray2Test {
    @Test
    fun test() {
        val value = StackedLongArray2(2, 2)
        assertEquals(StackedLongArray2.EMPTY, value.getFirst(0, 0))
        value.push(0, 0, 1L)
        value.push(0, 0, 2L)
        value.push(0, 0, 3L)

        assertEquals(3, value.getStackLevel(0, 0))
        assertEquals(1L, value.getFirst(0, 0))
        assertEquals(2L, value.get(0, 0, 1))
        assertEquals(3L, value.getLast(0, 0))

        value.removeLast(0, 0)
        assertEquals(1L, value.getFirst(0, 0))
        assertEquals(2L, value.getLast(0, 0))
        assertEquals(2, value.getStackLevel(0, 0))

        value.removeLast(0, 0)
        assertEquals(1L, value.getFirst(0, 0))
        assertEquals(1L, value.getLast(0, 0))
        assertEquals(1, value.getStackLevel(0, 0))

        value.removeLast(0, 0)
        assertEquals(StackedLongArray2.EMPTY, value.getFirst(0, 0))
        assertEquals(StackedLongArray2.EMPTY, value.getLast(0, 0))
        assertEquals(0, value.getStackLevel(0, 0))

        assertEquals(StackedLongArray2.EMPTY, value.getFirst(1, 0))
        assertEquals(0, value.getStackLevel(1, 0))
    }

    @Test
    fun testInitialSetLevel() {
        val s = StackedLongArray2(LongArray2(2, 2, longArrayOf(10L, 20L, 30L, 40L)))
        assertEquals(1, s.getStackLevel(0, 0))
        assertEquals(1, s.getStackLevel(1, 1))
    }
}
