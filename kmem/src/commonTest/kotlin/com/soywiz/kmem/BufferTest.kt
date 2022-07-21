package com.soywiz.kmem

import kotlin.test.Test
import kotlin.test.assertEquals

class BufferTest {
    @Test
    fun test() {
        val ba = Int8BufferAlloc(3).apply {
            this[0] = -1
            this[1] = -2
            this[2] = -3
        }
        assertEquals(-1, ba[0])
        assertEquals(-2, ba[1])
        assertEquals(-3, ba[2])
    }

    @Test
    fun testSlice() {
        val data = MemBufferAlloc(16 * 4)
        val sliceAll = data.sliceFloat32Buffer(0, 16)
        val slice1 = data.sliceFloat32Buffer(0, 8)
        val slice2 = data.sliceFloat32Buffer(8, 8)
        for (n in 0 until 16) sliceAll[n] = n.toFloat()
        assertEquals(FloatArray(16) { it.toFloat() }.toList(), (0 until sliceAll.size).map { sliceAll[it] })
        assertEquals(FloatArray(8) { it.toFloat() }.toList(), (0 until slice1.size).map { slice1[it] })
        assertEquals(FloatArray(8) { (8 + it).toFloat() }.toList(), (0 until slice2.size).map { slice2[it] })
    }
}
