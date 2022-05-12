package com.soywiz.korge.view

import com.soywiz.korge.input.*
import com.soywiz.korge.tests.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class StageTest : ViewsForTesting() {
    @Test
    fun test() {
        views.input.setMouseGlobalXY(10.0, 20.0, down = false)
        stage.scale(0.5, 0.5)
        stage.mouse.currentPosGlobal
        assertEquals(Point(20, 40), stage.mouseXY)
        assertEquals(Point(20, 40), views.globalMouseXY)
    }
}
