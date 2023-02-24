package com.soywiz.korge.tween

import com.soywiz.korge.view.DummyView
import com.soywiz.korge.view.xy
import com.soywiz.korma.geom.MPoint
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.line
import kotlin.test.Test
import kotlin.test.assertEquals

class TweenInterpolators {
    @Test
    fun test() {
        val path = buildVectorPath { line(0, 0, 100, 100) }
        val view = DummyView().xy(30, 30)
        val v2 = view::ipos[path]
        assertEquals(MPoint(30, 30), view.ipos)
        v2.init()
        v2.set(0.0)
        assertEquals(MPoint(0, 0), view.ipos)
        v2.set(0.5)
        assertEquals(MPoint(50, 50), view.ipos.mutable.round())
        v2.set(1.0)
        assertEquals(MPoint(100, 100), view.ipos.mutable.round())
    }
}
