package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class MapListTest {
    @Test
    fun test() {
        val map = linkedHashMapListOf("a" to 10, "a" to 20, "b" to 30)

        assertEquals(10, map.getFirst("a"))
        assertEquals(20, map.getLast("a"))

        assertEquals(30, map.getFirst("b"))
        assertEquals(30, map.getLast("b"))

        assertEquals(null, map.getLast("c"))

        assertEquals(listOf("a" to 10, "a" to 20, "b" to 30), map.flatten())
    }
}
