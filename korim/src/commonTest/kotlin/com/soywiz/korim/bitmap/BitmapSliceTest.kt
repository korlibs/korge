package com.soywiz.korim.bitmap

import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.ImageOrientation
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.RectangleInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class BitmapSliceTest {
    @Test
    fun test() {
        val bmp = Bitmap32(64, 64)
        assertEquals("Rectangle(x=0, y=0, width=32, height=32)", bmp.sliceWithSize(0, 0, 32, 32).bounds.toString())
        assertEquals("Rectangle(x=32, y=32, width=32, height=32)", bmp.sliceWithSize(32, 32, 32, 32).bounds.toString())
        assertEquals("Rectangle(x=48, y=48, width=16, height=16)", bmp.sliceWithSize(48, 48, 32, 32).bounds.toString())
        //assertEquals("Rectangle(x=48, y=48, width=-16, height=-16)", bmp.sliceWithBounds(48, 48, 32, 32).bounds.toString()) // Allow invalid bounds
        assertEquals("Rectangle(x=48, y=48, width=0, height=0)", bmp.sliceWithBounds(48, 48, 32, 32).bounds.toString())
        assertEquals("Rectangle(x=8, y=8, width=24, height=24)", bmp.sliceWithSize(16, 16, 32, 32).sliceWithSize(8, 8, 40, 40).bounds.toString())
    }

    @Test
    fun testBmpSize() {
        val slice = Bitmap32(128, 64).sliceWithSize(24, 16, 31, 17)
        assertEquals(
            """
                bmpSize=128,64
                coords=24,16,55,33
                size=31,17
                area=527
                trimmed=false
                frameOffset=0,0,31,17
            """.trimIndent(),
            """
                bmpSize=${slice.bmpWidth},${slice.bmpHeight}
                coords=${slice.left},${slice.top},${slice.right},${slice.bottom}
                size=${slice.width},${slice.height}
                area=${slice.area}
                trimmed=${slice.trimmed}
                frameOffset=${slice.frameOffsetX},${slice.frameOffsetY},${slice.frameWidth},${slice.frameHeight}
            """.trimIndent()
        )
    }

    @Test
    fun testRotate() {
        val bmp = Bitmap32(128, 64).slice()
        val bmp2 = bmp.rotatedRight()
        assertEquals("128x64", bmp.sizeString)
        assertEquals("64x128", bmp2.sizeString)
    }

    @Test
    fun testTransformed() {
        if (OS.isJvm) {
            val bmp = Bitmap32(20, 10)
            val slice = bmp.sliceWithSize(1, 1, 8, 18, imageOrientation = ImageOrientation.ROTATE_90)

            slice.setRgba(0, 0, Colors.RED)
            assertEquals(Colors.RED, slice.getRgba(0, 0))
            assertEquals(Colors.RED, bmp.getRgba(1, 8))

            slice.flippedX()
            assertEquals(Colors.RED, slice.getRgba(0, 0))
            slice.flippedX()

            val vfSlice = slice.virtFrame(2, 4, 12, 26)
            assertEquals(Colors.RED, vfSlice.getRgba(2, 4))
            assertEquals(Colors.TRANSPARENT_BLACK, vfSlice.getRgba(0, 0))
            assertEquals(Colors.TRANSPARENT_BLACK, vfSlice.getRgba(11, 25))
            assertFailsWith<IllegalArgumentException> { vfSlice.getRgba(0, -1) }
            assertFailsWith<IllegalArgumentException> { vfSlice.getRgba(-1, 0) }
            assertFailsWith<IllegalArgumentException> { vfSlice.getRgba(11, 26) }
            assertFailsWith<IllegalArgumentException> { vfSlice.getRgba(12, 25) }

            vfSlice.setRgba(0, 0, Colors.BLUE)
            assertEquals(Colors.BLUE, vfSlice.getRgba(0, 0))
            assertEquals(Colors.BLUE, vfSlice.base.getRgba(0, 0))
            vfSlice.setRgba(11, 25, Colors.BLUE)
            assertEquals(Colors.BLUE, vfSlice.getRgba(11, 25))
            assertEquals(Colors.BLUE, vfSlice.base.getRgba(11, 25))
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun testDeprecatedConstructors() {
        if (OS.isJvm) {
            val bmp = Bitmap32(20, 20)
            bmp.setRgba(1, 1, Colors.RED)
            bmp.setRgba(18, 8, Colors.GREEN)

            val r1 = RectangleInt(1, 1, 18, 8)
            val r2 = RectangleInt(1, 1, 8, 18)

            var slice = BitmapSlice(bmp, r1, rotated = false)
            assertEquals(Colors.RED, slice.getRgba(0, 0))
            assertEquals(Colors.GREEN, slice.getRgba(17, 7))

            slice = BitmapSlice(bmp, r2, rotated = true)
            assertEquals(Colors.RED, slice.getRgba(7, 0))
            assertEquals(Colors.GREEN, slice.getRgba(0, 17))

            val r3 = Rectangle(1, 1, 18, 8)
            val r4 = Rectangle(1, 1, 8, 18)

            var sliceCompat = BitmapSliceCompat(bmp, r3, r3, r3, false)
            assertEquals(Colors.RED, sliceCompat.getRgba(0, 0))
            assertEquals(Colors.GREEN, sliceCompat.getRgba(17, 7))

            sliceCompat = BitmapSliceCompat(bmp, r4, r4, r4, true)
            assertEquals(Colors.RED, sliceCompat.getRgba(7, 0))
            assertEquals(Colors.GREEN, sliceCompat.getRgba(0, 17))
        }
    }


    @Test
    fun testReadPixels() {
        if (OS.isJvm) {
            val bmp = Bitmap32(20, 10)
            val slice = bmp.sliceWithSize(1, 1, 8, 18, imageOrientation = ImageOrientation.ROTATE_90)

            slice.setRgba(0, 0, Colors.RED)
            slice.setRgba(7, 17, Colors.BLUE)
            assertEquals(Colors.RED, slice.getRgba(0, 0))
            assertEquals(Colors.RED, bmp.getRgba(1, 8))
            assertEquals(Colors.BLUE, slice.getRgba(7, 17))
            assertEquals(Colors.BLUE, bmp.getRgba(18, 1))

            val sliceDataTopLeft = slice.readPixels(0, 0, 2, 2)
            val sliceDataBottomRight = slice.readPixels(6, 16, 2, 2)
            assertEquals(Colors.RED, sliceDataTopLeft[0])
            assertEquals(Colors.BLUE, sliceDataBottomRight[3])

            assertFailsWith<IllegalStateException> { slice.readPixels(-1, 0, 2, 2) }
            assertFailsWith<IllegalStateException> { slice.readPixels(0, -1, 2, 2) }
            assertFailsWith<IllegalStateException> { slice.readPixels(7, 16, 2, 2) }
            assertFailsWith<IllegalStateException> { slice.readPixels(6, 17, 2, 2) }

            val sliceVirtFrame = slice.virtFrame(1, 1, 10, 20)
            val sliceVirtFrameDataTopLeft = sliceVirtFrame.readPixels(0, 0, 2, 2)
            val sliceVirtFrameDataBottomRight = sliceVirtFrame.readPixels(8, 18, 2, 2)
            assertEquals(Colors.RED, sliceVirtFrameDataTopLeft[3])
            assertEquals(Colors.BLUE, sliceVirtFrameDataBottomRight[0])

            assertFailsWith<IllegalStateException> { sliceVirtFrame.readPixels(-1, 0, 2, 2) }
            assertFailsWith<IllegalStateException> { sliceVirtFrame.readPixels(0, -1, 2, 2) }
            assertFailsWith<IllegalStateException> { sliceVirtFrame.readPixels(9, 18, 2, 2) }
            assertFailsWith<IllegalStateException> { sliceVirtFrame.readPixels(8, 19, 2, 2) }
        }
    }

    @Test
    fun testTransformFrame() {
        val baseSlice = Bitmap32(5, 10).slice().virtFrame(2, 2, 10, 20)

        var slice: BmpCoordsWithT<Bitmap> = baseSlice
        assertEquals(2, slice.frameOffsetX)
        assertEquals(2, slice.frameOffsetY)
        assertEquals(10, slice.frameWidth)
        assertEquals(20, slice.frameHeight)

        slice = baseSlice.rotatedRight()
        assertEquals(8, slice.frameOffsetX)
        assertEquals(2, slice.frameOffsetY)
        assertEquals(20, slice.frameWidth)
        assertEquals(10, slice.frameHeight)

        slice = baseSlice.rotatedLeft()
        assertEquals(2, slice.frameOffsetX)
        assertEquals(3, slice.frameOffsetY)
        assertEquals(20, slice.frameWidth)
        assertEquals(10, slice.frameHeight)

        slice = baseSlice.flippedY()
        assertEquals(2, slice.frameOffsetX)
        assertEquals(8, slice.frameOffsetY)
        assertEquals(10, slice.frameWidth)
        assertEquals(20, slice.frameHeight)

        slice = baseSlice.flippedX()
        assertEquals(3, slice.frameOffsetX)
        assertEquals(2, slice.frameOffsetY)
        assertEquals(10, slice.frameWidth)
        assertEquals(20, slice.frameHeight)
    }

    fun testExtract90(bmp: Bitmap) {
        bmp.setRgba(0, 0, Colors.WHITE)
        bmp.setRgba(1, 1, Colors.RED)
        bmp.setRgba(18, 8, Colors.BLUE)
        bmp.setRgba(19, 9, Colors.WHITE)
        val slice = bmp.sliceWithSize(1, 1, 8, 18, imageOrientation = ImageOrientation.ROTATE_90).virtFrame(2, 2, 12, 22)
        val bmp2 = slice.extract()
        assertNotEquals(Colors.WHITE, bmp2.getRgba(10, 1))
        assertEquals(Colors.RED, bmp2.getRgba(9, 2))
        assertEquals(Colors.BLUE, bmp2.getRgba(2, 19))
        assertNotEquals(Colors.WHITE, bmp2.getRgba(1, 20))
    }

    fun testExtract270(bmp: Bitmap) {
        bmp.setRgba(0, 0, Colors.WHITE)
        bmp.setRgba(1, 1, Colors.RED)
        bmp.setRgba(18, 8, Colors.BLUE)
        bmp.setRgba(19, 9, Colors.WHITE)
        val slice = bmp.sliceWithSize(1, 1, 8, 18, imageOrientation = ImageOrientation.ROTATE_270).virtFrame(2, 2, 12, 22)
        val bmp2 = slice.extract()
        assertNotEquals(Colors.WHITE, bmp2.getRgba(1, 20))
        assertEquals(Colors.RED, bmp2.getRgba(2, 19))
        assertEquals(Colors.BLUE, bmp2.getRgba(9, 2))
        assertNotEquals(Colors.WHITE, bmp2.getRgba(10, 1))
    }

    @Test
    fun textExtractBitmap32() {
        testExtract90(Bitmap32(20, 10))
        testExtract270(Bitmap32(20, 10))
    }

    @Test
    fun textExtractNativeImage() {
        testExtract90(NativeImage(20, 10))
        testExtract270(NativeImage(20, 10))
     }
}
