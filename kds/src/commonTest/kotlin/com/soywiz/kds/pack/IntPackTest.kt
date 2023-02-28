package com.soywiz.kds.pack

import kotlin.test.*

class IntPackTest {
    @Test
    fun test() {
        val pack1 = IntPack(100, 200)
        val pack2 = IntPack(-33, -77)
        assertEquals(300, pack1.x + pack1.y)
        assertEquals(-110, pack2.x + pack2.y)
        assertEquals("-999", (-999L).toString()) // Ensure we have not modified the -999 constant
    }

    @Test
    fun testCompareValue() {
        assertTrue("zeroEquals") { IntPack(0, 0) == IntPack(0, 0) }
    }

    @Test
    fun testCompareObject() {
        assertEquals(IntPack(0, 0), IntPack(0, 0))
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
