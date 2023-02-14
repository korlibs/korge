package com.soywiz.korge.view.filter

import com.soywiz.korma.geom.MVector4
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorMatrixFilterTest {
    @Test
    fun test() {
        val grayFilter = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX)

        assertEquals(
            MVector4(0.57f, 0.57f, 0.57f, 1f),
            grayFilter.colorMatrix.transform(MVector4(.75f, .5f, .25f, 1f))
        )
    }
}
