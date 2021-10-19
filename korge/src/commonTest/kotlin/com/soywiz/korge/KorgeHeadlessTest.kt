package com.soywiz.korge

import com.soywiz.klock.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlin.test.*

class KorgeHeadlessTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        KorgeHeadless(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
            val minDegrees = (-16).degrees
            val maxDegrees = (+16).degrees

            val image = solidRect(100, 100, Colors.RED) {
                rotation = maxDegrees
                anchor(.5, .5)
                scale(.8)
                position(256, 256)
            }

            while (true) {
                println("STEP")
                image.tween(image::rotation[minDegrees], time = 0.5.seconds, easing = Easing.EASE_IN_OUT)
                image.tween(image::rotation[maxDegrees], time = 0.5.seconds, easing = Easing.EASE_IN_OUT)
                views.gameWindow.close() // We close the window, finalizing the test here
            }
        }
    }

    @Test
    @Ignore
    fun testDraw() = suspendTest {
        val gameWindow = KorgeHeadless(width = 512, height = 512, bgcolor = Colors["#2b2b2b"], draw = true) {
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
        gameWindow.bitmap.showImageAndWait()
        println(gameWindow.bitmap)
    }
}
