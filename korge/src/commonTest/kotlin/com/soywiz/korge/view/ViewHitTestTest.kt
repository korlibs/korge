package com.soywiz.korge.view

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.ui.uiObservable
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.div
import com.soywiz.korma.geom.minus
import com.soywiz.korma.geom.sine
import com.soywiz.korma.geom.times
import com.soywiz.korma.geom.vector.circle
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewHitTestTest : ViewsForTesting() {
    @Test
    fun testShape() = viewsTest{
        val circleB = solidRect(128.0, 128.0, Colors.RED).anchor(Anchor.MIDDLE_CENTER)
            .position(256, 256)
            .hitShape { circle(64.0, 64.0, 64.0) }

        assertEquals(true, circleB.hitTestAny(256.0, 256.0))
        assertEquals(true, circleB.hitTestAny(200.0, 256.0))
        assertEquals(true, circleB.hitTestAny(300.0, 213.0))
        assertEquals(false, circleB.hitTestAny(306.0, 205.0))
    }

    @Test
    fun test() = viewsTest{
        val circleB = solidRect(128.0, 128.0, Colors.RED).anchor(Anchor.MIDDLE_CENTER)
            .position(256, 256)

        assertEquals(true, circleB.hitTestAny(256.0, 256.0))
        assertEquals(true, circleB.hitTestAny(200.0, 256.0))
        assertEquals(true, circleB.hitTestAny(300.0, 213.0))
        assertEquals(true, circleB.hitTestAny(306.0, 205.0))
        assertEquals(false, circleB.hitTestAny(322.0, 205.0))
    }
}


inline fun Container.polygon(
    radius: Double = 16.0,
    sides: Int = 5,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true,
    callback: Polygon.() -> Unit = {}
): Polygon = Polygon(radius, sides, color, autoScaling).addTo(this, callback)

class Polygon(
    radius: Double = 16.0,
    sides: Int = 5,
    color: RGBA = Colors.WHITE,
    autoScaling: Boolean = true
) : CpuGraphics(autoScaling = autoScaling) {
    /** Radius of the circle */
    var radius: Double by uiObservable(radius) { updateGraphics() }

    /** Number of sides of the polygon */
    var sides: Int by uiObservable(sides) { updateGraphics() }

    /** Color of the circle. Internally it uses the [colorMul] property */
    var color: RGBA
        get() = colorMul
        set(value) { colorMul = value }

    //override val bwidth get() = radius * 2
    //override val bheight get() = radius * 2

    init {
        this.color = color
        updateGraphics()
    }

    private fun updateGraphics() {
        val polygon = this
        updateShape {
            fill(Colors.WHITE) {
                for (n in 0 until polygon.sides) {
                    val angle = ((360.degrees * n) / polygon.sides) - 90.degrees
                    val x = polygon.radius * angle.cosine
                    val y = polygon.radius * angle.sine
                    //println("$x, $y")
                    if (n == 0) {
                        moveTo(x, y)
                    } else {
                        lineTo(x, y)
                    }
                }
                close()
            }
        }
    }
}
