package korlibs.korge.testing

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import org.junit.*

class KorgeScreenshotTest {
    // https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/arc
    @Test
    fun testArcShapes() = korgeScreenshotTest(SizeInt(200, 200), bgcolor = Colors.WHITE, logGl = false) {
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

                    ctx.arc(Point(x, y), radius.toFloat(), startAngle.radians, endAngle.radians, counterclockwise)

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
        SizeInt(512, 512),
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
            scale = 0.8
            position(350, 350)
        }

        assertScreenshot(rect2, "initial2", includeBackground = false)

        val rect3 = solidRect(150, 150, Colors.GREEN) {
            rotation = maxDegrees
            anchor(.5, .5)
            scale = 0.8
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

}
