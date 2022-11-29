package com.soywiz.kmem

import kotlin.test.*

class NBufferTest {
    fun NBufferInt32.str(): String = (0 until size).map { this[it] }.joinToString(",")
    fun NBufferInt32.strPos(): String = "[$size,${buffer.byteOffset},${buffer.sizeInBytes}]"

    @Test
    fun test() {
        val data = NBufferInt32(8)
        val data2 = data.sliceWithSize(1, 2)
        val data3 = data.sliceWithSize(1, 7).sliceWithSize(1, 6).sliceWithSize(1, 5).sliceWithSize(1, 4).sliceWithSize(1, 3)

        assertEquals(
            "[8,0,32], [2,4,8], [3,20,12]",
            listOf(data, data2, data3).joinToString(", ") { it.strPos() }
        )

        for (n in 0 until 8) data[n] = n * 10 + n

        assertEquals(
            """
                0,11,22,33,44,55,66,77
                11,22
                55,66,77
            """.trimIndent(),
            """
                ${data.str()}
                ${data2.str()}
                ${data3.str()}
            """.trimIndent()
        )

        data2.setArray(0, intArrayOf(-11, -22))
        data3.setArray(1, intArrayOf(-66))
        assertEquals("0,-11,-22,33,44,55,-66,77", data.str())
    }
}
