package korlibs.math.geom.vector

import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import kotlin.test.*

class VectorBuilderTest {
    @Test
    fun testParallelogram() {
        assertEquals("M0,0 L100,0 L100,50 L0,50", buildVectorPath { parallelogram(Rectangle(0, 0, 100, 50), angle = 0.degrees, direction = true) }.toSvgString(), "angle of 0 generates a rectangle")
        assertEquals("M0,0 L150,0 L100,50 L-50,50", buildVectorPath { parallelogram(Rectangle(0, 0, 100, 50), angle = 90.degrees, direction = true) }.toSvgString(), "angle of 90 with direction = true generates a parallelogram with lines doing a 45 degrees clockwise")
        assertEquals("M-50,0 L100,0 L150,50 L0,50", buildVectorPath { parallelogram(Rectangle(0, 0, 100, 50), angle = 90.degrees, direction = false) }.toSvgString(), "angle of 90 with direction = false generates a parallelogram with lines doing a 45 degrees counter-clockwise")
    }

    @Test
    fun testCircle() {
        assertEquals("M100,0 C100,-55,55,-100,0,-100 C-55,-100,-100,-55,-100,0 C-100,55,-55,100,0,100 C55,100,100,55,100,0 Z", buildVectorPath { circle(Point(0, 0), 100f) }.roundDecimalPlaces(0).toSvgString())
    }

    @Test
    fun testCircleHole() {
        assertEquals("M100,0 C100,55,55,100,0,100 C-55,100,-100,55,-100,0 C-100,-55,-55,-100,0,-100 C55,-100,100,-55,100,0 Z", buildVectorPath { circleHole(Point(0, 0), 100f) }.roundDecimalPlaces(0).toSvgString())
    }
}
