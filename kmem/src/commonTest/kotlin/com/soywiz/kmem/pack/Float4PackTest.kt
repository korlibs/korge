package com.soywiz.kmem.pack

import kotlin.test.*

class Float4PackTest {
    @Test
    fun test() {
        val pack = float4PackOf(1f, 2f, 3f, 4f)
        assertEquals(1f, pack.x)
        assertEquals(2f, pack.y)
        assertEquals(3f, pack.z)
        assertEquals(4f, pack.w)
    }
}
