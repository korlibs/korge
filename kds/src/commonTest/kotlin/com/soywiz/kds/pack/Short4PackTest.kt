package com.soywiz.kds.pack

import kotlin.test.*

class Short4PackTest {
    @Test
    fun test() {
        val pack1 = Short4Pack(1, 11, 33, 77)
        val pack2 = Short4Pack(-77, -33, -11, -17)
        assertEquals(122, pack1.x + pack1.y + pack1.z + pack1.w)
        assertEquals(-138, pack2.x + pack2.y + pack2.z + pack2.w)
        assertEquals(listOf(1, 11, 33, 77), listOf(pack1.x.toInt(), pack1.y.toInt(), pack1.z.toInt(), pack1.w.toInt()))
        assertEquals("-999", (-999L).toString()) // Ensure we have not modified the -999 constant
    }

    @Test
    fun testCompareValue() {
        assertTrue("zeroEquals") { Short4Pack(0, 0, 0, 0) == Short4Pack(0, 0, 0, 0) }
    }

    @Test
    fun testCompareObject() {
        assertEquals(Short4Pack(0, 0, 0, 0), Short4Pack(0, 0, 0, 0))
    }

    @Test
    fun performanceTest() {
        /*
        var sum = 0f
        for (n in 0 until 100000000) {
            val pack1 = ShortPack(100 * n, 200, 300, 50)
            val pack2 = ShortPack(-33, -77 * n, -1777, -11)
            sum += pack1.x + pack1.y
            sum += pack2.x + pack2.y
        }
        println(sum)
        */
    }
}
