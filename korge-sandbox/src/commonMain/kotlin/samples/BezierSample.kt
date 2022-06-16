package samples

import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.geom.vector.StrokeInfo

class BezierSample : Scene() {
    override suspend fun Container.sceneMain() {
        val p0 = Point(109, 135)
        val p1 = Point(25, 190)
        val p2 = Point(210, 250)
        val p3 = Point(234, 49)

        val graphics = sgraphics {
            useNativeRendering = true
            //useNativeRendering = false
        }

        fun updateGraphics() {
            graphics.clear()
            graphics.stroke(Colors.DIMGREY, info = StrokeInfo(thickness = 1.0)) {
                moveTo(p0)
                lineTo(p1)
                lineTo(p2)
                lineTo(p3)
            }
            graphics.stroke(Colors.WHITE, info = StrokeInfo(thickness = 2.0)) {
                cubic(p0, p1, p2, p3)
            }
            var ratio = 0.3
            val cubic2 = Bezier(p0, p1, p2, p3).split(ratio).leftCurve
            val cubic3 = Bezier(p0, p1, p2, p3).split(ratio).rightCurve

            graphics.stroke(Colors.PURPLE, info = StrokeInfo(thickness = 4.0)) {
                cubic(cubic2)
            }
            graphics.stroke(Colors.YELLOW, info = StrokeInfo(thickness = 4.0)) {
                cubic(cubic3)
            }
            //println("graphics.globalBounds=${graphics.globalBounds}, graphics.localBounds=${graphics.getLocalBounds()}")
        }

        updateGraphics()
        createPointController(p0, Colors.RED) { updateGraphics() }
        createPointController(p1, Colors.GREEN) { updateGraphics() }
        createPointController(p2, Colors.BLUE) { updateGraphics() }
        createPointController(p3, Colors.YELLOW) { updateGraphics() }
    }

    fun Container.createPointController(point: Point, color: Paint, onMove: () -> Unit) {
        lateinit var circle: View
        lateinit var text: Text
        val anchorView = container {
            circle = circle(6.0, fill = color, stroke = Colors.DARKGRAY, strokeThickness = 2.0).centered
            text = text("", 10.0).position(10.0, 6.0)
        }.position(point)

        fun updateText() {
            text.text = "(${anchorView.x.toInt()}, ${anchorView.y.toInt()})"
        }
        circle.mouse {
            onOver { circle.filter = ColorTransformFilter(ColorTransform(add = ColorAdd(+64, +64, +64, 0))) }
            onOut { circle.filter = ColorTransformFilter(ColorTransform(add = ColorAdd(0, 0, 0, 0))) }
        }
        updateText()
        anchorView.draggable(circle) {
            point.x = anchorView.x
            point.y = anchorView.y
            updateText()
            onMove()
        }
    }
}
