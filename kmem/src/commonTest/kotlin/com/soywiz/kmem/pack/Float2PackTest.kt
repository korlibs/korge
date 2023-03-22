package com.soywiz.kmem.pack

import kotlin.test.*

class Float2PackTest {
    @Test
    fun test() {
        val pack1 = float2PackOf(100f, 200f)
        val pack2 = float2PackOf(-33f, -77f)
        assertEquals(300f, pack1.f0 + pack1.f1)
        assertEquals(-110f, pack2.f0 + pack2.f1)
        assertEquals(listOf(100f, 200f), listOf(pack1.f0, pack1.f1))
        assertEquals("-999", (-999L).toString()) // Ensure we have not modified the -999 constant
    }

    @Test
    fun testCompareValue() {
        assertTrue("zeroEquals") { float2PackOf(0f, 0f) == float2PackOf(0f, 0f) }
    }

    @Test
    fun testCompareObject() {
        assertEquals(float2PackOf(0f, 0f), float2PackOf(0f, 0f))
    }

    @Test
    @Ignore
    fun performanceTest() {
        var sum = 0f
        for (n in 0 until 100000000) {
            val pack1 = float2PackOf(100f * n, 200f)
            val pack2 = float2PackOf(-33f, -77f * n)
            sum += pack1.f0 + pack1.f1
            sum += pack2.f0 + pack2.f1
            //assertEquals(300f, pack1.x + pack1.y)
            //assertEquals(-110f, pack2.x + pack2.y)
        }
        println(sum)
    }
}
