package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korio.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeImageTest {
    @Test
    fun test() = suspendTest {
        val bmp = NativeImage(4, 4)
        bmp.setRgba(0, 0, Colors.RED)
        assertEquals(Colors.RED, bmp.getRgba(0, 0))
        bmp.setRgba(1, 0, Colors.BLUE)
        bmp.setRgba(1, 1, Colors.GREEN)
        //bmp.setRgba(0, 1, Colors.PINK)
        bmp.context2d {
            fillStyle = Colors.PINK
            fillRect(0, 1, 1, 1)
        }
        bmp.copy(0, 0, bmp, 2, 2, 2, 2)
        assertEquals(Colors.RED, bmp.getRgba(2, 2))
        assertEquals(Colors.BLUE, bmp.getRgba(3, 2))
        assertEquals(Colors.GREEN, bmp.getRgba(3, 3))
        assertEquals(Colors.PINK, bmp.getRgba(2, 3))
        //bmp.showImageAndWait()
    }

    @Test
    fun testEmptyNativeImage() {
        val array = RgbaArray(0)
        val image = NativeImage(0, 0)
        image.writePixelsUnsafe(0, 0, 0, 0, array, 0)
        image.readPixelsUnsafe(0, 0, 0, 0, array, 0)
    }
}
