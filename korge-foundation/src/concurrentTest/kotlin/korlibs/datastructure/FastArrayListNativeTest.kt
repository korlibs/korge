package korlibs.datastructure

import kotlin.test.*

class FastArrayListNativeTest {
    @Test
    fun testContainsAndIndexOf() {
        val a = fastArrayListOf<String?>("a", "b")
        assertEquals(listOf("a", "b"), a.array.toList())
        assertEquals(listOf(true, true, false), listOf(a.contains("a"), a.contains("b"), a.contains(null)))
        assertEquals(listOf(0, 1, -1), listOf(a.indexOf("a"), a.indexOf("b"), a.indexOf(null)))
        assertEquals(listOf(0, 1, -1), listOf(a.lastIndexOf("a"), a.lastIndexOf("b"), a.lastIndexOf(null)))
        a.removeAt(1)
        assertEquals(listOf("a", null), a.array.toList())
        assertEquals(listOf(true, false, false), listOf(a.contains("a"), a.contains("b"), a.contains(null)))
        assertEquals(listOf(0, -1, -1), listOf(a.indexOf("a"), a.indexOf("b"), a.indexOf(null)))
        assertEquals(listOf(0, -1, -1), listOf(a.lastIndexOf("a"), a.lastIndexOf("b"), a.lastIndexOf(null)))
        a.add(null)
        assertEquals(listOf("a", null), a.array.toList())
        assertEquals(listOf(true, false, true), listOf(a.contains("a"), a.contains("b"), a.contains(null)))
        assertEquals(listOf(0, -1, 1), listOf(a.indexOf("a"), a.indexOf("b"), a.indexOf(null)))
        assertEquals(listOf(0, -1, 1), listOf(a.lastIndexOf("a"), a.lastIndexOf("b"), a.lastIndexOf(null)))

        assertEquals(2, fastArrayListOf("a", "b", "a").lastIndexOf("a"))
        assertEquals(1, fastArrayListOf("a", "b", "a").lastIndexOf("b"))
        assertEquals(-1, fastArrayListOf("a", "b", "a").lastIndexOf("c"))
    }
}
