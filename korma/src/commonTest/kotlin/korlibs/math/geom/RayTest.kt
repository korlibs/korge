package korlibs.math.geom

import kotlin.test.*

class RayTest {
    val ray = Ray(Point(100, 100), Vector2(+1, 0))

    @Test
    fun testTransformed() {
        assertEqualsFloat(Ray(Point(200, 100), Vector2(+1, 0)), ray.transformed(Matrix.IDENTITY.translated(100, 0)))
        assertEqualsFloat(Ray(Point(-100, 100), Vector2(0f, +1f)), ray.transformed(Matrix.IDENTITY.rotated(90.degrees)))
    }

    @Test
    fun testToLine() {
        assertEqualsFloat(Line(Point(100, 100), Point(200f, 100f)), ray.toLine(100f))
    }
}
