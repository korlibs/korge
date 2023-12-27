package korlibs.datastructure.extensions

import korlibs.datastructure.countMap
import korlibs.datastructure.flip
import korlibs.datastructure.incr
import korlibs.datastructure.linkedHashMapOf
import korlibs.datastructure.toLinkedMap
import kotlin.test.Test
import kotlin.test.assertEquals

class MapExtTest {
    @Test
    fun linked() {
        assertEquals("{a=1, b=2}", linkedHashMapOf("a" to 1, "b" to 2).toString())
        assertEquals("{a=1, b=2}", listOf("a" to 1, "b" to 2).toLinkedMap().toString())
    }

    @Test
    fun flip() {
        val map = mapOf("a" to "A", "b" to "B")
        assertEquals("{a=A, b=B}", map.toString())
        assertEquals("{A=a, B=b}", map.flip().toString())
    }

    @Test
    fun count() {
        val list = listOf("a", "a", "b", "a", "c", "b")
        assertEquals(mapOf("a" to 3, "b" to 2, "c" to 1), list.countMap())
    }

    @Test
    fun incr() {
        val map = hashMapOf<Boolean, Int>()
        map.incr(false, 100)
        map.incr(true, 1000)
        map.incr(true, 200)
        assertEquals("{false=100, true=1200}", map.toString())
    }
}
