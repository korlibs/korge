package com.soywiz.korim.vector

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.writeTo
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.fullPathNormalized
import com.soywiz.korio.file.std.tempVfs
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.degrees
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeRenderTest {
    @Test fun testNativeFill() = doTest(native = true, drawBitmap = false)
    @Test fun testNativeBitmap() = doTest(native = true, drawBitmap = true)
    @Test fun testNonNativeFill() = doTest(native = false, drawBitmap = false)
    @Test fun testNonNativeBitmap() = doTest(native = false, drawBitmap = true)

    fun doTest(native: Boolean, drawBitmap: Boolean) = suspendTest {
        val bmp = createBitmap(native, drawBitmap)
        checks(bmp, "${OS.platformNameLC}.native_$native.bmp_$drawBitmap")
    }

    fun createBitmap(native: Boolean, drawBitmap: Boolean): Bitmap32 {
        val bmp: Bitmap = NativeImageOrBitmap32(100, 100, native = native)
        return bmp.context2d {
            rotate(30.degrees)
            translate(20, 20)
            fillStyle = createColor(Colors.RED)
            if (drawBitmap) {
                drawImage(Bitmap32(20, 20, Colors.RED), 10, 10)
            } else {
                fillRect(10, 10, 20, 20)
            }
        }.toBMP32()
    }

    suspend fun checks(image: Bitmap32, name: String) {
        try {
            assertEquals(Colors.RED, image[14, 54])
            assertEquals(0, image[4, 45].a)
            assertEquals(0, image[19, 42].a)
        } catch (e: Throwable) {
            val file = tempVfs["output.$name.png"]
            try {
                image.writeTo(file, PNG)
                println("Failed image saved to: " + file.fullPathNormalized)
            } catch (e: Throwable) {
                println("Couldn't save failing image")
            }
            throw e
        }
    }
}
