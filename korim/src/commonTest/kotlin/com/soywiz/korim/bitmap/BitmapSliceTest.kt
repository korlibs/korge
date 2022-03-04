package com.soywiz.korim.bitmap

import kotlin.test.*

class BitmapSliceTest {
    @Test
    fun test() {
        val bmp = Bitmap32(64, 64)
        assertEquals("Rectangle(x=0, y=0, width=32, height=32)", bmp.sliceWithSize(0, 0, 32, 32).bounds.toString())
        assertEquals("Rectangle(x=32, y=32, width=32, height=32)", bmp.sliceWithSize(32, 32, 32, 32).bounds.toString())
        assertEquals("Rectangle(x=48, y=48, width=16, height=16)", bmp.sliceWithSize(48, 48, 32, 32).bounds.toString())
        //assertEquals("Rectangle(x=48, y=48, width=-16, height=-16)", bmp.sliceWithBounds(48, 48, 32, 32).bounds.toString()) // Allow invalid bounds
        assertEquals("Rectangle(x=48, y=48, width=0, height=0)", bmp.sliceWithBounds(48, 48, 32, 32).bounds.toString())
        assertEquals("Rectangle(x=24, y=24, width=24, height=24)", bmp.sliceWithSize(16, 16, 32, 32).sliceWithSize(8, 8, 40, 40).bounds.toString())
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
}
