package korlibs.korge.input

import korlibs.event.*
import korlibs.image.color.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.test.*

class MouseDragComponentTest : ViewsForTesting(
    virtualSize = Size(100, 100),
    windowSize = Size(200, 200),
) {
    @Test
    fun testStageScale() = viewsTest {
        assertEquals(1.0, stage.scale)
        assertEquals(Matrix.IDENTITY, stage.localMatrix)
        assertEquals(1.0, views.devicePixelRatio)
        assertEquals(.5, views.windowToGlobalScaleX)
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
