package com.soywiz.kmem.pack

import kotlin.test.*

class Short4PackTest {
    @Test
    fun test() {
        val pack1 = short4PackOf(1, 11, 33, 77)
        val pack2 = short4PackOf(-77, -33, -11, -17)
        assertEquals(122, pack1.s0 + pack1.s1 + pack1.s2 + pack1.s3)
        assertEquals(-138, pack2.s0 + pack2.s1 + pack2.s2 + pack2.s3)
        assertEquals(listOf(1, 11, 33, 77), listOf(pack1.s0.toInt(), pack1.s1.toInt(), pack1.s2.toInt(), pack1.s3.toInt()))
        assertEquals("-999", (-999L).toString()) // Ensure we have not modified the -999 constant
    }

    @Test
    fun testCompareValue() {
        assertTrue("zeroEquals") { short4PackOf(0, 0, 0, 0) == short4PackOf(0, 0, 0, 0) }
    }

    @Test
    fun testCompareObject() {
        assertEquals(short4PackOf(0, 0, 0, 0), short4PackOf(0, 0, 0, 0))
    }

    @Test
    @Ignore
    fun performanceTest() {
        var sum = 0f
        for (n in 0 until 100000000) {
            val pack1 = short4PackOf((100 * n).toShort(), 200, 300, 50)
            val pack2 = short4PackOf(-33, (-77 * n).toShort(), -1777, -11)
            sum += pack1.s0 + pack1.s1 + pack1.s2 + pack1.s3
            sum += pack2.s0 + pack2.s1 + pack2.s2 + pack2.s3
        }
        println(sum)
    }
}
