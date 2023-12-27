package korlibs.datastructure.extensions

import korlibs.datastructure.IntArray2
import korlibs.datastructure.doubleArrayListOf
import korlibs.datastructure.floatArrayListOf
import korlibs.datastructure.getCyclic
import korlibs.datastructure.intArrayListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class GetCyclicTest {
    @Test
    fun list() {
        val list = listOf(5, 10, 20, 30)
        val cyclicList = (0 until 4).map { list }.flatten()
        for (n in cyclicList.indices) {
            assertEquals(cyclicList[n], list.getCyclic(n))
        }

        val cyclicListNeg = (0 until 4).map { list.reversed() }.flatten()
        for (n in cyclicListNeg.indices) {
            assertEquals(cyclicListNeg[n], list.getCyclic(-n - 1))
        }

        assertEquals(5, list.getCyclic(0))
        assertEquals(10, list.getCyclic(1))
        assertEquals(20, list.getCyclic(2))
        assertEquals(30, list.getCyclic(3))

        assertEquals(30, list.getCyclic(-1))
        assertEquals(20, list.getCyclic(-2))
        assertEquals(10, list.getCyclic(-3))
        assertEquals(5, list.getCyclic(-4))
        assertEquals(30, list.getCyclic(-5))
    }

    @Test
    fun array() {
        assertEquals("a", arrayOf("a", "b").getCyclic(2))
        assertEquals("b", arrayOf("a", "b").getCyclic(-1))
    }

    @Test
    fun typedList() {
        assertEquals(10, intArrayListOf(10, 20).getCyclic(2))
        assertEquals(20, intArrayListOf(10, 20).getCyclic(-1))

        assertEquals(10f, floatArrayListOf(10f, 20f).getCyclic(2))
        assertEquals(20f, floatArrayListOf(10f, 20f).getCyclic(-1))

        assertEquals(10.0, doubleArrayListOf(10.0, 20.0).getCyclic(2))
        assertEquals(20.0, doubleArrayListOf(10.0, 20.0).getCyclic(-1))
    }

    @Test
    fun cyclicArray2() {
        val array = IntArray2(2, 2) { it }
        assertEquals(0, array.getCyclic(0, 0))
        assertEquals(1, array.getCyclic(1, 0))
        assertEquals(0, array.getCyclic(2, 0))
        assertEquals(1, array.getCyclic(3, 0))

        assertEquals(2, array.getCyclic(0, 1))
        assertEquals(3, array.getCyclic(1, 1))
        assertEquals(2, array.getCyclic(2, 1))
        assertEquals(3, array.getCyclic(3, 1))

        assertEquals(0, array.getCyclic(0, 2))
        assertEquals(1, array.getCyclic(1, 2))
        assertEquals(0, array.getCyclic(2, 2))
        assertEquals(1, array.getCyclic(3, 2))

        assertEquals(3, array.getCyclic(-1, -1))
        assertEquals(0, array.getCyclic(-2, -2))
        assertEquals(3, array.getCyclic(-3, -3))
    }
}
