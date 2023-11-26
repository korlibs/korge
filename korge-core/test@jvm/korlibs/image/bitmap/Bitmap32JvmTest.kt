package korlibs.image.bitmap

import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import kotlinx.coroutines.*
import kotlin.test.*

class Bitmap32JvmTest {
    @Test
    fun test() {
        val src = Bitmap32(16, 16, premultiplied = true)
        val dst = Bitmap32(32, 32, premultiplied = true)
        Bitmap32.copyRect(src, 8, 8, dst, 0, 0, 16, 16)
        Bitmap32.copyRect(src, 0, 0, dst, 0, 0, 32, 32)
        Bitmap32.copyRect(src, 16, 16, dst, 0, 0, 32, 32)
        Bitmap32.copyRect(src, 0, 0, dst, 16, 16, 32, 32)
        Bitmap32.copyRect(src, -64, -64, dst, -64, -64, 32, 32)

        val coords = listOf(-64, 0, 8, 16, 32, 64)
        for (srcXY in coords) {
            for (dstXY in coords) {
                for (wh in coords) {
                    Bitmap32.copyRect(src, srcXY, srcXY, dst, dstXY, dstXY, wh, wh)
                }
            }
        }
    }

    @Test
    fun test2() {
        val src = Bitmap32(32, 32, Colors.RED)
        val dst = Bitmap32(32, 32, Colors.BLUE)
        Bitmap32.copyRect(src, -16, -16, dst, 0, 0, 32, 32)
        Bitmap32.copyRect(src, 16, 16, dst, 16, 16, 32, 32)
        assertEquals(Colors.RED, dst[0, 0])
        assertEquals(Colors.RED, dst[24, 24])
        assertEquals(Colors.BLUE, dst[0, 24])
        assertEquals(Colors.BLUE, dst[24, 0])
    }
}
