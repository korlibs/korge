package com.soywiz.korim.bitmap

import com.soywiz.korma.geom.Rectangle
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
        assertEquals(Rectangle(0, 0, 16, 16), bmp.dirtyRegion)
        assertEquals(2, bmp.lock(Rectangle(0, 0, 8, 8)) {  })
        assertEquals(Rectangle(0, 0, 16, 16), bmp.dirtyRegion)

        bmp.clearDirtyRegion()

        bmp.lock(Rectangle.fromBounds(2, 2, 8, 8)) { }
        assertEquals(Rectangle.fromBounds(2, 2, 8, 8), bmp.dirtyRegion)

        bmp.lock(Rectangle.fromBounds(4, 4, 12, 12)) { }
        assertEquals(Rectangle.fromBounds(2, 2, 12, 12), bmp.dirtyRegion)
    }
}
