package korlibs.korge.view

import korlibs.image.color.*
import korlibs.korge.testing.*
import korlibs.math.geom.*
import org.junit.*

class SolidTriangleTest {

  @Test
  fun test() = korgeScreenshotTest(Size(200, 200)) {
    val triangle = solidTriangle(Point(10, 0), Point(0, 50), Point(110, 70), Colors.ANTIQUEWHITE)
      .position(100, 10)
      .rotation(30.degrees)
    addChild(triangle)
    assertScreenshot(posterize = 5)

    triangle.p1 += Point(10, 10)
    triangle.p2 += Point(20, 20)
    triangle.p3 -= Point(30, 30)
    triangle.colorMul = Colors.YELLOW
    assertScreenshot(posterize = 5)
  }
}
