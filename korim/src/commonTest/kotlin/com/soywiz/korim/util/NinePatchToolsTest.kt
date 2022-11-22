package com.soywiz.korim.util

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.doubleArrayListOf
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.pointArrayListOf
import com.soywiz.korma.geom.range.until
import kotlin.test.Test
import kotlin.test.assertEquals

class NinePatchToolsTest {
    @Test
    fun testTransform1D() {
        val result = NinePatchSlices(4.0 until 9.0).transform1D(
            listOf(
                doubleArrayListOf(1.0),
                doubleArrayListOf(5.0),
                doubleArrayListOf(10.0),
                doubleArrayListOf(15.0),
            ),
            oldLen = 15.0,
            newLen = 32.0
        )
        assertEquals(
            listOf(
                DoubleArrayList(1.0),
                DoubleArrayList(8.4),
                DoubleArrayList(27.0),
                DoubleArrayList(32.0),
            ),
            result.toList()
        )
    }

    @Test
    fun testTransform2D() {
        val result = NinePatchSlices2D(
            x = NinePatchSlices(4.0 until 9.0),
            y = NinePatchSlices(4.0 until 9.0),
        ).transform2D(
            listOf(
                pointArrayListOf(1.0, 1.0),
                pointArrayListOf(5.0, 5.0),
                pointArrayListOf(10.0, 10.0),
                pointArrayListOf(15.0, 15.0),
            ),
            oldSize = Size(15.0, 15.0),
            newSize = Size(32.0, 64.0)
        )

        assertEquals(
            listOf(
                pointArrayListOf(1.0, 1.0),
                pointArrayListOf(8.4, 14.8),
                pointArrayListOf(27.0, 59.0),
                pointArrayListOf(32.0, 64.0),
            ),
            result.toList()
        )
    }
}
