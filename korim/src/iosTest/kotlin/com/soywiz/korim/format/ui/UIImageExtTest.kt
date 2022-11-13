package com.soywiz.korim.format.ui

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import kotlin.test.*

class UIImageExtTest {
    @Test
    fun testUIImageConversions() {
        val bmp = Bitmap32(16, 16) { x, y -> RGBA(x * 16, y * 16, 0, 0xFF) }.premultiplied()
        val bmpC = bmp.clone()
        val uiImage = bmp.toUIImage()
        val bmp2 = uiImage.toBitmap32()
        assertTrue("Original bitmap shouldn't have been modified") { bmp.contentEquals(bmpC) }
        assertTrue("Conversion back and forth should work") { bmp.contentEquals(bmp2) }
    }
}
