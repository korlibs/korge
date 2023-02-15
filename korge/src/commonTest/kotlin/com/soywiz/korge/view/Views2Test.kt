package com.soywiz.korge.view

import com.soywiz.klogger.*
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.MPoint
import com.soywiz.korma.geom.MRectangle
import com.soywiz.korma.geom.ScaleMode
import com.soywiz.korma.geom.MSizeInt
import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Views2Test : ViewsForTesting(
    windowSize = MSizeInt(1280, 720),
    virtualSize = MSizeInt(640, 480)
) {
    val logger = Logger("Views2Test")

    fun str() = "window(${gameWindow.width},${gameWindow.height}),virtual(${views.virtualWidth},${views.virtualHeight}),stage(${stage.x},${stage.y},${stage.width},${stage.height},${stage.scaleX},${stage.scaleY})"

    @Test
    fun testScaleMode() = viewsTest {
        val lines = arrayListOf<String>()
        lines.add(str())
        resizeGameWindow(640, 480)
        lines.add(str())
        resizeGameWindow(1280, 720, scaleAnchor = Anchor.TOP_LEFT)
        lines.add(str())
        resizeGameWindow(1280, 720, scaleMode = ScaleMode.EXACT)
        lines.add(str())
        assertEquals(
            """
                window(1280,720),virtual(640,480),stage(0.0,0.0,640.0,480.0,1.0,1.0)
                window(640,480),virtual(640,480),stage(0.0,0.0,640.0,480.0,1.0,1.0)
                window(1280,720),virtual(640,480),stage(0.0,0.0,640.0,480.0,1.0,1.0)
                window(1280,720),virtual(640,480),stage(0.0,0.0,640.0,480.0,1.0,1.0)
            """.trimIndent(),
            lines.joinToString("\n")
        )
    }

    @Test
    fun testCenterOnStage() = viewsTest {
        val RECT_WIDTH = 100
        val RECT_HEIGHT = 70
        val rect = solidRect(RECT_WIDTH, RECT_HEIGHT, Colors.RED) {
            centerOnStage()
        }
        logger.info { str() }
        assertEquals(MPoint((virtualSize.width - RECT_WIDTH) / 2, (virtualSize.height - RECT_HEIGHT) / 2), rect.ipos)
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
            MPoint(
                CONTAINER_X + (CONTAINER_WIDTH - RECT_WIDTH) / 2,
                CONTAINER_Y + (CONTAINER_HEIGHT - RECT_HEIGHT) / 2
            ),
            rect2.ipos
        )
    }

    // @TODO: This should go into kotlin-test
    private fun assertEquals(expect: Double, actual: Double, epsilon: Double) {
        assertTrue { (expect - actual).absoluteValue <= epsilon }
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

        assertEquals(20.0, rect1.scaleX)
        assertEquals(6.66666, rect1.scaleY, 0.001)

        assertEquals(
            MPoint(
                CONTAINER_X + (CONTAINER_WIDTH - RECT_WIDTH) / 2,
                CONTAINER_Y + (CONTAINER_HEIGHT - RECT_HEIGHT) / 2
            ),
            rect2.ipos
        )
    }

    @Test
    fun testImageLocalBounds() = viewsTest {
        val image = image(Bitmap32(10, 10, Colors.TRANSPARENT_BLACK)).size(100, 100).xy(50, 50)
        assertEquals(MRectangle(0, 0, 10, 10).toStringBounds(), image.getLocalBoundsOptimizedAnchored().toStringBounds())
        assertEquals(MRectangle(50, 50, 100, 100).toStringBounds(), image.getBounds(image.parent!!).toStringBounds())
    }
}
