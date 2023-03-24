package korlibs.korge.input

import korlibs.event.MouseButton
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.solidRect
import korlibs.image.color.Colors
import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MouseDragComponentTest : ViewsForTesting(
    virtualSize = SizeInt(100, 100),
    windowSize = SizeInt(200, 200),
) {
    @Test
    fun testStageScale() = viewsTest {
        assertEquals(1.0, stage.scale)
        assertEquals(Matrix.IDENTITY, stage.localMatrix)
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
        assertEquals(Point(10, 10), views.globalMousePos)
        assertEquals(Point(20, 20), views.windowMousePos)
        mouseDown(MouseButton.LEFT)
        mouseMoveTo(10 + deltaX, 10 + deltaY)
        assertEquals(Point(30, 20), views.globalMousePos)
        assertEquals(Point(60, 40), views.windowMousePos)
        mouseUp(MouseButton.LEFT)
        assertEquals(Point(deltaX, deltaY), rect.pos)
    }
}