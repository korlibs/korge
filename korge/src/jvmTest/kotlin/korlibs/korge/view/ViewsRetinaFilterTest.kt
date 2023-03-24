package korlibs.korge.view

import korlibs.korge.testing.*
import korlibs.korge.tests.*
import korlibs.korge.view.filter.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.math.geom.*
import kotlin.test.*

class ViewsRetinaFilterTest {
    @Test
    fun test() = korgeScreenshotTest(
        windowSize = SizeInt(100, 100),
        devicePixelRatio = 2.0,
    ) {
        val container = container {
            image(Bitmap32(50, 50, Colors.RED.premultiplied))
            //solidRect(512, 512, Colors.RED)
                .filters(
                    SwizzleColorsFilter("rrra"),
                    ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX),
                )
        }
        assertScreenshot()
    }
}