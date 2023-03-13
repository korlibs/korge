package samples

import com.soywiz.korev.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*

class MainArc : Scene() {
    override suspend fun SContainer.sceneMain() {
        //graphics(renderer = GraphicsRenderer.GPU) { shape ->
        graphics(renderer = GraphicsRenderer.SYSTEM) { shape ->
            val p1 = Point(200, 100)
            val p2 = Point(300, 200)
            val radius = 100f

            stroke(Colors.BLUE, StrokeInfo(thickness = 10.0)) {
                //fill(Colors.BLUE) {
                circle(Arc.findArcCenter(p1, p2, radius), radius)
            }
            stroke(Colors.RED, StrokeInfo(thickness = 5.0)) {
                curves(Arc.createArc(p1, p2, radius))
            }
            stroke(Colors.PURPLE, StrokeInfo(thickness = 5.0)) {
                curves(Arc.createArc(p1, p2, radius, counterclockwise = true))
            }
            fill(Colors.WHITE) {
                circle(p1, 10f)
                circle(p2, 10f)
                circle(Arc.findArcCenter(p1, p2, radius), 10f)
            }
            shape.keys {
                down(Key.N9) { shape.antialiased = !shape.antialiased }
                down(Key.N0) { shape.debugDrawOnlyAntialiasedBorder = !shape.debugDrawOnlyAntialiasedBorder }
            }
        }
    }
}
