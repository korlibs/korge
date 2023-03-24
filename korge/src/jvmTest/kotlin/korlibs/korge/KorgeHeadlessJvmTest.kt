package korlibs.korge

import korlibs.logger.*
import korlibs.korge.testing.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.io.lang.*
import korlibs.math.geom.*
import org.junit.Test
import kotlin.test.*

class KorgeHeadlessJvmTest {
    val logger = Logger("KorgeHeadlessJvmTest")

    @Test
    fun testHeadlessTest() {
        if (Environment["DISABLE_HEADLESS_TEST"] == "true") return
        var wasCalled = false
        logger.info { "1" }
        korgeScreenshotTest(windowSize = SizeInt(256, 256), bgcolor = Colors["#2b2b2b"]) {
            val image = solidRect(100, 100, Colors.RED) {
                rotation = 16.degrees
                anchor(.5, .5)
                scale(.8)
                position(128, 128)
            }
            logger.info { "2" }
            val bmp = renderToBitmap(includeBackground = true)
            logger.info { "3" }
            assertEquals(views.clearColor, bmp[0, 0])
            assertEquals(Colors.RED, bmp[128, 128])
            logger.info { "4" }
            //bmp.showImageAndWait()
            //assertEquals()
            wasCalled = true
        }
        logger.info { "5" }
        assertEquals(true, wasCalled)
    }
}