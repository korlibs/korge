package com.soywiz.korim.bitmap.effect

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class BlurTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        Bitmap32(100, 100, premultiplied = true).context2d {
            fill(Colors.RED) {
                circle(50, 50, 40)
            }
        }
            .dropShadowInplace(0, 0, 10, Colors.BLUE)
            //.blur(10)
            .showImageAndWait()
    }
}
