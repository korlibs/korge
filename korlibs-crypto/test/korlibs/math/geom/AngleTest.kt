package korlibs.math.geom

import korlibs.math.*
import korlibs.math.interpolation.*
import kotlin.math.*
import kotlin.test.*

class AngleTest {
    @Test
    fun testAngleTo() {
        //assertEquals(Angle.fromDegrees(0.0), Point2d(0, 0).angleTo(Point2d(100, 0)))
        //assertEquals(Angle.fromDegrees(90.0), Point2d(0, 0).angleTo(Point2d(0, 100)))
        //assertEquals(Angle.fromDegrees(180.0), Point2d(0, 0).angleTo(Point2d(-100, 0)))
        //assertEquals(Angle.fromDegrees(270.0), Point2d(0, 0).angleTo(Point2d(0, -100)))
//
        //assertEquals(Angle.fromDegrees(0.0), Point2d(1000, 1000).angleTo(Point2d(1000 + 100, 1000 + 0)))
        //assertEquals(Angle.fromDegrees(90.0), Point2d(1000, 1000).angleTo(Point2d(1000 + 0, 1000 + 100)))
        //assertEquals(Angle.fromDegrees(180.0), Point2d(1000, 1000).angleTo(Point2d(1000 + -100, 1000 + 0)))
        //assertEquals(Angle.fromDegrees(270.0), Point2d(1000, 1000).angleTo(Point2d(1000 + 0, 1000 + -100)))
    }

    @Test
    fun testAngleOps() {

        assertEquals(180.degrees, 90.degrees + 90.degrees)
        assertEquals((-10).degrees, 90.degrees - 100.degrees)
        assertEquals((-350).degrees, 0.degrees - 350.degrees)
        assertEquals(180.degrees, 90.degrees * 2)
        assertEquals(45.degrees, 90.degrees / 2)
        assertEquals(2.0, 90.degrees / 45.degrees)

        assertEquals(0.degrees, (360 * 2.0).degrees.normalized)
        assertEquals(0.0, (360 * 2.0).degrees.normalized.degrees)
        assertEquals(0.0, (360 * 2.0).degrees.normalized.radians)

        assertEquals((-90).degrees, -(90.degrees))
        assertEquals((+90).degrees, +(90.degrees))

        assertEquals(90.degrees, (-(90).degrees).absoluteValue)
        assertEquals(90.degrees, abs(-(90).degrees))

        assertEquals(0.degrees, 360.degrees.normalized)

        //assertEquals(90.degrees, 180.degrees - 90.degrees)
    }

    @Test
    fun sinCos() {
        //assertEquals(0.0f, cos(90.degrees))
        //assertEquals(1.0f, sin(90.degrees))
        //assertEquals(1.0f, tan(45.degrees))

        assertEquals(0.0, cos(90.degrees))
        assertEquals(1.0, sin(90.degrees))
        assertEquals(1.0, tan(45.degrees))
    }

    @Test
    fun between() {
        assertEquals(0.degrees, Angle.between(Point(0, 0), Point(10, 0)))
        assertEquals(90.degrees, Angle.between(Point(0, 0), Point(0, 10)))
        assertEquals(180.degrees, Angle.between(Point(0, 0), Point(-10, 0)))
        assertEquals(270.degrees, Angle.between(Point(0, 0), Point(0, -10)))

        assertEquals(0.degrees, Angle.between(100, 100, 110, 100))
        assertEquals(90.degrees, Angle.between(100, 100, 100, 110))
        assertEquals(180.degrees, Angle.between(100, 100, -110, 100))
        assertEquals(270.degrees, Angle.between(100, 100, 100, -110))
    }

    @Test
    fun shortDistance() {
        assertEquals(0.degrees, 0.degrees.shortDistanceTo(0.degrees))
        assertEquals((-10).degrees, 0.degrees.shortDistanceTo(350.degrees))
        assertEquals((+10).degrees, 0.degrees.shortDistanceTo(10.degrees))

        assertEquals(0.degrees, 0.degrees.longDistanceTo(0.degrees))
        assertEquals((+350).degrees, 0.degrees.longDistanceTo(350.degrees))
        assertEquals((-350).degrees, 0.degrees.longDistanceTo(10.degrees))
    }

