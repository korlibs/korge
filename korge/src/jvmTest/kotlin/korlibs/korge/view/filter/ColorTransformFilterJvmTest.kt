package korlibs.korge.view.filter

import korlibs.image.color.*
import korlibs.korge.testing.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.test.*

class ColorTransformFilterJvmTest {
    @Test
    fun test() = korgeScreenshotTest(Size(30, 30)) {
        val rect = solidRect(10, 10, Colors.DARKGRAY).xy(10, 10)
        rect.filter = ColorTransformFilter(ColorTransform(add = ColorAdd(+127, 0, +127, +255)))
        assertScreenshot()
    }
}
