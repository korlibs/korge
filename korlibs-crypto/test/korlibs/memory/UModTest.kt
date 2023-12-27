package korlibs.memory

import korlibs.math.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UModTest {
    @Test
    fun test() {
        assertEquals(0, (-2) umod 2)
        assertEquals(1, (-1) umod 2)
        assertEquals(0, 0 umod 2)
        assertEquals(1, 1 umod 2)
        assertEquals(0, 2 umod 2)
        assertEquals(1, 3 umod 2)

        assertEquals(0.0, (-2.0) umod 2.0)
        assertEquals(1.0, (-1.0) umod 2.0)
        assertEquals(0.0, 0.0 umod 2.0)
        assertEquals(1.0, 1.0 umod 2.0)
        assertEquals(0.0, 2.0 umod 2.0)
        assertEquals(1.0, 3.0 umod 2.0)
    }

    @Test
    fun testFract() {
        assertEquals(0.1, fract(0.1))
        assertEquals(0.9, fract(-0.1))
    }
}
