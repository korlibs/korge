package com.soywiz.korma.geom.vector

import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.shape.buildVectorPath
import kotlin.test.Test
import kotlin.test.assertEquals

class VectorBuilderTest {
    @Test
    fun testParallelogram() {
        assertEquals("M0,0 L100,0 L100,50 L0,50", buildVectorPath { parallelogram(Rectangle(0, 0, 100, 50), angle = 0.degrees, direction = true) }.toSvgString(), "angle of 0 generates a rectangle")
        assertEquals("M0,0 L150,0 L100,50 L-50,50", buildVectorPath { parallelogram(Rectangle(0, 0, 100, 50), angle = 90.degrees, direction = true) }.toSvgString(), "angle of 90 with direction = true generates a parallelogram with lines doing a 45 degrees clockwise")
        assertEquals("M-50,0 L100,0 L150,50 L0,50", buildVectorPath { parallelogram(Rectangle(0, 0, 100, 50), angle = 90.degrees, direction = false) }.toSvgString(), "angle of 90 with direction = false generates a parallelogram with lines doing a 45 degrees counter-clockwise")
    }
}
