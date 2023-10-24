package korlibs.time

import kotlin.test.*

class TimedCacheTest {
    @Test
    fun test() {
        var now = DateTime.fromUnixMillis(1000L)
        val provider = TimeProvider { now }
        var value = 0
        val cache = TimedCache<Int>(1.seconds, provider) { value++ }
        assertEquals(0, cache.value)
        assertEquals(0, cache.value)
        assertEquals(0, cache.value)
        now = DateTime.fromUnixMillis(2000L)
        assertEquals(1, cache.value)
        assertEquals(1, cache.value)
        cache.value = 100
        assertEquals(100, cache.value)
    }

    @Test
    fun testInt() {
        var now = DateTime.fromUnixMillis(1000L)
        val provider = TimeProvider { now }
        var value = 0
        val cache = IntTimedCache(1.seconds, provider) { value++ }
        assertEquals(0, cache.value)
        assertEquals(0, cache.value)
        assertEquals(0, cache.value)
        now = DateTime.fromUnixMillis(2000L)
        assertEquals(1, cache.value)
        assertEquals(1, cache.value)
        cache.value = 100
        assertEquals(100, cache.value)
    }
}
