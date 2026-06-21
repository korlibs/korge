package korlibs.korge.tween

import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.interpolation.*
import kotlin.test.*

class TweenInterpolators {
    @Test
    fun test() {
        val path = buildVectorPath { line(Point(0, 0), Point(100, 100)) }
        val view = DummyView().xy(30, 30)
        val v2 = view::pos[path]
        assertEquals(Point(30, 30), view.pos)
        v2.init()
        v2.set(0.0)
        assertEquals(Point(0, 0), view.pos)
        v2.set(0.5)
        assertEquals(Point(50, 50), view.pos.round())
        v2.set(1.0)
        assertEquals(Point(100, 100), view.pos.round())
    }

    fun V2<*>.set(ratio: Double): Unit = set(ratio.toRatio())
}
