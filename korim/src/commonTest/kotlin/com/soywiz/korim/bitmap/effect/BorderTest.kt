package com.soywiz.korim.bitmap.effect

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korim.paint.*
import com.soywiz.korio.async.*
import kotlin.test.*

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
