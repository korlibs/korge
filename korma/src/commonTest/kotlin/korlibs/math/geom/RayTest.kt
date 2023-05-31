package korlibs.math.geom

import kotlin.test.*

class RayTest {
    val ray = Ray(Point(100, 100), Vector2(+1, 0))

    @Test
    fun testConstructAngle() {
        Ray(Point(1, 1), 0.degrees).also { ray ->
            assertEquals(Point(1, 1), ray.point)
            assertEquals(Vector2(1, 0), ray.direction)
        }
        Ray(Point(1, 1), 90.degrees).also { ray ->
            assertEquals(Point(1, 1), ray.point)
            assertEquals(Vector2(0, 1), ray.direction)
        }
    }

    @Test
    fun testTransformed() {
        assertEqualsFloat(Ray(Point(200, 100), Vector2(+1, 0)), ray.transformed(Matrix.IDENTITY.translated(100, 0)))
        assertEqualsFloat(Ray(Point(-100, 100), Vector2(0f, +1f)), ray.transformed(Matrix.IDENTITY.rotated(90.degrees)))
    }

    @Test
    fun testToLine() {
        assertEqualsFloat(Line(Point(100, 100), Point(200f, 100f)), ray.toLine(100f))
    }

    @Test
    fun testFromTwoPoints() {
        Ray.fromTwoPoints(Point(1, 0), Point(3, 0)).also { ray ->
            assertEquals(Point(1, 0), ray.point)
            assertEquals(Vector2(1, 0), ray.direction)
        }
        Ray.fromTwoPoints(Point(10, 10), Point(10, 7)).also { ray ->
            assertEquals(Point(10, 10), ray.point)
            assertEquals(Vector2(0, -1), ray.direction)
        }
        Ray.fromTwoPoints(Point(1, 1), Point(3, 3)).also { ray ->
            assertEquals(Point(1, 1), ray.point)
            assertEquals(Vector2Int(7, 7), (ray.direction * 10).toInt())
        }
    }
}
