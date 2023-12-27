package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class WeakMapTest {
    @Test
    fun test() {
        class Demo

        val map = WeakMap<Demo, String>()
        val demo1 = Demo()
        val demo2 = Demo()
        map[demo1] = "hello"

        assertEquals("hello", map[demo1])
        assertEquals(true, demo1 in map)

        assertEquals(null, map[demo2])
        assertEquals(false, demo2 in map)
    }
}
