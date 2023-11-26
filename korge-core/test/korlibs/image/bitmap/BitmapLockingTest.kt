package korlibs.image.bitmap

import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BitmapLockingTest {
    @Test
    fun testDirtyRegions() {
        val bmp = Bitmap32(16, 16, premultiplied = false)

        assertEquals(null, bmp.dirtyRegion)
        assertEquals(0, bmp.contentVersion)
        assertEquals(1, bmp.lock {  })
        assertEquals(1, bmp.contentVersion)
        assertEquals(RectangleInt(0, 0, 16, 16), bmp.dirtyRegion)
        assertEquals(2, bmp.lock(RectangleInt(0, 0, 8, 8)) {  })
        assertEquals(RectangleInt(0, 0, 16, 16), bmp.dirtyRegion)

        bmp.clearDirtyRegion()

        bmp.lock(RectangleInt.fromBounds(2, 2, 8, 8)) { }
        assertEquals(RectangleInt.fromBounds(2, 2, 8, 8), bmp.dirtyRegion)

        bmp.lock(RectangleInt.fromBounds(4, 4, 12, 12)) { }
        assertEquals(RectangleInt.fromBounds(2, 2, 12, 12), bmp.dirtyRegion)
    }
}
