package com.soywiz.kbignum

import kotlin.test.*

class BigIntRangeTest {
    @Test
    fun testContains() {
        assertEquals("0..10", (0.bi .. 10.bi).toString())
        assertEquals(0.bi, (0.bi .. 10.bi).start)
        assertEquals(10.bi, (0.bi .. 10.bi).endInclusive)
        assertEquals((0.bi .. 10.bi), (0.bi .. 10.bi))
        assertEquals((0.bi .. 10.bi).hashCode(), (0.bi .. 10.bi).hashCode())
        assertTrue { 0.bi in (0.bi .. 10.bi) }
        assertTrue { 5.bi in (0.bi .. 10.bi) }
        assertTrue { 10.bi in (0.bi .. 10.bi) }
        assertFalse { (-1).bi in (0.bi .. 10.bi) }
        assertFalse { 11.bi in (0.bi .. 10.bi) }
    }
}
