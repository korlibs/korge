package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class LRUCacheTest {
    @Test
    fun testSimple() {
        val cache = LRUCache<Int, String>(6, maxMemory = 10L) { it.length }
        fun info() = "${cache.size}:${cache.computedMemory}"
        cache[0] = "hello"
        cache[1] = "test"
        assertEquals("2:9", info())
        cache[2] = "demo"
        assertEquals("2:8", info())
        cache[3] = "ok"
        assertEquals("3:10", info())
        cache[4] = "1"
        assertEquals("3:7", info())
        cache[10] = "1".repeat(20)
        assertEquals("1:20", info())

        for (n in 0 until 20) cache[n] = "n"
        assertEquals("6:6", info())
    }

    @Test
    fun testAtLeastOneDisabled() {
        val cache = LRUCache<Int, String>(10, maxMemory = 10L, atLeastOne = false) { it.length }
        fun info() = "${cache.size}:${cache.computedMemory}"
        cache[10] = "1".repeat(20)
        assertEquals("0:0", info())
    }
}
