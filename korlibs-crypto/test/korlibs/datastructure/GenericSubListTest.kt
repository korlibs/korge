package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class GenericSubListTest {
    private val list = listOf(1, 2, 3, 4, 3)

    @Test
    fun test() {
        testSubList(list.subList(2, 5))
    }

    @Test
    fun test2() {
        testSubList(GenericSubList(list, 2, 5))
    }

    private fun testSubList(sub: List<Int>) {
        assertEquals(3, sub.size)
        assertEquals("[3, 4, 3]", sub.toString())
        assertEquals(-1, sub.indexOf(2))
        assertEquals(0, sub.indexOf(3))
        assertEquals(1, sub.indexOf(4))
        assertEquals(2, sub.lastIndexOf(3))
        assertEquals(false, sub.contains(2))
        assertEquals(true, sub.contains(3))
        assertEquals(false, sub.containsAll(listOf(2, 3)))
        assertEquals(true, sub.containsAll(listOf(3, 4)))
        assertEquals(false, sub.isEmpty())
        assertEquals(true, sub.isNotEmpty())
        assertEquals("[4, 3]", sub.subList(1, 3).toString())
        assertEquals("[3, 4, 3]", sub.listIterator().asSequence().toList().toString())
        assertEquals("[4, 3]", sub.listIterator(1).asSequence().toList().toString())
    }
}
