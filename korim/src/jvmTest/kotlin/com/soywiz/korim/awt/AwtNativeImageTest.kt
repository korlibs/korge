package com.soywiz.korim.awt

import com.soywiz.korma.geom.Matrix
import kotlin.test.Test
import kotlin.test.assertEquals

class AwtNativeImageTest {
    @Test
    fun testMatrixAwtTransform() {
        val matrix = Matrix(1, 2, 3, 4, 5, 6)
        assertEquals(matrix, matrix.clone().toAwt().toMatrix())
    }
}
