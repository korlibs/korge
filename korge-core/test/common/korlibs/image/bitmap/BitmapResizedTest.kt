package korlibs.image.bitmap

import korlibs.image.color.Colors
import korlibs.io.async.suspendTest
import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals(SizeInt(16, 32), out.size)
        //out.writeTo("/tmp/demo.png".uniVfs, PNG)
    }
}
