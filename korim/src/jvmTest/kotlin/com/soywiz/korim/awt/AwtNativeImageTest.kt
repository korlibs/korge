package com.soywiz.korim.awt

import com.soywiz.korma.geom.MMatrix
import kotlin.test.Test
import kotlin.test.assertEquals

class AwtNativeImageTest {
    @Test
    fun testMatrixAwtTransform() {
        val matrix = MMatrix(1, 2, 3, 4, 5, 6).immutable
        assertEquals(matrix, matrix.clone().toAwt().toMatrix())
    }
}
