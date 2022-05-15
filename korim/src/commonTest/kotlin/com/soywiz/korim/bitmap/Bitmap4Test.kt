package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import kotlin.test.Test
import kotlin.test.assertEquals

class Bitmap4Test {
    @Test
    fun test() {
        val bmp = Bitmap4(4, 4)
        bmp.palette[0] = Colors.RED
        bmp.palette[1] = Colors.GREEN
        bmp.palette[2] = Colors.BLUE
        bmp[0, 0] = 0
        bmp[1, 0] = 1
        bmp[2, 0] = 2
        assertEquals(0, bmp[0, 0])
        assertEquals(1, bmp[1, 0])
        assertEquals(2, bmp[2, 0])

        assertEquals(Colors.RED, bmp.getRgba(0, 0))
        assertEquals(Colors.GREEN, bmp.getRgba(1, 0))
        assertEquals(Colors.BLUE, bmp.getRgba(2, 0))

        val bmp32 = bmp.toBMP32()

        assertEquals(Colors.RED, bmp32.getRgba(0, 0))
        assertEquals(Colors.GREEN, bmp32.getRgba(1, 0))
        assertEquals(Colors.BLUE, bmp32.getRgba(2, 0))
    }
}
