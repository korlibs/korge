package korlibs.korge.view.filter

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.korge.testing.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.test.*

class DitherFilterJvmTest {
    @Test
    fun test() = korgeScreenshotTest(Size(32, 32)) {
        val container = container { }
            .filters(DitheringFilter(2.0))

        container.image(Bitmap32Context2d(32, 32) {
            fill(createLinearGradient(0, 0, 32, 32).add(Colors.RED, Colors.BLUE)) {
                rect(0, 0, 32, 32)
            }
        })

        assertScreenshot()
    }
}
