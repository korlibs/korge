package korlibs.memory.numeric

import korlibs.math.isAlignedTo
import korlibs.math.nextAlignedTo
import korlibs.math.prevAlignedTo
import kotlin.test.Test
import kotlin.test.assertEquals

class AlignmentTest {
    @Test
    fun isAligned() {
        assertEquals(true, (-1).isAlignedTo(0))
        assertEquals(true, 0.isAlignedTo(0))
        assertEquals(true, 1.isAlignedTo(0))
        assertEquals(true, 2.isAlignedTo(0))

        assertEquals(true, 0.isAlignedTo(1))
        assertEquals(true, 1.isAlignedTo(1))
        assertEquals(true, 2.isAlignedTo(1))

        assertEquals(true, 0.isAlignedTo(2))
        assertEquals(false, 1.isAlignedTo(2))
        assertEquals(true, 2.isAlignedTo(2))
        assertEquals(false, 3.isAlignedTo(2))
        assertEquals(true, 4.isAlignedTo(2))
    }

    @Test
    fun nextAlignedTo() {
        assertEquals(0, 0.nextAlignedTo(0))
        assertEquals(1, 1.nextAlignedTo(0))
        assertEquals(2, 2.nextAlignedTo(0))

        assertEquals(0, 0.nextAlignedTo(1))
        assertEquals(1, 1.nextAlignedTo(1))
        assertEquals(2, 2.nextAlignedTo(1))

        assertEquals(0, 0.nextAlignedTo(2))
        assertEquals(2, 1.nextAlignedTo(2))
        assertEquals(2, 2.nextAlignedTo(2))
        assertEquals(4, 3.nextAlignedTo(2))
    }

    @Test
    fun prevAlignedTo() {
        assertEquals(0, 0.prevAlignedTo(0))
        assertEquals(1, 1.prevAlignedTo(0))
        assertEquals(2, 2.prevAlignedTo(0))

        assertEquals(0, 0.prevAlignedTo(1))
        assertEquals(1, 1.prevAlignedTo(1))
        assertEquals(2, 2.prevAlignedTo(1))

        assertEquals(0, 0.prevAlignedTo(2))
        assertEquals(0, 1.prevAlignedTo(2))
        assertEquals(2, 2.prevAlignedTo(2))
        assertEquals(2, 3.prevAlignedTo(2))
    }

    @Test
    fun long() {
        assertEquals(false, 77L.isAlignedTo(10L))
        assertEquals(70L, 77L.prevAlignedTo(10L))
        assertEquals(80L, 77L.nextAlignedTo(10L))

        assertEquals(true, 80L.isAlignedTo(10L))
        assertEquals(80L, 80L.prevAlignedTo(10L))
        assertEquals(80L, 80L.nextAlignedTo(10L))
    }
}
