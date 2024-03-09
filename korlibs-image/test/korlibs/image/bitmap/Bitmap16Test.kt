package korlibs.image.bitmap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Bitmap16Test {
    @Test
    fun test() {
        val bmp = Bitmap16(10, 10, ShortArray(10 * 10) { it.toShort() })
        val bmp2 = bmp.clone()
        assertEquals(bmp.width, bmp2.width)
        assertEquals(bmp.height, bmp2.height)
        assertTrue { (bmp2 as Bitmap16).data.contentEquals(bmp.data) }
    }
}
