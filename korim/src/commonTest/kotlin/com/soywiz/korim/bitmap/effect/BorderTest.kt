package com.soywiz.korim.bitmap.effect

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korio.async.suspendTest
import kotlin.test.Test

class BorderTest {
    @Test
    fun test() = suspendTest {
        val bmp = Bitmap32(100, 100).context2d {
            drawText("Hello", x = 20.0, y = 20.0, font = DefaultTtfFont, paint = Colors.RED)
        }
        val bmpBorder = bmp.border(4, Colors.GREEN)
        //bmpBorder.showImageAndWait()
    }
}