    @Test
    fun inBetweenInclusive() {
        assertEquals(true, (-15).degrees inBetween ((-15).degrees .. 15.degrees))
        assertEquals(true, (+15).degrees inBetween ((-15).degrees .. 15.degrees))
        assertEquals(true, 0.degrees inBetween ((-15).degrees .. 15.degrees))
        assertEquals(true, 0.degrees inBetween (345.degrees .. 15.degrees))

        assertEquals(false, (-20).degrees inBetween ((-15).degrees .. 15.degrees))
        assertEquals(false, (+20).degrees inBetween ((-15).degrees .. 15.degrees))
        assertEquals(false, (-20).degrees inBetween (345.degrees .. 15.degrees))
        assertEquals(false, (+20).degrees inBetween (345.degrees .. 15.degrees))
    }

    @Test
    fun inBetweenExclusive() {
        assertEquals(true, (-15).degrees inBetween ((-15).degrees until 15.degrees))
        assertEquals(false, (+15).degrees inBetween ((-15).degrees until 15.degrees))
        assertEquals(true, 0.degrees inBetween ((-15).degrees until 15.degrees))
        assertEquals(true, 0.degrees inBetween (345.degrees until 15.degrees))

        assertEquals(false, (-20).degrees inBetween ((-15).degrees until 15.degrees))
        assertEquals(false, (+20).degrees inBetween ((-15).degrees until 15.degrees))
        assertEquals(false, (-20).degrees inBetween (345.degrees until 15.degrees))
        assertEquals(false, (+20).degrees inBetween (345.degrees until 15.degrees))
    }

    @Test
    fun testProperties() {
        //assertEquals(0.0f, 0.degrees.sineF)
        //assertEquals(1.0f, 0.degrees.cosineF)
        assertEquals(0.0, 0.degrees.sine)
        assertEquals(1.0, 0.degrees.cosine)
        assertTrue(0.degrees.tangent.isAlmostZero())
    }

    @Test
    fun testCompare() {
        assertTrue { 90.degrees == 90.degrees }
        assertTrue { 90.degrees <= 90.degrees }
        assertTrue { 90.degrees <= 100.degrees }
        assertTrue { 100.degrees >= 90.degrees }
    }

    @Test
    fun testFrom() {
        assertTrue { Angle.fromRadians(PI) == (Angle.fromDegrees(180.0)) }
        assertTrue { Angle.fromRadians(PI) == (Angle.fromDegrees(180.0)) }
        assertTrue { Angle.fromRatio(0.5) == (Angle.fromDegrees(180.0)) }
    }

    @Test
    fun testToString() {
        assertEquals("180.degrees", Angle.fromRatio(0.5).toString())
        assertEquals("180.degrees", PI.radians.toString())
        assertEquals("180.degrees", 180.degrees.toString())
    }

    // @TODO: Required to avoid: java.lang.AssertionError: expected:<3.141592653589793> but was:<Angle(180.0)>
    private fun assertEquals(a: Angle, b: Angle) {
        assertEquals(a.degrees, b.degrees)
    }

    private fun assertEquals(l: Double, r: Double, epsilon: Double = 0.0001) {
        assertTrue(abs(l - r) < epsilon, message = "$l != $r :: delta=$epsilon")
    }

    @Test
    fun testInterpolate() {
        //        0 360 -360
        //  -90 /+--+\
        // 270 |     | 90 -270
        //      \+--+/
        //        180
        //        -180

        assertEquals(202.5.degrees, Ratio(0.25).interpolateAngleNormalized(180.degrees, (-90).degrees))
        assertEquals(0.degrees, Ratio.HALF.interpolateAngleNormalized(350.degrees, (10).degrees))
        assertEquals(0.degrees, Ratio.HALF.interpolateAngleNormalized(10.degrees, (350).degrees))

        assertEquals(112.5.degrees, Ratio(0.25).interpolateAngleDenormalized(180.degrees, (-90).degrees))
        assertEquals(180.degrees, Ratio.HALF.interpolateAngleDenormalized(350.degrees, (10).degrees))
        assertEquals(180.degrees, Ratio.HALF.interpolateAngleDenormalized(10.degrees, (350).degrees))
    }

