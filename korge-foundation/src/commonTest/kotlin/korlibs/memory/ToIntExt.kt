package korlibs.memory

import korlibs.math.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ToIntExt {
    @Test
    fun toIntFloorRoundCeil() {
        // Float
        assertEquals(10, 10.1f.toIntFloor())
        assertEquals(10, 10.1f.toIntRound())
        assertEquals(11, 10.1f.toIntCeil())
        assertEquals(10, 10.9f.toIntFloor())
        assertEquals(11, 10.9f.toIntRound())
        assertEquals(11, 10.9f.toIntCeil())

        // Double
        assertEquals(10, 10.1.toIntFloor())
        assertEquals(10, 10.1.toIntRound())
        assertEquals(11, 10.1.toIntCeil())
        assertEquals(10, 10.9.toIntFloor())
        assertEquals(11, 10.9.toIntRound())
        assertEquals(11, 10.9.toIntCeil())
    }

    @Test
    fun toIntSafe() {
        assertEquals(1, 1L.toIntSafe())
        assertEquals(-1, (-1L).toIntSafe())
        assertEquals(Int.MIN_VALUE, Int.MIN_VALUE.toLong().toIntSafe())
        assertEquals(Int.MAX_VALUE, Int.MAX_VALUE.toLong().toIntSafe())
        assertFailsWith<IllegalArgumentException> { (Int.MIN_VALUE.toLong() - 1).toIntSafe() }
        assertFailsWith<IllegalArgumentException> { (Int.MAX_VALUE.toLong() + 1).toIntSafe() }
        assertFailsWith<IllegalArgumentException> { Long.MIN_VALUE.toIntSafe() }
        assertFailsWith<IllegalArgumentException> { Long.MAX_VALUE.toIntSafe() }
    }
}
