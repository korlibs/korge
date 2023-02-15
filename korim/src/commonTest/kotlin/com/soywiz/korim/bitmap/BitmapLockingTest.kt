package com.soywiz.korim.bitmap

import com.soywiz.korma.geom.*
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
        assertEquals(MRectangleInt(0, 0, 16, 16), bmp.dirtyRegion)
        assertEquals(2, bmp.lock(MRectangleInt(0, 0, 8, 8)) {  })
        assertEquals(MRectangleInt(0, 0, 16, 16), bmp.dirtyRegion)

        bmp.clearDirtyRegion()

        bmp.lock(MRectangleInt.fromBounds(2, 2, 8, 8)) { }
        assertEquals(MRectangleInt.fromBounds(2, 2, 8, 8), bmp.dirtyRegion)

        bmp.lock(MRectangleInt.fromBounds(4, 4, 12, 12)) { }
        assertEquals(MRectangleInt.fromBounds(2, 2, 12, 12), bmp.dirtyRegion)
    }
}
