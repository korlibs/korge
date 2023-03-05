package com.soywiz.korge.view

import com.soywiz.korge.input.mouse
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korma.geom.*
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
