package com.soywiz.korge.input

import com.soywiz.korev.MouseButton
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.MMatrix
import com.soywiz.korma.geom.MPoint
import com.soywiz.korma.geom.MSizeInt
import kotlin.test.Test
import kotlin.test.assertEquals

class MouseDragComponentTest : ViewsForTesting(
    virtualSize = MSizeInt(100, 100),
    windowSize = MSizeInt(200, 200),
) {
    @Test
    fun testStageScale() = viewsTest {
        assertEquals(1.0, stage.scale)
        assertEquals(MMatrix(), stage.localMatrix)
        assertEquals(1.0, views.devicePixelRatio)
        assertEquals(0.5, views.windowToGlobalScaleX)
        assertEquals(2.0, views.globalToWindowScaleX)
    }

    @Test
    fun testMouseCoords() = viewsTest {
        val rect = solidRect(100, 100, Colors.RED)
        rect.draggable()
        val deltaX = 20
        val deltaY = 10
        mouseMoveTo(10, 10)
        assertEquals(MPoint(10, 10), views.globalMouseXY)
        assertEquals(MPoint(20, 20), views.windowMouseXY)
        mouseDown(MouseButton.LEFT)
        mouseMoveTo(10 + deltaX, 10 + deltaY)
        assertEquals(MPoint(30, 20), views.globalMouseXY)
        assertEquals(MPoint(60, 40), views.windowMouseXY)
        mouseUp(MouseButton.LEFT)
        assertEquals(MPoint(deltaX, deltaY), rect.ipos)
    }
}
