package com.soywiz.kmem.pack

import com.soywiz.kmem.*
import kotlin.test.*

class Half4PackTest {
    @Test
    fun test() {
        val pack = Half4Pack(1f.toHalf(), 2f.toHalf(), 3f.toHalf(), 4f.toHalf())
        assertEquals(listOf(1f, 2f, 3f, 4f), listOf(pack.x.toFloat(), pack.y.toFloat(), pack.z.toFloat(), pack.w.toFloat()))
    }
}
