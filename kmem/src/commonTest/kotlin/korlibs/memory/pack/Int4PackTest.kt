package korlibs.memory.pack

import kotlin.test.*

class Int4PackTest {
    @Test
    fun test() {
        val pack = int4PackOf(1, 2, 3, 4)
        assertEquals(1, pack.i0)
        assertEquals(2, pack.i1)
        assertEquals(3, pack.i2)
        assertEquals(4, pack.i3)

        assertEquals(int4PackOf(1, 2, 3, 4), int4PackOf(1, 2, 3, 4))
    }
}