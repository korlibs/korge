package samples

import com.soywiz.klock.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.random.get
import kotlin.random.*

class MainBezier : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val shape = gpuShapeView(EmptyShape)
        val shape = graphics(EmptyShape, renderer = GraphicsRenderer.SYSTEM)
        //val shape = graphics(EmptyShape, renderer = GraphicsRenderer.GPU)
        fun getRandomPoint() = Point(Random[100..500], Random[100..500])
        class Bez {
            var p1: Point = getRandomPoint()
            var p2: Point = getRandomPoint()
            var p3: Point = getRandomPoint()
            var p4: Point = getRandomPoint()
        }
        val bez = Bez()

        addUpdater {
            shape.updateShape {
                //val curve = Bezier.Quad(bez.p1, bez.p2, bez.p3)
                val curve = Bezier(bez.p1, bez.p2, bez.p3, bez.p4)

                stroke(Colors.WHITE, lineWidth = 4.0) {
                    beginPath()
                    curve(curve)
                }

                stroke(Colors.DIMGREY, lineWidth = 1.6) {
                    moveTo(bez.p1)
                    lineTo(bez.p2)
                    lineTo(bez.p3)
                    lineTo(bez.p4)
                }
                stroke(Colors.PURPLE, lineWidth = 2.0) {
                    for (n in 0..50) {
                        val p = curve.calc(n.toDouble() / 50.0)
                        this.circle(p, 1f)
                    }
                }

                //stroke(Colors.YELLOW, lineWidth = 2.0) {
                fill(Colors.YELLOW) {
                    this.circle(bez.p1, 8f)
                    this.circle(bez.p2, 4f)
                    this.circle(bez.p1, 4f)
                    this.circle(bez.p3, 4f)
                    this.circle(bez.p4, 4f)
                }

                //fill(Colors.YELLOW) { this.circle(bez.p1, 8.0) }
                //fill(Colors.YELLOW) { this.circle(bez.p2, 4.0) }
                //fill(Colors.YELLOW) { this.circle(bez.p1, 4.0) }
                //fill(Colors.YELLOW) { this.circle(bez.p3, 4.0) }
                //fill(Colors.YELLOW) { this.circle(bez.p4, 4.0) }

                stroke(Colors.RED, lineWidth = 2.0) {
                    rect(curve.getBounds())
                }
            }
        }

        launch {
            while (true) {
                tween(
                    bez::p1[getRandomPoint()],
                    bez::p2[getRandomPoint()],
                    bez::p3[getRandomPoint()],
                    bez::p4[getRandomPoint()],
                    time = 1.seconds
                )
            }
        }

        /*
    run {
            val p0 = Point(109, 135)
            val p1 = Point(25, 190)
            val p2 = Point(210, 250)
            val p3 = Point(234, 49)
            val g = graphics()
            g.clear()
            g.stroke(Colors.DIMGREY, info = StrokeInfo(thickness = 1.0)) {
                moveTo(p0)
                lineTo(p1)
                lineTo(p2)
                lineTo(p3)
            }
            g.stroke(Colors.WHITE, info = StrokeInfo(thickness = 2.0)) {
                cubic(p0, p1, p2, p3)
            }
            val ratio = 0.3
            val cubic2 = Bezier.Cubic().setToSplitFirst(Bezier.Cubic(p0, p1, p2, p3), ratio)
            val cubic3 = Bezier.Cubic().setToSplitSecond(Bezier.Cubic(p0, p1, p2, p3), ratio)

            g.stroke(Colors.PURPLE, info = StrokeInfo(thickness = 4.0)) {
                cubic(cubic2)
            }
            g.stroke(Colors.YELLOW, info = StrokeInfo(thickness = 4.0)) {
                cubic(cubic3)
            }
            graphics {
                stroke(Colors.RED, StrokeInfo(thickness = 2.0)) {
                    rect(g.getLocalBounds())
                }
            }
        }
        return
         */
    }
}
