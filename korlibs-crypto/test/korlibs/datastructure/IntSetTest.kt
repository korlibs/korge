package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class IntSetTest {
    @Test
    fun testSmoke() {
        val set = intSetOf(1, 2, 4)
        assertEquals(3, set.size)

        assertEquals(false, 0 in set)
        assertEquals(true, 1 in set)
        assertEquals(true, 2 in set)
        assertEquals(false, 3 in set)
        assertEquals(true, 4 in set)
        assertEquals(false, 5 in set)

        set.remove(2)
        assertEquals(2, set.size)
        assertEquals(true, 1 in set)
        assertEquals(false, 2 in set)
        assertEquals(true, 4 in set)
    }

    @Test
    fun testToString() {
        assertEquals("[]", intSetOf().toString())
        assertEquals("[1, 2, 4]", intSetOf(1, 2, 4).toString())
        assertEquals("[1, 2, 4]", intSetOf(4, 2, 1).toString())
    }

    @Test
    fun test2() {
        assertEquals(listOf(1, 2, 3, 4), intSetOf(1, 2, 3, 4).toList().sorted())
    }
}
