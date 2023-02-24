package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korim.vector.buildShape
import com.soywiz.korim.vector.render
import com.soywiz.korio.async.suspendTest
import com.soywiz.korma.geom.MSize
import com.soywiz.korma.geom.vector.Winding
import com.soywiz.korma.geom.vector.circle
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeImageTest {
    @Test
    fun test() = suspendTest {
        val bmp = NativeImage(4, 4)
        bmp.setRgbaRaw(0, 0, Colors.RED)
        assertEquals(Colors.RED, bmp.getRgbaRaw(0, 0))
        bmp.setRgbaRaw(1, 0, Colors.BLUE)
        bmp.setRgbaRaw(1, 1, Colors.GREEN)
        //bmp.setRgba(0, 1, Colors.PINK)
        bmp.context2d {
            fillStyle = Colors.PINK
            fillRect(0, 1, 1, 1)
        }
        bmp.copy(0, 0, bmp, 2, 2, 2, 2)
        assertEquals(Colors.RED, bmp.getRgbaRaw(2, 2))
        assertEquals(Colors.BLUE, bmp.getRgbaRaw(3, 2))
        assertEquals(Colors.GREEN, bmp.getRgbaRaw(3, 3))
        assertEquals(Colors.PINK, bmp.getRgbaRaw(2, 3))
        //bmp.showImageAndWait()
    }

    @Test
    fun testEmptyNativeImage() {
        val array = IntArray(0)
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
            original.readPixelsUnsafe(0, 0, 2, 2).joinToString(",") { RGBA(it).hexStringNoAlpha }
        )
        assertEquals(
            "#00ff00,#ff0000,#ffc0cb,#0000ff",
            flippedX.readPixelsUnsafe(0, 0, 2, 2).joinToString(",") { RGBA(it).hexStringNoAlpha }
        )
        assertEquals(
            "#0000ff,#ffc0cb,#ff0000,#00ff00",
            flippedY.readPixelsUnsafe(0, 0, 2, 2).joinToString(",") { RGBA(it).hexStringNoAlpha }
        )
    }

    @Test
    fun testNativeVectorRenderingEvenOdd() {
        val image = buildShape {
            fill(Colors.RED, winding = Winding.EVEN_ODD) {
                circle(50, 50, 50)
                circle(50, 50, 25)
            }
        }.render()
        assertEquals(MSize(100, 100), image.size)
        assertEquals(1.0, image.getRgbaRaw(10, 50).ad)
        assertEquals(0.0, image.getRgbaRaw(50, 50).ad)
    }

    @Test
    fun testNativeVectorRenderingNonZero() {
        val image = buildShape {
            fill(Colors.RED, winding = Winding.NON_ZERO) {
                circle(50, 50, 50)
                circle(50, 50, 25)
            }
        }.render()
        assertEquals(MSize(100, 100), image.size)
        assertEquals(1.0, image.getRgbaRaw(10, 50).ad)
        assertEquals(1.0, image.getRgbaRaw(50, 50).ad)
    }
}
