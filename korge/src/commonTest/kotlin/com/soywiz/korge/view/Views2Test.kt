package com.soywiz.korge.view

import com.soywiz.korge.tests.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class Views2Test : ViewsForTesting(
    windowSize = SizeInt(1280, 720),
    virtualSize = SizeInt(640, 480)
) {
    @Test
    fun testScaleMode() = viewsTest {
        fun str() = "window(${gameWindow.width},${gameWindow.height}),virtual(${views.virtualWidth},${views.virtualHeight}),stage(${stage.x},${stage.y},${stage.width},${stage.height},${stage.scaleX},${stage.scaleY})"
        assertEquals("window(1280,720),virtual(640,480),stage(159.0,0.0,640.0,480.0,1.5,1.5)", str())
        resizeGameWindow(640, 480)
        assertEquals("window(640,480),virtual(640,480),stage(0.0,0.0,640.0,480.0,1.0,1.0)", str())
        resizeGameWindow(1280, 720, scaleAnchor = Anchor.TOP_LEFT)
        assertEquals("window(1280,720),virtual(640,480),stage(0.0,0.0,640.0,480.0,1.5,1.5)", str())
        resizeGameWindow(1280, 720, scaleMode = ScaleMode.EXACT)
        assertEquals("window(1280,720),virtual(640,480),stage(0.0,0.0,640.0,480.0,2.0,1.5)", str())
    }
}
