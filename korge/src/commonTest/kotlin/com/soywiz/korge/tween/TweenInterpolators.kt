package com.soywiz.korge.tween

import com.soywiz.korge.view.DummyView
import com.soywiz.korge.view.xy
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.shape.buildPath
import com.soywiz.korma.geom.vector.line
import kotlin.test.Test
import kotlin.test.assertEquals

class TweenInterpolators {
    @Test
    fun test() {
        val path = buildPath { line(0, 0, 100, 100) }
        val view = DummyView().xy(30, 30)
        val v2 = view::pos[path]
        assertEquals(Point(30, 30), view.pos)
        v2.init()
        v2.set(0.0)
        assertEquals(Point(0, 0), view.pos)
        v2.set(0.5)
        assertEquals(Point(50, 50), view.pos)
        v2.set(1.0)
        assertEquals(Point(100, 100), view.pos)
    }
}
