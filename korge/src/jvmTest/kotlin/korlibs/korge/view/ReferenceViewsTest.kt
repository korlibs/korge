package korlibs.korge.view

import korlibs.korge.testing.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.math.geom.*
import org.junit.*

class ReferenceViewsTest {
    @Test
    fun testClippedContainerInFlippedContainerInTexture() = korgeScreenshotTest(SizeInt(512, 512)) {
        container {
            yD = views.virtualHeightDouble; scaleYD = -1.0
            clipContainer(150, 100) {
                xy(75, 50)
                image(Bitmap32(300, 400) { x, y -> if (y <= 25) Colors.BLUE else Colors.RED }.premultiplied())
            }
        }
        assertScreenshot()
    }
}
