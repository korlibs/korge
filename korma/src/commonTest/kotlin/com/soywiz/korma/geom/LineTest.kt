package com.soywiz.korma.geom

import kotlin.test.*

class LineTest {
    val tolerance = 0.0001

    @Test
    fun testIntersecting() {
        val line1 = MLine(MPoint(284, 158), MPoint(246, 158))
        val line2 = MLine(MPoint(303.89273932825165, 198.88732201874024), MPoint(257.05152496464524, 155.2765362319343))
        assertEquals(true, line1.intersectsSegment(line2))
        assertEqualsFloat(38.0, line1.length)
        assertEqualsFloat(64.0, line2.length, absoluteTolerance = tolerance)
        assertEqualsFloat(180.0, line1.angle.degrees, absoluteTolerance = tolerance)
        assertEqualsFloat(222.95459151111274, line2.angle.degrees, absoluteTolerance = tolerance)
        assertEqualsFloat(MPoint(260.0, 158.0), line1.getSegmentIntersectionPoint(line2)?.round())
    }

    @Test
    fun testProjectedPoint() {
        assertEqualsFloat(MPoint(0, 50), MLine(MPoint(0, 0), MPoint(0, 100)).projectedPoint(MPoint(50, 50)))
        assertEqualsFloat(MPoint(50, 50), MLine(MPoint(0, 0), MPoint(100, 100)).projectedPoint(MPoint(100, 0)))

        // On line
        assertEqualsFloat(MPoint(0, 0), MLine(MPoint(0, 0), MPoint(0, 100)).projectedPoint(MPoint(0, 0)))
        assertEqualsFloat(MPoint(0, 50), MLine(MPoint(0, 0), MPoint(0, 100)).projectedPoint(MPoint(0, 50)))
        assertEqualsFloat(MPoint(0, 100), MLine(MPoint(0, 0), MPoint(0, 100)).projectedPoint(MPoint(0, 100)))
        assertEqualsFloat(MPoint(0, 150), MLine(MPoint(0, 0), MPoint(0, 100)).projectedPoint(MPoint(0, 150)))
    }

    @Test
    fun testLineData() {
        val gen = { MLine(MPoint(0, 0), MPoint(100, 100)) }
        assertEqualsFloat(gen(), gen())
        assertEquals(gen().hashCode(), gen().hashCode())
    }
}
