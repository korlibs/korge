package korlibs.korge

import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlin.test.*

class KorgeHeadlessTest {
    @Test
    fun test() = suspendTest {
        Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]).headless(draw = false) {
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
        val gameWindow = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]).headless(draw = true) {
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
