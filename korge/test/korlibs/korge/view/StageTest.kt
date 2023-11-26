package korlibs.korge.view

import korlibs.korge.input.mouse
import korlibs.korge.tests.ViewsForTesting
import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class StageTest : ViewsForTesting() {
    @Test
    fun test() {
        views.input.setMouseGlobalPos(Point(10.0, 20.0), down = false)
        stage.scale(0.5, 0.5)
        stage.mouse.currentPosGlobal
        assertEquals(Point(20, 40), stage.mousePos)
        assertEquals(Point(20, 40), views.globalMousePos)
    }
}
