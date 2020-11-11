import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

suspend fun main() = Korge(bgcolor = Colors["#111"]) {
    val p0 = Point(109, 135)
    val p1 = Point(25, 190)
    val p2 = Point(210, 250)
    val p3 = Point(234, 49)

    val graphics = graphics {
        useNativeRendering = true
        //useNativeRendering = false
    }

    fun updateGraphics() {
        graphics.clear()
        graphics.stroke(Colors.DIMGREY, info = Context2d.StrokeInfo(thickness = 1.0)) {
            moveTo(p0)
            lineTo(p1)
            lineTo(p2)
            lineTo(p3)
        }
        graphics.stroke(Colors.RED, info = Context2d.StrokeInfo(thickness = 2.0)) {
            moveTo(p0)
            cubicTo(p1, p2, p3)
        }
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
        text = text("", 16.0).position(10.0, 6.0)
    }.position(point)

    fun updateText() {
        text.text = "(${anchorView.x.toInt()}, ${anchorView.y.toInt()})"
    }
    circle.mouse {
        onOver { circle.colorAdd = ColorAdd(+64, +64, +64, 0) }
        onOut { circle.colorAdd = ColorAdd(0, 0, 0, 0) }
    }
    updateText()
    anchorView.draggable(circle) {
        point.x = anchorView.x
        point.y = anchorView.y
        updateText()
        onMove()
    }
}