    @Test
    fun testReferenceSystem() {
        run {
            assertEqualsFloat(listOf(1.0, 0.0), listOf(Angle.ZERO.cosine(), Angle.ZERO.sine()))
            assertEqualsFloat(listOf(0.0, 1.0), listOf(Angle.QUARTER.cosine(), Angle.QUARTER.sine()))
            assertEqualsFloat(listOf(-1.0, 0.0), listOf(Angle.HALF.cosine(), Angle.HALF.sine()))
            assertEqualsFloat(listOf(0.0, -1.0), listOf(Angle.THREE_QUARTERS.cosine(), Angle.THREE_QUARTERS.sine()))
        }
        run {
            val up = Vector2D.UP
            assertEqualsFloat(listOf(1.0, 0.0), listOf(Angle.ZERO.cosine(up), Angle.ZERO.sine(up)))
            assertEqualsFloat(listOf(0.0, 1.0), listOf(Angle.QUARTER.cosine(up), Angle.QUARTER.sine(up)))
            assertEqualsFloat(listOf(-1.0, 0.0), listOf(Angle.HALF.cosine(up), Angle.HALF.sine(up)))
            assertEqualsFloat(listOf(0.0, -1.0), listOf(Angle.THREE_QUARTERS.cosine(up), Angle.THREE_QUARTERS.sine(up)))
        }
        run {
            val up = Vector2D.UP_SCREEN
            assertEqualsFloat(listOf(1.0, 0.0), listOf(Angle.ZERO.cosine(up), Angle.ZERO.sine(up)))
            assertEqualsFloat(listOf(0.0, -1.0), listOf(Angle.QUARTER.cosine(up), Angle.QUARTER.sine(up)))
            assertEqualsFloat(listOf(-1.0, 0.0), listOf(Angle.HALF.cosine(up), Angle.HALF.sine(up)))
            assertEqualsFloat(listOf(0.0, +1.0), listOf(Angle.THREE_QUARTERS.cosine(up), Angle.THREE_QUARTERS.sine(up)))
        }
        assertEqualsFloat(listOf(0.0, 1.0), listOf(Angle.QUARTER.cosine(), Angle.QUARTER.sine()))
        assertEqualsFloat(listOf(0.0, 1.0), listOf(Angle.QUARTER.cosine(Vector2D.UP), Angle.QUARTER.sine(Vector2D.UP)))
        assertEqualsFloat(listOf(0.0, -1.0), listOf(Angle.QUARTER.cosine(Vector2D.UP_SCREEN), Angle.QUARTER.sine(Vector2D.UP_SCREEN)))
    }

    @Test
    fun testNormalizeHalf() {
        assertEquals((-90).degrees, 270.degrees.normalizedHalf)
        assertEquals(180.degrees, 180.degrees.normalizedHalf)
        assertEquals((-179).degrees, 181.degrees.normalizedHalf)

        assertEquals(0.degrees, (-360).degrees.normalizedHalf)
        assertEquals(90.degrees, (-270).degrees.normalizedHalf)
        assertEquals(180.degrees, (-180).degrees.normalizedHalf)
        assertEquals((-90).degrees, (-90).degrees.normalizedHalf)
        assertEquals(0.degrees, 0.degrees.normalizedHalf)
        assertEquals(90.degrees, 90.degrees.normalizedHalf)
        assertEquals(180.degrees, 180.degrees.normalizedHalf)
        assertEquals((-90).degrees, 270.degrees.normalizedHalf)
        assertEquals(0.degrees, 360.degrees.normalizedHalf)
        assertEquals(0.degrees, 720.degrees.normalizedHalf)
    }

    @Test
    fun testNormalize() {
        assertEquals(0.degrees, (-360).degrees.normalized)
        assertEquals(90.degrees, (-270).degrees.normalized)
        assertEquals(180.degrees, (-180).degrees.normalized)
        assertEquals(270.degrees, (-90).degrees.normalized)
        assertEquals(0.degrees, 0.degrees.normalized)
        assertEquals(90.degrees, 90.degrees.normalized)
        assertEquals(180.degrees, 180.degrees.normalized)
        assertEquals(270.degrees, 270.degrees.normalized)
        assertEquals(0.degrees, 360.degrees.normalized)
        assertEquals(0.degrees, 720.degrees.normalized)
    }

    @Test
    fun testClamp() {
        assertEquals((-30).degrees, (-45).degrees.clamp((-30).degrees, 30.degrees))
        assertEquals((-30).degrees, (-30).degrees.clamp((-30).degrees, 30.degrees))
        assertEquals((-20).degrees, (-20).degrees.clamp((-30).degrees, 30.degrees))
        assertEquals(0.degrees, 0.degrees.clamp((-30).degrees, 30.degrees))
        assertEquals(15.degrees, 15.degrees.clamp((-30).degrees, 30.degrees))
        assertEquals(30.degrees, 30.degrees.clamp((-30).degrees, 30.degrees))
        assertEquals(30.degrees, 45.degrees.clamp((-30).degrees, 30.degrees))
    }
}
