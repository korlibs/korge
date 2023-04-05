package korlibs.korge.view

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.testing.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import org.junit.*

class ReferenceOpenglTest {
    @Test
    fun testOpengl() = korgeScreenshotTest(Size(100, 100)) {
        image(resourcesVfs["texture.png"].readBitmap().mipmaps())
        assertScreenshot(posterize = 6)
    }

    @Test
    fun testOpenglShapeView() = korgeScreenshotTest(Size(500, 500)) {
        container {
            xy(300, 300)
            val shape = gpuShapeView({
                //val lineWidth = 6.12123231 * 2
                val lineWidth = 12.0
                val width = 300.0
                val height = 300.0
                //rotation = 180.degrees
                this.stroke(Colors.WHITE.withAd(0.5), lineWidth = lineWidth, lineJoin = LineJoin.MITER, lineCap = LineCap.BUTT) {
                    this.rect(
                        lineWidth / 2, lineWidth / 2,
                        width,
                        height
                    )
                }
            }) {
                xy(-150, -150)
            }
        }
        assertScreenshot()
    }
}
