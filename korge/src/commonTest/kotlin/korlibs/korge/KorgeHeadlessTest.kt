package korlibs.korge

import korlibs.time.seconds
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.korge.view.anchor
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.image.color.Colors
import korlibs.image.format.*
import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.*
import korlibs.math.interpolation.Easing
import kotlin.test.Ignore
import kotlin.test.Test

class KorgeHeadlessTest {
    @Test
    fun test() = suspendTest {
        Korge(windowSize = SizeInt(512, 512), bgcolor = Colors["#2b2b2b"]).headless(draw = false) {
            val minDegrees = (-16).degrees
            val maxDegrees = (+16).degrees

            val image = solidRect(100, 100, Colors.RED) {
                rotation = maxDegrees
                anchor(.5, .5)
                scale(.8)
                position(256, 256)
            }

            while (true) {
                //println("STEP")
                image.tween(image::rotation[minDegrees], time = 0.5.seconds, easing = Easing.EASE_IN_OUT)
                image.tween(image::rotation[maxDegrees], time = 0.5.seconds, easing = Easing.EASE_IN_OUT)
                views.gameWindow.close() // We close the window, finalizing the test here
            }
        }
    }

    @Test
    fun testDraw() = suspendTest {
        val gameWindow = Korge(windowSize = SizeInt(512, 512), bgcolor = Colors["#2b2b2b"]).headless(draw = true) {
            val bmp = resourcesVfs["korge.png"].readBitmap()
            repeat(1) { n ->
            //repeat(10) { n ->
                //solidRect(512, 512, Colors.GREEN) {
                image(bmp) {
                    //rotation = (45 + (n * 10)).degrees
                    rotation = (0 + (n * 10)).degrees
                    //rotation = 0.degrees
                    anchor(.5, .5)
                    scale(.5)
                    position(256, 256)
                    //position(256 + n * 10, 256 + n * 10)
                }
            }

            /*
            try {
                bmp = stage.renderToBitmap(views)
            } finally {
            }
            */
            gameWindow.frameRender()

            views.gameWindow.close() // We close the window, finalizing the test here
        }
        //gameWindow.bitmap.showImageAndWait()
        //println(gameWindow.bitmap)
    }
}