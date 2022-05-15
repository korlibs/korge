package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.vector.rect
import kotlin.test.Test
import kotlin.test.assertEquals

class Context2DCommonTest {
    @Test
    fun testFillAlpha() = suspendTest({ !OS.isAndroid }) {
        val semiTransparentAlpha = Colors.FUCHSIA.withAd(0.5)
        val image = NativeImage(10, 10).context2d {
            fill(semiTransparentAlpha) {
                rect(-1, -1, 11, 11)
            }
        }.toBMP32().depremultipliedIfRequired()
        assertEquals(semiTransparentAlpha, image[5, 5])
    }

    @Test
    fun testFillAlpha2() = suspendTest({ !OS.isAndroid }) {
        //val image = NativeImage(3, 3).context2d {
        val image = Bitmap32(3, 3).context2d {
            fill(Colors.WHITE) {
                rect(1, 1, 1, 1)
            }
        }.toBMP32().depremultipliedIfRequired()
        assertEquals(listOf(
            Colors.TRANSPARENT_BLACK, Colors.TRANSPARENT_BLACK, Colors.TRANSPARENT_BLACK,
            Colors.TRANSPARENT_BLACK, Colors.WHITE, Colors.TRANSPARENT_BLACK,
            Colors.TRANSPARENT_BLACK, Colors.TRANSPARENT_BLACK, Colors.TRANSPARENT_BLACK
        ), image.data.toList())
    }
}
