package com.soywiz.korim.bitmap.effect

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.suspendTest
import com.soywiz.korma.geom.vector.circle
import kotlin.test.Test

class BlurTest {
    @Test
    fun test() = suspendTest {
        val bmpWithDropShadow = Bitmap32(100, 100, premultiplied = true).context2d {
            fill(Colors.RED) {
                circle(50, 50, 40)
            }
        }.dropShadowInplace(0, 0, 5, Colors.BLUE)
        //bmpWithDropShadow.showImageAndWait()
    }
}
