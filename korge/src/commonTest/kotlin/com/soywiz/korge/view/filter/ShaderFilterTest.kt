package com.soywiz.korge.view.filter

import kotlin.test.Test
import kotlin.test.assertSame

class ShaderFilterTest {
    @Test
    fun testShaderFilterDoNotRecreatePrograms() {
        val filter1 = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX)
        val filter2 = ColorMatrixFilter(ColorMatrixFilter.SEPIA_MATRIX)
        assertSame(filter1.programProvider.getProgram(), filter2.programProvider.getProgram())
        assertSame(filter1.programProvider.getProgram(), filter2.programProvider.getProgram())
    }
}
