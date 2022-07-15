package com.soywiz.korge.view

import com.soywiz.korim.color.Colors
import kotlin.test.Test
import kotlin.test.assertEquals

class BlendModeTest {
    @Test
    fun testToString() {
        assertEquals("INHERIT", BlendMode.INHERIT.toString())
        assertEquals("NONE", BlendMode.NONE.toString())
        assertEquals("NORMAL", BlendMode.NORMAL.toString())
        assertEquals("ADD", BlendMode.ADD.toString())
    }

    @Test
    fun testNormal() {
        assertEquals(Colors["#7f0000ff"], BlendMode.NORMAL.apply(false, Colors["#7f0000ff"], Colors["#ffffffff"]))
        assertEquals(Colors["#7f0000ff"], BlendMode.NORMAL.apply(true, Colors["#7f0000ff"], Colors["#ffffffff"]))
        assertEquals(Colors["#bf7f7fff"], BlendMode.NORMAL.apply(false, Colors["#7f00007f"], Colors["#ffffffff"]))
        assertEquals(Colors["#ff7f7fff"], BlendMode.NORMAL.apply(true, Colors["#7f00007f"], Colors["#ffffffff"]))

        assertEquals("Blending(outRGB = (srcRGB * 1) + (dstRGB * (1 - srcA)), outA = (srcA * 1) + (dstA * (1 - srcA)))", BlendMode.NORMAL.factors.toString())
        assertEquals("Blending(outRGB = (srcRGB * srcA) + (dstRGB * (1 - srcA)), outA = (srcA * 1) + (dstA * (1 - srcA)))", BlendMode.NORMAL.nonPremultipliedFactors.toString())
    }

    @Test
    fun testAdd() {
        assertEquals(Colors["#cb4c4cff"], BlendMode.ADD.apply(true, Colors["#4c4c4c4c"], Colors["#7f0000ff"]))
        assertEquals(Colors["#951616ff"], BlendMode.ADD.apply(false, Colors["#4c4c4c4c"], Colors["#7f0000ff"]))
        assertEquals("Blending(outRGB = (srcRGB * 1) + (dstRGB * 1), outA = (srcA * 1) + (dstA * 1))", BlendMode.ADD.factors.toString())
        assertEquals("Blending(outRGB = (srcRGB * srcA) + (dstRGB * dstA), outA = (srcA * 1) + (dstA * 1))", BlendMode.ADD.nonPremultipliedFactors.toString())
    }
}
