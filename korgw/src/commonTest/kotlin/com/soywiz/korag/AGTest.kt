package com.soywiz.korag

import kotlin.test.*

class AGTest {
    @Test
    fun testCombineScissor() {
        assertEquals(AGScissor.NIL, AGScissor.combine(AGScissor.NIL, AGScissor.NIL))
        assertEquals(AGScissor(0, 0, 100, 100), AGScissor.combine(AGScissor(0, 0, 100, 100), AGScissor.NIL))
        assertEquals(AGScissor(50, 50, 50, 50), AGScissor.combine(AGScissor(0, 0, 100, 100), AGScissor(50, 50, 100, 100)))
        assertEquals(AGScissor(50, 50, 100, 100), AGScissor.combine(AGScissor.NIL, AGScissor(50, 50, 100, 100)))
        assertEquals(AGScissor(0, 0, 0, 0), AGScissor.combine(AGScissor(2000, 2000, 100, 100), AGScissor(50, 50, 100, 100)))
    }
}
