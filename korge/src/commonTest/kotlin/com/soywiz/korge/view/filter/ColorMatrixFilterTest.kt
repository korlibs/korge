package com.soywiz.korge.view.filter

import com.soywiz.korma.geom.*
import kotlin.test.*

class ColorMatrixFilterTest {
    @Test
    fun test() {
        val grayFilter = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX)

        assertEquals(
            Vector3D(0.57f, 0.57f, 0.57f, 1f),
            grayFilter.colorMatrix.transform(Vector3D(.75f, .5f, .25f, 1f))
        )
    }
}
