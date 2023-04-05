package korlibs.korge.view

import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.kgl.*
import korlibs.korge.testing.*
import korlibs.korge.tests.*
import korlibs.korge.view.filter.*
import korlibs.math.geom.*
import kotlin.test.*

class ViewFixedSizeContainerScreenshotTest : ViewsForTesting(
    windowSize = SizeInt(150, 150),
    virtualSize = SizeInt(150, 150),
    log = true
) {
    val glLog = KmlGlProxyLogToString()
    override fun createAg(): AG = AGOpengl(glLog)

    @Test
    fun testClipping() = korgeScreenshotTest(SizeInt(150, 150)) {
        fixedSizeContainer(Size(50, 50), clip = true) {
            image(Bitmap32(100, 100) { x, y -> if (y <= 25) Colors.RED else Colors.BLUE}.premultiplied())
            xy(25, 25)
        }
        assertScreenshot(useTexture = true)
        assertScreenshot(useTexture = false)
    }

    @Test
    fun testClippingMain() = viewsTest(forceRenderEveryFrame = false) {
        fixedSizeContainer(Size(50, 50), clip = true) {
            image(Bitmap32(100, 100) { x, y -> if (y <= 25) Colors.RED else Colors.BLUE}.premultiplied())
            xy(25, 25)
        }
        delayFrame()
        val logAsString = glLog.getLogAsString().lines().filter { it.startsWith("scissor") }
        assertTrue("$logAsString") { logAsString.contains("scissor(25, 75, 50, 50)") }
    }

    @Test
    fun testClippingNoMain() = viewsTest(forceRenderEveryFrame = false) {
        container {
            fixedSizeContainer(Size(50, 50), clip = true) {
                image(Bitmap32(100, 100) { x, y -> if (y <= 25) Colors.RED else Colors.BLUE}.premultiplied())
                xy(25, 25)
            }
            xy(111, 333)
        }.filters(IdentityFilter)
        delayFrame()
        val logAsString = glLog.getLogAsString().lines().filter { it.startsWith("scissor") }
        assertTrue("$logAsString") { logAsString.contains("scissor(0, 0, 50, 50)") }
    }
}
