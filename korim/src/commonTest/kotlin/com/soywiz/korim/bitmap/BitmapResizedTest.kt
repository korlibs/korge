package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class BitmapResizedTest {
    @Test
    fun test() = suspendTest {
        val bmp = Bitmap32(16, 16, Colors.RED)
        val out = bmp.resized(32, 16, ScaleMode.FIT, Anchor.MIDDLE_CENTER, native = false)
        //out.writeTo("/tmp/demo.png".uniVfs, PNG)
    }
}
