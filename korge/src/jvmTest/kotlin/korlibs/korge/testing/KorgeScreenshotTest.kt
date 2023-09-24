package korlibs.korge.testing

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import org.junit.*

class KorgeScreenshotTest {
    // https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/arc
    @Test
    fun testArcShapes() = korgeScreenshotTest(Size(200, 200), bgcolor = Colors.WHITE, logGl = false) {
        image(NativeImageContext2d(200, 200) {
            val ctx = this
            // Draw shapes
            for (i in 0..3) {
                for (j in 0..2) {
                    ctx.beginPath()
                    val x = 25+j * 50 // x coordinate
                    val y = 25+i * 50 // y coordinate
                    val radius = 20 // Arc radius
                    val startAngle = 0 // Starting point on circle
                    val endAngle = kotlin.math.PI +(kotlin.math.PI * j) / 2 // End point on circle
                    val counterclockwise = i % 2 == 1 // Draw counterclockwise

                    ctx.arc(Point(x, y), radius.toDouble(), startAngle.radians, endAngle.radians, counterclockwise)

                    if (i > 1) {
                        ctx.fill()
                    } else {
                        ctx.stroke()
                    }
                }
            }
        })

        assertScreenshot(this, "shapes", posterize = 7)
    }

    @Test
    fun test1() = korgeScreenshotTest(
        Size(512, 512),
        bgcolor = Colors.RED
    ) {
        val maxDegrees = (+16).degrees

        val rect1 = solidRect(100, 100, Colors.RED) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale = 0.8
            position(200, 200)
        }

        assertScreenshot(this, "initial1")

        val rect2 = solidRect(150, 150, Colors.YELLOW) {
            rotation = maxDegrees
            anchor(.5, .5)
            scaleAvg = 0.8
            position(350, 350)
        }

        assertScreenshot(rect2, "initial2", includeBackground = false)

        val rect3 = solidRect(150, 150, Colors.GREEN) {
            rotation = maxDegrees
            anchor(.5, .5)
            scaleAvg = 0.8
            position(100, 350)
        }

        assertScreenshot(this, "initial3", includeBackground = false)

        //val rectContainer = container {
        //    val a = 100
        //    solidRect(a, a, Colors.BROWN)
        //    solidRect(a / 2, a / 2, Colors.YELLOW)
        //}
        //assertScreenshot(rectContainer, "initial4")
    }

    @Test
    fun testContextLost() = korgeScreenshotTest(
        Size(32, 32),
        bgcolor = Colors.RED
    ) {
        val rect1 = solidRect(16, 16, Colors.GREEN).position(0, 0)
        image(Bitmap32(16, 16, Colors.BLUE).premultipliedIfRequired()).position(16, 0)
        assertScreenshot()
        simulateContextLost()
        assertScreenshot()
    }

    @Test
    fun testContextLost2() = korgeScreenshotTest(
        Size(32, 32),
        bgcolor = Colors.RED
    ) {
        val rect = solidRect(16, 16, Colors.GREEN).position(0, 0)
        //for (n in 0 until 32) solidRect(16, 16, Colors.GREEN.withR(n * 7)).position(n, 0)
        //assertScreenshot()
        //println("[1]")
        simulateRenderFrame()
        //println("[2]")
        simulateContextLost()
        //println("[3]")
        val STEPS = 8
        for (n in 0 .. STEPS) {
            simulateRenderFrame()
            rect.x = (STEPS - n.toFloat()).toDouble()
        }
        //println("[4]")
        assertScreenshot()
        //println("[5]")
    }

    @Test
    fun testMipmaps() = korgeScreenshotTest(
        Size(32, 32),
        bgcolor = Colors.BLUE
    ) {
        val bmp = Bitmap32(128, 128).context2d(antialiased = false) {
            stroke(Colors.RED, lineWidth = 24f) {
                circle(Point(64, 64), 45.0)
            }
        }
        val bmp1 = bmp.clone()
        val bmp2 = bmp.clone().mipmaps(enable = true)

        image(bmp1).scale(0.125).xy(0, 0)
        image(bmp2).scale(0.125).xy(16, 0)

        assertScreenshot(posterize = 4)
    }

    val DIFF_BY_PIXELS_SETTINGS = KorgeScreenshotValidationSettings(
        listOf(AbsolutePixelDifferenceValidator(1000))
    )

    @Test
    fun test12() = korgeScreenshotTestV2(
        Korge(
            windowSize = Size(512, 512),
            virtualSize = Size(512, 512),
            backgroundColor = Colors.LIGHTGRAY
        ),
    ) {
        val maxDegrees = (+16).degrees

        val rect1 = solidRect(100, 100, Colors.RED) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale = 0.8
            position(200, 200)
        }

        it.recordGolden(this, "initial1")

        val rect2 = solidRect(150, 150, Colors.YELLOW) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale = 0.8
            position(350, 350)
        }

        it.recordGolden(rect2, "initial2")

        val rect3 = solidRect(150, 150, Colors.GREEN) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale = 0.8
            position(100, 350)
        }

        it.recordGolden(this, "initial3")

        val rectContainer = container {
            val a = 100
            solidRect(a, a, Colors.BROWN)
            solidRect(a / 2, a / 2, Colors.YELLOW)
        }

        it.recordGolden(rectContainer, "initial4", DIFF_BY_PIXELS_SETTINGS)

        it.endTest()
    }
}
