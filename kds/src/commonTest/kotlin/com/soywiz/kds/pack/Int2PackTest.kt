package com.soywiz.kds.pack

import kotlin.test.*

class Int2PackTest {
    @Test
    fun test() {
        val pack1 = Int2Pack(100, 200)
        val pack2 = Int2Pack(-33, -77)
        assertEquals(300, pack1.x + pack1.y)
        assertEquals(-110, pack2.x + pack2.y)
        assertEquals(listOf(100, 200), listOf(pack1.x, pack1.y))
        assertEquals("-999", (-999L).toString()) // Ensure we have not modified the -999 constant
    }

    @Test
    fun testCompareValue() {
        assertTrue("zeroEquals") { Int2Pack(0, 0) == Int2Pack(0, 0) }
    }

    @Test
    fun testCompareObject() {
        assertEquals(Int2Pack(0, 0), Int2Pack(0, 0))
    }

    @Test
    fun performanceTest() {
        /*
        var sum = 0f
        for (n in 0 until 100000000) {
            val pack1 = IntPack(100 * n, 200)
            val pack2 = IntPack(-33, -77 * n)
            sum += pack1.x + pack1.y
            sum += pack2.x + pack2.y
        }
        println(sum)
        */
    }
}
