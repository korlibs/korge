package com.soywiz.kds.pack

import kotlin.test.*

class Float2PackTest {
    @Test
    fun test() {
        val pack1 = Float2Pack(100f, 200f)
        val pack2 = Float2Pack(-33f, -77f)
        assertEquals(300f, pack1.x + pack1.y)
        assertEquals(-110f, pack2.x + pack2.y)
        assertEquals(listOf(100f, 200f), listOf(pack1.x, pack1.y))
        assertEquals("-999", (-999L).toString()) // Ensure we have not modified the -999 constant
    }

    @Test
    fun performanceTest() {
        /*
        var sum = 0f
        for (n in 0 until 100000000) {
            val pack1 = FloatPack(100f * n, 200f)
            val pack2 = FloatPack(-33f, -77f * n)
            sum += pack1.x + pack1.y
            sum += pack2.x + pack2.y
            //assertEquals(300f, pack1.x + pack1.y)
            //assertEquals(-110f, pack2.x + pack2.y)
        }
        println(sum)
        */
    }
}
