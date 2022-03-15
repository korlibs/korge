package com.soywiz.korge.view

import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import kotlin.test.*

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
