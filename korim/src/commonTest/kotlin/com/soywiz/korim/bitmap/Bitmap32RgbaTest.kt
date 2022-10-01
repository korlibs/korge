package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.ImageDecodingProps
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.readBitmapNoNative
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.suspendTestNoBrowser
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import kotlin.test.Test
import kotlin.test.assertEquals

class Bitmap32RgbaTest {
    @Test
    fun testNative() = suspendTestNoBrowser {
        if (OS.isNative) return@suspendTestNoBrowser
        //if (OS.isMac) return@suspendTestNoBrowser // kotlin.AssertionError: Expected <#ff0000ff>, actual <#fb0007ff>.
        //if (OS.isTvos) return@suspendTestNoBrowser

        val bmp = resourcesVfs["rgba.png"].readBitmap(ImageDecodingProps.DEFAULT_PREMULT.copy(format = PNG))
        val i = bmp.toBMP32()
        assertEquals(Colors.RED, i[0, 0])
        assertEquals(Colors.GREEN, i[1, 0])
        assertEquals(Colors.BLUE, i[2, 0])
        assertEquals(Colors.TRANSPARENT_BLACK, i[3, 0])
    }

    @Test
    fun testNormal() = suspendTestNoBrowser {
        val bmp = resourcesVfs["rgba.png"].readBitmapNoNative(ImageDecodingProps.DEFAULT.copy(format = PNG))
        val i = bmp.toBMP32()
        assertEquals(Colors.RED, i[0, 0])
        assertEquals(Colors.GREEN, i[1, 0])
        assertEquals(Colors.BLUE, i[2, 0])
        assertEquals(Colors.TRANSPARENT_BLACK, i[3, 0])
    }
}
