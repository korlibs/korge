package korlibs.image.bitmap

import korlibs.image.color.Colors
import kotlin.test.Test
import kotlin.test.assertEquals

class Bitmap2Test {
    @Test
    fun test() {
        val bmp = Bitmap2(2, 2)
        bmp.palette[0] = Colors.RED
        bmp.palette[1] = Colors.GREEN
        bmp.palette[2] = Colors.BLUE
        bmp.palette[3] = Colors.WHITE
        bmp[0, 0] = 0
        bmp[1, 0] = 1
        bmp[0, 1] = 2
        bmp[1, 1] = 3
        val bmp32 = bmp.toBMP32()
        assertEquals(Colors.RED, bmp32[0, 0])
        assertEquals(Colors.GREEN, bmp32[1, 0])
        assertEquals(Colors.BLUE, bmp32[0, 1])
        assertEquals(Colors.WHITE, bmp32[1, 1])
    }
}
