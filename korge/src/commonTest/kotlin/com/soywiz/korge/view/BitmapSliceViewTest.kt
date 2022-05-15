package com.soywiz.korge.view

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.UntransformedSizeBmpCoordsWithInstance
import com.soywiz.korim.bitmap.rotatedRight
import com.soywiz.korim.bitmap.slice
import com.soywiz.korma.geom.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class BitmapSliceViewTest {
    @Test
    fun test() {
        val coords = Bitmap32(64, 128).slice().rotatedRight()
        val image1 = Image(coords)
        val image2 = Image(UntransformedSizeBmpCoordsWithInstance(coords))

        assertEquals(Size(128, 64), image1.getLocalBoundsOptimized().size)
        assertEquals(Size(64, 128), image2.getLocalBoundsOptimized().size)
    }
}
