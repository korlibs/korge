package com.soywiz.korge.view

import com.soywiz.korim.color.RGBA
import kotlin.test.Test
import kotlin.test.assertEquals

class BlendModeTest {
    @Test
    fun testNormalOpaqueOut() {
        assertEquals(
            RGBA.float(.5f, 0f, 0f, 1f),
            BlendMode.NORMAL.apply(false, RGBA.float(.5f, 0f, 0f, 1f), RGBA.float(1f, 1f, 1f, 1f)),
            message = "Normal full opacity replaces the color"
        )
        assertEquals(
            "Blending(outRGB = (srcRGB * 1) + (dstRGB * (1 - srcA)), outA = (srcA * 1) + (dstA * (1 - srcA)))",
            BlendMode.NORMAL.factors.toString()
        )
        assertEquals(
            "Blending(outRGB = (srcRGB * srcA) + (dstRGB * (1 - srcA)), outA = (srcA * 1) + (dstA * (1 - srcA)))",
            BlendMode.NORMAL.nonPremultipliedFactors.toString()
        )
    }
}
