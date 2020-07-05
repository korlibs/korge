package com.soywiz.korge.view

import com.soywiz.korge.tests.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class Views2Test : ViewsForTesting(
    windowSize = SizeInt(1280, 720),
    virtualSize = SizeInt(640, 480)
) {
    fun str() = "window(${gameWindow.width},${gameWindow.height}),virtual(${views.virtualWidth},${views.virtualHeight}),stage(${stage.x},${stage.y},${stage.width},${stage.height},${stage.scaleX},${stage.scaleY})"

    @Test
    fun testScaleMode() = viewsTest {
        assertEquals("window(1280,720),virtual(640,480),stage(160.0,0.0,640.0,480.0,1.5,1.5)", str())
        resizeGameWindow(640, 480)
        assertEquals("window(640,480),virtual(640,480),stage(0.0,0.0,640.0,480.0,1.0,1.0)", str())
        resizeGameWindow(1280, 720, scaleAnchor = Anchor.TOP_LEFT)
        assertEquals("window(1280,720),virtual(640,480),stage(0.0,0.0,640.0,480.0,1.5,1.5)", str())
        resizeGameWindow(1280, 720, scaleMode = ScaleMode.EXACT)
        assertEquals("window(1280,720),virtual(640,480),stage(0.0,0.0,640.0,480.0,2.0,1.5)", str())
    }

    @Test
    fun testCenterOnStage() = viewsTest {
        val RECT_WIDTH = 100
        val RECT_HEIGHT = 70
        val rect = solidRect(RECT_WIDTH, RECT_HEIGHT, Colors.RED) {
            centerOnStage()
        }
        println(str())
        assertEquals(Point((virtualSize.width - RECT_WIDTH) / 2, (virtualSize.height - RECT_HEIGHT) / 2), rect.pos)
    }

    @Test
    fun testCenterOnSibling() = viewsTest {
        val CONTAINER_WIDTH = 300
        val CONTAINER_HEIGHT = 100
        val CONTAINER_X = 200
        val CONTAINER_Y = 200
        val RECT_WIDTH = 70
        val RECT_HEIGHT = 50

        val rect1 = solidRect(CONTAINER_WIDTH, CONTAINER_HEIGHT, Colors.RED).xy(CONTAINER_X, CONTAINER_Y)
        val rect2 = solidRect(RECT_WIDTH, RECT_HEIGHT, Colors.RED) {
            centerOn(rect1)
        }
        assertEquals(
            Point(
                CONTAINER_X + (CONTAINER_WIDTH - RECT_WIDTH) / 2,
                CONTAINER_Y + (CONTAINER_HEIGHT - RECT_HEIGHT) / 2
            ),
            rect2.pos
        )
    }


    @Test
    fun testImageCenterOnSibling() = viewsTest {
        val CONTAINER_WIDTH = 300
        val CONTAINER_HEIGHT = 100
        val CONTAINER_X = 200
        val CONTAINER_Y = 200
        val RECT_WIDTH = 70
        val RECT_HEIGHT = 50

        val bmp = Bitmap32(15, 15, Colors.RED)

        val rect1 = image(bmp).size(CONTAINER_WIDTH, CONTAINER_HEIGHT).xy(CONTAINER_X, CONTAINER_Y)
        val rect2 = image(bmp).size(RECT_WIDTH, RECT_HEIGHT).apply {
            centerOn(rect1)
        }
        assertEquals(
            Point(
                CONTAINER_X + (CONTAINER_WIDTH - RECT_WIDTH) / 2,
                CONTAINER_Y + (CONTAINER_HEIGHT - RECT_HEIGHT) / 2
            ),
            rect2.pos
        )
    }
}
