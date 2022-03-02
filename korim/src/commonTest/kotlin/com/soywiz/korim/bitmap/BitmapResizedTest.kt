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

    @Test
    fun testResizedUpTo() = suspendTest {
        val bmp = Bitmap32(128, 256, Colors.RED)
        val out = bmp.resizedUpTo(32, 32)
        assertEquals(Size(16, 32), out.size)
        //out.writeTo("/tmp/demo.png".uniVfs, PNG)
    }
}
