package korlibs.korge.view

import korlibs.image.color.*
import korlibs.korge.testing.*
import korlibs.math.geom.*
import org.junit.*

class SolidTriangleTest {

  @Test
  fun test() = korgeScreenshotTest(Size(200, 200)) {
    addChild(
      solidTriangle(Point(10, 0), Point(0, 50), Point(110, 70), Colors.ANTIQUEWHITE)
        .position(100, 10)
        .rotation(30.degrees)
    )

    assertScreenshot(posterize = 5)
  }
}
