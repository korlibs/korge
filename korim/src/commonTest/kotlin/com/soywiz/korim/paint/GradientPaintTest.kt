package com.soywiz.korim.paint

import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class GradientPaintTest {
    @Test
    fun testLinear() {
        // FILL: (300,20)-(500,220)

        run {
            val filler = GradientFiller().set(
                LinearGradientPaint(0.0, 0.0, 100.0, 100.0, transform = Matrix().scale(0.5).pretranslate(300, 0)),
                Context2d.State(transform = Matrix(a=2.0, b=0.0, c=0.0, d=2.0, tx=100.0, ty=20.0))
            )
            assertEquals(-0.5, filler.getRatio(300.0, 20.0), absoluteTolerance = 0.1)
            assertEquals(1.5, filler.getRatio(500.0, 220.0), absoluteTolerance = 0.1)
        }
        run {
            val filler = GradientFiller().set(
                LinearGradientPaint(150.0, 0.0, 200.0, 50.0),
                Context2d.State(transform = Matrix(a=2.0, b=0.0, c=0.0, d=2.0, tx=100.0, ty=20.0))
            )
            assertEquals(-0.5, filler.getRatio(300.0, 20.0), absoluteTolerance = 0.1)
            assertEquals(1.5, filler.getRatio(500.0, 220.0), absoluteTolerance = 0.1)
        }
    }

    @Test
    fun testRadial() {
        run {
            val filler = GradientFiller().set(
                RadialGradientPaint(150,150,30, 130,180,70),
                Context2d.State(transform = Matrix(a=2.0, b=0.0, c=0.0, d=2.0, tx=100.0, ty=20.0))
            )
            assertEquals(2.038292349534667, filler.getRatio(300.0, 220.0), absoluteTolerance = 0.01)
            assertEquals(-0.39444872453601043, filler.getRatio(400.0, 320.0), absoluteTolerance = 0.01)
            assertEquals(-0.02180112465690942, filler.getRatio(450.0, 350.0), absoluteTolerance = 0.01)
            assertEquals(-0.0502161423675096, filler.getRatio(420.0, 370.0), absoluteTolerance = 0.01)
            assertEquals(0.10288401556160343, filler.getRatio(420.0, 390.0), absoluteTolerance = 0.01)
            assertEquals(0.156021135134224, filler.getRatio(410.0, 400.0), absoluteTolerance = 0.01)
            assertEquals(0.24242076091726772, filler.getRatio(415.0, 410.0), absoluteTolerance = 0.01)
            assertEquals(1.099261043394813, filler.getRatio(500.0, 420.0), absoluteTolerance = 0.01)
        }
    }
}
