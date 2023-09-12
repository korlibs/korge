package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class CaseInsensitiveStringMapTest {
    @Test
    fun test() {
        val map = mapOf("hELLo" to 1, "World" to 2).toCaseInsensitiveMap()
        assertEquals(2, map.size)
        assertEquals(1, map["hello"])
        assertEquals(2, map["world"])
        assertEquals(1, map["HELLo"])
        assertEquals(2, map["World"])
        assertEquals(listOf("World", "hELLo"), map.keys.sorted())
    }

    @Test
    fun test2() {
        val map = CaseInsensitiveStringMap("hELLo" to 1, "World" to 2)
        assertEquals(2, map.size)
        assertEquals(1, map["hello"])
        assertEquals(2, map["world"])
        assertEquals(1, map["HELLo"])
        assertEquals(2, map["World"])
        assertEquals(listOf("World", "hELLo"), map.keys.sorted())
    }

    @Test
    fun test3() {
        val map = CaseInsensitiveStringMap("hELLo" to 1)
        map["HELLO"] = 2
        assertEquals(2, map["hello"])
        assertEquals(listOf("HELLO"), map.keys.toList())

        val map2 = CaseInsensitiveStringMap(mapOf("hELLo" to 1))
        assertEquals(listOf("hELLo"), map2.keys.toList())
    }
}
