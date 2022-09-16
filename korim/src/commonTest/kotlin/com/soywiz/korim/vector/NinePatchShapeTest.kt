package com.soywiz.korim.vector

import com.soywiz.korim.color.Colors
import com.soywiz.korim.util.NinePatchSlices
import com.soywiz.korim.util.NinePatchSlices2D
import com.soywiz.korim.vector.format.readSVG
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.range.until
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NinePatchShapeTest {
    @Test
    fun test() = suspendTest {
        val ninePatch = resourcesVfs["chat-bubble.svg"].readSVG().toShape().toNinePatchWithGuides(color = Colors.FUCHSIA)
        assertEquals(Size(128, 128), ninePatch.size)
        assertEquals(NinePatchSlices2D(
            NinePatchSlices(30.0 until 33.0, 80.0 until 100.0),
            NinePatchSlices(40.0 until 80.0)
        ), ninePatch.slices)
        assertTrue { ninePatch.shape is FillShape }
        assertEquals(
            "M0,0 M128,28 C128,12,116,0,100,0 L28,0 C12,0,0,12,0,28 L0,83 C0,98,12,111,28,111 L38,111 L34,128 L76,111 L100,111 C116,111,128,98,128,83 L128,28 Z",
            ninePatch.shape.getPath().roundDecimalPlaces(0).toSvgString()
        )
    }
}
