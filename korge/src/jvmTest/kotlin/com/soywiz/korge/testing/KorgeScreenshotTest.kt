package com.soywiz.korge.testing

import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import org.junit.*

class KorgeScreenshotTest {
    @Test
    fun test1() = korgeScreenshotTest(
        width = 512, height = 512,
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
