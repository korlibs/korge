package korlibs.datastructure

import kotlin.test.*

class StackedDoubleArray2Test {
    @Test
    fun test() {
        val value = StackedDoubleArray2(2, 2)
        assertEquals(StackedDoubleArray2.EMPTY, value.getFirst(0, 0))
        value.push(0, 0, 1.0)
        value.push(0, 0, 2.0)
        value.push(0, 0, 3.0)

        assertEquals(3, value.getStackLevel(0, 0))
        assertEquals(1.0, value.getFirst(0, 0))
        assertEquals(2.0, value.get(0, 0, 1))
        assertEquals(3.0, value.getLast(0, 0))

        value.removeLast(0, 0)
        assertEquals(1.0, value.getFirst(0, 0))
        assertEquals(2.0, value.getLast(0, 0))
        assertEquals(2, value.getStackLevel(0, 0))

        value.removeLast(0, 0)
        assertEquals(1.0, value.getFirst(0, 0))
        assertEquals(1.0, value.getLast(0, 0))
        assertEquals(1, value.getStackLevel(0, 0))

        value.removeLast(0, 0)
        assertEquals(StackedDoubleArray2.EMPTY, value.getFirst(0, 0))
        assertEquals(StackedDoubleArray2.EMPTY, value.getLast(0, 0))
        assertEquals(0, value.getStackLevel(0, 0))

        assertEquals(StackedDoubleArray2.EMPTY, value.getFirst(1, 0))
        assertEquals(0, value.getStackLevel(1, 0))
    }

    @Test
    fun testInitialSetLevel() {
        val s = StackedDoubleArray2(DoubleArray2(2, 2, doubleArrayOf(10.0, 20.0, 30.0, 40.0)))
        assertEquals(1, s.getStackLevel(0, 0))
        assertEquals(1, s.getStackLevel(1, 1))
    }
}
