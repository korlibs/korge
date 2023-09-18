package korlibs.memory

import korlibs.math.*
import kotlin.test.Test
import kotlin.test.assertEquals

class IntDivExtTest {
    @Test
    fun test() {
        assertEquals(10, 100.divFloor(10))
        assertEquals(10, 100.divCeil(10))

        assertEquals(10, 101.divFloor(10))
        assertEquals(11, 101.divCeil(10))
    }
}
