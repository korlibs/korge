package com.soywiz.korim.format.jpg

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class JpegFormatTest {
    val formats = ImageFormats(JPEG, PNG)

    @Test
    fun jpeg() = suspendTestNoBrowser {
        val bitmap = resourcesVfs["kotlin.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
        //bitmap.writeTo(LocalVfs("c:/temp/img1.jpg.png"), formats = formats)
    }

    @Test
    fun jpeg2() = suspendTestNoBrowser {
        val bitmap = resourcesVfs["img1.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(460, 460)", bitmap.toString())
        //bitmap.writeTo(LocalVfs("c:/temp/img1.jpg.tga"), formats = formats)
    }

    @Test
    @Ignore
    fun jpegNative() = suspendTestNoBrowser {
        val bitmap = resourcesVfs["kotlin.jpg"].readBitmap(formats = formats)
        //assertTrue(bitmap is NativeImage)
        assertEquals("Bitmap32(190, 190)", bitmap.toBMP32().toString())

        val bitmapExpected = resourcesVfs["kotlin.jpg.png"].readBitmap(formats = formats)
        assertTrue(Bitmap32.matches(bitmapExpected, bitmap))

        //val diff = Bitmap32.diff(bitmapExpected, bitmap)
        //diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
        //awtShowImage(diff); Thread.sleep(10000L)
    }

    @Test
    @Ignore
    fun jpeg2Native() = suspendTestNoBrowser {
        val bitmap = resourcesVfs["img1.jpg"].readBitmap(formats = formats)
        //assertTrue(bitmap is NativeImage)
        assertEquals("Bitmap32(460, 460)", bitmap.toBMP32().toString())

        val bitmapExpected = resourcesVfs["img1.jpg.png"].readBitmap(formats = formats)
        assertTrue(Bitmap32.matches(bitmapExpected, bitmap, threshold = 32))

        //val diff = Bitmap32.diff(bitmapExpected, bitmap)
        //diff.transformColor { RGBA.pack(RGBA.getR(it) * 4, RGBA.getG(it) * 4, RGBA.getB(it) * 4, 0xFF) }
        //diff.transformColor { RGBA.pack(RGBA.getR(it) * 0xFF, RGBA.getG(it) * 0xFF, RGBA.getB(it) * 0xFF, 0xFF) }
        //awtShowImage(diff); Thread.sleep(10000L)
    }

    @Test
    fun jpegEncoder() = suspendTestNoBrowser {
        val bitmapOriginal = resourcesVfs["kotlin32.png"].readBitmapNoNative(formats).toBMP32()
        val bytes = JPEG.encode(bitmapOriginal, ImageEncodingProps(quality = 0.5))
        //val bitmapOriginal = LocalVfs("/tmp/aa.jpg").readBitmapNoNative().toBMP32()
        //bitmapOriginal.writeTo(LocalVfs("/tmp/out.jpg"))
    }

    @Test
    fun ajpeg() = suspendTestNoBrowser {
        val bitmap = resourcesVfs["kotlin.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(190, 190)", bitmap.toString())
    }

    @Test
    fun ajpeg2() = suspendTestNoBrowser {
        val bitmap = resourcesVfs["img1.jpg"].readBitmapNoNative(formats)
        assertEquals("Bitmap32(460, 460)", bitmap.toString())
    }
}
