package com.soywiz.korge.view

import com.soywiz.korge.input.mouse
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korma.geom.MPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class StageTest : ViewsForTesting() {
    @Test
    fun test() {
        views.input.setMouseGlobalXY(10.0, 20.0, down = false)
        stage.scale(0.5, 0.5)
        stage.mouse.currentPosGlobal
        assertEquals(MPoint(20, 40), stage.mouseXY)
        assertEquals(MPoint(20, 40), views.globalMouseXY)
    }
}
