package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class CacheMapTest {
    @Test
    fun test() {
        val freeLog = arrayListOf<String>()
        val cache = object : CacheMap<String, Int>(maxSize = 2) {
            override fun freed(key: String, value: Int) {
                super.freed(key, value)
                freeLog += "$key:$value"
            }
        }
        cache["a"] = 1
        cache["b"] = 2
        cache["c"] = 3
        assertEquals("{b=2, c=3}", cache.toString())
        assertEquals("a:1", freeLog.joinToString(", "))

        assertEquals(false, "a" in cache)
        assertEquals(true, "b" in cache)
        assertEquals(true, "c" in cache)

        assertEquals(2, cache.getOrPut("b") { 20 })
        assertEquals(10, cache.getOrPut("a") { 10 })
        assertEquals(3, cache.getOrPut("d") { 3 })

        cache.putAll(mapOf("aa" to 1, "bb" to 2, "cc" to 3))

        assertEquals("{bb=2, cc=3}", cache.toString())
    }
}
