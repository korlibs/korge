package samples

import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import kotlin.reflect.*

class MainBezierSample : Scene() {
    var p0 = Point(109, 135)
    var p1 = Point(25, 190)
    var p2 = Point(210, 250)
    var p3 = Point(234, 49)

    override suspend fun SContainer.sceneMain() {
        val graphics = cpuGraphics(autoScaling = true)
        val graphics2 = gpuShapeView().xy(0, 300)

        fun ShapeBuilder.updateBezier() {
            stroke(Colors.DIMGREY, info = StrokeInfo(thickness = 1.0)) {
                moveTo(p0)
                lineTo(p1)
                lineTo(p2)
                lineTo(p3)
            }
            stroke(Colors.WHITE, info = StrokeInfo(thickness = 2.0)) {
                cubic(p0, p1, p2, p3)
            }
            val ratio = 0.3f
            val split = Bezier(p0, p1, p2, p3).split(ratio.toRatio())
            val cubic2 = split.leftCurve
            val cubic3 = split.rightCurve

            stroke(Colors.PURPLE, info = StrokeInfo(thickness = 4.0)) {
                curve(cubic2)
            }
            stroke(Colors.YELLOW, info = StrokeInfo(thickness = 4.0)) {
                curve(cubic3)
            }
        }

        fun updateGraphics() {
            graphics.updateShape {
                updateBezier()
            }
            graphics2.updateShape {
                updateBezier()
            }
        }

        updateGraphics()
        createPointController(this@MainBezierSample::p0, Colors.RED) { updateGraphics() }
        createPointController(this@MainBezierSample::p1, Colors.GREEN) { updateGraphics() }
        createPointController(this@MainBezierSample::p2, Colors.BLUE) { updateGraphics() }
        createPointController(this@MainBezierSample::p3, Colors.YELLOW) { updateGraphics() }
    }

    fun Container.createPointController(pointRef: KMutableProperty0<Point>, color: Paint, onMove: () -> Unit) {
        lateinit var circle: View
        lateinit var text: Text
        val anchorView = container {
            circle = circle(6.0, fill = color, stroke = Colors.DARKGRAY, strokeThickness = 2.0).centered
            text = text("", 10.0).position(10.0, 6.0)
        }.position(pointRef.get())

        fun updateText() {
            text.text = "(${anchorView.x.toInt()}, ${anchorView.y.toInt()})"
        }
        circle.mouse {
            onOver { circle.filter = ColorTransformFilter(ColorTransform(add = ColorAdd(+64, +64, +64, 0))) }
            onOut { circle.filter = null }
        }
        updateText()
        anchorView.draggable(circle) {
            pointRef.set(anchorView.pos)
            updateText()
            onMove()
        }
    }
}
