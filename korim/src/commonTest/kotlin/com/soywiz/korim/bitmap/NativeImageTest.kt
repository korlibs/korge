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

    @Test
    fun testFlipNativeImage() {
        val pixels = RgbaArray(intArrayOf(
            Colors.RED.value, Colors.GREEN.value,
            Colors.BLUE.value, Colors.PINK.value,
        ))

        val original = NativeImage(2, 2, pixels)
        val flippedX = NativeImage(2, 2, pixels).flipX()
        val flippedY = NativeImage(2, 2, pixels).flipY()

        assertEquals(
            "#ff0000,#00ff00,#0000ff,#ffc0cb",
            original.readPixelsUnsafe(0, 0, 2, 2).joinToString(",") { it.hexStringNoAlpha }
        )
        assertEquals(
            "#00ff00,#ff0000,#ffc0cb,#0000ff",
            flippedX.readPixelsUnsafe(0, 0, 2, 2).joinToString(",") { it.hexStringNoAlpha }
        )
        assertEquals(
            "#0000ff,#ffc0cb,#ff0000,#00ff00",
            flippedY.readPixelsUnsafe(0, 0, 2, 2).joinToString(",") { it.hexStringNoAlpha }
        )
    }
}
